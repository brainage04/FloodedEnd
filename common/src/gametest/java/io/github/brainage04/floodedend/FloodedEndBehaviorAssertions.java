package io.github.brainage04.floodedend;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;

/** Exercises real command dispatch, generated blocks and structure templates on both loaders. */
public final class FloodedEndBehaviorAssertions {
	private FloodedEndBehaviorAssertions() {
	}

	public static void assertCommandsRespectPermissionsAndReportErrors(GameTestHelper helper) {
		Feedback feedback = new Feedback();
		CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack().withSource(feedback);
		try {
			command(source.withPermission(LevelBasedPermissionSet.ALL), "status");
			helper.fail("players without gamemaster permission must not execute FloodedEnd commands");
		} catch (CommandSyntaxException expected) {
			// Brigadier must reject the command, not merely hide its suggestions.
		}
		try {
			helper.assertValueEqual(command(source, "status"), 1, "the live End ocean is active");
			helper.assertTrue(feedback.contains("End sea level 58, default fluid minecraft:water"), "status reads the live generator");
			helper.assertValueEqual(command(source, "preset list"), 2, "both presets are listed");
			helper.assertTrue(feedback.contains("moored") && feedback.contains("drowned"), "list names both presets");
			helper.assertValueEqual(command(source, "preset apply unknown"), 0, "unknown presets fail");
			helper.assertTrue(feedback.contains("Unknown FloodedEnd preset 'unknown'"), "unknown preset feedback is useful");
			helper.assertValueEqual(command(source, "probe 29999984 29999984"), 0, "probing an unloaded chunk fails");
			helper.assertTrue(feedback.contains("No End chunk loaded"), "probe explains unloaded chunks");
		} catch (CommandSyntaxException error) {
			throw new AssertionError("the registered commands must execute", error);
		}
		helper.succeed();
	}

	public static void assertGeneratedOceanAndProbe(GameTestHelper helper) {
		ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
		helper.assertTrue(end != null, "the End is loaded");
		// Inside the central void ring, away from the island and the arrival platform.
		end.getChunk(32, 32);
		EndOcean.Column column = EndOcean.probe(end, 512, 512);
		helper.assertValueEqual(column.waterSurfaceY(), 57, "generated ocean surface");
		helper.assertValueEqual(column.deepestWaterY(), 0, "generated ocean floor");
		helper.assertValueEqual(column.waterBlocks(), 58, "the entire void column floods");
		helper.assertTrue(end.getBlockState(new BlockPos(512, 58, 512)).isAir(), "blocks above the sea remain air");
		Feedback feedback = new Feedback();
		CommandSourceStack source = end.getServer().createCommandSourceStack().withSource(feedback)
				.withPosition(new net.minecraft.world.phys.Vec3(512, 64, 512));
		try {
			helper.assertValueEqual(command(source, "probe"), 1, "probe uses the source's x/z in the End");
			helper.assertValueEqual(command(source, "probe 512 512"), 1, "explicit probe coordinates work");
			helper.assertTrue(feedback.contains("water 58 blocks from y=0 to y=57"), "probe describes real blocks");
		} catch (CommandSyntaxException error) {
			throw new AssertionError(error);
		}
		helper.succeed();
	}

