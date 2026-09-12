package com.factoryos.modules.edge.dto;

public class EdgeGatewayHeartbeatRequestDto {

    private String status = "ONLINE";
    private int bufferedRecordCount = 0;
    private String firmwareVersion;
    private String ipAddress;

    public EdgeGatewayHeartbeatRequestDto() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getBufferedRecordCount() {
        return bufferedRecordCount;
    }

    public void setBufferedRecordCount(int bufferedRecordCount) {
        this.bufferedRecordCount = bufferedRecordCount;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
