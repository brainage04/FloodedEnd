package io.github.brainage04.floodedend;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FloodedEndGameTest {
	@GameTest
	public void endGeneratesItsOcean(GameTestHelper helper) {
		FloodedEndAssertions.assertEndIsFlooded(helper);
	}

	@GameTest
	public void floodReadbackCommandIsRegistered(GameTestHelper helper) {
		FloodedEndAssertions.assertCommandIsRegistered(helper);
	}

	@GameTest
	public void endCityIsFloodedEnds(GameTestHelper helper) {
		FloodedEndAssertions.assertEndCityIsFloodedEnds(helper);
	}

	@GameTest
	public void endShipTemplateResolves(GameTestHelper helper) {
		FloodedEndAssertions.assertEndShipTemplateResolves(helper);
	}

}
