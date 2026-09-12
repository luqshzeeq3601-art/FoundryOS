package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
import com.factoryos.modules.analytics.dto.LineOeeBenchmarkDto;
import com.factoryos.modules.analytics.dto.PlantOeeBenchmarkDto;
import com.factoryos.modules.downtime.domain.DowntimeEvent;
import com.factoryos.modules.downtime.domain.DowntimeReasonCode;
import com.factoryos.modules.downtime.repository.DowntimeEventRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.domain.MachineStatus;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.domain.ProductionOrderStatus;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.tenant.domain.Enterprise;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.domain.ProductionArea;
import com.factoryos.modules.tenant.domain.ProductionLine;
import com.factoryos.modules.tenant.repository.EnterpriseRepository;
import com.factoryos.modules.tenant.repository.PlantRepository;
import com.factoryos.modules.tenant.repository.ProductionLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseAnalyticsServiceTest {

    @Mock
    private EnterpriseRepository enterpriseRepository;

    @Mock
    private PlantRepository plantRepository;

    @Mock
    private ProductionLineRepository lineRepository;

    @Mock
    private MachineRepository machineRepository;

    @Mock
    private ProductionOrderRepository productionOrderRepository;

    @Mock
    private DowntimeEventRepository downtimeEventRepository;

    @InjectMocks
    private EnterpriseAnalyticsService analyticsService;

    private Enterprise enterprise;
    private Plant plantAustin;
    private Plant plantBerlin;
    private ProductionArea areaMachining;
    private ProductionLine lineMill;
    private Machine machine1;
    private Machine machine2;

    @BeforeEach
    void setUp() {
        enterprise = new Enterprise();
        enterprise.setId(UUID.randomUUID());
        enterprise.setCode("ENT-GLOBAL");
        enterprise.setName("Global Enterprise");

        plantAustin = new Plant();
        plantAustin.setId(UUID.randomUUID());
        plantAustin.setEnterprise(enterprise);
        plantAustin.setCode("PLANT-AUSTIN-01");
        plantAustin.setName("Austin Gigafactory");
        plantAustin.setStatus("ACTIVE");
        plantAustin.setTimezone("America/Chicago");

        plantBerlin = new Plant();
        plantBerlin.setId(UUID.randomUUID());
        plantBerlin.setEnterprise(enterprise);
        plantBerlin.setCode("PLANT-BERLIN-02");
        plantBerlin.setName("Berlin Advanced");
        plantBerlin.setStatus("ACTIVE");
        plantBerlin.setTimezone("Europe/Berlin");

        areaMachining = new ProductionArea();
        areaMachining.setId(UUID.randomUUID());
        areaMachining.setPlant(plantAustin);
        areaMachining.setCode("AREA-MACHINING");
        areaMachining.setName("Heavy Machining");

        lineMill = new ProductionLine();
        lineMill.setId(UUID.randomUUID());
        lineMill.setArea(areaMachining);
        lineMill.setCode("LINE-MILL-01");
        lineMill.setName("CNC Milling Line A");

        machine1 = new Machine();
        machine1.setId(UUID.randomUUID());
        machine1.setPlant(plantAustin);
        machine1.setArea(areaMachining);
        machine1.setLine(lineMill);
        machine1.setName("CNC Mill 01");
        machine1.setSerialNumber("CNC-001");
        machine1.setStatus(MachineStatus.RUNNING);

        machine2 = new Machine();
        machine2.setId(UUID.randomUUID());
        machine2.setPlant(plantAustin);
        machine2.setArea(areaMachining);
        machine2.setLine(lineMill);
        machine2.setName("CNC Mill 02");
        machine2.setSerialNumber("CNC-002");
        machine2.setStatus(MachineStatus.DOWN);
    }

    @Test
    void shouldCalculateMultiPlantOeeMatrixAndBenchmarking() {
        when(enterpriseRepository.findAllByIsDeletedFalseOrderByCodeAsc()).thenReturn(List.of(enterprise));
        when(plantRepository.findByEnterpriseIdAndIsDeletedFalseOrderByCodeAsc(enterprise.getId()))
                .thenReturn(List.of(plantAustin, plantBerlin));

        // Austin setup: 2 machines (1 RUNNING, 1 DOWN) -> 50% Availability
        when(machineRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of(machine1, machine2));
        when(lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(plantAustin.getId())).thenReturn(List.of(lineMill));

        ProductionOrder order1 = new ProductionOrder();
        order1.setId(UUID.randomUUID());
        order1.setPlant(plantAustin);
        order1.setMachine(machine1);
        order1.setGoodQuantity(95);
        order1.setScrapQuantity(5);
        order1.setStatus(ProductionOrderStatus.IN_PROGRESS);
        order1.setCreatedAt(Instant.now());

        when(productionOrderRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of(order1));

        DowntimeEvent dt1 = new DowntimeEvent();
        dt1.setId(UUID.randomUUID());
        dt1.setPlant(plantAustin);
        dt1.setMachine(machine2);
        dt1.setStartTime(Instant.now().minus(60, ChronoUnit.MINUTES));
        dt1.setEndTime(Instant.now());
        dt1.setReasonCode(DowntimeReasonCode.BREAKDOWN);

        when(downtimeEventRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of(dt1));

        // Berlin setup: 1 machine (RUNNING) -> 100% Availability
        Machine machineBerlin = new Machine();
        machineBerlin.setId(UUID.randomUUID());
        machineBerlin.setPlant(plantBerlin);
        machineBerlin.setStatus(MachineStatus.RUNNING);

        when(machineRepository.findByPlantIdAndIsDeletedFalse(plantBerlin.getId())).thenReturn(List.of(machineBerlin));
        when(lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(plantBerlin.getId())).thenReturn(List.of());

        ProductionOrder orderBerlin = new ProductionOrder();
        orderBerlin.setId(UUID.randomUUID());
        orderBerlin.setPlant(plantBerlin);
        orderBerlin.setGoodQuantity(100);
        orderBerlin.setScrapQuantity(0);
        orderBerlin.setStatus(ProductionOrderStatus.COMPLETED);
        orderBerlin.setCreatedAt(Instant.now());

        when(productionOrderRepository.findByPlantIdAndIsDeletedFalse(plantBerlin.getId())).thenReturn(List.of(orderBerlin));
        when(downtimeEventRepository.findByPlantIdAndIsDeletedFalse(plantBerlin.getId())).thenReturn(List.of());

        EnterpriseOeeMatrixDto result = analyticsService.getEnterpriseOeeMatrix("24H", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getEnterpriseCode()).isEqualTo("ENT-GLOBAL");
        assertThat(result.getPlantMetrics()).hasSize(2);

        // Check Fleet Summary
        assertThat(result.getFleetSummary().getTotalPlants()).isEqualTo(2);
        assertThat(result.getFleetSummary().getTotalMachines()).isEqualTo(3);
        assertThat(result.getFleetSummary().getRunningMachines()).isEqualTo(2);
        assertThat(result.getFleetSummary().getDownMachines()).isEqualTo(1);
        assertThat(result.getFleetSummary().getTotalGoodQuantity()).isEqualTo(195);
        assertThat(result.getFleetSummary().getTotalScrapQuantity()).isEqualTo(5);

        // Check Ranking: Berlin should rank #1 (higher OEE) and Austin rank #2
        PlantOeeBenchmarkDto first = result.getPlantMetrics().get(0);
        PlantOeeBenchmarkDto second = result.getPlantMetrics().get(1);

        assertThat(first.getRank()).isEqualTo(1);
        assertThat(first.getPlantCode()).isEqualTo("PLANT-BERLIN-02");
        assertThat(first.getOee()).isGreaterThan(second.getOee());
        assertThat(first.getBenchmarkTier()).isEqualTo("WORLD_CLASS");

        assertThat(second.getRank()).isEqualTo(2);
        assertThat(second.getPlantCode()).isEqualTo("PLANT-AUSTIN-01");
        assertThat(second.getAvailability()).isEqualTo(50.0);
        assertThat(second.getQuality()).isEqualTo(95.0);
        assertThat(second.getDownMachines()).isEqualTo(1);

        // Check Line Drill-down in Austin
        assertThat(second.getLineMetrics()).hasSize(1);
        LineOeeBenchmarkDto lineMetric = second.getLineMetrics().get(0);
        assertThat(lineMetric.getLineCode()).isEqualTo("LINE-MILL-01");
        assertThat(lineMetric.isBottleneck()).isTrue(); // machine2 is down
        assertThat(lineMetric.getStatus()).isEqualTo("CRITICAL_BOTTLENECK");
    }

    @Test
    void shouldHandleEmptyPlantsGracefully() {
        when(enterpriseRepository.findAllByIsDeletedFalseOrderByCodeAsc()).thenReturn(List.of(enterprise));
        when(plantRepository.findByEnterpriseIdAndIsDeletedFalseOrderByCodeAsc(enterprise.getId()))
                .thenReturn(List.of(plantAustin));
        when(machineRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of());
        when(lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(plantAustin.getId())).thenReturn(List.of());
        when(productionOrderRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of());
        when(downtimeEventRepository.findByPlantIdAndIsDeletedFalse(plantAustin.getId())).thenReturn(List.of());

        EnterpriseOeeMatrixDto result = analyticsService.getEnterpriseOeeMatrix("24H", null, null);

        assertThat(result).isNotNull();
        assertThat(result.getFleetSummary().getTotalMachines()).isEqualTo(0);
        assertThat(result.getPlantMetrics().get(0).getOee()).isEqualTo(100.0);
        assertThat(result.getPlantMetrics().get(0).getRank()).isEqualTo(1);
    }
}
