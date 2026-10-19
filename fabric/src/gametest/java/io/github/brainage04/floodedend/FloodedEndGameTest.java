package io.github.brainage04.floodedend;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public final class FloodedEndGameTest {
	@GameTest
	public void presetInstallsAnOcean(GameTestHelper helper) {
		FloodedEndAssertions.assertPresetInstallsAnOcean(helper);
	}

	@GameTest
	public void commandsRespectPermissionsAndReportErrors(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertCommandsRespectPermissionsAndReportErrors(helper);
	}

	@GameTest
	public void generatedOceanAndProbe(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertGeneratedOceanAndProbe(helper);
	}

	@GameTest
	public void probeHandlesDryAndCappedColumns(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertProbeHandlesDryAndCappedColumns(helper);
	}

	@GameTest
	public void shipsMoorAtBothWaterlines(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertShipsMoorAtBothWaterlines(helper);
	}

	@GameTest
	public void cityGenerationPreservesDryVanilla(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertCityGenerationPreservesDryVanilla(helper);
	}
}
