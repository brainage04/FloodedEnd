package io.github.brainage04.floodedend;

import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Loader-independent assertions for the contracts the mod ships: a flooded End, its End cities, and its readback command. */
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

	/**
	 * The flooded End's own End city must be the structure that generates: it is what keeps city placement off the
	 * water heightmap and what moors the ships, so a broken data pack would silently hand both back to vanilla.
	 */
	public static void assertEndCityIsFloodedEnds(GameTestHelper helper) {
		Structure endCity = helper.getLevel()
				.registryAccess()
				.lookupOrThrow(Registries.STRUCTURE)
				.get(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("end_city")))
				.map(Holder.Reference::value)
				.orElseThrow(() -> new AssertionError("minecraft:end_city must exist in the structure registry"));

		helper.assertTrue(
				endCity instanceof EndCityAtSea,
				"minecraft:end_city must generate through " + EndCityAtSea.class.getSimpleName()
						+ " but resolves to " + endCity.getClass().getName()
		);
		helper.assertTrue(
				endCity.type() == EndCityAtSea.TYPE,
				"minecraft:end_city must be registered as " + EndCityAtSea.NAME + " but reports " + endCity.type()
		);
		helper.succeed();
	}
}