	public static void assertProbeHandlesDryAndCappedColumns(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
		List<BlockState> original = new ArrayList<>();
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX(), 0, origin.getZ());
		try {
			for (int y = level.getMinY(); y < level.getMaxY(); y++) {
				cursor.setY(y);
				original.add(level.getBlockState(cursor));
				level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 2);
			}
			level.setBlock(origin, Blocks.END_STONE.defaultBlockState(), 2);
			EndOcean.Column dry = EndOcean.probe(level, origin.getX(), origin.getZ());
			helper.assertTrue(!dry.isFlooded() && dry.describe().endsWith("; no water"), "dry columns report no water");
			level.setBlock(origin.above(), Blocks.WATER.defaultBlockState(), 2);
			level.setBlock(origin.above(3), Blocks.WATER.defaultBlockState(), 2);
			level.setBlock(origin.above(4), Blocks.END_STONE.defaultBlockState(), 2);
			EndOcean.Column capped = EndOcean.probe(level, origin.getX(), origin.getZ());
			helper.assertValueEqual(capped.waterBlocks(), 2, "air gaps are not counted as water");
			helper.assertValueEqual(capped.deepestWaterY(), origin.getY() + 1, "lowest water block");
			helper.assertValueEqual(capped.waterSurfaceY(), origin.getY() + 3, "highest water block");
			helper.assertValueEqual(capped.landY(), origin.getY() + 4, "solid cap is the highest non-water block");
			helper.assertValueEqual(capped.surface(), "minecraft:end_stone", "solid cap is the surface");
		} finally {
			for (int i = 0; i < original.size(); i++) {
				level.setBlock(cursor.set(origin.getX(), level.getMinY() + i, origin.getZ()), original.get(i), 2);
			}
		}
		helper.succeed();
	}

	public static void assertShipsMoorAtBothWaterlines(GameTestHelper helper) {
		ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
		var generator = end.getChunkSource().getGenerator();
		Structure.GenerationContext context = new Structure.GenerationContext(
				end.registryAccess(), generator, generator.getBiomeSource(), end.getChunkSource().randomState(),
				end.getStructureManager(), end.getSeed(), new ChunkPos(32, 32), end, biome -> true);
		for (Rotation rotation : Rotation.values()) {
			for (EndOceanPreset preset : EndOceanPreset.PRESETS) {
				var ship = new EndCityPieces.EndCityPiece(end.getStructureManager(), "ship", new BlockPos(512, 120, 512), rotation, true);
				var tower = new EndCityPieces.EndCityPiece(end.getStructureManager(), "base_floor", new BlockPos(512, 64, 512), rotation, true);
				BlockPos towerPosition = tower.templatePosition();
				EndShipMooring.moor(context, List.of(ship, tower), new EndOcean(preset.seaLevel(), Blocks.WATER.defaultBlockState(), false));
				helper.assertValueEqual(ship.getBoundingBox().minY(), preset.seaLevel() - 4, "ship keel follows the preset waterline");
				helper.assertValueEqual(tower.templatePosition(), towerPosition, "mooring does not move city towers");
			}
		}
		// An out-of-reference ship must not be moved into a position that will be truncated on placement.
		var outside = new EndCityPieces.EndCityPiece(end.getStructureManager(), "ship", new BlockPos(1024, 120, 1024), Rotation.NONE, true);
		BlockPos position = outside.templatePosition();
		EndShipMooring.moor(context, List.<StructurePiece>of(outside), EndOcean.of(end));
		helper.assertValueEqual(outside.templatePosition(), position, "a ship with no placeable mooring stays at its original position");
		helper.succeed();
	}

	public static void assertCityGenerationPreservesDryVanilla(GameTestHelper helper) {
		ServerLevel end = helper.getLevel().getServer().getLevel(Level.END);
		var city = (EndCityAtSea) end.registryAccess().lookupOrThrow(Registries.STRUCTURE)
				.getOrThrow(ResourceKey.create(Registries.STRUCTURE, Identifier.withDefaultNamespace("end_city"))).value();
		var vanilla = new EndCityStructure(new Structure.StructureSettings(
				city.biomes(), city.spawnOverrides(), city.step(), city.terrainAdaptation()));
		var flooded = (NoiseBasedChunkGenerator) end.getChunkSource().getGenerator();
		var settings = flooded.generatorSettings().value();
		var drySettings = new NoiseGeneratorSettings(settings.noiseSettings(), settings.defaultBlock(), Blocks.AIR.defaultBlockState(),
				settings.noiseRouter(), settings.surfaceRule(), settings.spawnTarget(), 0, settings.disableMobGeneration(),
				settings.aquifersEnabled(), settings.oreVeinsEnabled(), settings.useLegacyRandomSource());
		var dry = new NoiseBasedChunkGenerator(flooded.getBiomeSource(), Holder.direct(drySettings));
		int ships = 0;
		for (int seed = 0; seed < 16; seed++) {
			var expected = vanilla.findGenerationPoint(cityContext(end, dry, seed)).orElseThrow()
					.getPiecesBuilder().build().pieces();
			var actual = city.findGenerationPoint(cityContext(end, dry, seed)).orElseThrow()
					.getPiecesBuilder().build().pieces();
			helper.assertValueEqual(actual.size(), expected.size(), "dry End has vanilla's piece count");
			for (int i = 0; i < expected.size(); i++) {
				helper.assertValueEqual(actual.get(i).getBoundingBox().toString(), expected.get(i).getBoundingBox().toString(),
						"dry End keeps every vanilla piece in place");
			}
			var wet = city.findGenerationPoint(cityContext(end, flooded, seed)).orElseThrow()
					.getPiecesBuilder().build().pieces();
			helper.assertValueEqual(wet.size(), expected.size(), "flooding preserves the city's pieces");
			for (int i = 0; i < wet.size(); i++) {
				var piece = (net.minecraft.world.level.levelgen.structure.TemplateStructurePiece) wet.get(i);
				if (piece.template() == end.getStructureManager().getOrCreate(Identifier.withDefaultNamespace("end_city/ship"))) {
					ships++;
				} else {
					helper.assertValueEqual(piece.getBoundingBox().toString(), expected.get(i).getBoundingBox().toString(),
							"flooding does not move city buildings");
				}
			}
		}
		helper.assertTrue(ships > 0, "the deterministic city samples include ships");
		helper.succeed();
	}

	private static Structure.GenerationContext cityContext(ServerLevel end, ChunkGenerator generator, long seed) {
		return new Structure.GenerationContext(end.registryAccess(), generator, generator.getBiomeSource(),
				end.getChunkSource().randomState(), end.getStructureManager(), seed, new ChunkPos(0, 0), end, biome -> true);
	}


	static int command(CommandSourceStack source, String arguments) throws CommandSyntaxException {
		return source.getServer().getCommands().getDispatcher().execute("floodedend " + arguments, source);
	}

	static final class Feedback implements CommandSource {
		private final List<String> messages = new ArrayList<>();

		@Override
		public void sendSystemMessage(Component message) {
			messages.add(message.getString());
		}
		boolean contains(String text) {
			return messages.stream().anyMatch(message -> message.contains(text));
		}

		@Override
		public boolean acceptsSuccess() {
			return true;
		}

		@Override
		public boolean acceptsFailure() {
			return true;
		}

		@Override
		public boolean shouldInformAdmins() {
			return false;
		}
	}
}
