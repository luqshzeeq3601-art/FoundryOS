package com.factoryos.modules.telemetry.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TelemetryIngestResponse {

    private UUID machineId;
    private int ingestedCount;
    private String status;
    private Instant timestamp;
    private List<String> alerts;

    public TelemetryIngestResponse() {
        this.alerts = new ArrayList<>();
        this.timestamp = Instant.now();
    }

    public TelemetryIngestResponse(UUID machineId, int ingestedCount, String status, List<String> alerts) {
        this.machineId = machineId;
        this.ingestedCount = ingestedCount;
        this.status = status;
        this.timestamp = Instant.now();
        this.alerts = alerts != null ? alerts : new ArrayList<>();
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public int getIngestedCount() {
        return ingestedCount;
    }

    public void setIngestedCount(int ingestedCount) {
        this.ingestedCount = ingestedCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }
}
