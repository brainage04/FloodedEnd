package io.github.brainage04.floodedend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public final class FloodedEndAssertions {
	private FloodedEndAssertions() {
	}

	public static void assertPresetInstallsAnOcean(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		var feedback = new FloodedEndBehaviorAssertions.Feedback();
		var source = server.createCommandSourceStack().withSource(feedback);
		try {
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
			for (EndOceanPreset preset : EndOceanPreset.PRESETS) {
				helper.assertValueEqual(
						FloodedEndBehaviorAssertions.command(source, "preset apply " + preset.id()), 1,
						"the preset command installs " + preset.id());
				var location = new PackLocationInfo("floodedend-test", Component.literal("FloodedEnd test"), PackSource.WORLD, Optional.empty());
				try (var pack = new PathPackResources(location, EndOceanPreset.directory(server))) {
					helper.assertTrue(pack.getMetadataSection(PackMetadataSection.SERVER_TYPE) != null,
							"Minecraft must decode the installed pack metadata without missing format fields");
				}
				helper.assertTrue(EndOceanPreset.installed(server), "the preset writes real world data");
				Path ocean = EndOceanPreset.directory(server).resolve("data/minecraft/worldgen/noise_settings/end.json");
				NoiseGeneratorSettings settings = NoiseGeneratorSettings.CODEC
						.parse(ops, JsonParser.parseString(Files.readString(ocean, StandardCharsets.UTF_8)))
						.getOrThrow(error -> new AssertionError("the installed ocean must decode: " + error)).value();
				helper.assertValueEqual(settings.seaLevel(), preset.seaLevel(), "the installed preset's sea level");
				helper.assertTrue(settings.defaultFluid().is(Blocks.WATER), "a preset still floods the End with water");
			}
			helper.assertValueEqual(FloodedEndBehaviorAssertions.command(source, "preset list"), 2, "list works before restart");
			helper.assertTrue(feedback.contains("this world's preset data pack is installed"), "list distinguishes files on disk from live generation");
			helper.assertTrue(feedback.contains("Restart the server"), "apply explains when world generation changes");
			helper.assertValueEqual(FloodedEndBehaviorAssertions.command(source, "preset clear"), 1, "clear removes an installed preset");
			helper.assertTrue(!Files.exists(EndOceanPreset.directory(server)), "clear removes the whole data pack");
			helper.assertValueEqual(FloodedEndBehaviorAssertions.command(source, "preset clear"), 0, "clear fails when no preset exists");
			helper.assertTrue(feedback.contains("No FloodedEnd preset is installed."), "an absent preset gets honest feedback");
		} catch (IOException | CommandSyntaxException error) {
			throw new AssertionError("the preset could not be installed or cleared", error);
		} finally {
			try {
				EndOceanPreset.remove(server);
			} catch (IOException error) {
				throw new AssertionError("could not clean up the test preset", error);
			}
		}
		helper.succeed();
	}

	/**
	 * A preset applied under Minecraft 26.2 holds 26.2's noise settings, which 26.3 cannot decode; a world carrying it
	 * fails to load ("Failed to load datapacks, can't proceed with server load"). The mod rewrites such a pack before
	 * the world's data packs are read and keeps its sea level. Both metadata shapes 26.2 builds wrote are covered:
	 * {@code pack_format} only (FloodedEnd 1.0.0 as released) and with {@code min_format}/{@code max_format}.
	 */
	public static void assertStalePresetIsUpgradedBeforeLoading(GameTestHelper helper) {
		RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
		Path datapacks = null;
		try {
			String stale;
			try (var stream = FloodedEndAssertions.class.getResourceAsStream("/floodedend-gametest/end-26.2-drowned.json")) {
				helper.assertTrue(stream != null, "the 26.2 preset fixture is on the test classpath");
				stale = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
			}
			helper.assertTrue(NoiseGeneratorSettings.CODEC.parse(ops, JsonParser.parseString(stale)).isError(),
					"26.2's noise settings no longer decode, so an unconverted preset would stop the world loading");

			datapacks = Files.createTempDirectory("floodedend-stale-preset");
			Path root = datapacks.resolve(EndOceanPreset.DIRECTORY);
			Path ocean = root.resolve("data/minecraft/worldgen/noise_settings/end.json");
			for (String formats : List.of("\"pack_format\": 107", "\"pack_format\": 107, \"min_format\": 107, \"max_format\": 107")) {
				Files.createDirectories(ocean.getParent());
				Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\": {" + formats
						+ ", \"description\": \"FloodedEnd preset: drowned (End sea level 72)\"}}", StandardCharsets.UTF_8);
				Files.writeString(ocean, stale, StandardCharsets.UTF_8);

				helper.assertValueEqual(EndOceanPreset.upgradeStale(datapacks), Optional.of(EndOceanPreset.DROWNED),
						"a stale preset is rewritten as the preset with its sea level");
				var location = new PackLocationInfo("floodedend-test", Component.literal("FloodedEnd test"), PackSource.WORLD, Optional.empty());
				try (var pack = new PathPackResources(location, root)) {
					helper.assertTrue(pack.getMetadataSection(PackMetadataSection.SERVER_TYPE) != null,
							"the rewritten pack metadata decodes");
				}
				NoiseGeneratorSettings settings = NoiseGeneratorSettings.CODEC
						.parse(ops, JsonParser.parseString(Files.readString(ocean, StandardCharsets.UTF_8)))
						.getOrThrow(error -> new AssertionError("the rewritten ocean must decode: " + error)).value();
				helper.assertValueEqual(settings.seaLevel(), 72, "the rewritten preset keeps its sea level");
				helper.assertTrue(settings.defaultFluid().is(Blocks.WATER), "the rewritten preset still floods the End");
				helper.assertValueEqual(EndOceanPreset.upgradeStale(datapacks), Optional.empty(),
						"a preset written by this version is left alone");
			}
			helper.assertValueEqual(EndOceanPreset.upgradeStale(datapacks.resolve("missing")), Optional.empty(),
					"a world without a preset is left alone");
		} catch (IOException error) {
			throw new AssertionError("the stale preset could not be set up or upgraded", error);
		} finally {
			if (datapacks != null) {
				try (var paths = Files.walk(datapacks)) {
					for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
						Files.delete(path);
					}
				} catch (IOException error) {
					throw new AssertionError("could not clean up the stale preset", error);
				}
			}
		}
		helper.succeed();
	}
}
