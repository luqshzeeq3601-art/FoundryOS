package com.factoryos.modules.vibration.application;

import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineVibrationClass;
import com.factoryos.modules.vibration.dto.IsoSeverityResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Iso10816StandardsEngineTest {

    private Iso10816StandardsEngine isoEngine;

    @BeforeEach
    void setUp() {
        isoEngine = new Iso10816StandardsEngine();
    }

    @Test
    void evaluateSeverity_ClassIIMedium_CorrectlyCategorizesZones() {
        MachineVibrationClass mClass = MachineVibrationClass.CLASS_II_MEDIUM;
        // Boundaries: Zone A <= 1.12, Zone B <= 2.8, Zone C <= 7.1, Zone D > 7.1

        IsoSeverityResultDto resA = isoEngine.evaluateSeverity(0.85, mClass);
        assertEquals(IsoSeverityZone.ZONE_A, resA.getZone());
        assertFalse(resA.isAlert());
        assertFalse(resA.isCritical());

        IsoSeverityResultDto resB = isoEngine.evaluateSeverity(2.10, mClass);
        assertEquals(IsoSeverityZone.ZONE_B, resB.getZone());
        assertFalse(resB.isAlert());

        IsoSeverityResultDto resC = isoEngine.evaluateSeverity(4.50, mClass);
        assertEquals(IsoSeverityZone.ZONE_C, resC.getZone());
        assertTrue(resC.isAlert());
        assertFalse(resC.isCritical());

        IsoSeverityResultDto resD = isoEngine.evaluateSeverity(8.20, mClass);
        assertEquals(IsoSeverityZone.ZONE_D, resD.getZone());
        assertTrue(resD.isAlert());
        assertTrue(resD.isCritical());
    }

    @Test
    void evaluateSeverity_ClassIIILargeRigid_CorrectlyCategorizesZones() {
        MachineVibrationClass mClass = MachineVibrationClass.CLASS_III_LARGE_RIGID;
        // Boundaries: Zone A <= 1.80, Zone B <= 4.50, Zone C <= 11.20, Zone D > 11.20

        assertEquals(IsoSeverityZone.ZONE_A, isoEngine.evaluateSeverity(1.50, mClass).getZone());
        assertEquals(IsoSeverityZone.ZONE_B, isoEngine.evaluateSeverity(3.20, mClass).getZone());
        assertEquals(IsoSeverityZone.ZONE_C, isoEngine.evaluateSeverity(6.80, mClass).getZone());
        assertEquals(IsoSeverityZone.ZONE_D, isoEngine.evaluateSeverity(12.50, mClass).getZone());
    }
}
