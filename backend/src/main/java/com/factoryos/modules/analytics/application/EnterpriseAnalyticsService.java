package com.factoryos.modules.analytics.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.FleetSummaryDto;
import com.factoryos.modules.analytics.dto.LineOeeBenchmarkDto;
import com.factoryos.modules.analytics.dto.PlantOeeBenchmarkDto;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Enterprise;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.ProductionLine;
import com.factoryos.modules.tenant.repository.EnterpriseRepository;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.ProductionLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnterpriseAnalyticsService {

    private final EnterpriseRepository enterpriseRepository;
    private final PlantRepository plantRepository;
    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final DowntimeEventRepository downtimeEventRepository;

    public EnterpriseAnalyticsService(
            EnterpriseRepository enterpriseRepository,
            PlantRepository plantRepository,
            ProductionLineRepository lineRepository,
            MachineRepository machineRepository,
            ProductionOrderRepository productionOrderRepository,
            DowntimeEventRepository downtimeEventRepository
    ) {
        this.enterpriseRepository = enterpriseRepository;
        this.plantRepository = plantRepository;
        this.lineRepository = lineRepository;
        this.machineRepository = machineRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.downtimeEventRepository = downtimeEventRepository;
    }

    @Transactional(readOnly = true)
    public EnterpriseOeeMatrixDto getEnterpriseOeeMatrix(String intervalStr, UUID enterpriseId, String plantStatus) {
        String interval = (intervalStr != null && !intervalStr.isBlank()) ? intervalStr.trim().toUpperCase() : "24H";
        Instant timeThreshold = resolveIntervalStart(interval);

        Enterprise enterprise;
        if (enterpriseId != null) {
            enterprise = enterpriseRepository.findByIdAndIsDeletedFalse(enterpriseId)
                    .orElseThrow(() -> AppException.notFound("Enterprise not found with ID: " + enterpriseId));
        } else {
            List<Enterprise> enterprises = enterpriseRepository.findAllByIsDeletedFalseOrderByCodeAsc();
            if (enterprises.isEmpty()) {
                throw AppException.notFound("No enterprise found in the system");
            }
            enterprise = enterprises.get(0);
        }

        List<Plant> plants = plantRepository.findByEnterpriseIdAndIsDeletedFalseOrderByCodeAsc(enterprise.getId());
        if (plantStatus != null && !plantStatus.isBlank()) {
            plants = plants.stream()
                    .filter(p -> p.getStatus().equalsIgnoreCase(plantStatus.trim()))
                    .toList();
        }

        List<PlantOeeBenchmarkDto> plantMetrics = new ArrayList<>();
        long totalFleetGood = 0;
        long totalFleetScrap = 0;
        long totalFleetDowntime = 0;
        int totalFleetMachines = 0;
        int totalFleetRunning = 0;
        int totalFleetIdle = 0;
        int totalFleetDown = 0;
        int totalFleetActiveLines = 0;

        for (Plant plant : plants) {
            PlantOeeBenchmarkDto pDto = calculatePlantBenchmark(plant, timeThreshold);
            plantMetrics.add(pDto);

            totalFleetGood += pDto.getTotalGoodQuantity();
            totalFleetScrap += pDto.getTotalScrapQuantity();
            totalFleetDowntime += pDto.getTotalDowntimeMinutes();
            totalFleetMachines += pDto.getTotalMachines();
            totalFleetRunning += pDto.getRunningMachines();
            totalFleetIdle += pDto.getIdleMachines();
            totalFleetDown += pDto.getDownMachines();
            totalFleetActiveLines += pDto.getLineMetrics().size();
        }

        // Compute fleet averages
        double fleetAvgOee = 0.0;
        double fleetAvgAvailability = 0.0;
        double fleetAvgPerformance = 0.0;
        double fleetAvgQuality = 0.0;

        if (!plantMetrics.isEmpty()) {
            fleetAvgOee = plantMetrics.stream().mapToDouble(PlantOeeBenchmarkDto::getOee).average().orElse(0.0);
            fleetAvgAvailability = plantMetrics.stream().mapToDouble(PlantOeeBenchmarkDto::getAvailability).average().orElse(0.0);
            fleetAvgPerformance = plantMetrics.stream().mapToDouble(PlantOeeBenchmarkDto::getPerformance).average().orElse(0.0);
            fleetAvgQuality = plantMetrics.stream().mapToDouble(PlantOeeBenchmarkDto::getQuality).average().orElse(0.0);
        }

        fleetAvgOee = round1(fleetAvgOee);
        fleetAvgAvailability = round1(fleetAvgAvailability);
        fleetAvgPerformance = round1(fleetAvgPerformance);
        fleetAvgQuality = round1(fleetAvgQuality);

        long totalFleetProduced = totalFleetGood + totalFleetScrap;
        double fleetScrapRate = totalFleetProduced > 0 ? round1(((double) totalFleetScrap / totalFleetProduced) * 100.0) : 0.0;

        FleetSummaryDto fleetSummary = new FleetSummaryDto(
                plantMetrics.size(),
                totalFleetActiveLines,
                totalFleetMachines,
                totalFleetRunning,
                totalFleetIdle,
                totalFleetDown,
                fleetAvgOee,
                fleetAvgAvailability,
                fleetAvgPerformance,
                fleetAvgQuality,
                totalFleetGood,
                totalFleetScrap,
                fleetScrapRate,
                totalFleetDowntime
        );

        // Sort plants by OEE descending, calculate rank, delta vs fleet average, and benchmark tiers
        plantMetrics.sort(Comparator.comparingDouble(PlantOeeBenchmarkDto::getOee).reversed());
        for (int i = 0; i < plantMetrics.size(); i++) {
            PlantOeeBenchmarkDto p = plantMetrics.get(i);
            p.setRank(i + 1);
            p.setOeeDeltaVsFleetAvg(round1(p.getOee() - fleetAvgOee));
            if (p.getOee() >= 85.0) {
                p.setBenchmarkTier("WORLD_CLASS");
            } else if (p.getOee() >= 70.0) {
                p.setBenchmarkTier("TARGET");
            } else {
                p.setBenchmarkTier("UNDERPERFORMING");
            }
        }

        return new EnterpriseOeeMatrixDto(
                enterprise.getId(),
                enterprise.getCode(),
                enterprise.getName(),
                interval,
                Instant.now(),
                fleetSummary,
                plantMetrics
        );
    }

    private PlantOeeBenchmarkDto calculatePlantBenchmark(Plant plant, Instant timeThreshold) {
        PlantOeeBenchmarkDto dto = new PlantOeeBenchmarkDto();
        dto.setPlantId(plant.getId());
        dto.setPlantCode(plant.getCode());
        dto.setPlantName(plant.getName());
        dto.setTimezone(plant.getTimezone());
        dto.setAddress(plant.getAddress());
        dto.setStatus(plant.getStatus());

        List<Machine> plantMachines = machineRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
        int totalMachines = plantMachines.size();
        int runningMachines = 0;
        int idleMachines = 0;
        int downMachines = 0;

        for (Machine m : plantMachines) {
            if (m.getStatus() == MachineStatus.RUNNING) {
                runningMachines++;
            } else if (m.getStatus() == MachineStatus.DOWN) {
                downMachines++;
            } else {
                idleMachines++;
            }
        }

        dto.setTotalMachines(totalMachines);
        dto.setRunningMachines(runningMachines);
        dto.setIdleMachines(idleMachines);
        dto.setDownMachines(downMachines);

        // Production orders
        List<ProductionOrder> allOrders = productionOrderRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
        List<ProductionOrder> filteredOrders = allOrders.stream()
                .filter(o -> isOrderInInterval(o, timeThreshold))
                .toList();

        long goodQty = filteredOrders.stream().mapToLong(ProductionOrder::getGoodQuantity).sum();
        long scrapQty = filteredOrders.stream().mapToLong(ProductionOrder::getScrapQuantity).sum();
        long totalProduced = goodQty + scrapQty;
        double scrapRate = totalProduced > 0 ? round1(((double) scrapQty / totalProduced) * 100.0) : 0.0;

        int activeOrders = (int) filteredOrders.stream().filter(o -> o.getStatus() == ProductionOrderStatus.IN_PROGRESS).count();
        int completedOrders = (int) filteredOrders.stream().filter(o -> o.getStatus() == ProductionOrderStatus.COMPLETED).count();

        dto.setTotalGoodQuantity(goodQty);
        dto.setTotalScrapQuantity(scrapQty);
        dto.setScrapRate(scrapRate);
        dto.setActiveOrdersCount(activeOrders);
        dto.setCompletedOrdersCount(completedOrders);

        // Downtime events
        List<DowntimeEvent> allDowntimes = downtimeEventRepository.findByPlantIdAndIsDeletedFalse(plant.getId());
        List<DowntimeEvent> filteredDowntimes = allDowntimes.stream()
                .filter(d -> isDowntimeInInterval(d, timeThreshold))
                .toList();

        long downtimeMinutes = 0;
        for (DowntimeEvent dt : filteredDowntimes) {
            Instant end = dt.getEndTime() != null ? dt.getEndTime() : Instant.now();
            Instant start = dt.getStartTime().isBefore(timeThreshold) ? timeThreshold : dt.getStartTime();
            if (end.isAfter(start)) {
                downtimeMinutes += Duration.between(start, end).toMinutes();
            }
        }

        dto.setTotalDowntimeMinutes(downtimeMinutes);
        dto.setDowntimeEventsCount(filteredDowntimes.size());

        // Standardized Plant OEE calculation
        double availability = totalMachines > 0
                ? ((double) (totalMachines - downMachines) / totalMachines) * 100.0
                : 100.0;
        double performance = activeOrders > 0 ? 88.5 : (completedOrders > 0 ? 92.0 : 100.0);
        double quality = totalProduced > 0 ? ((double) goodQty / totalProduced) * 100.0 : 100.0;
        double oee = (availability * performance * quality) / 10000.0;

        dto.setAvailability(round1(availability));
        dto.setPerformance(round1(performance));
        dto.setQuality(round1(quality));
        dto.setOee(round1(oee));

        // Line-level drill-down
        List<ProductionLine> lines = lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(plant.getId());
        List<LineOeeBenchmarkDto> lineDtos = new ArrayList<>();

        Map<UUID, List<Machine>> machinesByLine = plantMachines.stream()
                .filter(m -> m.getLine() != null)
                .collect(Collectors.groupingBy(m -> m.getLine().getId()));

        for (ProductionLine line : lines) {
            List<Machine> lineMachines = machinesByLine.getOrDefault(line.getId(), List.of());
            int lineTotal = lineMachines.size();
            int lineRunning = (int) lineMachines.stream().filter(m -> m.getStatus() == MachineStatus.RUNNING).count();
            int lineDown = (int) lineMachines.stream().filter(m -> m.getStatus() == MachineStatus.DOWN).count();
            int lineIdle = lineTotal - lineRunning - lineDown;

            Set<UUID> lineMachineIds = lineMachines.stream().map(Machine::getId).collect(Collectors.toSet());
            List<ProductionOrder> lineOrders = filteredOrders.stream()
                    .filter(o -> o.getMachine() != null && lineMachineIds.contains(o.getMachine().getId()))
                    .toList();

            long lineGood = lineOrders.stream().mapToLong(ProductionOrder::getGoodQuantity).sum();
            long lineScrap = lineOrders.stream().mapToLong(ProductionOrder::getScrapQuantity).sum();
            long lineProduced = lineGood + lineScrap;
            double lineScrapRate = lineProduced > 0 ? round1(((double) lineScrap / lineProduced) * 100.0) : 0.0;

            int lineActive = (int) lineOrders.stream().filter(o -> o.getStatus() == ProductionOrderStatus.IN_PROGRESS).count();
            int lineCompleted = (int) lineOrders.stream().filter(o -> o.getStatus() == ProductionOrderStatus.COMPLETED).count();

            double lineAvailability = lineTotal > 0 ? ((double) (lineTotal - lineDown) / lineTotal) * 100.0 : 100.0;
            double linePerformance = lineActive > 0 ? 88.5 : (lineCompleted > 0 ? 92.0 : 100.0);
            double lineQuality = lineProduced > 0 ? ((double) lineGood / lineProduced) * 100.0 : 100.0;
            double lineOee = (lineAvailability * linePerformance * lineQuality) / 10000.0;

            boolean isBottleneck = lineOee < 70.0 || lineDown > 0 || lineScrapRate > 5.0;
            String lineStatus = lineDown > 0 ? "CRITICAL_BOTTLENECK" : (isBottleneck ? "DEGRADED" : "OPERATIONAL");

            lineDtos.add(new LineOeeBenchmarkDto(
                    line.getId(),
                    line.getCode(),
                    line.getName(),
                    line.getArea() != null ? line.getArea().getId() : null,
                    line.getArea() != null ? line.getArea().getCode() : "N/A",
                    line.getArea() != null ? line.getArea().getName() : "General Area",
                    round1(lineOee),
                    round1(lineAvailability),
                    round1(linePerformance),
                    round1(lineQuality),
                    lineGood,
                    lineScrap,
                    lineScrapRate,
                    lineTotal,
                    lineRunning,
                    lineIdle,
                    lineDown,
                    isBottleneck,
                    lineStatus
            ));
        }

        dto.setLineMetrics(lineDtos);
        return dto;
    }

    private Instant resolveIntervalStart(String interval) {
        Instant now = Instant.now();
        return switch (interval) {
            case "24H" -> now.minus(24, ChronoUnit.HOURS);
            case "7D" -> now.minus(7, ChronoUnit.DAYS);
            case "30D" -> now.minus(30, ChronoUnit.DAYS);
            case "ALL" -> Instant.EPOCH;
            default -> now.minus(24, ChronoUnit.HOURS);
        };
    }

    private boolean isOrderInInterval(ProductionOrder order, Instant threshold) {
        if (threshold.equals(Instant.EPOCH)) {
            return true;
        }
        if (order.getStatus() == ProductionOrderStatus.IN_PROGRESS) {
            return true;
        }
        if (order.getStartedAt() != null && !order.getStartedAt().isBefore(threshold)) {
            return true;
        }
        if (order.getCompletedAt() != null && !order.getCompletedAt().isBefore(threshold)) {
            return true;
        }
        return order.getCreatedAt() != null && !order.getCreatedAt().isBefore(threshold);
    }

    private boolean isDowntimeInInterval(DowntimeEvent dt, Instant threshold) {
        if (threshold.equals(Instant.EPOCH)) {
            return true;
        }
        if (dt.getEndTime() == null) {
            return true;
        }
        return dt.getEndTime().isAfter(threshold);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
