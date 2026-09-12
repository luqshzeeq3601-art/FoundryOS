package com.factoryos.modules.sop.dto;

import com.factoryos.modules.sop.domain.GateStatus;
import jakarta.validation.constraints.NotNull;

public class QualitySignOffRequestDto {

    @NotNull(message = "Gate status is required")
    private GateStatus gateStatus; // PASSED or REJECTED / FAILED

    private String signOffComments;
    private String digitalSignatureStamp;

    public QualitySignOffRequestDto() {
    }

    public GateStatus getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(GateStatus gateStatus) {
        this.gateStatus = gateStatus;
    }

    public String getSignOffComments() {
        return signOffComments;
    }

    public void setSignOffComments(String signOffComments) {
        this.signOffComments = signOffComments;
    }

    public String getDigitalSignatureStamp() {
        return digitalSignatureStamp;
    }

    public void setDigitalSignatureStamp(String digitalSignatureStamp) {
        this.digitalSignatureStamp = digitalSignatureStamp;
    }
}
