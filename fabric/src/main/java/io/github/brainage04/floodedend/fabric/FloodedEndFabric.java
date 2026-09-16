package io.github.brainage04.floodedend.fabric;

import io.github.brainage04.floodedend.EndCityAtSea;
import io.github.brainage04.floodedend.FloodedEnd;
import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FloodedEndFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FloodedEnd.initialize();
		Registry.register(BuiltInRegistries.STRUCTURE_TYPE, FloodedEnd.of(EndCityAtSea.NAME), EndCityAtSea.TYPE);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				FloodedEndCommand.register(dispatcher));
	}
}
