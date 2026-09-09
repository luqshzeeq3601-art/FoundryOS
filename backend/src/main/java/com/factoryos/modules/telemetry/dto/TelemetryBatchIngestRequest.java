package com.factoryos.modules.telemetry.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public class TelemetryBatchIngestRequest {

    @NotNull(message = "Machine ID is required")
    private UUID machineId;

    private String gatewayId;

    @NotEmpty(message = "Telemetry points list cannot be empty")
    @Valid
    private List<TelemetryPointDto> points;

    public TelemetryBatchIngestRequest() {
    }

    public TelemetryBatchIngestRequest(UUID machineId, String gatewayId, List<TelemetryPointDto> points) {
        this.machineId = machineId;
        this.gatewayId = gatewayId;
        this.points = points;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getGatewayId() {
        return gatewayId;
    }

    public void setGatewayId(String gatewayId) {
        this.gatewayId = gatewayId;
    }

    public List<TelemetryPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<TelemetryPointDto> points) {
        this.points = points;
    }
}
