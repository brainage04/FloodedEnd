package io.github.brainage04.floodedend.mixin;

import io.github.brainage04.floodedend.EndIslandStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.EndIslandFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps a small End island out of the structures already placed around its chunk, the moored ships
 * included. See {@link EndIslandStructures} for why the island can reach them at all.
 */
@Mixin(EndIslandFeature.class)
public class EndIslandFeatureMixin {
    @Unique
    private static final ThreadLocal<EndIslandStructures.KeepOut> floodedend$keepOut =
            new ThreadLocal<>();

    @Inject(method = "place", at = @At("HEAD"))
    private void floodedend$readStructures(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {
        floodedend$keepOut.set(EndIslandStructures.keepOut(level, origin));
    }

    @Inject(method = "place", at = @At("RETURN"))
    private void floodedend$forgetStructures(
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {
        floodedend$keepOut.remove();
    }

    @Redirect(
            method = "place",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/world/level/levelgen/feature/EndIslandFeature;setBlock(Lnet/minecraft/world/level/LevelWriter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void floodedend$keepStructures(
            EndIslandFeature feature, LevelWriter writer, BlockPos pos, BlockState state) {
        EndIslandStructures.KeepOut keepOut = floodedend$keepOut.get();
        if (keepOut != null && keepOut.blocks(pos)) {
            return;
        }

        feature.setBlock(writer, pos, state);
    }
}
