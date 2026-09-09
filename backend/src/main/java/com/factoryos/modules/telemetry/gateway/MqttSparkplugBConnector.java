package com.factoryos.modules.telemetry.gateway;

import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.TelemetryPointDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MqttSparkplugBConnector extends AbstractProtocolConnector {

    private boolean simulatedNetworkFailure = false;

    public MqttSparkplugBConnector(ConnectorConfig config) {
        super(config);
    }

    @Override
    public String getProtocolName() {
        return "MQTT_SPARKPLUG_B";
    }

    @Override
    public synchronized void connect() throws Exception {
        if (simulatedNetworkFailure) {
            state = ConnectionState.FAILED;
            throw new RuntimeException("MQTT broker connection refused at " + config.getEndpointUri());
        }
        state = ConnectionState.CONNECTED;
        reconnectAttempts = 0;
        log.info("[MQTT_SPARKPLUG_B] Connected to broker: {}", config.getEndpointUri());
    }

    @Override
    public synchronized void disconnect() {
        state = ConnectionState.DISCONNECTED;
        log.info("[MQTT_SPARKPLUG_B] Disconnected from broker: {}", config.getEndpointUri());
    }

    @Override
    public List<TelemetryPointDto> pollTags(List<String> tagAddresses) throws Exception {
        if (state != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Cannot poll tags: MQTT connector is in state " + state);
        }
        if (simulatedNetworkFailure) {
            state = ConnectionState.DISCONNECTED;
            throw new RuntimeException("MQTT keepalive timeout severed connection");
        }

        List<TelemetryPointDto> points = new ArrayList<>();
        Instant now = Instant.now();

        for (String addr : tagAddresses) {
            String tagName = parseTopicMetric(addr);
            points.add(new TelemetryPointDto(tagName, 2200.0, "RPM", TelemetryQuality.GOOD, now));
        }

        return points;
    }

    public void setSimulatedNetworkFailure(boolean failure) {
        this.simulatedNetworkFailure = failure;
    }

    private String parseTopicMetric(String topic) {
        if (topic.contains("/")) {
            return topic.substring(topic.lastIndexOf('/') + 1).toUpperCase();
        }
        return topic.toUpperCase();
    }
}
