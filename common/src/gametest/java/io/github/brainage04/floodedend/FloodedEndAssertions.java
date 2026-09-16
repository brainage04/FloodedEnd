package io.github.brainage04.floodedend;

import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/** Loader-independent assertions for the two contracts the mod ships: a flooded End, and its readback command. */
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

	public static void assertCommandIsRegistered(GameTestHelper helper) {
		helper.assertTrue(
				helper.getLevel().getServer().getCommands().getDispatcher().getRoot()
						.getChild(FloodedEndCommand.COMMAND_NAME) != null,
				"/" + FloodedEndCommand.COMMAND_NAME + " must be registered on the dedicated server"
		);
		helper.succeed();
	}
}
