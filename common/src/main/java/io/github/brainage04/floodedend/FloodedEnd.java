package io.github.brainage04.floodedend;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FloodedEnd {
	public static final String MOD_ID = "floodedend";
	public static final String MOD_NAME = "FloodedEnd";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

	private FloodedEnd() {
	}

	public static void initialize() {
		LOGGER.info("{} initialised: the End generates with its ocean restored.", MOD_NAME);
	}

	public static Identifier of(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
