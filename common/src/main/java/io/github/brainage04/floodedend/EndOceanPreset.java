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
		remove(server);
		Path root = directory(server);
		Files.createDirectories(root.resolve(OCEAN_PATH).getParent());
		Files.writeString(root.resolve("pack.mcmeta"), packMeta(), StandardCharsets.UTF_8);
		Files.writeString(root.resolve(OCEAN_PATH), ocean(), StandardCharsets.UTF_8);
	}

	public static boolean remove(MinecraftServer server) throws IOException {
		Path root = directory(server);
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
				+ "    \"min_format\": " + packFormat + ",\n"
				+ "    \"max_format\": " + packFormat + ",\n"
				+ "    \"description\": \"FloodedEnd preset: " + id + " (End sea level " + seaLevel + ")\"\n"
				+ "  }\n"
				+ "}\n";
	}
}
