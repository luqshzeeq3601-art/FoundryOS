package com.factoryos.modules.erp;

import com.factoryos.modules.erp.adapter.MockErpClientAdapter;
import com.factoryos.modules.erp.application.ErpSyncService;
import com.factoryos.modules.erp.domain.*;
import com.factoryos.modules.erp.dto.CreateErpConnectorRequest;
import com.factoryos.modules.erp.dto.ErpConnectorDto;
import com.factoryos.modules.erp.dto.ErpOrderConfirmationDto;
import com.factoryos.modules.erp.repository.ErpConnectorRepository;
import com.factoryos.modules.erp.repository.ErpOrderConfirmationRepository;
import com.factoryos.modules.erp.repository.ErpSyncLogRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpSyncServiceTest {

    @Mock
    private ErpConnectorRepository connectorRepository;
    @Mock
    private ErpOrderConfirmationRepository confirmationRepository;
    @Mock
    private ErpSyncLogRepository syncLogRepository;
    @Mock
    private ProductionOrderRepository productionOrderRepository;
    @Mock
    private PlantRepository plantRepository;

    private ErpSyncService erpSyncService;
    private MockErpClientAdapter mockAdapter;

    @BeforeEach
    void setUp() {
        mockAdapter = new MockErpClientAdapter();
        erpSyncService = new ErpSyncService(
                connectorRepository,
                confirmationRepository,
                syncLogRepository,
                productionOrderRepository,
                plantRepository,
                List.of(mockAdapter)
        );
    }

    @Test
    @DisplayName("Should create connector and test healthy connection")
    void testCreateAndTestConnector() {
        UUID connectorId = UUID.randomUUID();
        ErpConnector connector = new ErpConnector();
        connector.setId(connectorId);
        connector.setName("Austin SAP Hub");
        connector.setErpType(ErpType.MOCK_ERP);
        connector.setBaseUrl("https://sap-mock.austin.factoryos.io");
        connector.setActive(true);

        when(connectorRepository.save(any(ErpConnector.class))).thenReturn(connector);
        when(connectorRepository.findByIdAndIsDeletedFalse(connectorId)).thenReturn(Optional.of(connector));

        CreateErpConnectorRequest req = new CreateErpConnectorRequest();
        req.setName("Austin SAP Hub");
        req.setErpType(ErpType.MOCK_ERP);
        req.setBaseUrl("https://sap-mock.austin.factoryos.io");

        ErpConnectorDto dto = erpSyncService.createConnector(req);
        assertThat(dto.getName()).isEqualTo("Austin SAP Hub");

        boolean testResult = erpSyncService.testConnector(connectorId);
        assertThat(testResult).isTrue();
        assertThat(connector.getHealthStatus()).isEqualTo("HEALTHY");
    }

    @Test
    @DisplayName("Should poll released orders and map to ProductionOrder")
    void testPollReleasedOrders() {
        UUID connectorId = UUID.randomUUID();
        Plant plant = new Plant();
        plant.setId(UUID.randomUUID());
        plant.setCode("PLANT-AUS");
        plant.setName("Austin Gigafactory");

        ErpConnector connector = new ErpConnector();
        connector.setId(connectorId);
        connector.setName("Austin SAP Hub");
        connector.setErpType(ErpType.MOCK_ERP);
        connector.setPlant(plant);
        connector.setActive(true);

        when(connectorRepository.findByIdAndIsDeletedFalse(connectorId)).thenReturn(Optional.of(connector));
        when(productionOrderRepository.findByErpOrderIdAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(productionOrderRepository.save(any(ProductionOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<ProductionOrder> orders = erpSyncService.pollReleasedOrders(connectorId);

        assertThat(orders).isNotEmpty();
        ProductionOrder order = orders.get(0);
        assertThat(order.getErpOrderId()).isEqualTo("SAP-1008492");
        assertThat(order.getProductCode()).isEqualTo("SPINDLE-ROT-V2");
        assertThat(order.getPlant()).isEqualTo(plant);
        assertThat(order.getErpSyncStatus()).isEqualTo(ErpSyncStatus.SYNCED.name());
    }

    @Test
    @DisplayName("Should submit outbound order confirmation back to ERP")
    void testSubmitOrderConfirmation() {
        UUID orderId = UUID.randomUUID();
        Plant plant = new Plant();
        plant.setId(UUID.randomUUID());
        plant.setCode("PLANT-AUS");

        ProductionOrder po = new ProductionOrder();
        po.setId(orderId);
        po.setOrderNumber("PO-SAP-1008492");
        po.setErpOrderId("SAP-1008492");
        po.setErpSystem("MOCK_ERP");
        po.setPlant(plant);

        ErpConnector connector = new ErpConnector();
        connector.setId(UUID.randomUUID());
        connector.setErpType(ErpType.MOCK_ERP);
        connector.setActive(true);
        connector.setPlant(plant);

        when(productionOrderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(Optional.of(po));
        when(connectorRepository.findByPlantIdAndIsDeletedFalse(plant.getId())).thenReturn(List.of(connector));
        when(confirmationRepository.save(any(ErpOrderConfirmation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ErpOrderConfirmationDto confDto = erpSyncService.submitOrderConfirmation(
                orderId,
                240,
                10,
                "DIMENSION_OUT_OF_TOLERANCE",
                4.5,
                3.8
        );

        assertThat(confDto.getErpPostingStatus()).isEqualTo("POSTED");
        assertThat(confDto.getErpDocumentNumber()).startsWith("MOCK-DOC-");
        assertThat(po.getErpSyncStatus()).isEqualTo(ErpSyncStatus.CONFIRMED.name());
    }
}
