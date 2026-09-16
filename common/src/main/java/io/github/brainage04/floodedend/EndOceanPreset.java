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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.storage.LevelResource;

/**
 * A sea level the mod can generate the End with, installed into a world as a data pack.
 *
 * <p>Minecraft decides the End's waterline from the {@code minecraft:end} noise settings, and those are read from
 * data packs when a world is loaded. A higher or lower ocean therefore cannot be a runtime setting: it is a data
 * pack the world carries. This record is the mod's own way of producing that data pack, so one mod ships both the
 * shipped ocean and a drowned End without anyone hand-editing JSON.</p>
 *
 * <p>{@code minecraft:end_city} reads the live sea level back out of the chunk generator, so a world using a
 * preset moors its End ships at that preset's waterline.</p>
 */
public record EndOceanPreset(String id, int seaLevel, String description) {
	/** The shipped ocean: the waterline sits on the main island's rim, which keeps the island and the exit portal dry. */
	public static final EndOceanPreset MOORED = new EndOceanPreset(
			"moored",
			58,
			"the End sea at the main island's rim (y=57). The island, the exit portal and the obsidian pillars stay "
					+ "dry; the arrival platform is under water and every End ship is moored at the waterline."
	);
	/** The drowned End: the waterline is above the main island, so the whole arena is under water. */
	public static final EndOceanPreset DROWNED = new EndOceanPreset(
			"drowned",
			72,
			"the End sea eight blocks above the main island's highest ground (y=63). The island, the arrival "
					+ "platform and every End city base are under water; the exit portal is rebuilt on the water surface "
					+ "(y=71) with the dragon egg above it, and the obsidian pillars, their crystals and the End ships "
					+ "stay above the surface."
	);
	public static final List<EndOceanPreset> PRESETS = List.of(MOORED, DROWNED);

	/** The directory the mod writes into the world's {@code datapacks} folder. */
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

	/**
	 * Writes this preset into the world as a data pack, replacing any preset already installed there. The new sea
	 * level applies to chunks generated after the next server start.
	 */
	public void install(MinecraftServer server) throws IOException {
		remove(server);
		Path root = directory(server);
		Files.createDirectories(root.resolve(OCEAN_PATH).getParent());
		Files.writeString(root.resolve("pack.mcmeta"), packMeta(), StandardCharsets.UTF_8);
		Files.writeString(root.resolve(OCEAN_PATH), ocean(), StandardCharsets.UTF_8);
	}

	/** Removes any installed preset, leaving the world on the sea level the mod ships. */
	public static void remove(MinecraftServer server) throws IOException {
		Path root = directory(server);
		if (!Files.exists(root)) {
			return;
		}

		try (Stream<Path> paths = Files.walk(root)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
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
		int packFormat = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();
		return "{\n"
				+ "  \"pack\": {\n"
				+ "    \"pack_format\": " + packFormat + ",\n"
				+ "    \"description\": \"FloodedEnd preset: " + id + " (End sea level " + seaLevel + ")\"\n"
				+ "  }\n"
				+ "}\n";
	}
}
