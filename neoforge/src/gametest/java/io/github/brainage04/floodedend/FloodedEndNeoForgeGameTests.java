package io.github.brainage04.floodedend;

import net.minecraft.gametest.framework.GameTestHelper;

public final class FloodedEndNeoForgeGameTests {
	public void endGeneratesItsOcean(GameTestHelper helper) {
		FloodedEndAssertions.assertEndIsFlooded(helper);
	}

	public void floodReadbackCommandIsRegistered(GameTestHelper helper) {
		FloodedEndAssertions.assertCommandIsRegistered(helper);
	}

	public void endCityIsFloodedEnds(GameTestHelper helper) {
		FloodedEndAssertions.assertEndCityIsFloodedEnds(helper);
	}
}
