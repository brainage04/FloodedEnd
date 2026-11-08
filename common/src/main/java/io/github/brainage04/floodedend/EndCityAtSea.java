package io.github.brainage04.floodedend;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;

/**
 * Vanilla asks the world surface heightmap where the ground is. A higher ocean can raise that answer above the real
 * terrain and admit cities without suitable ground, so placement asks the ocean floor heightmap once the End is
 * flooded.
 */
public final class EndCityAtSea extends Structure {
	public static final String NAME = "end_city";
	public static final MapCodec<EndCityAtSea> CODEC = simpleCodec(EndCityAtSea::new);
	public static final StructureType<EndCityAtSea> TYPE = () -> CODEC;

	/** Vanilla's placement gate: an End city only generates where the ground reaches this height. */
	private static final int MINIMUM_GROUND_Y = 60;

	public EndCityAtSea(StructureSettings settings) {
		super(settings);
	}

	@Override
	protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		EndOcean ocean = EndOcean.of(context.chunkGenerator());
		Rotation rotation = Rotation.getRandom(context.random());
		int groundY = lowestGroundY(context, rotation, ocean);
		if (groundY < MINIMUM_GROUND_Y) {
			return Optional.empty();
		}

		BlockPos origin = new BlockPos(
				context.chunkPos().getBlockX(7),
				groundY,
				context.chunkPos().getBlockZ(7)
		);
		return Optional.of(new GenerationStub(origin, builder -> generatePieces(builder, origin, rotation, context, ocean)));
	}

	/**
	 * Measured exactly as {@code Structure#getLowestYIn5by5BoxOffset7Blocks}, which always asks the world surface
	 * heightmap; this asks the ocean floor once the End is flooded.
	 */
	private static int lowestGroundY(GenerationContext context, Rotation rotation, EndOcean ocean) {
		int dx = 5;
		int dz = 5;
		if (rotation == Rotation.CLOCKWISE_90) {
			dx = -5;
		} else if (rotation == Rotation.CLOCKWISE_180) {
			dx = -5;
			dz = -5;
		} else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
			dz = -5;
		}

		Heightmap.Types heightmap = ocean.flooded() ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
		int x = context.chunkPos().getBlockX(7);
		int z = context.chunkPos().getBlockZ(7);
		return Math.min(
				Math.min(height(context, heightmap, x, z), height(context, heightmap, x, z + dz)),
				Math.min(height(context, heightmap, x + dx, z), height(context, heightmap, x + dx, z + dz))
		);
	}

	private static int height(GenerationContext context, Heightmap.Types heightmap, int x, int z) {
		return context.chunkGenerator().getFirstOccupiedHeight(
				x, z, heightmap, context.heightAccessor(), context.randomState()
		);
	}

	private void generatePieces(
			StructurePiecesBuilder builder,
			BlockPos origin,
			Rotation rotation,
			GenerationContext context,
			EndOcean ocean
	) {
		List<StructurePiece> pieces = Lists.newArrayList();
		EndCityPieces.startHouseTower(context.structureTemplateManager(), origin, rotation, pieces, context.random());
		if (ocean.flooded()) {
			EndShipMooring.moor(context, pieces, ocean);
		}

		pieces.forEach(builder::addPiece);
	}

	@Override
	public StructureType<?> type() {
		return TYPE;
	}
}
