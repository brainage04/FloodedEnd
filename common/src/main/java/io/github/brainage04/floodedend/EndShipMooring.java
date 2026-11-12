package io.github.brainage04.floodedend;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public final class EndShipMooring {
    private static final Identifier SHIP_TEMPLATE =
            Identifier.withDefaultNamespace("end_city/ship");
    public static final int SHIP_DRAFT = 3;

    /**
     * Measured moorings need 1 to 14 blocks; keeping the reach small also keeps a moored ship
     * inside the neighbourhood its city's structure was saved with.
     */
    private static final int MOORING_REACH = 48;

    private static final int MOORING_STEP = 8;
    private static final int MOORING_CLEARANCE = 2;

    private EndShipMooring() {}

    public static int keelY(int waterSurfaceY) {
        return waterSurfaceY - SHIP_DRAFT;
    }

    public static void moor(
            Structure.GenerationContext context, List<StructurePiece> pieces, EndOcean ocean) {
        StructureTemplate ship = context.structureTemplateManager().getOrCreate(SHIP_TEMPLATE);
        int keelY = keelY(ocean.waterSurfaceY());
        for (StructurePiece piece : pieces) {
            if (!(piece instanceof TemplateStructurePiece template)
                    || template.template() != ship) {
                continue;
            }

            BlockPos current = template.templatePosition();
            Optional<BlockPos> mooring = findMooring(context, piece, keelY);
            if (mooring.isEmpty()) {
                FloodedEnd.LOGGER.warn(
                        "FloodedEnd: no water deep enough for the End ship at {}, {} within {} blocks; it stays at "
                                + "y={} above the End sea, whose surface is y={}.",
                        current.getX(),
                        current.getZ(),
                        MOORING_REACH,
                        current.getY(),
                        ocean.waterSurfaceY());
                continue;
            }

            BlockPos target = mooring.get();
            piece.move(
                    target.getX() - current.getX(),
                    target.getY() - current.getY(),
                    target.getZ() - current.getZ());
        }
    }

    /**
     * Where a moved ship may end up. A chunk only places the pieces of a structure start within
     * {@link ChunkStatus#MAX_STRUCTURE_DISTANCE} chunks of it, so a ship pushed further than that
     * would be placed only as far as the radius reaches and would stand in the world half built.
     */
    private static BoundingBox placeableArea(Structure.GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMinBlockX();
        int z = chunk.getMinBlockZ();
        int distance = ChunkStatus.MAX_STRUCTURE_DISTANCE * 16;
        return new BoundingBox(
                x - distance,
                context.heightAccessor().getMinY(),
                z - distance,
                x + 15 + distance,
                context.heightAccessor().getMaxY(),
                z + 15 + distance);
    }

    private static boolean inside(BoundingBox area, BoundingBox box) {
        return box.minX() >= area.minX()
                && box.maxX() <= area.maxX()
                && box.minZ() >= area.minZ()
                && box.maxZ() <= area.maxZ();
    }

    private static Optional<BlockPos> findMooring(
            Structure.GenerationContext context, StructurePiece ship, int keelY) {
        BlockPos origin = ((TemplateStructurePiece) ship).templatePosition();
        BoundingBox hull = ship.getBoundingBox();
        BoundingBox area = placeableArea(context);
        Long2IntOpenHashMap seabed = new Long2IntOpenHashMap();
        seabed.defaultReturnValue(Integer.MIN_VALUE);
        for (int reach = 0; reach <= MOORING_REACH; reach += MOORING_STEP) {
            for (int dz = -reach; dz <= reach; dz += MOORING_STEP) {
                for (int dx = -reach; dx <= reach; dx += MOORING_STEP) {
                    if (reach > 0 && Math.abs(dx) != reach && Math.abs(dz) != reach) {
                        continue;
                    }

                    BoundingBox moved = hull.moved(dx, 0, dz);
                    if (inside(area, moved) && floats(context, seabed, moved, keelY)) {
                        return Optional.of(origin.offset(dx, keelY - origin.getY(), dz));
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static boolean floats(
            Structure.GenerationContext context,
            Long2IntOpenHashMap seabed,
            BoundingBox hull,
            int keelY) {
        for (int x = hull.minX(); x <= hull.maxX(); x++) {
            for (int z = hull.minZ(); z <= hull.maxZ(); z++) {
                if (seabed(context, seabed, x, z) > keelY - MOORING_CLEARANCE) {
                    return false;
                }
            }
        }

        return true;
    }

    private static int seabed(
            Structure.GenerationContext context, Long2IntOpenHashMap seabed, int x, int z) {
        long key = BlockPos.asLong(x, 0, z);
        int height = seabed.get(key);
        if (height == Integer.MIN_VALUE) {
            height =
                    context.chunkGenerator()
                            .getFirstOccupiedHeight(
                                    x,
                                    z,
                                    Heightmap.Types.OCEAN_FLOOR_WG,
                                    context.heightAccessor(),
                                    context.randomState());
            seabed.put(key, height);
        }
        return height;
    }
}
