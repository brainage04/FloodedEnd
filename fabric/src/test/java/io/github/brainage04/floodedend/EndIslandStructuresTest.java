package io.github.brainage04.floodedend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** The island's keep-out area against vanilla's island and its FEATURES generation step. */
class EndIslandStructuresTest {
    static {
        WorldgenFixtures.bootstrap();
    }

    private static final ChunkStep FEATURES =
            ChunkPyramid.GENERATION_PYRAMID.getStepTo(ChunkStatus.FEATURES);

    /**
     * Vanilla's island writes only into its origin's chunk and the eight around it, the chunks
     * {@code EndIslandStructures} checks for references to starts its scan could not read.
     */
    @Property(tries = 2000)
    void vanillasIslandWritesOnlyIntoTheChunksAroundItsOwn(
            @ForAll long seed,
            @ForAll @IntRange(min = -30_000_000, max = 30_000_000) int x,
            @ForAll @IntRange(min = 0, max = 255) int y,
            @ForAll @IntRange(min = -30_000_000, max = 30_000_000) int z) {
        BlockPos origin = new BlockPos(x, y, z);
        List<BlockPos> written = new ArrayList<>();
        new EndIslandFeature().place(recorder(written), null, RandomSource.create(seed), origin);
        assertFalse(written.isEmpty());
        ChunkPos own = ChunkPos.containing(origin);
        for (BlockPos pos : written) {
            int distance =
                    own.getChessboardDistance(
                            SectionPos.blockToSectionCoord(pos.getX()),
                            SectionPos.blockToSectionCoord(pos.getZ()));
            assertTrue(distance <= 1, "the island at " + origin + " wrote " + pos);
            assertTrue(distance <= FEATURES.blockStateWriteRadius());
        }
    }

    /**
     * For an island in the chunk the FEATURES step decorates, every chunk the structure scan reads
     * lies within what the step's region serves at STRUCTURE_STARTS ({@code
     * WorldGenRegion.java:131-158}), so vanilla's own placement skips none. One chunk further, as
     * from a datapack island offset into the neighbouring chunk, it serves nothing.
     */
    @Property
    void theFeaturesRegionServesTheWholeScanAroundItsOwnChunk(
            @ForAll @IntRange(min = 0, max = ChunkStatus.MAX_STRUCTURE_DISTANCE) int distance) {
        assertTrue(distance < FEATURES.directDependencies().size());
        assertTrue(
                FEATURES.directDependencies()
                        .get(distance)
                        .isOrAfter(ChunkStatus.STRUCTURE_STARTS));
        assertEquals(ChunkStatus.MAX_STRUCTURE_DISTANCE + 1, FEATURES.directDependencies().size());
    }

    /**
     * The island keeps out of a block exactly when a structure box holds it or it lies in a chunk
     * the scan left alone.
     */
    @Property(tries = 3000)
    void theKeepOutBlocksExactlyTheBoxesAndTheChunksLeftAlone(
            @ForAll @Size(max = 4)
                    List<@Size(6) List<@IntRange(min = -40, max = 40) Integer>> boxes,
            @ForAll @Size(max = 3) List<@Size(2) List<@IntRange(min = -3, max = 3) Integer>> chunks,
            @ForAll @IntRange(min = -48, max = 48) int x,
            @ForAll @IntRange(min = -40, max = 40) int y,
            @ForAll @IntRange(min = -48, max = 48) int z) {
        List<BoundingBox> keptBoxes = new ArrayList<>();
        for (List<Integer> box : boxes) {
            keptBoxes.add(
                    new BoundingBox(
                            Math.min(box.get(0), box.get(3)),
                            Math.min(box.get(1), box.get(4)),
                            Math.min(box.get(2), box.get(5)),
                            Math.max(box.get(0), box.get(3)),
                            Math.max(box.get(1), box.get(4)),
                            Math.max(box.get(2), box.get(5))));
        }
        List<ChunkPos> leftAlone = new ArrayList<>();
        for (List<Integer> chunk : chunks) leftAlone.add(new ChunkPos(chunk.get(0), chunk.get(1)));
        BlockPos pos = new BlockPos(x, y, z);

        boolean expected = leftAlone.contains(ChunkPos.containing(pos));
        for (BoundingBox box : keptBoxes) {
            expected |=
                    x >= box.minX()
                            && x <= box.maxX()
                            && y >= box.minY()
                            && y <= box.maxY()
                            && z >= box.minZ()
                            && z <= box.maxZ();
        }
        assertEquals(expected, new EndIslandStructures.KeepOut(keptBoxes, leftAlone).blocks(pos));
    }

    /** A level that records every block written through it and answers nothing else. */
    private static WorldGenLevel recorder(List<BlockPos> written) {
        return (WorldGenLevel)
                Proxy.newProxyInstance(
                        WorldGenLevel.class.getClassLoader(),
                        new Class<?>[] {WorldGenLevel.class},
                        (proxy, method, args) -> {
                            if (method.getName().startsWith("setBlock")) {
                                written.add(((BlockPos) args[0]).immutable());
                                return true;
                            }
                            throw new UnsupportedOperationException(method.toString());
                        });
    }
}
