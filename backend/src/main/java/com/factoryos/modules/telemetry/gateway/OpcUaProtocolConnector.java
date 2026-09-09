package com.factoryos.modules.telemetry.gateway;

import com.factoryos.modules.telemetry.domain.TelemetryQuality;
import com.factoryos.modules.telemetry.dto.TelemetryPointDto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OpcUaProtocolConnector extends AbstractProtocolConnector {

    private boolean simulatedNetworkFailure = false;

    public OpcUaProtocolConnector(ConnectorConfig config) {
        super(config);
    }

    @Override
    public String getProtocolName() {
        return "OPC_UA";
    }

    @Override
    public synchronized void connect() throws Exception {
        if (simulatedNetworkFailure) {
            state = ConnectionState.FAILED;
            throw new RuntimeException("OPC-UA connection failed: endpoint unreachable at " + config.getEndpointUri());
        }
        state = ConnectionState.CONNECTED;
        reconnectAttempts = 0;
        log.info("[OPC_UA] Connected to server: {}", config.getEndpointUri());
    }

    @Override
    public synchronized void disconnect() {
        state = ConnectionState.DISCONNECTED;
        log.info("[OPC_UA] Disconnected from server: {}", config.getEndpointUri());
    }

    @Override
    public List<TelemetryPointDto> pollTags(List<String> tagAddresses) throws Exception {
        if (state != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Cannot poll tags: OPC-UA connector is in state " + state);
        }
        if (simulatedNetworkFailure) {
            state = ConnectionState.DISCONNECTED;
            throw new RuntimeException("OPC-UA socket severed during read");
        }

        List<TelemetryPointDto> points = new ArrayList<>();
        Instant now = Instant.now();

        for (String addr : tagAddresses) {
            // Extracts tag name from OPC node id (e.g. "ns=2;s=Device1.SpindleSpeed" -> "SPINDLE_SPEED")
            String tagName = deriveTagNameFromAddress(addr);
            double value = simulateMetricValue(tagName);
            String unit = deriveUnit(tagName);

            points.add(new TelemetryPointDto(tagName, value, unit, TelemetryQuality.GOOD, now));
        }

        return points;
    }

    public void setSimulatedNetworkFailure(boolean failure) {
        this.simulatedNetworkFailure = failure;
    }

    private String deriveTagNameFromAddress(String addr) {
        if (addr.contains(".")) {
            String sub = addr.substring(addr.lastIndexOf('.') + 1);
            return sub.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
        }
        return addr.toUpperCase();
    }

    private String deriveUnit(String tagName) {
        if (tagName.contains("SPEED")) return "RPM";
        if (tagName.contains("VIBRATION")) return "mm/s";
        if (tagName.contains("CURRENT")) return "A";
        if (tagName.contains("TEMP")) return "°C";
        return "";
    }

    private double simulateMetricValue(String tagName) {
        if (tagName.contains("SPEED")) return 8000.0;
        if (tagName.contains("VIBRATION")) return 1.8;
        if (tagName.contains("CURRENT")) return 20.5;
        if (tagName.contains("TEMP")) return 45.0;
        return 1.0;
    }
}
