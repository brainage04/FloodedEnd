package io.github.brainage04.floodedend;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Loader-independent behavioral assertions for FloodedEnd's server suites. */
public final class FloodedEndAssertions {
	private FloodedEndAssertions() {
	}

	/** The End's shipped world generation must resolve to water below a positive sea level. */
	public static void assertEndIsFlooded(GameTestHelper helper) {
		NoiseGeneratorSettings end = helper.getLevel()
				.registryAccess()
				.lookupOrThrow(Registries.NOISE_SETTINGS)
				.getOrThrow(NoiseGeneratorSettings.END)
				.value();

		helper.assertTrue(
				end.defaultFluid().is(Blocks.WATER),
				"minecraft:end must flood its basins with water but generates " + end.defaultFluid()
		);
		helper.assertTrue(
				end.seaLevel() > 0,
				"minecraft:end must have a sea level above the dimension floor but generates at " + end.seaLevel()
		);
		helper.succeed();
	}

	/**
	 * A preset must install the world's End ocean as a data pack the game itself can read back at the requested
	 * waterline, and clearing it must leave the world on the shipped ocean again.
	 */
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
				helper.assertTrue(EndOceanPreset.installed(server), "the preset writes real world data");
				Path ocean = EndOceanPreset.directory(server).resolve("data/minecraft/worldgen/noise_settings/end.json");
				NoiseGeneratorSettings settings = NoiseGeneratorSettings.CODEC
						.parse(ops, JsonParser.parseString(Files.readString(ocean, StandardCharsets.UTF_8)))
						.getOrThrow(error -> new AssertionError("the installed ocean must decode: " + error)).value();
				helper.assertValueEqual(settings.seaLevel(), preset.seaLevel(), "the installed preset's sea level");
				helper.assertTrue(settings.defaultFluid().is(Blocks.WATER), "a preset still floods the End with water");
			}
			helper.assertTrue(feedback.contains("Restart the server"), "apply explains when world generation changes");
			helper.assertValueEqual(FloodedEndBehaviorAssertions.command(source, "preset clear"), 1, "clear removes an installed preset");
			helper.assertTrue(!Files.exists(EndOceanPreset.directory(server)), "clear removes the whole data pack");
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
}
