package io.github.brainage04.floodedend;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.levelgen.structure.Structure;

public final class EndIslandRegionAssertions {
    private EndIslandRegionAssertions() {}

    /**
     * Vanilla's in_square placement keeps an island's origin in the decorated chunk, but a datapack
     * that offsets it into the neighbouring chunk moves the structure scan past the chunks the
     * generation region serves, and the region throws for those instead of returning null.
     */
    public static void offsetIslandSkipsChunksTheRegionCannotServe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenRegion region = fixtureRegion(level);

        // The origin of an island placed one chunk east of the decorated one.
        BlockPos origin = offsetOrigin(level);
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

    /**
     * With the origin one chunk off the region's centre, a start eight chunks from the island's
     * chunk can be nine from the centre, where the region cannot serve it, so the scan skips it.
     * A chunk in the island's reach that references such a start must be left alone like one that
     * references a start beyond the scan's distance.
     */
    // deprecated-api: hasChunk is the only way to ask a generation region which chunks it serves.
    @SuppressWarnings("deprecation")
    public static void offsetIslandKeepsOutOfChunkWhoseStartTheRegionCannotServe(
            GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldGenRegion region = fixtureRegion(level);
        BlockPos origin = offsetOrigin(level);
        ChunkPos islandChunk = ChunkPos.containing(origin);
        ChunkPos start =
                new ChunkPos(islandChunk.x() + ChunkStatus.MAX_STRUCTURE_DISTANCE, islandChunk.z());
        helper.assertFalse(
                region.hasChunk(start.x(), start.z()),
                "the start lies outside what the region serves");
        Structure city =
                level.registryAccess()
                        .lookupOrThrow(Registries.STRUCTURE)
                        .getOrThrow(
                                ResourceKey.create(
                                        Registries.STRUCTURE,
                                        Identifier.withDefaultNamespace("end_city")))
                        .value();
        // What the level's own generation writes for a start whose box reaches the chunk.
        region.getChunk(islandChunk.x(), islandChunk.z())
                .addReferenceForStructure(city, start.pack());

        EndIslandStructures.KeepOut keepOut = EndIslandStructures.keepOut(region, origin);

        helper.assertTrue(
                keepOut.beyondTheScan().contains(islandChunk),
                "the island's chunk references a start the region could not serve, so the island"
                        + " must leave it alone, but the chunks left alone were "
                        + keepOut.beyondTheScan());
        helper.succeed();
    }

    private static WorldGenRegion fixtureRegion(ServerLevel level) {
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
        return new WorldGenRegion(level, cache, step, centerChunk);
    }

    /** An origin one chunk east of the region's centre chunk (0, 0). */
    private static BlockPos offsetOrigin(ServerLevel level) {
        return new BlockPos(24, level.getMinY() + 64, 8);
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
