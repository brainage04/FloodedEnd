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

    public void smallEndIslandsKeepOutOfShips(GameTestHelper helper) {
        FloodedEndBehaviorAssertions.assertSmallEndIslandsKeepOutOfShips(helper);
    }

    public void islandKeepsOutOfChunkWhoseStartIsBeyondTheScan(GameTestHelper helper) {
        FloodedEndBehaviorAssertions.assertIslandKeepsOutOfChunkWhoseStartIsBeyondTheScan(helper);
    }

    public void stalePresetIsUpgradedBeforeLoading(GameTestHelper helper) {
        FloodedEndAssertions.assertStalePresetIsUpgradedBeforeLoading(helper);
    }

    public void mainIslandStaysAboveTheWater(GameTestHelper helper) {
        FloodedEndBehaviorAssertions.assertMainIslandStaysAboveTheWater(helper);
    }

    public void endCityOutsideTheEndStaysVanilla(GameTestHelper helper) {
        FloodedEndGenerationAssertions.assertEndCityOutsideTheEndStaysVanilla(helper);
    }

    public void shippedEndSettingsFollowVanilla(GameTestHelper helper) {
        FloodedEndGenerationAssertions.assertShippedEndSettingsFollowVanilla(helper);
    }

    public void offsetIslandSkipsChunksTheRegionCannotServe(GameTestHelper helper) {
        EndIslandRegionAssertions.offsetIslandSkipsChunksTheRegionCannotServe(helper);
    }

    public void offsetIslandKeepsOutOfChunkWhoseStartTheRegionCannotServe(GameTestHelper helper) {
        EndIslandRegionAssertions.offsetIslandKeepsOutOfChunkWhoseStartTheRegionCannotServe(helper);
    }
}
