package io.github.brainage04.floodedend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkPyramid;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;

class EndShipMooringTest {
    static {
        WorldgenFixtures.bootstrap();
    }

    /**
     * How far from a start's chunk vanilla references it. {@code ChunkGenerator#createReferences}
     * reads the starts of every chunk within its range ({@code ChunkGenerator.java:633-650}), so
     * the STRUCTURE_REFERENCES step requires STRUCTURE_STARTS over that same radius.
     */
    private static final int REFERENCE_RADIUS =
            ChunkPyramid.GENERATION_PYRAMID
                    .getStepTo(ChunkStatus.STRUCTURE_REFERENCES)
                    .getAccumulatedRadiusOf(ChunkStatus.STRUCTURE_STARTS);

    /**
     * A hull anywhere in the placeable area touches only chunks that reference its city's start, so
     * every chunk it stands in places it whole.
     */
    @Property
    void aHullInTheAreaStandsOnlyInChunksThatReferenceItsStart(
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int chunkX,
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int chunkZ,
            @ForAll @Size(4) List<@IntRange(min = 0, max = 1_000_000) Integer> corners) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        BoundingBox area = EndShipMooring.placeableArea(chunk, WorldgenFixtures.END);
        int width = area.maxX() - area.minX() + 1;
        int x1 = area.minX() + corners.get(0) % width;
        int x2 = area.minX() + corners.get(1) % width;
        int z1 = area.minZ() + corners.get(2) % width;
        int z2 = area.minZ() + corners.get(3) % width;
        BoundingBox hull =
                new BoundingBox(
                        Math.min(x1, x2),
                        area.minY(),
                        Math.min(z1, z2),
                        Math.max(x1, x2),
                        area.maxY(),
                        Math.max(z1, z2));
        for (int x = SectionPos.blockToSectionCoord(hull.minX());
                x <= SectionPos.blockToSectionCoord(hull.maxX());
                x++) {
            for (int z = SectionPos.blockToSectionCoord(hull.minZ());
                    z <= SectionPos.blockToSectionCoord(hull.maxZ());
                    z++) {
                assertTrue(
                        chunk.getChessboardDistance(x, z) <= REFERENCE_RADIUS,
                        hull + " stands in chunk " + x + ", " + z);
            }
        }
    }

    /**
     * The area reaches the last chunk that references the start on every side, and the world's
     * build limits above and below.
     */
    @Property
    void theAreaEndsWhereReferencesEnd(
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int chunkX,
            @ForAll @IntRange(min = -1_000_000, max = 1_000_000) int chunkZ) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        BoundingBox area = EndShipMooring.placeableArea(chunk, WorldgenFixtures.END);
        assertEquals(chunkX - REFERENCE_RADIUS, SectionPos.blockToSectionCoord(area.minX()));
        assertEquals(
                chunkX - REFERENCE_RADIUS - 1, SectionPos.blockToSectionCoord(area.minX() - 1));
        assertEquals(chunkX + REFERENCE_RADIUS, SectionPos.blockToSectionCoord(area.maxX()));
        assertEquals(
                chunkX + REFERENCE_RADIUS + 1, SectionPos.blockToSectionCoord(area.maxX() + 1));
        assertEquals(chunkZ - REFERENCE_RADIUS, SectionPos.blockToSectionCoord(area.minZ()));
        assertEquals(
                chunkZ - REFERENCE_RADIUS - 1, SectionPos.blockToSectionCoord(area.minZ() - 1));
        assertEquals(chunkZ + REFERENCE_RADIUS, SectionPos.blockToSectionCoord(area.maxZ()));
        assertEquals(
                chunkZ + REFERENCE_RADIUS + 1, SectionPos.blockToSectionCoord(area.maxZ() + 1));
        assertEquals(WorldgenFixtures.END.getMinY(), area.minY());
        assertEquals(WorldgenFixtures.END.getMaxY(), area.maxY());
    }

    /** A hull is inside the area exactly when vanilla's box holds both of its corners. */
    @Property(tries = 3000)
    void aHullIsInsideTheAreaWhenTheAreaHoldsBothItsCorners(
            @ForAll @IntRange(min = -1000, max = 1000) int chunkX,
            @ForAll @IntRange(min = -1000, max = 1000) int chunkZ,
            @ForAll @Size(6) List<@IntRange(min = -2, max = 2) Integer> nearEdges,
            @ForAll @Size(6) List<Boolean> fromMin) {
        BoundingBox area =
                EndShipMooring.placeableArea(new ChunkPos(chunkX, chunkZ), WorldgenFixtures.END);
        int[] mins = {area.minX(), area.minY(), area.minZ()};
        int[] maxs = {area.maxX(), area.maxY(), area.maxZ()};
        int[] corner = new int[6];
        for (int i = 0; i < 6; i++) {
            int axis = i % 3;
            corner[i] = (fromMin.get(i) ? mins[axis] : maxs[axis]) + nearEdges.get(i);
        }
        BoundingBox hull =
                new BoundingBox(
                        Math.min(corner[0], corner[3]),
                        Math.min(corner[1], corner[4]),
                        Math.min(corner[2], corner[5]),
                        Math.max(corner[0], corner[3]),
                        Math.max(corner[1], corner[4]),
                        Math.max(corner[2], corner[5]));
        assertEquals(
                area.isInside(hull.minX(), hull.minY(), hull.minZ())
                        && area.isInside(hull.maxX(), hull.maxY(), hull.maxZ()),
                EndShipMooring.inside(area, hull));
    }

    /**
     * Over made-up sea floors, the mooring is the first candidate where the hull floats clear of
     * the floor and stays in the placeable area, in the order the search promises: the ship's own
     * spot, then each square ring of candidates one step further out, rows north to south and each
     * row west to east. Where no candidate works the ship stays where it is.
     */
    @Property(tries = 1000)
    void theShipMoorsAtTheNearestCandidateThatFloats(
            @ForAll @IntRange(min = -2000, max = 2000) int chunkX,
            @ForAll @IntRange(min = -2000, max = 2000) int chunkZ,
            @ForAll @IntRange(min = -80, max = 95) int originX,
            @ForAll @IntRange(min = -80, max = 95) int originZ,
            @ForAll @IntRange(min = 30, max = 140) int originY,
            @ForAll @IntRange(min = 1, max = 30) int sizeX,
            @ForAll @IntRange(min = 1, max = 30) int sizeZ,
            @ForAll @IntRange(min = 1, max = 25) int height,
            @ForAll @IntRange(min = 0, max = 29) int anchorX,
            @ForAll @IntRange(min = 0, max = 29) int anchorZ,
            @ForAll @IntRange(min = 10, max = 240) int keelY,
            @ForAll long terrainSeed,
            @ForAll @IntRange(min = 0, max = 40) int shallowPercent) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        BlockPos origin =
                new BlockPos(
                        chunk.getMinBlockX() + originX, originY, chunk.getMinBlockZ() + originZ);
        int minX = origin.getX() - anchorX % sizeX;
        int minZ = origin.getZ() - anchorZ % sizeZ;
        BoundingBox hull =
                new BoundingBox(
                        minX,
                        originY,
                        minZ,
                        minX + sizeX - 1,
                        originY + height - 1,
                        minZ + sizeZ - 1);
        Seabed seabed = new Seabed(origin, keelY, terrainSeed, shallowPercent);
        WorldgenFixtures.Terrain terrain = (x, z, heightmap) -> seabed.at(x, z);

        Optional<BlockPos> mooring =
                EndShipMooring.findMooring(
                        WorldgenFixtures.context(terrain, chunk, 0L, true),
                        ship(origin, hull),
                        keelY);

        BoundingBox area = EndShipMooring.placeableArea(chunk, WorldgenFixtures.END);
        List<int[]> floating = new ArrayList<>();
        int reach = EndShipMooring.MOORING_REACH;
        for (int dz = -reach; dz <= reach; dz += EndShipMooring.MOORING_STEP) {
            for (int dx = -reach; dx <= reach; dx += EndShipMooring.MOORING_STEP) {
                BoundingBox moved = hull.moved(dx, keelY - originY, dz);
                if (area.isInside(moved.minX(), moved.minY(), moved.minZ())
                        && area.isInside(moved.maxX(), moved.maxY(), moved.maxZ())
                        && seabed.clears(moved, keelY - EndShipMooring.MOORING_CLEARANCE)) {
                    floating.add(new int[] {dx, dz});
                }
            }
        }
        if (floating.isEmpty()) {
            assertTrue(mooring.isEmpty(), "moored at " + mooring + " though nothing floats");
            return;
        }
        int[] expected = floating.getFirst();
        for (int[] candidate : floating) {
            if (ring(candidate) < ring(expected)) expected = candidate;
        }
        assertEquals(
                Optional.of(origin.offset(expected[0], keelY - originY, expected[1])), mooring);
    }

    private static int ring(int[] offset) {
        return Math.max(Math.abs(offset[0]), Math.abs(offset[1]));
    }

    /**
     * A sea floor in cells of four by four blocks, each shallow enough to ground the ship with the
     * given chance and otherwise deep enough to float it, by one block or more.
     */
    private record Seabed(BlockPos origin, int keelY, long seed, int shallowPercent) {
        int at(int x, int z) {
            long cell =
                    HashCommon.mix(
                            seed
                                    ^ HashCommon.mix(
                                            Math.floorDiv(x - origin.getX(), 4) * 1_000_003L
                                                    + Math.floorDiv(z - origin.getZ(), 4)));
            int roll = (int) Math.floorMod(cell, 100L);
            int highestFloating = keelY - EndShipMooring.MOORING_CLEARANCE;
            return roll < shallowPercent
                    ? highestFloating + 1 + (int) Math.floorMod(cell >>> 8, 4L)
                    : highestFloating - (int) Math.floorMod(cell >>> 16, 4L);
        }

        boolean clears(BoundingBox hull, int highest) {
            for (int x = hull.minX(); x <= hull.maxX(); x++) {
                for (int z = hull.minZ(); z <= hull.maxZ(); z++) {
                    if (at(x, z) > highest) return false;
                }
            }
            return true;
        }
    }

    /** The ship piece as the search reads it: its template position and its box. */
    private static StructurePiece ship(BlockPos origin, BoundingBox hull) {
        EndCityPieces.EndCityPiece piece =
                WorldgenFixtures.allocate(EndCityPieces.EndCityPiece.class);
        WorldgenFixtures.set(TemplateStructurePiece.class, piece, "templatePosition", origin);
        WorldgenFixtures.set(StructurePiece.class, piece, "boundingBox", hull);
        return piece;
    }
}
