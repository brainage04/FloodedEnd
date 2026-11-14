package io.github.brainage04.floodedend;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Where vanilla's small End island must not write.
 *
 * <p>Vanilla places a chunk's structures before that chunk's own features, but after the features
 * of the chunks generated before it, and a chunk's features may write one chunk around their own
 * ({@code ChunkPyramid.java:23-29}, {@code ChunkGenerator.java:360-400}). An island whose own chunk
 * is decorated after a neighbouring chunk already placed its structures therefore writes into that
 * neighbour after those structures are there, and vanilla's island writes every block of its blob
 * without asking ({@code EndIslandFeature.java:27}): it replaces a moored ship's hull with end
 * stone.
 */
public final class EndIslandStructures {
    private EndIslandStructures() {}

    /** The boxes an island must keep out of, and the chunks it must not write into at all. */
    public record KeepOut(List<BoundingBox> boxes, List<ChunkPos> beyondTheScan) {
        public boolean blocks(BlockPos pos) {
            for (BoundingBox box : boxes) {
                if (box.isInside(pos)) {
                    return true;
                }
            }

            if (!beyondTheScan.isEmpty()) {
                int chunkX = SectionPos.blockToSectionCoord(pos.getX());
                int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
                for (ChunkPos chunk : beyondTheScan) {
                    if (chunk.x() == chunkX && chunk.z() == chunkZ) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    /**
     * Every structure piece within {@link ChunkStatus#MAX_STRUCTURE_DISTANCE} chunks of the
     * island's own chunk, which is the bound vanilla's structure placement uses and the one {@link
     * EndShipMooring} keeps a moored ship inside, plus the chunks in the island's reach that hold a
     * structure start this scan could not read.
     */
    // hasChunk is deprecated with the rest of LevelReader's presence checks
    // (LevelReader.java:33-34), but it is the only way to ask a generation region which chunks it
    // can serve (WorldGenRegion.java:162-165); getChunk throws a ReportedException for the others.
    // deprecated-api: no replacement tells a generation region's served chunks from the rest.
    @SuppressWarnings("deprecation")
    public static KeepOut keepOut(LevelReader level, BlockPos origin) {
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int distance = ChunkStatus.MAX_STRUCTURE_DISTANCE;
        List<BoundingBox> boxes = new ArrayList<>();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dz = -distance; dz <= distance; dz++) {
                // A datapack that offsets the island into a neighbouring chunk moves the scan past
                // the chunks a generation region serves, and asking for one of those throws. Every
                // other level answers with null for a chunk it does not hold, and the structure
                // references it holds for the others, so the level itself keeps the scan honest.
                if (level instanceof WorldGenRegion region
                        && !region.hasChunk(chunkX + dx, chunkZ + dz)) {
                    continue;
                }

                ChunkAccess chunk =
                        level.getChunk(
                                chunkX + dx, chunkZ + dz, ChunkStatus.STRUCTURE_STARTS, false);
                if (chunk == null) {
                    continue;
                }

                for (StructureStart start : chunk.getAllStarts().values()) {
                    if (!start.isValid()) {
                        continue;
                    }

                    for (StructurePiece piece : start.getPieces()) {
                        boxes.add(piece.getBoundingBox());
                    }
                }
            }
        }

        return new KeepOut(boxes, beyondTheScan(level, chunkX, chunkZ));
    }

    /**
     * The chunks within the island's reach that hold a structure start the scan could not read.
     *
     * <p>Vanilla stores in a chunk a reference to every structure start whose box reaches it
     * ({@code ChunkGenerator.java:633-650}), a chunk places the pieces of the starts it references
     * ({@code StructureManager.java:64-78}), and an island writes one chunk around its own. A start
     * one chunk outside the scan can therefore stand in a chunk the island reaches, and {@link
     * EndShipMooring} lets a ship moor the whole structure distance from its city's start. A chunk
     * that has not computed its references yet holds no structure at all, so it keeps the plain box
     * check.
     */
    private static List<ChunkPos> beyondTheScan(LevelReader level, int chunkX, int chunkZ) {
        int distance = ChunkStatus.MAX_STRUCTURE_DISTANCE;
        List<ChunkPos> beyondTheScan = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkAccess chunk =
                        level.getChunk(
                                chunkX + dx, chunkZ + dz, ChunkStatus.STRUCTURE_STARTS, false);
                if (chunk == null
                        || !chunk.getPersistedStatus()
                                .isOrAfter(ChunkStatus.STRUCTURE_REFERENCES)) {
                    continue;
                }

                if (referencesAStartBeyond(chunk, chunkX, chunkZ, distance)) {
                    beyondTheScan.add(chunk.getPos());
                }
            }
        }

        return beyondTheScan;
    }

    private static boolean referencesAStartBeyond(
            ChunkAccess chunk, int chunkX, int chunkZ, int distance) {
        for (LongSet starts : chunk.getAllReferences().values()) {
            for (long start : starts) {
                if (Math.abs(ChunkPos.getX(start) - chunkX) > distance
                        || Math.abs(ChunkPos.getZ(start) - chunkZ) > distance) {
                    return true;
                }
            }
        }

        return false;
    }
}
