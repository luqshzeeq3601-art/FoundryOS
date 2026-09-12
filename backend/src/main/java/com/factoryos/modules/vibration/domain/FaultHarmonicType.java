package com.factoryos.modules.vibration.domain;

public enum FaultHarmonicType {
    NORMAL("Normal Kinematics", "No abnormal mechanical or electrical vibration patterns detected."),
    UNBALANCE_1X("1X Dynamic Rotor Unbalance", "High 1X rotational frequency peak, indicating rotor mass unbalance."),
    MISALIGNMENT_2X("2X Shaft / Coupling Misalignment", "Strong 2X running speed harmonic, characteristic of angular or parallel coupling misalignment."),
    LOOSENESS_3X("3X Mechanical Looseness", "3X running harmonic or sub-harmonic floor elevation, indicating loose mounting bolts or excessive bearing clearance."),
    BPFO_BEARING_OUTER("BPFO Bearing Outer Race Defect", "Ball Pass Frequency Outer race defect signature with impact modulating sidebands."),
    BPFI_BEARING_INNER("BPFI Bearing Inner Race Defect", "Ball Pass Frequency Inner race defect signature, modulating at shaft running speed."),
    BSF_BALL_SPIN("BSF Rolling Element Defect", "Ball Spin Frequency defect, indicating spalling on rolling elements."),
    HIGH_FREQUENCY_NOISE("High-Frequency Friction / Cavitation", "Broadband high-frequency energy elevation (>2 kHz), typical of lubrication failure or hydraulic cavitation.");

    private final String title;
    private final String description;

    FaultHarmonicType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
