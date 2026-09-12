package com.factoryos.modules.vibration.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.auth.repository.UserRepository;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.maintenance.domain.MaintenancePriority;
import com.factoryos.modules.maintenance.domain.MaintenanceStatus;
import com.factoryos.modules.maintenance.domain.MaintenanceWorkOrder;
import com.factoryos.modules.maintenance.dto.WorkOrderDto;
import com.factoryos.modules.maintenance.repository.MaintenanceWorkOrderRepository;
import com.factoryos.modules.vibration.domain.*;
import com.factoryos.modules.vibration.dto.DiagnosticSnapshotDto;
import com.factoryos.modules.vibration.dto.SpectralPeakDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PrescriptiveMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptiveMaintenanceService.class);
    private static final Duration DEDUPLICATION_WINDOW = Duration.ofHours(24);

    private final MaintenanceWorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final AuditRecordingService auditRecordingService;
    private final ObjectMapper objectMapper;

    public PrescriptiveMaintenanceService(
            MaintenanceWorkOrderRepository workOrderRepository,
            UserRepository userRepository,
            AuditRecordingService auditRecordingService,
            ObjectMapper objectMapper
    ) {
        this.workOrderRepository = workOrderRepository;
        this.userRepository = userRepository;
        this.auditRecordingService = auditRecordingService;
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluates a machine health assessment and automatically generates or deduplicates
     * a prescriptive maintenance work order when health degrades below 60%.
     */
    @Transactional
    public Optional<WorkOrderDto> evaluateAndCreatePrescriptiveOrder(
            MachineHealthAssessment assessment,
            VibrationBurstSample burst,
            List<VibrationSpectralPeak> peaks,
            User actor
    ) {
        // Trigger condition: Health Score < 60% OR Status is WARNING/CRITICAL OR ISO Zone C/D
        boolean isDegraded = assessment.getHealthScore() < 60 
                || assessment.getHealthStatus() == MachineHealthStatus.WARNING 
                || assessment.getHealthStatus() == MachineHealthStatus.CRITICAL
                || assessment.getIsoSeverityZone() == IsoSeverityZone.ZONE_C
                || assessment.getIsoSeverityZone() == IsoSeverityZone.ZONE_D;

        if (!isDegraded) {
            return Optional.empty();
        }

        Machine machine = assessment.getMachine();
        Instant cutoff = Instant.now().minus(DEDUPLICATION_WINDOW);

        // 1. Check for existing active prescriptive work order for this machine within 24 hours
        List<MaintenanceWorkOrder> activeOrders = workOrderRepository
                .findActivePrescriptiveOrdersForMachineSince(machine.getId(), cutoff);

        // Map domain fault details to subsystem & recommended parts
        FaultHarmonicType fault = FaultHarmonicType.NORMAL;
        if (assessment.getDominantFaultType() != null) {
            try {
                fault = FaultHarmonicType.valueOf(assessment.getDominantFaultType());
            } catch (Exception ignored) {}
        }

        String suspectedSubsystem = determineSuspectedSubsystem(fault);
        List<String> recommendedParts = determineRecommendedParts(fault);
        String prescriptiveAction = assessment.getRecommendedAction() != null 
                ? assessment.getRecommendedAction() 
                : "Perform immediate vibration diagnostic inspection and bearing alignment check.";

        // Build Diagnostic Snapshot
        DiagnosticSnapshotDto snapshot = new DiagnosticSnapshotDto();
        snapshot.setAssessmentId(assessment.getId());
        snapshot.setMachineId(machine.getId());
        snapshot.setMachineName(machine.getName());
        if (assessment.getPlant() != null) {
            snapshot.setPlantName(assessment.getPlant().getName());
        }
        snapshot.setAxis(burst != null ? burst.getAxis() : "RADIAL_X");
        snapshot.setHealthScore(assessment.getHealthScore());
        snapshot.setHealthStatus(assessment.getHealthStatus());
        snapshot.setIsoSeverityZone(assessment.getIsoSeverityZone());
        snapshot.setRmsVelocityMmS(assessment.getRmsVelocityMmS());
        snapshot.setSpindleTemperatureC(assessment.getSpindleTemperatureC());
        if (burst != null) {
            snapshot.setCrestFactor(burst.getCrestFactor());
            snapshot.setKurtosis(burst.getKurtosis());
            snapshot.setRunningSpeedRpm(burst.getRunningSpeedRpm() != null ? burst.getRunningSpeedRpm() : 3000.0);
        }
        snapshot.setDominantFault(fault);
        snapshot.setSuspectedSubsystem(suspectedSubsystem);
        snapshot.setRecommendedParts(recommendedParts);
        snapshot.setPrescriptiveAction(prescriptiveAction);
        if (peaks != null) {
            snapshot.setDominantPeaks(peaks.stream()
                    .map(p -> new SpectralPeakDto(p.getFrequencyHz(), p.getAmplitudeMmS(), p.getOrderMultiple(), p.getFaultHarmonicType(), p.getConfidence()))
                    .collect(Collectors.toList()));
        }
        snapshot.setCapturedAt(Instant.now());

        String snapshotJson = "{}";
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            log.warn("Failed to serialize diagnostic snapshot JSON", e);
        }

        // 2. DEDUPLICATION PATH: Update existing active order
        if (!activeOrders.isEmpty()) {
            MaintenanceWorkOrder existing = activeOrders.get(0);
            existing.setLastTriggeredAt(Instant.now());
            existing.setDiagnosticSnapshot(snapshotJson);
            existing.setHealthAssessmentId(assessment.getId());
            existing.setUpdatedAt(Instant.now());
            MaintenanceWorkOrder saved = workOrderRepository.save(existing);

            log.info("Deduplicated prescriptive maintenance trigger for machine '{}' under existing work order '{}'",
                    machine.getName(), existing.getWorkOrderNumber());

            auditRecordingService.record(
                    actor != null ? actor.getId() : null,
                    "PRESCRIPTIVE_MAINTENANCE_DEDUPLICATED",
                    "MaintenanceWorkOrder",
                    saved.getId(),
                    null,
                    Map.of(
                            "workOrderNumber", saved.getWorkOrderNumber(),
                            "machineName", machine.getName(),
                            "healthScore", assessment.getHealthScore(),
                            "dominantFault", fault.name(),
                            "deduplicatedWithinHours", 24
                    )
            );

            return Optional.of(WorkOrderDto.from(saved));
        }

        // 3. CREATION PATH: Auto-generate new Prescriptive Maintenance Work Order
        String woNumber = generateWorkOrderNumber();
        MaintenancePriority priority = (assessment.getHealthStatus() == MachineHealthStatus.CRITICAL 
                || assessment.getIsoSeverityZone() == IsoSeverityZone.ZONE_D 
                || assessment.getHealthScore() < 40)
                ? MaintenancePriority.CRITICAL
                : MaintenancePriority.HIGH;

        String title = String.format("PRESCRIPTIVE: %s Anomaly on %s (%s)", 
                formatFaultTitle(fault), machine.getName(), assessment.getIsoSeverityZone().name());

        StringBuilder desc = new StringBuilder();
        desc.append(String.format("AUTOMATED PRESCRIPTIVE WORK ORDER GENERATED BY FOUNDRY//OS TELEMETRY BUS.\n\n"));
        desc.append(String.format("• Machine: %s (Serial: %s)\n", machine.getName(), machine.getSerialNumber()));
        desc.append(String.format("• Health Score: %d%% (%s)\n", assessment.getHealthScore(), assessment.getHealthStatus().name()));
        desc.append(String.format("• ISO 10816 Zone: %s (RMS Velocity: %.2f mm/s)\n", assessment.getIsoSeverityZone().name(), assessment.getRmsVelocityMmS()));
        if (assessment.getSpindleTemperatureC() != null) {
            desc.append(String.format("• Bearing Temp: %.1f °C\n", assessment.getSpindleTemperatureC()));
        }
        desc.append(String.format("• Dominant Fault: %s\n", fault.name()));
        desc.append(String.format("• Suspected Subsystem: %s\n", suspectedSubsystem));
        desc.append(String.format("• Recommended Spare Parts: %s\n\n", String.join(", ", recommendedParts)));
        desc.append(String.format("PRESCRIPTIVE ACTION:\n%s\n\n", prescriptiveAction));
        desc.append("Attached: High-frequency FFT Spectral Snapshot with Kinematic Peak Markers.");

        MaintenanceWorkOrder newOrder = new MaintenanceWorkOrder();
        newOrder.setWorkOrderNumber(woNumber);
        newOrder.setMachine(machine);
        newOrder.setTitle(title);
        newOrder.setDescription(desc.toString());
        newOrder.setPriority(priority);
        newOrder.setStatus(MaintenanceStatus.OPEN);
        newOrder.setPrescriptive(true);
        newOrder.setHealthAssessmentId(assessment.getId());
        newOrder.setDiagnosticSnapshot(snapshotJson);
        newOrder.setSuspectedSubsystem(suspectedSubsystem);
        newOrder.setRecommendedParts(String.join("; ", recommendedParts));
        newOrder.setLastTriggeredAt(Instant.now());
        newOrder.setDueAt(Instant.now().plus(Duration.ofHours(priority == MaintenancePriority.CRITICAL ? 8 : 24)));
        newOrder.setCreatedBy(actor != null ? actor.getId() : null);
        newOrder.setUpdatedBy(actor != null ? actor.getId() : null);

        // Optional: Auto-assign on-duty technician if available
        List<User> technicians = userRepository.searchUsers(RoleType.TECHNICIAN, true, null, PageRequest.of(0, 1)).getContent();
        if (!technicians.isEmpty()) {
            newOrder.setAssignedTo(technicians.get(0));
            newOrder.setStatus(MaintenanceStatus.ASSIGNED);
        }

        MaintenanceWorkOrder saved = workOrderRepository.save(newOrder);

        log.warn("AUTOMATED PRESCRIPTIVE WORK ORDER CREATED: {} for machine '{}' with priority {}",
                saved.getWorkOrderNumber(), machine.getName(), saved.getPriority());

        // Dispatch alert audit event & technician dispatch
        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "PRESCRIPTIVE_MAINTENANCE_TRIGGERED",
                "MaintenanceWorkOrder",
                saved.getId(),
                null,
                Map.of(
                        "workOrderNumber", saved.getWorkOrderNumber(),
                        "machineId", machine.getId().toString(),
                        "machineName", machine.getName(),
                        "priority", saved.getPriority().name(),
                        "healthScore", assessment.getHealthScore(),
                        "dominantFault", fault.name(),
                        "suspectedSubsystem", suspectedSubsystem,
                        "recommendedParts", recommendedParts,
                        "assignedTo", saved.getAssignedTo() != null ? saved.getAssignedTo().getDisplayName() : "UNASSIGNED"
                )
        );

        return Optional.of(WorkOrderDto.from(saved));
    }

    private String generateWorkOrderNumber() {
        String ts = String.valueOf(System.currentTimeMillis() % 1000000);
        return "WO-PREDICT-" + ts;
    }

    private String determineSuspectedSubsystem(FaultHarmonicType fault) {
        if (fault == null) return "Drive Spindle Subsystem";
        switch (fault) {
            case BPFO_BEARING_OUTER:
            case BPFI_BEARING_INNER:
                return "Drive-End Spindle Bearing Assembly";
            case MISALIGNMENT_2X:
                return "Shaft Coupling & Motor-Spindle Alignment Interface";
            case UNBALANCE_1X:
                return "Rotor Dynamic Balance & Chuck Assembly";
            case LOOSENESS_3X:
                return "Spindle Housing Base Mounts & Anchor Fasteners";
            default:
                return "Drive Spindle Mechanical Kinematics";
        }
    }

    private List<String> determineRecommendedParts(FaultHarmonicType fault) {
        if (fault == null) return List.of("Spindle Lubrication Cartridge");
        switch (fault) {
            case BPFO_BEARING_OUTER:
                return List.of("SKF 7014 CD/P4A Angular Contact Ball Bearing Set", "Spindle Labyrinth Seal Ring", "Klüber Isoflex NBU 15 High-Speed Grease");
            case BPFI_BEARING_INNER:
                return List.of("FAG HCB7014-C-T-P4S Ceramic Hybrid Bearing Kit", "Spindle Shaft Tolerance Ring", "O-Ring Viton Spindle Housing Kit");
            case MISALIGNMENT_2X:
                return List.of("KTR ROTEX GS 28 Flexible Coupling Spider (98 Shore-A)", "Laser Alignment Shim Pack (0.05mm - 1.0mm)");
            case UNBALANCE_1X:
                return List.of("Spindle Rotor Dynamic Balance Set Screws", "Toolholder Pull Stud Clamping Gripper Set");
            case LOOSENESS_3X:
                return List.of("M16 Grade 12.9 High-Tensile Foundation Anchor Bolts", "Nord-Lock Wedge-Locking Washers NL16");
            default:
                return List.of("Precision Spindle Maintenance Kit", "Vibration Damper Pad Set");
        }
    }

    private String formatFaultTitle(FaultHarmonicType fault) {
        if (fault == null || fault == FaultHarmonicType.NORMAL) return "Vibration Severity";
        switch (fault) {
            case BPFO_BEARING_OUTER: return "Bearing Outer Race Spall (BPFO)";
            case BPFI_BEARING_INNER: return "Bearing Inner Race Fault (BPFI)";
            case MISALIGNMENT_2X: return "2X Shaft Misalignment";
            case UNBALANCE_1X: return "1X Dynamic Unbalance";
            case LOOSENESS_3X: return "3X Mechanical Looseness";
            default: return fault.name();
        }
    }
}
