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
 * Every value is read back out of the live dimension's chunk generator rather than duplicated in
 * code, so a server that did not load the shipped world generation data is reported as dry instead
 * of silently assumed flooded.
 */
public record EndOcean(int seaLevel, BlockState fluid, boolean aquifersEnabled) {
    /** Water fills every y below the sea level, so the topmost water block sits one below it. */
    public static final int SURFACE_BELOW_SEA_LEVEL = 1;

    public static EndOcean of(ServerLevel level) {
        return of(level.getChunkSource().getGenerator());
    }

    /**
     * The ocean of a generator that builds its terrain from the {@code minecraft:end} noise
     * settings, the settings FloodedEnd floods. That is vanilla's own test for its End ({@code
     * WorldDimensions.isStableEnd}, {@code WorldDimensions.java:146-151} in 26.3), and it also
     * holds for a datapack dimension that reuses those settings. Any other generator has no End
     * ocean, even one that places End biomes over water, such as the Single Biome world type set to
     * an End biome, which generates from the Overworld's settings ({@code WorldPresets.java:150}).
     */
    public static EndOcean of(ChunkGenerator generator) {
        if (generatesFromEndSettings(generator)) {
            NoiseGeneratorSettings settings =
                    ((NoiseBasedChunkGenerator) generator).generatorSettings().value();
            return new EndOcean(
                    settings.seaLevel(), settings.defaultFluid(), settings.aquifers().isPresent());
        }

        return new EndOcean(0, Blocks.AIR.defaultBlockState(), false);
    }

    public static boolean generatesFromEndSettings(ChunkGenerator generator) {
        return generator instanceof NoiseBasedChunkGenerator noise
                && noise.stable(NoiseGeneratorSettings.END);
    }

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
        return "End sea level "
                + seaLevel
                + ", default fluid "
                + fluidName()
                + ", aquifers "
                + (aquifersEnabled ? "enabled" : "disabled")
                + ", water surface y="
                + waterSurfaceY();
    }

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

        return new Column(
                x,
                z,
                surfaceY,
                name(surface),
                waterSurfaceY,
                deepestWaterY,
                waterBlocks,
                landY,
                name(land));
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
            String land) {
        public boolean isFlooded() {
            return waterBlocks > 0;
        }

        public String describe() {
            String column =
                    "column (" + x + ", " + z + "): surface " + surface + " at y=" + surfaceY;
            if (!isFlooded()) {
                return column + "; no water";
            }

            return column
                    + "; water "
                    + waterBlocks
                    + " blocks from y="
                    + deepestWaterY
                    + " to y="
                    + waterSurfaceY
                    + "; highest non-water block "
                    + land
                    + " at y="
                    + landY;
        }
    }
}
