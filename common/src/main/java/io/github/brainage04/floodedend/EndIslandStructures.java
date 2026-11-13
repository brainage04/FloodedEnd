package io.github.brainage04.floodedend;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
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

    /**
     * Every structure piece around the island's chunk. A piece lies at most {@link
     * ChunkStatus#MAX_STRUCTURE_DISTANCE} chunks from the chunk that stores its structure start,
     * which is the bound vanilla's structure placement uses and the one {@link EndShipMooring}
     * keeps a moored ship inside, so this neighbourhood holds every piece an island can reach.
     */
    public static List<BoundingBox> around(LevelReader level, BlockPos origin) {
        int chunkX = SectionPos.blockToSectionCoord(origin.getX());
        int chunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int distance = ChunkStatus.MAX_STRUCTURE_DISTANCE;
        List<BoundingBox> boxes = new ArrayList<>();
        for (int dx = -distance; dx <= distance; dx++) {
            for (int dz = -distance; dz <= distance; dz++) {
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

        return boxes;
    }

    public static boolean inside(List<BoundingBox> boxes, BlockPos pos) {
        for (BoundingBox box : boxes) {
            if (box.isInside(pos)) {
                return true;
            }
        }

        return false;
    }
}
