package com.factoryos.modules.analytics.application;

import com.factoryos.modules.analytics.dto.EnterpriseOeeMatrixDto;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnterpriseAnalyticsIntegrationTest {

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
    private Plant austinPlant;
    private Plant berlinPlant;
    private ProductionArea machiningArea;
    private ProductionArea stampingArea;
    private ProductionLine millLine;
    private ProductionLine pressLine;
    private Machine millMachine1;
    private Machine millMachine2;
    private Machine pressMachine;

    @BeforeEach
    void setUp() {
        enterprise = new Enterprise(UUID.randomUUID(), "ENT-GLOBAL", "FactoryOS Global Enterprise", "Global Operations");

        austinPlant = new Plant(UUID.randomUUID(), enterprise, "PLANT-AUSTIN-01", "Austin Gigafactory", "America/Chicago", "Austin TX", "ACTIVE");
        berlinPlant = new Plant(UUID.randomUUID(), enterprise, "PLANT-BERLIN-02", "Berlin Advanced", "Europe/Berlin", "Berlin DE", "ACTIVE");

        machiningArea = new ProductionArea(UUID.randomUUID(), austinPlant, "AREA-MACHINING", "Heavy Machining", "Machining cells");
        stampingArea = new ProductionArea(UUID.randomUUID(), berlinPlant, "AREA-STAMPING", "Stamping Area", "Press cells");

        millLine = new ProductionLine(UUID.randomUUID(), machiningArea, "LINE-MILL-01", "CNC Milling Line A", "Milling");
        pressLine = new ProductionLine(UUID.randomUUID(), stampingArea, "LINE-PRESS-01", "Heavy Press Line 1", "Pressing");

        millMachine1 = new Machine();
        millMachine1.setId(UUID.randomUUID());
        millMachine1.setName("CNC Mill 01");
        millMachine1.setSerialNumber("CNC-001");
        millMachine1.setPlant(austinPlant);
        millMachine1.setArea(machiningArea);
        millMachine1.setLine(millLine);
        millMachine1.setStatus(MachineStatus.RUNNING);

        millMachine2 = new Machine();
        millMachine2.setId(UUID.randomUUID());
        millMachine2.setName("CNC Mill 02");
        millMachine2.setSerialNumber("CNC-002");
        millMachine2.setPlant(austinPlant);
        millMachine2.setArea(machiningArea);
        millMachine2.setLine(millLine);
        millMachine2.setStatus(MachineStatus.IDLE);

        pressMachine = new Machine();
        pressMachine.setId(UUID.randomUUID());
        pressMachine.setName("Stamping Press 01");
        pressMachine.setSerialNumber("PRESS-001");
        pressMachine.setPlant(berlinPlant);
        pressMachine.setArea(stampingArea);
        pressMachine.setLine(pressLine);
        pressMachine.setStatus(MachineStatus.RUNNING);
    }

    @Test
    void shouldAggregateMultiPlantFleetMetricsWithAccurateFormulas() {
        when(enterpriseRepository.findAllByIsDeletedFalseOrderByCodeAsc()).thenReturn(List.of(enterprise));
        when(plantRepository.findByEnterpriseIdAndIsDeletedFalseOrderByCodeAsc(enterprise.getId()))
                .thenReturn(List.of(austinPlant, berlinPlant));

        // Austin: 2 machines (1 RUNNING, 1 IDLE, 0 DOWN) -> 100% Availability
        when(machineRepository.findByPlantIdAndIsDeletedFalse(austinPlant.getId()))
                .thenReturn(List.of(millMachine1, millMachine2));
        when(lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(austinPlant.getId()))
                .thenReturn(List.of(millLine));

        ProductionOrder austinOrder = new ProductionOrder();
        austinOrder.setId(UUID.randomUUID());
        austinOrder.setPlant(austinPlant);
        austinOrder.setMachine(millMachine1);
        austinOrder.setGoodQuantity(900);
        austinOrder.setScrapQuantity(100);
        austinOrder.setStatus(ProductionOrderStatus.IN_PROGRESS);
        austinOrder.setCreatedAt(Instant.now());

        when(productionOrderRepository.findByPlantIdAndIsDeletedFalse(austinPlant.getId()))
                .thenReturn(List.of(austinOrder));
        when(downtimeEventRepository.findByPlantIdAndIsDeletedFalse(austinPlant.getId()))
                .thenReturn(List.of());

        // Berlin: 1 machine (RUNNING) -> 100% Availability
        when(machineRepository.findByPlantIdAndIsDeletedFalse(berlinPlant.getId()))
                .thenReturn(List.of(pressMachine));
        when(lineRepository.findByAreaPlantIdAndIsDeletedFalseOrderByCodeAsc(berlinPlant.getId()))
                .thenReturn(List.of(pressLine));

        ProductionOrder berlinOrder = new ProductionOrder();
        berlinOrder.setId(UUID.randomUUID());
        berlinOrder.setPlant(berlinPlant);
        berlinOrder.setMachine(pressMachine);
        berlinOrder.setGoodQuantity(1000);
        berlinOrder.setScrapQuantity(0);
        berlinOrder.setStatus(ProductionOrderStatus.COMPLETED);
        berlinOrder.setCreatedAt(Instant.now());

        when(productionOrderRepository.findByPlantIdAndIsDeletedFalse(berlinPlant.getId()))
                .thenReturn(List.of(berlinOrder));

        DowntimeEvent berlinDt = new DowntimeEvent();
        berlinDt.setId(UUID.randomUUID());
        berlinDt.setPlant(berlinPlant);
        berlinDt.setMachine(pressMachine);
        berlinDt.setStartTime(Instant.now().minus(30, ChronoUnit.MINUTES));
        berlinDt.setEndTime(Instant.now());
        berlinDt.setReasonCode(DowntimeReasonCode.BREAKDOWN);

        when(downtimeEventRepository.findByPlantIdAndIsDeletedFalse(berlinPlant.getId()))
                .thenReturn(List.of(berlinDt));

        EnterpriseOeeMatrixDto matrix = analyticsService.getEnterpriseOeeMatrix("24H", null, null);

        assertThat(matrix).isNotNull();
        assertThat(matrix.getFleetSummary().getTotalPlants()).isEqualTo(2);
        assertThat(matrix.getFleetSummary().getActiveLines()).isEqualTo(2);
        assertThat(matrix.getFleetSummary().getTotalMachines()).isEqualTo(3);
        assertThat(matrix.getFleetSummary().getTotalGoodQuantity()).isEqualTo(1900);
        assertThat(matrix.getFleetSummary().getTotalScrapQuantity()).isEqualTo(100);
        assertThat(matrix.getFleetSummary().getTotalDowntimeMinutes()).isEqualTo(30);

        // Austin Quality: 900 / 1000 = 90.0%
        // Austin Performance: 88.5% (active order)
        // Austin Availability: 100.0%
        // Austin OEE: 100 * 88.5 * 90 / 10000 = 79.7% (TARGET)
        PlantOeeBenchmarkDto austinDto = matrix.getPlantMetrics().stream()
                .filter(p -> p.getPlantCode().equals("PLANT-AUSTIN-01"))
                .findFirst().orElseThrow();
        assertThat(austinDto.getQuality()).isEqualTo(90.0);
        assertThat(austinDto.getAvailability()).isEqualTo(100.0);
        assertThat(austinDto.getPerformance()).isEqualTo(88.5);
        assertThat(austinDto.getOee()).isEqualTo(79.7);
        assertThat(austinDto.getBenchmarkTier()).isEqualTo("TARGET");

        // Berlin Quality: 1000 / 1000 = 100.0%
        // Berlin Performance: 92.0% (completed order)
        // Berlin Availability: 100.0%
        // Berlin OEE: 100 * 92.0 * 100 / 10000 = 92.0% (WORLD_CLASS)
        PlantOeeBenchmarkDto berlinDto = matrix.getPlantMetrics().stream()
                .filter(p -> p.getPlantCode().equals("PLANT-BERLIN-02"))
                .findFirst().orElseThrow();
        assertThat(berlinDto.getOee()).isEqualTo(92.0);
        assertThat(berlinDto.getRank()).isEqualTo(1);
        assertThat(berlinDto.getBenchmarkTier()).isEqualTo("WORLD_CLASS");

        assertThat(matrix.getFleetSummary().getFleetAvgOee()).isEqualTo(85.9);
        assertThat(berlinDto.getOeeDeltaVsFleetAvg()).isEqualTo(6.1);
        assertThat(austinDto.getOeeDeltaVsFleetAvg()).isEqualTo(-6.2);
    }
}
