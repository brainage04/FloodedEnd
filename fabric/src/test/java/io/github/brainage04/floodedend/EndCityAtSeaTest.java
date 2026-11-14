package io.github.brainage04.floodedend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unimi.dsi.fastutil.HashCommon;
import java.lang.reflect.Method;
import java.util.Optional;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.EndCityStructure;

/** End city placement against vanilla's {@code EndCityStructure} over made-up terrain. */
class EndCityAtSeaTest {
    static {
        WorldgenFixtures.bootstrap();
    }

    /**
     * Over a dry End the city starts wherever vanilla's would, at the same height, and is turned
     * down wherever vanilla's is: the same rotation from the same structure random, the same
     * corners, the same ground limit and the same biome check.
     */
    @Property(tries = 2000)
    void aDryEndPlacesCitiesWhereVanillaDoes(
            @ForAll long seed,
            @ForAll @IntRange(min = -100_000, max = 100_000) int chunkX,
            @ForAll @IntRange(min = -100_000, max = 100_000) int chunkZ,
            @ForAll long terrainSeed,
            @ForAll boolean biomeValid) {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        WorldgenFixtures.Terrain terrain = terrain(terrainSeed);
        Optional<BlockPos> vanilla =
                WorldgenFixtures.allocate(EndCityStructure.class)
                        .findGenerationPoint(
                                WorldgenFixtures.context(terrain, chunk, seed, biomeValid))
                        .map(Structure.GenerationStub::position);
        Optional<BlockPos> ours =
                WorldgenFixtures.allocate(EndCityAtSea.class)
                        .findGenerationPoint(
                                WorldgenFixtures.context(terrain, chunk, seed, biomeValid))
                        .map(Structure.GenerationStub::position);
        assertEquals(vanilla, ours);
    }

    /**
     * Over a flooded End the city measures the ocean floor at the corners vanilla measures the
     * world surface at, for every rotation.
     */
    @Property(tries = 2000)
    void aFloodedEndMeasuresTheOceanFloorAtVanillasCorners(
            @ForAll Rotation rotation,
            @ForAll @IntRange(min = -100_000, max = 100_000) int chunkX,
            @ForAll @IntRange(min = -100_000, max = 100_000) int chunkZ,
            @ForAll long terrainSeed)
            throws ReflectiveOperationException {
        ChunkPos chunk = new ChunkPos(chunkX, chunkZ);
        WorldgenFixtures.Terrain terrain = terrain(terrainSeed);
        WorldgenFixtures.Terrain floorAsSurface =
                (x, z, heightmap) ->
                        heightmap == Heightmap.Types.WORLD_SURFACE_WG
                                ? terrain.occupied(x, z, Heightmap.Types.OCEAN_FLOOR_WG)
                                : terrain.occupied(x, z, heightmap);
        EndOcean flooded = new EndOcean(64, Blocks.WATER.defaultBlockState(), true);

        int ours =
                EndCityAtSea.lowestGroundY(
                        WorldgenFixtures.context(terrain, chunk, 0L, true), rotation, flooded);

        assertEquals(vanillaLowestY(floorAsSurface, chunk, rotation), ours);
    }

    /** The y vanilla's {@code Structure#getLowestYIn5by5Box} gives the End city's start column. */
    private static int vanillaLowestY(
            WorldgenFixtures.Terrain terrain, ChunkPos chunk, Rotation rotation)
            throws ReflectiveOperationException {
        Method lowest =
                Structure.class.getDeclaredMethod(
                        "getLowestYIn5by5Box",
                        Structure.GenerationContext.class,
                        int.class,
                        int.class,
                        Rotation.class);
        lowest.setAccessible(true);
        BlockPos pos =
                (BlockPos)
                        lowest.invoke(
                                WorldgenFixtures.allocate(EndCityStructure.class),
                                WorldgenFixtures.context(terrain, chunk, 0L, true),
                                chunk.getBlockX(7),
                                chunk.getBlockZ(7),
                                rotation);
        return pos.getY();
    }

    /**
     * Heights from 40 to 90 that differ by column and by heightmap, around vanilla's ground limit
     * of 60, so each corner can decide.
     */
    private static WorldgenFixtures.Terrain terrain(long seed) {
        return (x, z, heightmap) ->
                40
                        + (int)
                                Math.floorMod(
                                        HashCommon.mix(
                                                seed
                                                        ^ HashCommon.mix(
                                                                (long) x * 31L
                                                                        + (long) z * 1_000_003L
                                                                        + heightmap.ordinal())),
                                        51L);
    }
}
