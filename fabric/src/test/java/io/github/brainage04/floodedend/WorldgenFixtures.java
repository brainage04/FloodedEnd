package io.github.brainage04.floodedend;

import com.mojang.serialization.MapCodec;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Generation contexts over made-up terrain, built without a server. */
final class WorldgenFixtures {
    /** The vanilla End's build height. */
    static final LevelHeightAccessor END;

    static {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        DimensionType end =
                VanillaRegistries.createWorldLookup()
                        .lookupOrThrow(Registries.DIMENSION_TYPE)
                        .getOrThrow(BuiltinDimensionTypes.END)
                        .value();
        END = LevelHeightAccessor.create(end.minY(), end.height());
    }

    private WorldgenFixtures() {}

    static void bootstrap() {
        // Runs the static initializer.
    }

    /** The first occupied y of a column on one heightmap. */
    interface Terrain {
        int occupied(int x, int z, Heightmap.Types heightmap);
    }

    /**
     * A context for the chunk {@code chunk} with the structure random vanilla seeds for it ({@code
     * Structure.GenerationContext#makeRandom}).
     */
    static Structure.GenerationContext context(
            Terrain terrain, ChunkPos chunk, long seed, boolean biomeValid) {
        Holder<Biome> biome = Holder.direct(allocate(Biome.class));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(seed, chunk.x(), chunk.z());
        Predicate<Holder<Biome>> valid = holder -> biomeValid;
        FixedBiomeSource biomes = new FixedBiomeSource(biome);
        return new Structure.GenerationContext(
                null,
                new TerrainGenerator(terrain),
                biomes,
                null,
                biomes.createResolver(null),
                null,
                null,
                random,
                seed,
                chunk,
                END,
                valid);
    }

    /** A generator whose heightmaps answer from {@link Terrain} and that generates nothing. */
    private static final class TerrainGenerator extends ChunkGenerator {
        private final Terrain terrain;

        private TerrainGenerator(Terrain terrain) {
            super(null);
            this.terrain = terrain;
        }

        @Override
        public int getBaseHeight(
                int x,
                int z,
                Heightmap.Types type,
                LevelHeightAccessor heightAccessor,
                RandomState randomState) {
            // getFirstOccupiedHeight answers one below the base height
            // (ChunkGenerator.java:687-690).
            return terrain.occupied(x, z, type) + 1;
        }

        @Override
        protected MapCodec<? extends ChunkGenerator> codec() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void spawnOriginalMobs(WorldGenRegion worldGenRegion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getGenDepth() {
            return END.getHeight();
        }

        @Override
        public CompletableFuture<ChunkAccess> buildTerrain(
                ChunkAccess chunk,
                Blender blender,
                RandomState randomState,
                StructureManager structureManager,
                BiomeManager biomeManager,
                WorldGenRegion carverBiomeRegion,
                Set<Holder<Biome>> possibleBiomes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getSeaLevel() {
            return 0;
        }

        @Override
        public int getMinY() {
            return END.getMinY();
        }

        @Override
        public NoiseColumn getBaseColumn(
                int x, int z, LevelHeightAccessor heightAccessor, RandomState randomState) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addDebugScreenInfo(
                List<String> result,
                RandomState randomState,
                BlockPos feetPos,
                SamplerContext samplerContext) {
            throw new UnsupportedOperationException();
        }
    }

    static void set(Class<?> owner, Object target, String name, Object value) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** An instance whose constructor never ran, for classes the tests read no field of. */
    static <T> T allocate(Class<T> type) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
            return type.cast(allocateInstance.invoke(field.get(null), type));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
