package com.factoryos.modules.telemetry.gateway;

public class ConnectorConfig {

    private String endpointUri;
    private long pollIntervalMs = 1000L; // Default 1 second, allowed 100ms - 5000ms
    private long timeoutMs = 3000L;
    private long initialBackoffMs = 500L;
    private double backoffMultiplier = 2.0;
    private long maxBackoffMs = 30000L;
    private int maxRetries = 5;

    public ConnectorConfig() {
    }

    public ConnectorConfig(String endpointUri, long pollIntervalMs) {
        this.endpointUri = endpointUri;
        setPollIntervalMs(pollIntervalMs);
    }

    public String getEndpointUri() {
        return endpointUri;
    }

    public void setEndpointUri(String endpointUri) {
        this.endpointUri = endpointUri;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        if (pollIntervalMs < 100L) {
            this.pollIntervalMs = 100L;
        } else if (pollIntervalMs > 5000L) {
            this.pollIntervalMs = 5000L;
        } else {
            this.pollIntervalMs = pollIntervalMs;
        }
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public void setInitialBackoffMs(long initialBackoffMs) {
        this.initialBackoffMs = initialBackoffMs;
    }

    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }

    public void setMaxBackoffMs(long maxBackoffMs) {
        this.maxBackoffMs = maxBackoffMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}
