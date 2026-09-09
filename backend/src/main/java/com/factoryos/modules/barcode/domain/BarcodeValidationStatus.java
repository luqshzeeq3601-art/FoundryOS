package com.factoryos.modules.barcode.domain;

public enum BarcodeValidationStatus {
    VALID,
    INVALID_BOM,
    NOT_FOUND,
    EXPIRED,
    QUARANTINED,
    UNAUTHORIZED,
    ERROR
}
