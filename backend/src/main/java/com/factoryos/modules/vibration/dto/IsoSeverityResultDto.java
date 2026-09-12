package com.factoryos.modules.vibration.dto;

import com.factoryos.modules.vibration.domain.IsoSeverityZone;
import com.factoryos.modules.vibration.domain.MachineVibrationClass;

public class IsoSeverityResultDto {

    private IsoSeverityZone zone;
    private String zoneTitle;
    private String zoneDescription;
    private MachineVibrationClass vibrationClass;
    private double rmsVelocityMmS;
    private double zoneABoundary;
    private double zoneBBoundary;
    private double zoneCBoundary;
    private boolean isAlert;
    private boolean isCritical;

    public IsoSeverityResultDto() {
    }

    public IsoSeverityResultDto(IsoSeverityZone zone, MachineVibrationClass vibrationClass, double rmsVelocityMmS) {
        this.zone = zone;
        this.zoneTitle = zone.getTitle();
        this.zoneDescription = zone.getDescription();
        this.vibrationClass = vibrationClass;
        this.rmsVelocityMmS = rmsVelocityMmS;
        this.zoneABoundary = vibrationClass.getZoneABoundary();
        this.zoneBBoundary = vibrationClass.getZoneBBoundary();
        this.zoneCBoundary = vibrationClass.getZoneCBoundary();
        this.isAlert = (zone == IsoSeverityZone.ZONE_C || zone == IsoSeverityZone.ZONE_D);
        this.isCritical = (zone == IsoSeverityZone.ZONE_D);
    }

    public IsoSeverityZone getZone() {
        return zone;
    }

    public void setZone(IsoSeverityZone zone) {
        this.zone = zone;
    }

    public String getZoneTitle() {
        return zoneTitle;
    }

    public void setZoneTitle(String zoneTitle) {
        this.zoneTitle = zoneTitle;
    }

    public String getZoneDescription() {
        return zoneDescription;
    }

    public void setZoneDescription(String zoneDescription) {
        this.zoneDescription = zoneDescription;
    }

    public MachineVibrationClass getVibrationClass() {
        return vibrationClass;
    }

    public void setVibrationClass(MachineVibrationClass vibrationClass) {
        this.vibrationClass = vibrationClass;
    }

    public double getRmsVelocityMmS() {
        return rmsVelocityMmS;
    }

    public void setRmsVelocityMmS(double rmsVelocityMmS) {
        this.rmsVelocityMmS = rmsVelocityMmS;
    }

    public double getZoneABoundary() {
        return zoneABoundary;
    }

    public void setZoneABoundary(double zoneABoundary) {
        this.zoneABoundary = zoneABoundary;
    }

    public double getZoneBBoundary() {
        return zoneBBoundary;
    }

    public void setZoneBBoundary(double zoneBBoundary) {
        this.zoneBBoundary = zoneBBoundary;
    }

    public double getZoneCBoundary() {
        return zoneCBoundary;
    }

    public void setZoneCBoundary(double zoneCBoundary) {
        this.zoneCBoundary = zoneCBoundary;
    }

    public boolean isAlert() {
        return isAlert;
    }

    public void setAlert(boolean alert) {
        isAlert = alert;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public void setCritical(boolean critical) {
        isCritical = critical;
    }
}
