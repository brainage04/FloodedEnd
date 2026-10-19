package io.github.brainage04.floodedend;

import net.minecraft.gametest.framework.GameTestHelper;

public final class FloodedEndNeoForgeGameTests {
	public void presetInstallsAnOcean(GameTestHelper helper) {
		FloodedEndAssertions.assertPresetInstallsAnOcean(helper);
	}

	public void commandsRespectPermissionsAndReportErrors(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertCommandsRespectPermissionsAndReportErrors(helper);
	}

	public void generatedOceanAndProbe(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertGeneratedOceanAndProbe(helper);
	}

	public void probeHandlesDryAndCappedColumns(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertProbeHandlesDryAndCappedColumns(helper);
	}

	public void shipsMoorAtBothWaterlines(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertShipsMoorAtBothWaterlines(helper);
	}

	public void cityGenerationPreservesDryVanilla(GameTestHelper helper) {
		FloodedEndBehaviorAssertions.assertCityGenerationPreservesDryVanilla(helper);
	}
}
