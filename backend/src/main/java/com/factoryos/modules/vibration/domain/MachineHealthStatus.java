package com.factoryos.modules.vibration.domain;

public enum MachineHealthStatus {
    EXCELLENT(90, 100, "Optimal kinematic baseline. Zero significant defect harmonics."),
    GOOD(75, 89, "Acceptable performance with minimal wear or baseline noise."),
    FAIR_DEGRADED(60, 74, "Early degradation signature detected. Schedule routine inspection."),
    WARNING(40, 59, "Moderate fault signatures detected. Maintenance required within 48 hours."),
    CRITICAL(0, 39, "Severe vibration anomaly or bearing breakdown imminent. Trip / halt required.");

    private final int minScore;
    private final int maxScore;
    private final String description;

    MachineHealthStatus(int minScore, int maxScore, String description) {
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.description = description;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public String getDescription() {
        return description;
    }

    public static MachineHealthStatus fromScore(int score) {
        if (score >= 90) return EXCELLENT;
        if (score >= 75) return GOOD;
        if (score >= 60) return FAIR_DEGRADED;
        if (score >= 40) return WARNING;
        return CRITICAL;
    }
}
