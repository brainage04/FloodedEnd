package io.github.brainage04.floodedend.mixin;

import io.github.brainage04.floodedend.EndOceanPreset;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Upgrades a FloodedEnd preset written by an older Minecraft version before the world's data packs are read. The
 * dedicated server and the client's world loading both build a world's pack repository here.
 */
@Mixin(ServerPacksSource.class)
public class ServerPacksSourceMixin {
	@Inject(
			method = "createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;",
			at = @At("HEAD")
	)
	private static void floodedend$upgradeStalePreset(LevelStorageSource.LevelStorageAccess access,
			CallbackInfoReturnable<PackRepository> cir) {
		EndOceanPreset.upgradeStaleOnLoad(access.getLevelPath(LevelResource.DATAPACK_DIR));
	}
}
