package io.github.brainage04.floodedend;

import io.github.brainage04.floodedend.command.FloodedEndCommand;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Loader-independent assertions for the contracts the mod ships: a flooded End, its End cities and their moored
 * ships, its readback command, and the sea-level presets.
 */
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

	/**
	 * The mooring code finds the End ship by template id alone, so a template that does not resolve would leave
	 * every ship in the sky without an error anywhere.
	 */
	public static void assertEndShipTemplateResolves(GameTestHelper helper) {
		StructureTemplate ship = helper.getLevel()
				.getServer()
				.getStructureManager()
				.getOrCreate(Identifier.withDefaultNamespace("end_city/ship"));
		Vec3i size = ship.getSize();
		helper.assertTrue(
				size.getX() > 0 && size.getY() > 0 && size.getZ() > 0,
				"the End ship template minecraft:end_city/ship must resolve to a real template but is " + size
		);
		helper.succeed();
	}

	/**
	 * A preset must install the world's End ocean as a data pack the game itself can read back at the requested
	 * waterline, and clearing it must leave the world on the shipped ocean again.
	 */
	public static void assertPresetInstallsAnOcean(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		EndOceanPreset preset = EndOceanPreset.DROWNED;
		try {
			preset.install(server);
			helper.assertTrue(
					EndOceanPreset.installed(server),
					"installing " + preset.id() + " must leave " + EndOceanPreset.DIRECTORY + " in the world's data packs"
			);

			Path ocean = EndOceanPreset.directory(server).resolve("data/minecraft/worldgen/noise_settings/end.json");
			RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
			NoiseGeneratorSettings settings = NoiseGeneratorSettings.CODEC
					.parse(ops, JsonParser.parseString(Files.readString(ocean, StandardCharsets.UTF_8)))
					.getOrThrow(error -> new AssertionError("the installed End ocean must be valid noise settings: " + error))
					.value();
			helper.assertTrue(
					settings.seaLevel() == preset.seaLevel(),
					"the installed ocean must have " + preset.id() + "'s sea level " + preset.seaLevel()
							+ " but has " + settings.seaLevel()
			);
			helper.assertTrue(
					settings.defaultFluid().is(Blocks.WATER),
					"a preset must still flood the End with water but writes " + settings.defaultFluid()
			);

			EndOceanPreset.remove(server);
			helper.assertTrue(
					!EndOceanPreset.installed(server),
					"clearing the preset must remove it from the world's data packs"
			);
		} catch (IOException error) {
			throw new AssertionError("the preset could not be installed or cleared", error);
		}

		helper.succeed();
	}
}
