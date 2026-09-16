package io.github.brainage04.floodedend;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * The End's ocean as the running server actually generates it.
 *
 * <p>Every value is read back out of the live dimension's chunk generator rather than duplicated in code, so the
 * shipped world generation data stays the single source of truth and a server that did not load it is reported as
 * dry instead of silently assumed flooded.</p>
 */
public record EndOcean(int seaLevel, BlockState fluid, boolean aquifersEnabled) {
	/** Water fills every y below the sea level, so the topmost water block sits one below it. */
	public static final int SURFACE_BELOW_SEA_LEVEL = 1;

	public static EndOcean of(ServerLevel level) {
		return of(level.getChunkSource().getGenerator());
	}

	/**
	 * The ocean a chunk generator was built with. World generation asks this before any level exists, which is how
	 * the structure that moors the End ships learns the waterline of the world it is generating into.
	 */
	public static EndOcean of(ChunkGenerator generator) {
		if (generator instanceof NoiseBasedChunkGenerator noise) {
			NoiseGeneratorSettings settings = noise.generatorSettings().value();
			return new EndOcean(settings.seaLevel(), settings.defaultFluid(), settings.isAquifersEnabled());
		}

		return new EndOcean(0, Blocks.AIR.defaultBlockState(), false);
	}

	/** True when the End generates its ocean: a positive sea level flooded with water. */
	public boolean flooded() {
		return seaLevel > 0 && fluid.is(Blocks.WATER);
	}

	public int waterSurfaceY() {
		return seaLevel - SURFACE_BELOW_SEA_LEVEL;
	}

	public String fluidName() {
		return BuiltInRegistries.BLOCK.getKey(fluid.getBlock()).toString();
	}

	public String describe() {
		return "End sea level " + seaLevel
				+ ", default fluid " + fluidName()
				+ ", aquifers " + (aquifersEnabled ? "enabled" : "disabled")
				+ ", water surface y=" + waterSurfaceY();
	}

	/** Reads one vertical column of the flooded End back out of the live level. */
	public static Column probe(ServerLevel level, int x, int z) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, 0, z);
		int surfaceY = -1;
		int waterSurfaceY = -1;
		int deepestWaterY = -1;
		int waterBlocks = 0;
		int landY = -1;
		BlockState surface = Blocks.AIR.defaultBlockState();
		BlockState land = Blocks.AIR.defaultBlockState();

		for (int y = level.getMinY(); y < level.getMaxY(); y++) {
			BlockState state = level.getBlockState(cursor.set(x, y, z));
			if (state.isAir()) {
				continue;
			}

			surfaceY = y;
			surface = state;

			if (state.is(Blocks.WATER)) {
				waterBlocks++;
				deepestWaterY = deepestWaterY == -1 ? y : Math.min(deepestWaterY, y);
				waterSurfaceY = y;
			} else {
				landY = y;
				land = state;
			}
		}

		return new Column(x, z, surfaceY, name(surface), waterSurfaceY, deepestWaterY, waterBlocks, landY, name(land));
	}

	private static String name(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	public record Column(
			int x,
			int z,
			int surfaceY,
			String surface,
			int waterSurfaceY,
			int deepestWaterY,
			int waterBlocks,
			int landY,
			String land
	) {
		public boolean isFlooded() {
			return waterBlocks > 0;
		}

		public String describe() {
			String column = "column (" + x + ", " + z + "): surface " + surface + " at y=" + surfaceY;
			if (!isFlooded()) {
				return column + "; no water";
			}

			return column + "; water " + waterBlocks + " blocks from y=" + deepestWaterY + " to y=" + waterSurfaceY
					+ "; highest non-water block " + land + " at y=" + landY;
		}
	}
}
