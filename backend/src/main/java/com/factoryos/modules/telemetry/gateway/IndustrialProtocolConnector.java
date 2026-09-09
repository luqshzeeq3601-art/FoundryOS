package com.factoryos.modules.telemetry.gateway;

import com.factoryos.modules.telemetry.dto.TelemetryPointDto;
import java.util.List;

public interface IndustrialProtocolConnector {

    String getProtocolName();

    ConnectorConfig getConfig();

    ConnectionState getConnectionState();

    void connect() throws Exception;

    void disconnect();

    List<TelemetryPointDto> pollTags(List<String> tagAddresses) throws Exception;

    boolean reconnect();

    long calculateBackoffMs(int attempt);
}
