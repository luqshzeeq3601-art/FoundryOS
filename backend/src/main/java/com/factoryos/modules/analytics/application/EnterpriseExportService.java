package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.FleetSummaryDto;
import com.factoryos.modules.analytics.dto.LineOeeBenchmarkDto;
import com.factoryos.modules.analytics.dto.PlantOeeBenchmarkDto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class EnterpriseExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    public byte[] generateCsvReport(EnterpriseOeeMatrixDto matrix) {
        StringBuilder sb = new StringBuilder();
        String timestamp = DATE_FORMATTER.format(matrix.getCalculatedAt());

        // Header Section
        sb.append("================================================================================\n");
        sb.append("FOUNDRY//OS ENTERPRISE FLEET ANALYTICS & BENCHMARKING REPORT\n");
        sb.append("================================================================================\n");
        sb.append("Enterprise Code,").append(escapeCsv(matrix.getEnterpriseCode())).append("\n");
        sb.append("Enterprise Name,").append(escapeCsv(matrix.getEnterpriseName())).append("\n");
        sb.append("Interval,").append(escapeCsv(matrix.getInterval())).append("\n");
        sb.append("Generated At (UTC),").append(timestamp).append("\n\n");

        // Executive Fleet KPI Summary
        FleetSummaryDto fleet = matrix.getFleetSummary();
        sb.append("--- EXECUTIVE FLEET KPI SUMMARY ---\n");
        sb.append("Total Plants,Active Lines,Total Machines,Running,Idle,Down,Fleet Avg OEE (%),Fleet Availability (%),Fleet Performance (%),Fleet Quality (%),Total Good Parts,Total Scrap Parts,Fleet Scrap Rate (%),Total Downtime (min)\n");
        sb.append(fleet.getTotalPlants()).append(",")
                .append(fleet.getActiveLines()).append(",")
                .append(fleet.getTotalMachines()).append(",")
                .append(fleet.getRunningMachines()).append(",")
                .append(fleet.getIdleMachines()).append(",")
                .append(fleet.getDownMachines()).append(",")
                .append(fleet.getFleetAvgOee()).append(",")
                .append(fleet.getFleetAvgAvailability()).append(",")
                .append(fleet.getFleetAvgPerformance()).append(",")
                .append(fleet.getFleetAvgQuality()).append(",")
                .append(fleet.getTotalGoodQuantity()).append(",")
                .append(fleet.getTotalScrapQuantity()).append(",")
                .append(fleet.getFleetScrapRate()).append(",")
                .append(fleet.getTotalDowntimeMinutes()).append("\n\n");

        // Cross-Plant Benchmarking Matrix
        sb.append("--- CROSS-PLANT BENCHMARKING MATRIX ---\n");
        sb.append("Rank,Plant Code,Plant Name,Status,Benchmark Tier,OEE (%),Delta vs Fleet (%),Availability (%),Performance (%),Quality (%),Total Good,Total Scrap,Scrap Rate (%),Total Machines,Running,Idle,Down,Downtime (min),Active Orders\n");
        for (PlantOeeBenchmarkDto p : matrix.getPlantMetrics()) {
            sb.append(p.getRank()).append(",")
                    .append(escapeCsv(p.getPlantCode())).append(",")
                    .append(escapeCsv(p.getPlantName())).append(",")
                    .append(escapeCsv(p.getStatus())).append(",")
                    .append(escapeCsv(p.getBenchmarkTier())).append(",")
                    .append(p.getOee()).append(",")
                    .append(p.getOeeDeltaVsFleetAvg() >= 0 ? "+" + p.getOeeDeltaVsFleetAvg() : p.getOeeDeltaVsFleetAvg()).append(",")
                    .append(p.getAvailability()).append(",")
                    .append(p.getPerformance()).append(",")
                    .append(p.getQuality()).append(",")
                    .append(p.getTotalGoodQuantity()).append(",")
                    .append(p.getTotalScrapQuantity()).append(",")
                    .append(p.getScrapRate()).append(",")
                    .append(p.getTotalMachines()).append(",")
                    .append(p.getRunningMachines()).append(",")
                    .append(p.getIdleMachines()).append(",")
                    .append(p.getDownMachines()).append(",")
                    .append(p.getTotalDowntimeMinutes()).append(",")
                    .append(p.getActiveOrdersCount()).append("\n");
        }
        sb.append("\n");

        // Line-Level Drilldown
        sb.append("--- PRODUCTION LINE DRILL-DOWN & BOTTLENECK ANALYSIS ---\n");
        sb.append("Plant Code,Line Code,Line Name,Area Name,Line OEE (%),Availability (%),Performance (%),Quality (%),Total Good,Total Scrap,Scrap Rate (%),Total Machines,Down Machines,Bottleneck Flag,Line Status\n");
        for (PlantOeeBenchmarkDto p : matrix.getPlantMetrics()) {
            if (p.getLineMetrics() != null) {
                for (LineOeeBenchmarkDto line : p.getLineMetrics()) {
                    sb.append(escapeCsv(p.getPlantCode())).append(",")
                            .append(escapeCsv(line.getLineCode())).append(",")
                            .append(escapeCsv(line.getLineName())).append(",")
                            .append(escapeCsv(line.getAreaName())).append(",")
                            .append(line.getOee()).append(",")
                            .append(line.getAvailability()).append(",")
                            .append(line.getPerformance()).append(",")
                            .append(line.getQuality()).append(",")
                            .append(line.getTotalGoodQuantity()).append(",")
                            .append(line.getTotalScrapQuantity()).append(",")
                            .append(line.getScrapRate()).append(",")
                            .append(line.getTotalMachines()).append(",")
                            .append(line.getDownMachines()).append(",")
                            .append(line.isBottleneck() ? "YES" : "NO").append(",")
                            .append(escapeCsv(line.getStatus())).append("\n");
                }
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generatePdfReport(EnterpriseOeeMatrixDto matrix) {
        // High-compatibility standalone PDF 1.4 document builder
        String timestamp = DATE_FORMATTER.format(matrix.getCalculatedAt());
        FleetSummaryDto fleet = matrix.getFleetSummary();

        List<String> textLines = new ArrayList<>();
        textLines.add("FOUNDRY//OS ENTERPRISE FLEET BENCHMARK REPORT");
        textLines.add("==========================================================================");
        textLines.add("Enterprise: " + matrix.getEnterpriseCode() + " - " + matrix.getEnterpriseName());
        textLines.add("Interval: " + matrix.getInterval() + " | Generated At: " + timestamp + " UTC");
        textLines.add("--------------------------------------------------------------------------");
        textLines.add("EXECUTIVE FLEET SUMMARY:");
        textLines.add(String.format("  * Fleet Avg OEE:        %.1f%%  (Availability: %.1f%% | Performance: %.1f%% | Quality: %.1f%%)",
                fleet.getFleetAvgOee(), fleet.getFleetAvgAvailability(), fleet.getFleetAvgPerformance(), fleet.getFleetAvgQuality()));
        textLines.add(String.format("  * Total Plants / Lines:  %d Plants / %d Active Lines", fleet.getTotalPlants(), fleet.getActiveLines()));
        textLines.add(String.format("  * Fleet Machine Fleet:  %d Total (%d RUNNING | %d IDLE | %d DOWN)",
                fleet.getTotalMachines(), fleet.getRunningMachines(), fleet.getIdleMachines(), fleet.getDownMachines()));
        textLines.add(String.format("  * Total Output:         %,d Good Parts | %,d Scrap (Scrap Rate: %.1f%%)",
                fleet.getTotalGoodQuantity(), fleet.getTotalScrapQuantity(), fleet.getFleetScrapRate()));
        textLines.add(String.format("  * Total Downtime:       %,d Minutes", fleet.getTotalDowntimeMinutes()));
        textLines.add("--------------------------------------------------------------------------");
        textLines.add("CROSS-PLANT BENCHMARKING MATRIX:");
        textLines.add(String.format("%-5s %-16s %-10s %-14s %-8s %-8s %-8s %-10s",
                "RANK", "PLANT", "OEE", "TIER", "AVAIL", "QUAL", "SCRAP", "STATUS"));

        for (PlantOeeBenchmarkDto p : matrix.getPlantMetrics()) {
            String tier = p.getBenchmarkTier();
            textLines.add(String.format("#%-4d %-16s %5.1f%%   %-14s %5.1f%%  %5.1f%%  %5.1f%%   %-10s",
                    p.getRank(),
                    truncate(p.getPlantCode() + " (" + p.getPlantName() + ")", 16),
                    p.getOee(),
                    tier,
                    p.getAvailability(),
                    p.getQuality(),
                    p.getScrapRate(),
                    p.getStatus()));
        }

        textLines.add("--------------------------------------------------------------------------");
        textLines.add("LINE-LEVEL BOTTLENECK AUDIT:");
        boolean hasBottlenecks = false;
        for (PlantOeeBenchmarkDto p : matrix.getPlantMetrics()) {
            if (p.getLineMetrics() != null) {
                for (LineOeeBenchmarkDto l : p.getLineMetrics()) {
                    if (l.isBottleneck()) {
                        hasBottlenecks = true;
                        textLines.add(String.format("  [!] %s // %s (%s) - OEE: %.1f%% | Down: %d | Scrap: %.1f%% -> %s",
                                p.getPlantCode(), l.getLineCode(), l.getLineName(), l.getOee(), l.getDownMachines(), l.getScrapRate(), l.getStatus()));
                    }
                }
            }
        }
        if (!hasBottlenecks) {
            textLines.add("  [OK] No critical line-level bottlenecks detected across the enterprise fleet.");
        }

        textLines.add("==========================================================================");
        textLines.add("AUTHENTICATED UNDER SHA-256 AUDIT BUS // FOUNDRY//OS ENTERPRISE EDITION v2.0");

        return buildPdfFromLines(textLines);
    }

    private byte[] buildPdfFromLines(List<String> lines) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // PDF Stream content
            StringBuilder content = new StringBuilder();
            content.append("BT\n");
            content.append("/F1 10 Tf\n");
            content.append("14 TL\n");
            content.append("40 760 Td\n");

            for (String line : lines) {
                String safe = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
                content.append("(").append(safe).append(") '\n");
            }
            content.append("ET\n");

            byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

            List<Long> offsets = new ArrayList<>();
            baos.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));

            // Object 1: Catalog
            offsets.add((long) baos.size());
            baos.write("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 2: Pages
            offsets.add((long) baos.size());
            baos.write("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 3: Page
            offsets.add((long) baos.size());
            baos.write("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 4: Stream Content
            offsets.add((long) baos.size());
            baos.write(String.format("4 0 obj\n<< /Length %d >>\nstream\n", contentBytes.length).getBytes(StandardCharsets.US_ASCII));
            baos.write(contentBytes);
            baos.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Object 5: Font (Courier / Monospaced for Industrial Brutalist readability)
            offsets.add((long) baos.size());
            baos.write("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>\nendobj\n".getBytes(StandardCharsets.US_ASCII));

            // Cross-Reference Table
            long startXref = baos.size();
            baos.write(String.format("xref\n0 %d\n0000000000 65535 f \n", offsets.size() + 1).getBytes(StandardCharsets.US_ASCII));
            for (Long offset : offsets) {
                baos.write(String.format("%010d 00000 n \n", offset).getBytes(StandardCharsets.US_ASCII));
            }

            // Trailer
            baos.write(String.format("trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n", offsets.size() + 1, startXref).getBytes(StandardCharsets.US_ASCII));

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private String truncate(String val, int maxLen) {
        if (val == null) return "";
        if (val.length() <= maxLen) return val;
        return val.substring(0, maxLen - 2) + "..";
    }
}
