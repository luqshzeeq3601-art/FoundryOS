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
public class OracleNetSuiteRestAdapter implements ErpClientAdapter {

    private static final Logger log = LoggerFactory.getLogger(OracleNetSuiteRestAdapter.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OracleNetSuiteRestAdapter() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public OracleNetSuiteRestAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ErpType getSupportedType() {
        return ErpType.ORACLE_NETSUITE;
    }

    @Override
    public boolean testConnection(ErpConnector connector) {
        try {
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String testUrl = baseUrl + "/services/rest/record/v1/workOrder?limit=1";
            HttpHeaders headers = createHeaders(connector);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(testUrl, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("NetSuite connection test failed for connector {}: {}", connector.getId(), e.getMessage());
            return false;
        }
    }

    @Override
    public List<InboundErpOrderDto> fetchReleasedOrders(ErpConnector connector, Instant since) {
        List<InboundErpOrderDto> orders = new ArrayList<>();
        try {
            String plantCode = connector.getPlant() != null ? connector.getPlant().getCode() : "1000";
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String queryUrl = baseUrl + "/services/rest/record/v1/workOrder?q=orderStatus IS 'RELEASED'";

            HttpHeaders headers = createHeaders(connector);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(queryUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    for (JsonNode node : items) {
                        InboundErpOrderDto dto = new InboundErpOrderDto();
                        dto.setErpOrderId(node.path("id").asText(""));
                        dto.setOrderNumber("WO-NS-" + node.path("tranId").asText(dto.getErpOrderId()));
                        dto.setPlantCode(plantCode);
                        dto.setProductCode(node.path("item").path("refName").asText(node.path("itemId").asText("ITEM-DEFAULT")));
                        dto.setProductName(node.path("item").path("refName").asText(dto.getProductCode()));
                        dto.setTargetQuantity(new BigDecimal(node.path("quantity").asText("100.00")));
                        dto.setUnitOfMeasure(node.path("units").path("refName").asText("EA"));
                        dto.setBatchNumber(node.path("memo").asText(null));
                        dto.setPriority("MEDIUM");
                        orders.add(dto);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch released orders from NetSuite for connector {}: {}", connector.getId(), e.getMessage());
        }
        return orders;
    }

    @Override
    public ErpConfirmationResult postConfirmation(ErpConnector connector, ErpOrderConfirmation confirmation) {
        try {
            String baseUrl = connector.getBaseUrl().replaceAll("/+$", "");
            String postUrl = baseUrl + "/services/rest/record/v1/workOrderCompletion";

            Map<String, Object> payload = new HashMap<>();
            payload.put("createdFrom", Map.of("id", confirmation.getErpOrderId()));
            payload.put("completedQuantity", confirmation.getConfirmedGoodQty());
            payload.put("scrapQuantity", confirmation.getConfirmedScrapQty());
            payload.put("isBackflush", true);

            String jsonPayload = objectMapper.writeValueAsString(payload);
            HttpHeaders headers = createHeaders(connector);
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.exchange(postUrl, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String docNum = root.path("id").asText("NS-COMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                return ErpConfirmationResult.success(docNum, response.getBody());
            } else {
                return ErpConfirmationResult.failure("HTTP " + response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to post completion to NetSuite: {}", e.getMessage());
            return ErpConfirmationResult.failure(e.getMessage(), null);
        }
    }

    private HttpHeaders createHeaders(ErpConnector connector) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        if (connector.getSecretOrToken() != null && !connector.getSecretOrToken().isBlank()) {
            headers.set("Authorization", "Bearer " + connector.getSecretOrToken());
        } else if (connector.getApiKeyOrUser() != null && !connector.getApiKeyOrUser().isBlank()) {
            headers.set("Authorization", "NLAuth nlauth_account=" + (connector.getCompanyIdOrClient() != null ? connector.getCompanyIdOrClient() : "DEMO") + ", nlauth_email=" + connector.getApiKeyOrUser() + ", nlauth_signature=" + connector.getSecretOrToken());
        }
        return headers;
    }
}
