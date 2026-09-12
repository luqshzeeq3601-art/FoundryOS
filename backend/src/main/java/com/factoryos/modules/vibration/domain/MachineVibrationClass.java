package com.factoryos.modules.vibration.domain;

public enum MachineVibrationClass {
    CLASS_I_SMALL("Class I: Small Machines (< 15 kW)", 0.71, 1.8, 4.5),
    CLASS_II_MEDIUM("Class II: Medium Machines (15 to 75 kW)", 1.12, 2.8, 7.1),
    CLASS_III_LARGE_RIGID("Class III: Large Machines (> 75 kW, Rigid Foundation)", 1.80, 4.5, 11.2),
    CLASS_IV_LARGE_FLEXIBLE("Class IV: Large Machines (> 75 kW, Flexible Foundation)", 2.80, 7.1, 18.0);

    private final String description;
    private final double zoneABoundary; // Max velocity RMS for Zone A (Good)
    private final double zoneBBoundary; // Max velocity RMS for Zone B (Satisfactory)
    private final double zoneCBoundary; // Max velocity RMS for Zone C (Unsatisfactory / Alert), > is Zone D (Unacceptable)

    MachineVibrationClass(String description, double zoneABoundary, double zoneBBoundary, double zoneCBoundary) {
        this.description = description;
        this.zoneABoundary = zoneABoundary;
        this.zoneBBoundary = zoneBBoundary;
        this.zoneCBoundary = zoneCBoundary;
    }

    public String getDescription() {
        return description;
    }

    public double getZoneABoundary() {
        return zoneABoundary;
    }

    public double getZoneBBoundary() {
        return zoneBBoundary;
    }

    public double getZoneCBoundary() {
        return zoneCBoundary;
    }

    public IsoSeverityZone evaluateZone(double rmsVelocityMmS) {
        if (rmsVelocityMmS <= zoneABoundary) {
            return IsoSeverityZone.ZONE_A;
        } else if (rmsVelocityMmS <= zoneBBoundary) {
            return IsoSeverityZone.ZONE_B;
        } else if (rmsVelocityMmS <= zoneCBoundary) {
            return IsoSeverityZone.ZONE_C;
        } else {
            return IsoSeverityZone.ZONE_D;
        }
    }
}
