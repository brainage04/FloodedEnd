package io.github.brainage04.floodedend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Minecraft decides the End's waterline from the {@code minecraft:end} noise settings, and those are read from data
 * packs when a world is loaded. A higher or lower ocean therefore cannot be a runtime setting: it is a data pack the
 * world carries.
 */
public record EndOceanPreset(String id, int seaLevel, String description) {
	public static final EndOceanPreset MOORED = new EndOceanPreset(
			"moored",
			58,
			"the End sea at the main island's rim (y=57). The island, the exit portal and the obsidian pillars stay "
					+ "dry; the arrival platform is under water and every End ship is moored at the waterline."
	);
	public static final EndOceanPreset DROWNED = new EndOceanPreset(
			"drowned",
			72,
			"the End sea eight blocks above the main island's highest ground (y=63). The island, the arrival "
					+ "platform and every End city base are under water; the exit portal is rebuilt on the water surface "
					+ "(y=71) with the dragon egg above it, and the obsidian pillars, their crystals and the End ships "
					+ "stay above the surface."
	);
	public static final List<EndOceanPreset> PRESETS = List.of(MOORED, DROWNED);

	public static final String DIRECTORY = "floodedend-preset";
	private static final String OCEAN_RESOURCE = "/data/minecraft/worldgen/noise_settings/end.json";
	private static final String OCEAN_PATH = "data/minecraft/worldgen/noise_settings/end.json";

	public static Optional<EndOceanPreset> byId(String id) {
		return PRESETS.stream().filter(preset -> preset.id().equals(id)).findFirst();
	}

	public static Path directory(MinecraftServer server) {
		return server.getWorldPath(LevelResource.DATAPACK_DIR).resolve(DIRECTORY);
	}

	public static boolean installed(MinecraftServer server) {
		return Files.isRegularFile(directory(server).resolve(OCEAN_PATH));
	}

	public void install(MinecraftServer server) throws IOException {
		install(directory(server));
	}

	private void install(Path root) throws IOException {
		remove(root);
		Files.createDirectories(root.resolve(OCEAN_PATH).getParent());
		Files.writeString(root.resolve("pack.mcmeta"), packMeta(), StandardCharsets.UTF_8);
		Files.writeString(root.resolve(OCEAN_PATH), ocean(), StandardCharsets.UTF_8);
	}

	public static boolean remove(MinecraftServer server) throws IOException {
		return remove(directory(server));
	}

	private static boolean remove(Path root) throws IOException {
		if (!Files.exists(root)) {
			return false;
		}

		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
		return true;
	}

	/**
	 * Rewrites a preset that an older Minecraft version wrote into a world, keeping its sea level.
	 *
	 * <p>The preset's {@code end.json} is in the noise settings format of the version that wrote it, and a data pack
	 * whose world generation does not decode stops the world from loading ("Failed to load datapacks, can't proceed
	 * with server load"). A pack written for an older data pack format is therefore regenerated from this version's
	 * shipped ocean before the world's data packs are read.</p>
	 *
	 * @param datapacks the world's {@code datapacks} directory
	 * @return the preset now installed, if the pack was stale and has been rewritten
	 */
	public static Optional<EndOceanPreset> upgradeStale(Path datapacks) throws IOException {
		Path root = datapacks.resolve(DIRECTORY);
		Path meta = root.resolve("pack.mcmeta");
		Path ocean = root.resolve(OCEAN_PATH);
		if (!Files.isRegularFile(meta) || !Files.isRegularFile(ocean)) {
			return Optional.empty();
		}

		JsonObject pack = JsonParser.parseString(Files.readString(meta, StandardCharsets.UTF_8)).getAsJsonObject()
				.getAsJsonObject("pack");
		int written = major(pack.has("max_format") ? pack.get("max_format") : pack.get("pack_format"));
		if (written >= currentPackFormat()) {
			return Optional.empty();
		}

		int seaLevel = JsonParser.parseString(Files.readString(ocean, StandardCharsets.UTF_8)).getAsJsonObject()
				.get("sea_level").getAsInt();
		EndOceanPreset preset = PRESETS.stream().filter(candidate -> candidate.seaLevel() == seaLevel).findFirst()
				.orElseGet(() -> new EndOceanPreset("custom", seaLevel, "a sea level kept from an older preset"));
		preset.install(root);
		return Optional.of(preset);
	}

	/** {@link #upgradeStale} for world loading: reports the result and never stops the world from opening. */
	public static void upgradeStaleOnLoad(Path datapacks) {
		try {
			upgradeStale(datapacks).ifPresent(preset -> FloodedEnd.LOGGER.info(
					"FloodedEnd: rewrote the {} preset (End sea level {}) in {} for this Minecraft version",
					preset.id(), preset.seaLevel(), datapacks.resolve(DIRECTORY)));
		} catch (IOException | RuntimeException error) {
			FloodedEnd.LOGGER.error("FloodedEnd: could not check the preset in {}; if the world does not load, "
					+ "delete that folder and apply the preset again", datapacks.resolve(DIRECTORY), error);
		}
	}

	/** A format is a major number or, in newer packs, a {@code [major, minor]} pair. */
	private static int major(JsonElement format) {
		return format.isJsonArray() ? format.getAsJsonArray().get(0).getAsInt() : format.getAsInt();
	}

	private static int currentPackFormat() {
		return SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
	}

	/**
	 * The preset's ocean settings: the mod's own {@code minecraft:end} noise settings with this sea level written
	 * into it, so the shipped world generation data stays the single source of truth.
	 */
	private String ocean() throws IOException {
		try (InputStream stream = EndOceanPreset.class.getResourceAsStream(OCEAN_RESOURCE)) {
			if (stream == null) {
				throw new IOException("FloodedEnd is missing its own " + OCEAN_RESOURCE);
			}

			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				JsonObject settings = JsonParser.parseReader(reader).getAsJsonObject();
				settings.addProperty("sea_level", seaLevel);
				return new GsonBuilder().setPrettyPrinting().create().toJson(settings) + "\n";
			}
		}
	}

	private String packMeta() {
		int packFormat = currentPackFormat();
		return "{\n"
				+ "  \"pack\": {\n"
				+ "    \"pack_format\": " + packFormat + ",\n"
				+ "    \"min_format\": " + packFormat + ",\n"
				+ "    \"max_format\": " + packFormat + ",\n"
				+ "    \"description\": \"FloodedEnd preset: " + id + " (End sea level " + seaLevel + ")\"\n"
				+ "  }\n"
				+ "}\n";
	}
}
