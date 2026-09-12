package com.factoryos.modules.vibration.domain;

public enum IsoSeverityZone {
    ZONE_A("Good / Newly Commissioned", "Vibration of newly commissioned machines; fully unrestricted long-term continuous operation."),
    ZONE_B("Satisfactory", "Machines with vibration within this zone are considered acceptable for unrestricted long-term operation."),
    ZONE_C("Unsatisfactory / Warning", "Vibration is unsatisfactory for continuous operation. Machine may operate for a limited period until remedial maintenance."),
    ZONE_D("Unacceptable / Danger", "Vibration severity is sufficient to cause damage to the machine. Immediate shutdown or mitigation required.");

    private final String title;
    private final String description;

    IsoSeverityZone(String title, String description) {
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
