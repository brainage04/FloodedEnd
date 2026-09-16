package io.github.brainage04.floodedend.fabric;

import io.github.brainage04.floodedend.FloodedEnd;
import io.github.brainage04.floodedend.command.FloodedEndCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public final class FloodedEndFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FloodedEnd.initialize();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				FloodedEndCommand.register(dispatcher));
	}
}
