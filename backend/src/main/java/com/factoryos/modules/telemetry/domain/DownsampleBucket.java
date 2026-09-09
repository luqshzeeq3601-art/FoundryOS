package com.factoryos.modules.telemetry.domain;

public enum DownsampleBucket {
    ONE_SECOND("1s"),
    ONE_MINUTE("1m"),
    ONE_HOUR("1h");

    private final String code;

    DownsampleBucket(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static DownsampleBucket fromCode(String code) {
        if (code == null || code.isBlank()) return null;
        for (DownsampleBucket b : values()) {
            if (b.code.equalsIgnoreCase(code.trim()) || b.name().equalsIgnoreCase(code.trim())) {
                return b;
            }
        }
        return ONE_MINUTE;
    }
}
