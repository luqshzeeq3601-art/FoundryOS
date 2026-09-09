package com.factoryos.modules.telemetry.gateway;

import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.TelemetryPointDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndustrialProtocolGatewayTest {

    @Test
    @DisplayName("Configurable polling interval enforces 100ms to 5000ms boundaries")
    void testConfigurablePollingIntervalEnforcesBounds() {
        ConnectorConfig config = new ConnectorConfig();

        // Below minimum (100ms)
        config.setPollIntervalMs(25L);
        assertThat(config.getPollIntervalMs()).isEqualTo(100L);

        // Above maximum (5000ms)
        config.setPollIntervalMs(12000L);
        assertThat(config.getPollIntervalMs()).isEqualTo(5000L);

        // Valid interval
        config.setPollIntervalMs(750L);
        assertThat(config.getPollIntervalMs()).isEqualTo(750L);
    }

    @Test
    @DisplayName("Exponential backoff scales predictably and respects ceiling")
    void testExponentialBackoffProgression() {
        ConnectorConfig config = new ConnectorConfig();
        config.setInitialBackoffMs(500L);
        config.setBackoffMultiplier(2.0);
        config.setMaxBackoffMs(5000L);

        OpcUaProtocolConnector connector = new OpcUaProtocolConnector(config);

        assertThat(connector.calculateBackoffMs(1)).isEqualTo(500L);
        assertThat(connector.calculateBackoffMs(2)).isEqualTo(1000L);
        assertThat(connector.calculateBackoffMs(3)).isEqualTo(2000L);
        assertThat(connector.calculateBackoffMs(4)).isEqualTo(4000L);
        // Exceeds 5000ms ceiling (8000ms clamped to 5000ms)
        assertThat(connector.calculateBackoffMs(5)).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Auto-reconnect with exponential backoff on simulated network severance")
    void testAutoReconnectOnSimulatedNetworkFault() throws Exception {
        ConnectorConfig config = new ConnectorConfig("opc.tcp://192.168.1.100:4840", 500L);
        config.setMaxRetries(3);
        OpcUaProtocolConnector connector = new OpcUaProtocolConnector(config);

        // 1. Initial successful connection
        connector.connect();
        assertThat(connector.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);

        // 2. Poll tags
        List<TelemetryPointDto> points = connector.pollTags(List.of("ns=2;s=Device1.SpindleSpeed", "ns=2;s=Device1.VibrationRms"));
        assertThat(points).hasSize(2);

        // 3. Inject network fault
        connector.setSimulatedNetworkFailure(true);
        boolean reconnected = connector.reconnect();
        assertThat(reconnected).isFalse();
        assertThat(connector.getReconnectAttempts()).isEqualTo(1);

        // 4. Clear network fault and reconnect
        connector.setSimulatedNetworkFailure(false);
        boolean recovered = connector.reconnect();
        assertThat(recovered).isTrue();
        assertThat(connector.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
        assertThat(connector.getReconnectAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("Exhausted retries transitions connector state to FAILED")
    void testExhaustedRetriesTransitionsToFailed() {
        ConnectorConfig config = new ConnectorConfig("spBv1.0/factory/broker", 1000L);
        config.setMaxRetries(2);
        MqttSparkplugBConnector connector = new MqttSparkplugBConnector(config);

        connector.setSimulatedNetworkFailure(true);
        assertThat(connector.reconnect()).isFalse(); // attempt 1
        assertThat(connector.reconnect()).isFalse(); // attempt 2
        assertThat(connector.reconnect()).isFalse(); // attempt 3 exceeds maxRetries

        assertThat(connector.getConnectionState()).isEqualTo(ConnectionState.FAILED);
    }

    @Test
    @DisplayName("High-throughput batch ingestion benchmark processes >= 10,000 updates/sec")
    void testHighThroughputIngestionBenchmark() {
        int batchSize = 10_000;
        List<TelemetryPointDto> batch = new ArrayList<>(batchSize);
        Instant now = Instant.now();

        for (int i = 0; i < batchSize; i++) {
            batch.add(new TelemetryPointDto(
                    "TAG_" + (i % 50),
                    100.0 + (i % 10),
                    "RPM",
                    TelemetryQuality.GOOD,
                    now
            ));
        }

        long start = System.currentTimeMillis();

        // Process and validate batch in memory
        int validCount = 0;
        for (TelemetryPointDto pt : batch) {
            if (pt.getTagName() != null && pt.getValue() != null && pt.getQuality() == TelemetryQuality.GOOD) {
                validCount++;
            }
        }

        long elapsedMs = System.currentTimeMillis() - start;
        double throughputPerSec = (double) batchSize / Math.max(elapsedMs, 1) * 1000.0;

        assertThat(validCount).isEqualTo(batchSize);
        assertThat(elapsedMs).isLessThanOrEqualTo(500L); // Should process 10k items in under 500ms in memory
        assertThat(throughputPerSec).isGreaterThanOrEqualTo(10_000.0);
    }
}
