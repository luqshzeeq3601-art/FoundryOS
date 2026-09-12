package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.FleetSummaryDto;
import com.factoryos.modules.analytics.dto.LineOeeBenchmarkDto;
import com.factoryos.modules.analytics.dto.PlantOeeBenchmarkDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnterpriseExportServiceTest {

    private EnterpriseExportService exportService;
    private EnterpriseOeeMatrixDto matrix;

    @BeforeEach
    void setUp() {
        exportService = new EnterpriseExportService();

        FleetSummaryDto fleet = new FleetSummaryDto(
                2, 3, 10, 8, 1, 1,
                82.4, 90.0, 94.0, 97.5,
                15000, 385, 2.5, 120
        );

        PlantOeeBenchmarkDto plant1 = new PlantOeeBenchmarkDto();
        plant1.setPlantId(UUID.randomUUID());
        plant1.setPlantCode("PLANT-AUSTIN-01");
        plant1.setPlantName("Austin Gigafactory");
        plant1.setStatus("ACTIVE");
        plant1.setRank(1);
        plant1.setOee(86.5);
        plant1.setAvailability(92.0);
        plant1.setPerformance(96.0);
        plant1.setQuality(98.0);
        plant1.setOeeDeltaVsFleetAvg(4.1);
        plant1.setBenchmarkTier("WORLD_CLASS");
        plant1.setTotalGoodQuantity(10000);
        plant1.setTotalScrapQuantity(200);
        plant1.setScrapRate(2.0);
        plant1.setTotalMachines(6);
        plant1.setRunningMachines(5);
        plant1.setIdleMachines(1);
        plant1.setDownMachines(0);
        plant1.setTotalDowntimeMinutes(45);
        plant1.setActiveOrdersCount(3);

        LineOeeBenchmarkDto line1 = new LineOeeBenchmarkDto(
                UUID.randomUUID(), "LINE-MILL-01", "CNC Milling Line",
                UUID.randomUUID(), "AREA-MACHINING", "Machining",
                88.0, 95.0, 95.0, 97.5,
                5000, 100, 2.0, 3, 3, 0, 0,
                false, "OPERATIONAL"
        );
        plant1.setLineMetrics(List.of(line1));

        matrix = new EnterpriseOeeMatrixDto(
                UUID.randomUUID(),
                "ENT-GLOBAL",
                "FactoryOS Global Enterprise",
                "24H",
                Instant.now(),
                fleet,
                List.of(plant1)
        );
    }

    @Test
    void shouldGenerateValidCsvReport() {
        byte[] csvBytes = exportService.generateCsvReport(matrix);
        assertThat(csvBytes).isNotEmpty();

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("FOUNDRY//OS ENTERPRISE FLEET ANALYTICS");
        assertThat(csvContent).contains("ENT-GLOBAL");
        assertThat(csvContent).contains("PLANT-AUSTIN-01");
        assertThat(csvContent).contains("WORLD_CLASS");
        assertThat(csvContent).contains("LINE-MILL-01");
        assertThat(csvContent).contains("86.5");
    }

    @Test
    void shouldGenerateValidPdfReport() {
        byte[] pdfBytes = exportService.generatePdfReport(matrix);
        assertThat(pdfBytes).isNotEmpty();

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertThat(pdfHeader).startsWith("%PDF-1.4");

        String pdfFooter = new String(pdfBytes, pdfBytes.length - 20, 20, StandardCharsets.US_ASCII);
        assertThat(pdfFooter).contains("%%EOF");
    }
}
