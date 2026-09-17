package io.github.brainage04.floodedend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Moors the End ships of a generated city onto the End's sea.
 *
 * <p>Vanilla builds the End ship from {@code end_city/ship} at the top of the city's tower, tens of blocks above the
 * water, where it reads as floating in the sky. This class finds those ships in a finished city and lowers each one
 * until its keel is {@value #SHIP_DRAFT} blocks under the water surface, at the nearest column whose seabed leaves
 * the whole hull clear of ground — so a ship sits in the water instead of in a hillside, and stays as close to its
 * city as the sea allows.</p>
 *
 * <p>Nothing else about the city is touched: the towers, the bridges that used to lead to each ship and the rest of
 * the structure keep vanilla's placement. A ship is never moved outside the structure's own reference radius, because
 * a chunk only places the pieces of a start within {@link ChunkStatus#MAX_STRUCTURE_DISTANCE} chunks of it.</p>
 */
public final class EndShipMooring {
	/** The ship template is the only End city piece that puts a hull in the world. */
	private static final Identifier SHIP_TEMPLATE = Identifier.withDefaultNamespace("end_city/ship");
	/** How deep the hull sits in the water, counted from the keel up to the water surface. */
	public static final int SHIP_DRAFT = 3;
	/**
	 * A ship may be moved this far from where vanilla puts it, to find water deep enough to float in. Measured
	 * moorings need 1 to 14 blocks, so this is generous; keeping it small also keeps a moored ship inside the
	 * neighbourhood its city's structure was saved with.
	 */
	private static final int MOORING_REACH = 48;
	/** Mooring search and seabed sampling resolution; the seabed is sampled on this block grid. */
	private static final int MOORING_STEP = 8;
	/** Blocks of water left between the seabed and the keel, so no hull ever meets ground. */
	private static final int MOORING_CLEARANCE = 2;
	/** A hull is sampled over its footprint at these offsets, which covers a ship template turned any way. */
	private static final int[] FOOTPRINT_OFFSETS = {-16, -8, 0, 8, 16};

	private EndShipMooring() {
	}

	/** The y a ship's keel floats at when the water surface is {@code waterSurfaceY}. */
	public static int keelY(int waterSurfaceY) {
		return waterSurfaceY - SHIP_DRAFT;
	}

	/**
	 * Lowers every ship in {@code pieces} onto the sea. Ships that have no water deep enough within
	 * {@value #MOORING_REACH} blocks are left where vanilla put them and reported, rather than being dropped into
	 * ground or silently moved somewhere that reads worse.
	 */
	public static void moor(Structure.GenerationContext context, List<StructurePiece> pieces, EndOcean ocean) {
		StructureTemplate ship = context.structureTemplateManager().getOrCreate(SHIP_TEMPLATE);
		int keelY = keelY(ocean.waterSurfaceY());
		for (StructurePiece piece : pieces) {
			if (!(piece instanceof TemplateStructurePiece template) || template.template() != ship) {
				continue;
			}

			BlockPos current = template.templatePosition();
			Optional<BlockPos> mooring = findMooring(context, piece, keelY);
			if (mooring.isEmpty()) {
				FloodedEnd.LOGGER.warn(
						"FloodedEnd: no water deep enough for the End ship at {}, {} within {} blocks; it stays at "
								+ "y={} above the End sea, whose surface is y={}.",
						current.getX(), current.getZ(), MOORING_REACH, current.getY(), ocean.waterSurfaceY()
				);
				continue;
			}

			BlockPos target = mooring.get();
			piece.move(target.getX() - current.getX(), target.getY() - current.getY(), target.getZ() - current.getZ());
		}
	}

	/**
	 * Where a moved ship may end up. A chunk only places the pieces of a structure start within
	 * {@link ChunkStatus#MAX_STRUCTURE_DISTANCE} chunks of it, so a ship pushed further than that would be placed
	 * only as far as the radius reaches and would stand in the world half built.
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
				z + 15 + distance
		);
	}

	private static boolean inside(BoundingBox area, BoundingBox box) {
		return box.minX() >= area.minX() && box.maxX() <= area.maxX()
				&& box.minZ() >= area.minZ() && box.maxZ() <= area.maxZ();
	}

	/**
	 * The nearest column whose seabed leaves the whole hull clear of ground, searched outwards from where vanilla
	 * puts the ship, so a ship stays as close to its city as the sea allows.
	 */
	private static Optional<BlockPos> findMooring(Structure.GenerationContext context, StructurePiece ship, int keelY) {
		BlockPos origin = ((TemplateStructurePiece) ship).templatePosition();
		BoundingBox hull = ship.getBoundingBox();
		BoundingBox area = placeableArea(context);
		Map<Long, Integer> seabed = new HashMap<>();
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

	/** True when the seabed under the whole hull stays {@value #MOORING_CLEARANCE} blocks below the keel. */
	private static boolean floats(
			Structure.GenerationContext context,
			Map<Long, Integer> seabed,
			BoundingBox hull,
			int keelY
	) {
		int centreX = (hull.minX() + hull.maxX()) / 2;
		int centreZ = (hull.minZ() + hull.maxZ()) / 2;
		for (int offsetX : FOOTPRINT_OFFSETS) {
			for (int offsetZ : FOOTPRINT_OFFSETS) {
				if (seabed(context, seabed, centreX + offsetX, centreZ + offsetZ) > keelY - MOORING_CLEARANCE) {
					return false;
				}
			}
		}

		return true;
	}

	/** The ocean floor height, sampled once per {@value #MOORING_STEP}-block cell of the search area. */
	private static int seabed(Structure.GenerationContext context, Map<Long, Integer> seabed, int x, int z) {
		int gridX = x & ~(MOORING_STEP - 1);
		int gridZ = z & ~(MOORING_STEP - 1);
		long key = ((long) (gridX / MOORING_STEP) << 32) | ((gridZ / MOORING_STEP) & 0xFFFFFFFFL);
		return seabed.computeIfAbsent(
				key,
				ignored -> context.chunkGenerator().getFirstOccupiedHeight(
						gridX, gridZ, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState()
				)
		);
	}
}
