package com.factoryos.modules.erp.adapter;

import com.factoryos.modules.erp.domain.ErpConnector;
import com.factoryos.modules.erp.domain.ErpOrderConfirmation;
import com.factoryos.modules.erp.domain.ErpType;
import com.factoryos.modules.erp.dto.InboundErpOrderDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Component
public class SapS4HanaODataAdapter implements ErpClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(SapS4HanaODataAdapter.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SapS4HanaODataAdapter() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public SapS4HanaODataAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ErpType getSupportedType() {
        return ErpType.SAP_S4HANA;
    }

    @Override
    public boolean testConnection(ErpConnector connector) {
        try {
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String testUrl = baseUrl + "/sap/opu/odata4/sap/api_productionorder/srvd_a2x/sap/productionorder/0001/$metadata";
            HttpHeaders headers = createHeaders(connector);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("SAP S/4HANA connection test failed for connector {}: {}", connector.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<InboundErpOrderDto> fetchReleasedOrders(ErpConnector connector, Instant since) {
        List<InboundErpOrderDto> orders = new ArrayList<>();
        try {
            String plantCode = connector.getPlant() != null ? connector.getPlant().getCode() : "1000";
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String queryUrl = baseUrl + "/sap/opu/odata4/sap/api_productionorder/srvd_a2x/sap/productionorder/0001/A_ProductionOrder2"
                    + "?$filter=ManufacturingOrderCategory eq '10' and ProductionPlant eq '" + plantCode + "'";
            if (since != null) {
                queryUrl += " and LastChangeDateTime ge " + since.toString();
            }

            HttpHeaders headers = createHeaders(connector);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(queryUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode valueArray = root.has("value") ? root.get("value") : (root.has("d") && root.get("d").has("results") ? root.get("d").get("results") : null);
                if (valueArray != null && valueArray.isArray()) {
                    for (JsonNode node : valueArray) {
                        InboundErpOrderDto dto = new InboundErpOrderDto();
                        dto.setErpOrderId(node.path("ManufacturingOrder").asText(node.path("ProductionOrder").asText("")));
                        dto.setOrderNumber("PO-SAP-" + dto.getErpOrderId());
                        dto.setPlantCode(node.path("ProductionPlant").asText(plantCode));
                        dto.setProductCode(node.path("Material").asText("MAT-DEFAULT"));
                        dto.setProductName(node.path("MaterialDescription").asText(dto.getProductCode()));
                        dto.setTargetQuantity(new BigDecimal(node.path("TotalQuantity").asText("100.00")));
                        dto.setUnitOfMeasure(node.path("ProductionUnit").asText("EA"));
                        dto.setBatchNumber(node.path("Batch").asText(null));
                        dto.setPriority(mapSapPriority(node.path("ManufacturingOrderImportance").asText("3")));
                        orders.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch released orders from SAP S/4HANA for connector {}: {}", connector.getId(), e.getMessage());
        }
        return orders;
    }

    @Override
    public ErpConfirmationResult postConfirmation(ErpConnector connector, ErpOrderConfirmation confirmation) {
        try {
            String plantCode = connector.getPlant() != null ? connector.getPlant().getCode() : "1000";
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String postUrl = baseUrl + "/sap/opu/odata4/sap/api_productionorderconfirmation/srvd_a2x/sap/productionorderconfirmation/0001/A_ProductionOrderConfirmation";

            Map<String, Object> payload = new HashMap<>();
            payload.put("ManufacturingOrder", confirmation.getErpOrderId());
            payload.put("Plant", plantCode);
            payload.put("YieldQuantity", confirmation.getConfirmedGoodQty());
            payload.put("ScrapQuantity", confirmation.getConfirmedScrapQty());
            if (confirmation.getScrapReason() != null) {
                payload.put("ScrapReasonCode", confirmation.getScrapReason());
            }
            payload.put("ActualLaborHours", confirmation.getLaborHours());
            payload.put("ActualMachineHours", confirmation.getMachineHours());
            payload.put("PostingDate", Instant.now().toString());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = createHeaders(connector);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String docNum = root.path("ConfirmationGroup").asText(root.path("ManufacturingOrderConfirmation").asText("SAP-CONF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()));
                return ErpConfirmationResult.success(docNum, response.getBody());
            } else {
                return ErpConfirmationResult.failure("HTTP " + response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to post confirmation to SAP S/4HANA: {}", e.getMessage());
            return ErpConfirmationResult.failure(e.getMessage(), null);
        }
    }

    private HttpHeaders createHeaders(ErpConnector connector) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (connector.getSecretOrToken() != null && !connector.getSecretOrToken().isBlank()) {
            headers.set("Authorization", "Bearer " + connector.getSecretOrToken());
        } else if (connector.getApiKeyOrUser() != null && connector.getSecretOrToken() != null) {
            String auth = connector.getApiKeyOrUser() + ":" + connector.getSecretOrToken();
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
            headers.set("Authorization", "Basic " + new String(encodedAuth));
        }
        return headers;
    }

    private String mapSapPriority(String priority) {
        return switch (priority) {
            case "1" -> "CRITICAL";
            case "2" -> "HIGH";
            case "4" -> "LOW";
            default -> "MEDIUM";
        };
    }
}
