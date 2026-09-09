package com.factoryos.modules.downtime.dto;

import java.util.UUID;

public class AutomatedEvaluationResultDto {
    private UUID machineId;
    private String actionTaken;
    private UUID downtimeId;
    private String message;

    public AutomatedEvaluationResultDto() {
    }

    public AutomatedEvaluationResultDto(UUID machineId, String actionTaken, UUID downtimeId, String message) {
        this.machineId = machineId;
        this.actionTaken = actionTaken;
        this.downtimeId = downtimeId;
        this.message = message;
    }

    public UUID getMachineId() {
        return machineId;
    }

    public void setMachineId(UUID machineId) {
        this.machineId = machineId;
    }

    public String getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }

    public UUID getDowntimeId() {
        return downtimeId;
    }

    public void setDowntimeId(UUID downtimeId) {
        this.downtimeId = downtimeId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
