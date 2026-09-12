package com.factoryos.modules.edge.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EdgeSyncBatchRequestDto {

    @NotBlank(message = "Gateway code is required")
    private String gatewayCode;

    @NotBlank(message = "Batch ID is required")
    private String batchId;

    @NotNull(message = "Plant ID is required")
    private UUID plantId;

    private long sequenceStart;
    private long sequenceEnd;

    @NotNull(message = "Disconnected at timestamp is required")
    private Instant disconnectedAt;

    @NotNull(message = "Reconnected at timestamp is required")
    private Instant reconnectedAt;

    @NotEmpty(message = "Transaction list cannot be empty")
    @Valid
    private List<EdgeTransactionRecordDto> transactions = new ArrayList<>();

    public EdgeSyncBatchRequestDto() {
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public void setGatewayCode(String gatewayCode) {
        this.gatewayCode = gatewayCode;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public UUID getPlantId() {
        return plantId;
    }

    public void setPlantId(UUID plantId) {
        this.plantId = plantId;
    }

    public long getSequenceStart() {
        return sequenceStart;
    }

    public void setSequenceStart(long sequenceStart) {
        this.sequenceStart = sequenceStart;
    }

    public long getSequenceEnd() {
        return sequenceEnd;
    }

    public void setSequenceEnd(long sequenceEnd) {
        this.sequenceEnd = sequenceEnd;
    }

    public Instant getDisconnectedAt() {
        return disconnectedAt;
    }

    public void setDisconnectedAt(Instant disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    public Instant getReconnectedAt() {
        return reconnectedAt;
    }

    public void setReconnectedAt(Instant reconnectedAt) {
        this.reconnectedAt = reconnectedAt;
    }

    public List<EdgeTransactionRecordDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<EdgeTransactionRecordDto> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }
}
