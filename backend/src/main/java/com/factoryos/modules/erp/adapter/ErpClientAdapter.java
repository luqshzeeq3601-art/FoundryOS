package com.factoryos.modules.erp.adapter;

import com.factoryos.modules.erp.domain.ErpConnector;
import com.factoryos.modules.erp.domain.ErpOrderConfirmation;
import com.factoryos.modules.erp.domain.ErpType;
import com.factoryos.modules.erp.dto.InboundErpOrderDto;

import java.time.Instant;
import java.util.List;

public interface ErpClientAdapter {

    ErpType getSupportedType();

    boolean testConnection(ErpConnector connector);

    List<InboundErpOrderDto> fetchReleasedOrders(ErpConnector connector, Instant since);

    ErpConfirmationResult postConfirmation(ErpConnector connector, ErpOrderConfirmation confirmation);

    class ErpConfirmationResult {
        private final boolean success;
        private final String erpDocumentNumber;
        private final String responsePayload;
        private final String errorMessage;

        public ErpConfirmationResult(boolean success, String erpDocumentNumber, String responsePayload, String errorMessage) {
            this.success = success;
            this.erpDocumentNumber = erpDocumentNumber;
            this.responsePayload = responsePayload;
            this.errorMessage = errorMessage;
        }

        public static ErpConfirmationResult success(String documentNumber, String response) {
            return new ErpConfirmationResult(true, documentNumber, response, null);
        }

        public static ErpConfirmationResult failure(String errorMessage, String response) {
            return new ErpConfirmationResult(false, null, response, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErpDocumentNumber() {
            return erpDocumentNumber;
        }

        public String getResponsePayload() {
            return responsePayload;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}