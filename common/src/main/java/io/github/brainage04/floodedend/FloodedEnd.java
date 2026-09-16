package io.github.brainage04.floodedend;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared identity and lifecycle for the flooded End world generation.
 *
 * <p>The mod restores the End's ocean: {@code minecraft:end} generates with water as its default fluid and a
 * positive sea level, so the dimension's basins, hollows and the void between its islands fill with water and the
 * main island and outer islands rise out of a global End sea.</p>
 */
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
