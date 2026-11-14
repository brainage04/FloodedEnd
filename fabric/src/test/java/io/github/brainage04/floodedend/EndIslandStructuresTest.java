package io.github.brainage04.floodedend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;

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

    /** A chunk of a made-up world: how far it generated, its starts and its references. */
    record FakeChunk(
            int dx,
            int dz,
            ChunkStatus status,
            List<List<BoundingBox>> starts,
            List<ChunkPos> references) {}

    @Provide
    Arbitrary<List<FakeChunk>> worlds() {
        Arbitrary<BoundingBox> box =
                Arbitraries.integers()
                        .between(-200, 200)
                        .list()
                        .ofSize(3)
                        .map(
                                c ->
                                        BoundingBox.fromCorners(
                                                new BlockPos(c.get(0), c.get(1), c.get(2)),
                                                new BlockPos(
                                                        c.get(0) + 5, c.get(1) + 5, c.get(2) + 5)));
        Arbitrary<ChunkPos> reference =
                Arbitraries.integers()
                        .between(-12, 12)
                        .list()
                        .ofSize(2)
                        .map(c -> new ChunkPos(c.get(0), c.get(1)));
        Arbitrary<FakeChunk> chunk =
                Combinators.combine(
                                Arbitraries.integers().between(-10, 10),
                                Arbitraries.integers().between(-10, 10),
                                Arbitraries.of(
                                        ChunkStatus.STRUCTURE_STARTS,
                                        ChunkStatus.STRUCTURE_REFERENCES,
                                        ChunkStatus.FEATURES,
                                        ChunkStatus.FULL),
                                box.list().ofMaxSize(2).list().ofMaxSize(2),
                                reference.list().ofMaxSize(3))
                        .as(FakeChunk::new);
        return chunk.list().ofMaxSize(40).uniqueElements(c -> new ChunkPos(c.dx(), c.dz()));
    }

    /**
     * Over a made-up world around the island, the scan asks only for chunks within the structure
     * distance of the island's chunk, collects every piece of every start with pieces in the chunks
     * the level holds there, and leaves alone exactly the chunks around the island's own that hold
     * their references and reference a start beyond that distance.
     */
    @Property(tries = 1000)
    void theScanReadsTheStructureDistanceAndLeavesAloneChunksReferencingBeyondIt(
            @ForAll("worlds") List<FakeChunk> world,
            @ForAll @IntRange(min = -1000, max = 1000) int islandChunkX,
            @ForAll @IntRange(min = -1000, max = 1000) int islandChunkZ,
            @ForAll @IntRange(min = 0, max = 15) int inChunkX,
            @ForAll @IntRange(min = 0, max = 15) int inChunkZ) {
        ChunkPos island = new ChunkPos(islandChunkX, islandChunkZ);
        Map<ChunkPos, ChunkAccess> chunks = new HashMap<>();
        List<String> expectedBoxes = new ArrayList<>();
        Set<ChunkPos> expectedAlone = new HashSet<>();
        int distance = ChunkStatus.MAX_STRUCTURE_DISTANCE;
        for (FakeChunk fake : world) {
            ChunkPos pos = new ChunkPos(island.x() + fake.dx(), island.z() + fake.dz());
            List<ChunkPos> references = new ArrayList<>();
            for (ChunkPos reference : fake.references()) {
                references.add(
                        new ChunkPos(island.x() + reference.x(), island.z() + reference.z()));
            }
            List<List<BoundingBox>> starts = new ArrayList<>();
            for (List<BoundingBox> start : fake.starts()) {
                List<BoundingBox> moved = new ArrayList<>();
                for (BoundingBox piece : start) {
                    moved.add(piece.moved(pos.getMinBlockX(), 0, pos.getMinBlockZ()));
                }
                starts.add(moved);
            }
            chunks.put(pos, chunk(pos, fake.status(), starts, references));
            if (island.getChessboardDistance(pos) <= distance) {
                for (List<BoundingBox> start : starts) {
                    for (BoundingBox piece : start) expectedBoxes.add(piece.toString());
                }
            }
            if (island.getChessboardDistance(pos) <= 1
                    && fake.status().isOrAfter(ChunkStatus.STRUCTURE_REFERENCES)
                    && references.stream()
                            .anyMatch(start -> island.getChessboardDistance(start) > distance)) {
                expectedAlone.add(pos);
            }
        }
        List<ChunkPos> asked = new ArrayList<>();
        LevelReader level = reader(chunks, asked);

        EndIslandStructures.KeepOut keepOut =
                EndIslandStructures.keepOut(
                        level,
                        new BlockPos(island.getBlockX(inChunkX), 64, island.getBlockZ(inChunkZ)));

        for (ChunkPos pos : asked) {
            assertTrue(
                    island.getChessboardDistance(pos) <= distance,
                    "the scan asked for " + pos + " from the island in " + island);
        }
        List<String> boxes = new ArrayList<>();
        for (BoundingBox box : keepOut.boxes()) boxes.add(box.toString());
        Collections.sort(boxes);
        Collections.sort(expectedBoxes);
        assertEquals(expectedBoxes, boxes);
        assertEquals(expectedAlone, new HashSet<>(keepOut.beyondTheScan()));
        assertEquals(expectedAlone.size(), keepOut.beyondTheScan().size());
    }

    /**
     * A level that holds {@code chunks}, answers null for any other, as a level does for a chunk it
     * does not hold, and records each chunk it is asked for.
     */
    private static LevelReader reader(Map<ChunkPos, ChunkAccess> chunks, List<ChunkPos> asked) {
        return (LevelReader)
                Proxy.newProxyInstance(
                        LevelReader.class.getClassLoader(),
                        new Class<?>[] {LevelReader.class},
                        (proxy, method, args) -> {
                            if (method.getName().equals("getChunk") && args.length == 4) {
                                assertEquals(ChunkStatus.STRUCTURE_STARTS, args[2]);
                                assertEquals(false, args[3]);
                                ChunkPos pos = new ChunkPos((Integer) args[0], (Integer) args[1]);
                                asked.add(pos);
                                return chunks.get(pos);
                            }
                            throw new UnsupportedOperationException(method.toString());
                        });
    }

    /** A chunk holding one start per list of piece boxes, an empty list making an invalid start. */
    private static ChunkAccess chunk(
            ChunkPos pos,
            ChunkStatus status,
            List<List<BoundingBox>> starts,
            List<ChunkPos> references) {
        ProtoChunk chunk = WorldgenFixtures.allocate(ProtoChunk.class);
        WorldgenFixtures.set(ChunkAccess.class, chunk, "chunkPos", pos);
        WorldgenFixtures.set(ProtoChunk.class, chunk, "status", status);
        Map<Structure, StructureStart> startMap = new HashMap<>();
        for (List<BoundingBox> start : starts) {
            List<StructurePiece> pieces = new ArrayList<>();
            for (BoundingBox box : start) {
                EndCityPieces.EndCityPiece piece =
                        WorldgenFixtures.allocate(EndCityPieces.EndCityPiece.class);
                WorldgenFixtures.set(StructurePiece.class, piece, "boundingBox", box);
                pieces.add(piece);
            }
            Structure structure = WorldgenFixtures.allocate(EndCityAtSea.class);
            startMap.put(
                    structure, new StructureStart(structure, pos, 0, new PiecesContainer(pieces)));
        }
        WorldgenFixtures.set(ChunkAccess.class, chunk, "structureStarts", startMap);
        Map<Structure, LongSet> referenceMap = new HashMap<>();
        if (!references.isEmpty()) {
            LongSet packed = new LongOpenHashSet();
            for (ChunkPos reference : references) packed.add(reference.pack());
            referenceMap.put(WorldgenFixtures.allocate(EndCityAtSea.class), packed);
        }
        WorldgenFixtures.set(ChunkAccess.class, chunk, "structureReferences", referenceMap);
        return chunk;
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
