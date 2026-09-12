package com.factoryos.modules.erp.adapter;

import com.factoryos.modules.erp.domain.ErpConnector;
import com.factoryos.modules.erp.domain.ErpOrderConfirmation;
import com.factoryos.modules.erp.domain.ErpType;
import com.factoryos.modules.erp.dto.InboundErpOrderDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockErpClientAdapter implements ErpClientAdapter {

    private final Map<String, List<InboundErpOrderDto>> mockOrdersByPlant = new ConcurrentHashMap<>();

    public MockErpClientAdapter() {
        initMockOrders();
    }

    private void initMockOrders() {
        InboundErpOrderDto order1 = new InboundErpOrderDto();
        order1.setErpOrderId("SAP-1008492");
        order1.setOrderNumber("PO-SAP-1008492");
        order1.setPlantCode("PLANT-AUS");
        order1.setProductCode("SPINDLE-ROT-V2");
        order1.setProductName("High-Speed CNC Spindle Assembly V2");
        order1.setTargetQuantity(new BigDecimal("250.00"));
        order1.setUnitOfMeasure("EA");
        order1.setBatchNumber("BATCH-2026-Q3-01");
        order1.setPriority("HIGH");

        InboundErpOrderDto order2 = new InboundErpOrderDto();
        order2.setErpOrderId("NS-WO-88402");
        order2.setOrderNumber("WO-NS-88402");
        order2.setPlantCode("PLANT-STU");
        order2.setProductCode("BEARING-CERAMIC-608");
        order2.setProductName("Hybrid Ceramic Ball Bearing 608-P4");
        order2.setTargetQuantity(new BigDecimal("500.00"));
        order2.setUnitOfMeasure("EA");
        order2.setBatchNumber("BATCH-STU-CER-44");
        order2.setPriority("CRITICAL");

        mockOrdersByPlant.put("PLANT-AUS", new ArrayList<>(List.of(order1)));
        mockOrdersByPlant.put("PLANT-STU", new ArrayList<>(List.of(order2)));
    }

    @Override
    public ErpType getSupportedType() {
        return ErpType.MOCK_ERP;
    }

    @Override
    public boolean testConnection(ErpConnector connector) {
        return connector.isActive();
    }

    @Override
    public List<InboundErpOrderDto> fetchReleasedOrders(ErpConnector connector, Instant since) {
        String plantCode = connector.getPlant() != null ? connector.getPlant().getCode() : "PLANT-AUS";
        return mockOrdersByPlant.getOrDefault(plantCode, Collections.emptyList());
    }

    @Override
    public ErpConfirmationResult postConfirmation(ErpConnector connector, ErpOrderConfirmation confirmation) {
        String docNum = "MOCK-DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String payload = String.format("{\"status\":\"CONFIRMED\",\"docNumber\":\"%s\",\"erpOrderId\":\"%s\",\"yield\":%d,\"scrap\":%d}",
                docNum, confirmation.getErpOrderId(), confirmation.getConfirmedGoodQty(), confirmation.getConfirmedScrapQty());
        return ErpConfirmationResult.success(docNum, payload);
    }
}
