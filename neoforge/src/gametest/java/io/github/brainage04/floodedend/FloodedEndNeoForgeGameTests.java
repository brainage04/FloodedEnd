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

	public void endShipTemplateResolves(GameTestHelper helper) {
		FloodedEndAssertions.assertEndShipTemplateResolves(helper);
	}

	public void presetInstallsAnOcean(GameTestHelper helper) {
		FloodedEndAssertions.assertPresetInstallsAnOcean(helper);
	}
}
