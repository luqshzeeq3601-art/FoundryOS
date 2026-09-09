package com.factoryos.modules.telemetry.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractProtocolConnector implements IndustrialProtocolConnector {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final ConnectorConfig config;
    protected volatile ConnectionState state = ConnectionState.DISCONNECTED;
    protected int reconnectAttempts = 0;

    protected AbstractProtocolConnector(ConnectorConfig config) {
        this.config = config != null ? config : new ConnectorConfig();
    }

    @Override
    public ConnectorConfig getConfig() {
        return config;
    }

    @Override
    public ConnectionState getConnectionState() {
        return state;
    }

    @Override
    public long calculateBackoffMs(int attempt) {
        if (attempt <= 0) return config.getInitialBackoffMs();
        double backoff = config.getInitialBackoffMs() * Math.pow(config.getBackoffMultiplier(), attempt - 1);
        return Math.min((long) backoff, config.getMaxBackoffMs());
    }

    @Override
    public synchronized boolean reconnect() {
        if (reconnectAttempts >= config.getMaxRetries()) {
            state = ConnectionState.FAILED;
            log.error("[{}] Max reconnect retries ({}) exhausted. Connector marked FAILED.", getProtocolName(), config.getMaxRetries());
            return false;
        }

        reconnectAttempts++;
        state = ConnectionState.RECONNECTING;
        long delay = calculateBackoffMs(reconnectAttempts);
        log.warn("[{}] Attempting reconnection #{} after {}ms backoff delay...", getProtocolName(), reconnectAttempts, delay);

        try {
            connect();
            state = ConnectionState.CONNECTED;
            reconnectAttempts = 0;
            log.info("[{}] Successfully reconnected to endpoint: {}", getProtocolName(), config.getEndpointUri());
            return true;
        } catch (Exception e) {
            log.error("[{}] Reconnection attempt #{} failed: {}", getProtocolName(), reconnectAttempts, e.getMessage());
            return false;
        }
    }

    public void resetAttempts() {
        this.reconnectAttempts = 0;
    }

    public int getReconnectAttempts() {
        return reconnectAttempts;
    }
}
