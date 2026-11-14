package io.github.brainage04.floodedend;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;

public final class EndIslandRegionAssertions {
    private EndIslandRegionAssertions() {}

    /**
     * Vanilla's in_square placement keeps an island's origin in the decorated chunk, but a datapack
     * that offsets it into the neighbouring chunk moves the structure scan past the chunks the
     * generation region serves, and the region throws for those instead of returning null.
     */
    public static void offsetIslandSkipsChunksTheRegionCannotServe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Biome> biome =
                level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.THE_END);
        ChunkPos center = new ChunkPos(0, 0);
        ChunkAccess centerChunk = new EmptyLevelChunk(level, center, biome);
        ChunkStep step = ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);
        StaticCache2D<GenerationChunkHolder> cache =
                StaticCache2D.create(
                        center.x(),
                        center.z(),
                        ChunkStatus.MAX_STRUCTURE_DISTANCE,
                        (x, z) ->
                                new FixtureHolder(
                                        new ChunkPos(x, z),
                                        new EmptyLevelChunk(level, new ChunkPos(x, z), biome)));
        WorldGenRegion region = new WorldGenRegion(level, cache, step, centerChunk);

        // The origin of an island placed one chunk east of the decorated one.
        BlockPos origin = new BlockPos(center.getMinBlockX() + 24, level.getMinY() + 64, 8);
        EndIslandStructures.KeepOut keepOut;
        try {
            keepOut = EndIslandStructures.keepOut(region, origin);
        } catch (RuntimeException | Error failure) {
            throw new AssertionError(
                    "An island origin one chunk off the region's centre must not ask the region"
                            + " for a chunk it cannot serve",
                    failure);
        }

        helper.assertTrue(
                keepOut.boxes().isEmpty(),
                "The fixture's chunks hold no structure starts, so the scan must find none");
        helper.assertTrue(
                keepOut.beyondTheScan().isEmpty(),
                "The fixture's chunks hold no structure references, so none is left out");
        helper.succeed();
    }

    private static final class FixtureHolder extends GenerationChunkHolder {
        private final ChunkAccess chunk;

        private FixtureHolder(ChunkPos pos, ChunkAccess chunk) {
            super(pos);
            this.chunk = chunk;
        }

        @Override
        public ChunkAccess getChunkIfPresentUnchecked(ChunkStatus status) {
            return chunk;
        }

        @Override
        protected void addSaveDependency(CompletableFuture<?> sync) {
            // The fixture is never generated or saved.
        }

        @Override
        public int getTicketLevel() {
            return 33;
        }

        @Override
        public int getQueueLevel() {
            return 0;
        }
    }
}
