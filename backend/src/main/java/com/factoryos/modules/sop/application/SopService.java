package com.factoryos.modules.sop.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.machine.domain.Machine;
import com.factoryos.modules.machine.repository.MachineRepository;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.sop.domain.*;
import com.factoryos.modules.sop.dto.*;
import com.factoryos.modules.sop.repository.*;
import com.factoryos.modules.tenant.context.TenantContextHolder;
import com.factoryos.modules.tenant.domain.Plant;
import com.factoryos.modules.tenant.repository.PlantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SopService {

    private static final Logger log = LoggerFactory.getLogger(SopService.class);

    private final StandardOperatingProcedureRepository sopRepository;
    private final SopStepRepository sopStepRepository;
    private final SopExecutionSessionRepository sessionRepository;
    private final SopStepExecutionRecordRepository stepRecordRepository;
    private final QualitySignOffGateRepository qualityGateRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final MachineRepository machineRepository;
    private final PlantRepository plantRepository;
    private final AuditRecordingService auditRecordingService;

    public SopService(
            StandardOperatingProcedureRepository sopRepository,
            SopStepRepository sopStepRepository,
            SopExecutionSessionRepository sessionRepository,
            SopStepExecutionRecordRepository stepRecordRepository,
            QualitySignOffGateRepository qualityGateRepository,
            ProductionOrderRepository productionOrderRepository,
            MachineRepository machineRepository,
            PlantRepository plantRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.sopRepository = sopRepository;
        this.sopStepRepository = sopStepRepository;
        this.sessionRepository = sessionRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.qualityGateRepository = qualityGateRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.machineRepository = machineRepository;
        this.plantRepository = plantRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public List<SopDto> getAllSops(String productCode, SopCategory category) {
        UUID plantId = TenantContextHolder.getCurrentPlantId();
        List<StandardOperatingProcedure> sops = sopRepository.searchSops(plantId, category, productCode);
        return sops.stream().map(this::toSopDto).collect(Collectors.toList());
    }

    public SopDto getSopById(UUID id) {
        StandardOperatingProcedure sop = sopRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> AppException.notFound("SOP not found with ID: " + id));
        return toSopDto(sop);
    }

    public SopDto getSopByCode(String sopCode) {
        StandardOperatingProcedure sop = sopRepository.findBySopCodeAndIsDeletedFalse(sopCode.toUpperCase())
                .orElseThrow(() -> AppException.notFound("SOP not found with code: " + sopCode));
        return toSopDto(sop);
    }

    public Optional<SopDto> getSopForProduct(String productCode) {
        return sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse(productCode)
                .stream().findFirst().map(this::toSopDto);
    }

    @Transactional
    public SopDto createSop(SopCreateRequestDto request, User actor) {
        String code = request.getSopCode().trim().toUpperCase();
        if (sopRepository.existsBySopCodeAndIsDeletedFalse(code)) {
            throw AppException.conflict("DUPLICATE_SOP_CODE", "SOP with code '" + code + "' already exists");
        }

        Plant plant = null;
        if (request.getPlantId() != null) {
            plant = plantRepository.findByIdAndIsDeletedFalse(request.getPlantId())
                    .orElseThrow(() -> AppException.notFound("Plant not found with ID: " + request.getPlantId()));
        }

        StandardOperatingProcedure sop = new StandardOperatingProcedure();
        sop.setSopCode(code);
        sop.setTitle(request.getTitle());
        sop.setProductCode(request.getProductCode());
        sop.setPlant(plant);
        sop.setVersion(request.getVersion() != null ? request.getVersion() : "1.0");
        sop.setCategory(request.getCategory() != null ? request.getCategory() : SopCategory.ASSEMBLY);
        sop.setStatus(SopStatus.PUBLISHED);
        sop.setDescription(request.getDescription());
        sop.setCadDrawingUrl(request.getCadDrawingUrl());
        sop.setSafetyPrecautions(request.getSafetyPrecautions());
        sop.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes() > 0 ? request.getEstimatedDurationMinutes() : 30);
        sop.setRequiresQualitySignOff(request.isRequiresQualitySignOff());
        sop.setCreatedBy(actor != null ? actor.getId() : null);

        int stepNum = 1;
        if (request.getSteps() != null) {
            for (SopStepDto sDto : request.getSteps()) {
                SopStep step = new SopStep();
                step.setStepNumber(sDto.getStepNumber() > 0 ? sDto.getStepNumber() : stepNum++);
                step.setTitle(sDto.getTitle());
                step.setInstructionText(sDto.getInstructionText());
                step.setStepType(sDto.getStepType() != null ? sDto.getStepType() : SopStepType.INSTRUCTION);
                step.setImageUrl(sDto.getImageUrl());
                step.setCadViewNode(sDto.getCadViewNode());
                step.setMandatory(sDto.isMandatory());
                step.setNominalValue(sDto.getNominalValue());
                step.setMinTolerance(sDto.getMinTolerance());
                step.setMaxTolerance(sDto.getMaxTolerance());
                step.setUnitOfMeasure(sDto.getUnitOfMeasure());
                step.setSafetyAlert(sDto.getSafetyAlert());
                sop.addStep(step);
            }
        }

        StandardOperatingProcedure saved = sopRepository.save(sop);
        log.info("Created SOP template: {} for product {}", saved.getSopCode(), saved.getProductCode());

        return toSopDto(saved);
    }

    @Transactional
    public SopExecutionSessionDto startExecutionSession(StartSopSessionRequestDto request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(request.getProductionOrderId())
                .orElseThrow(() -> AppException.notFound("Production order not found with ID: " + request.getProductionOrderId()));

        StandardOperatingProcedure sop;
        if (request.getSopId() != null) {
            sop = sopRepository.findByIdAndIsDeletedFalse(request.getSopId())
                    .orElseThrow(() -> AppException.notFound("SOP not found with ID: " + request.getSopId()));
        } else {
            sop = sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse(order.getProductCode())
                    .stream().findFirst()
                    .orElseGet(() -> sopRepository.findByProductCodeAndIsDeletedFalse("*")
                            .stream().findFirst()
                            .orElseThrow(() -> AppException.badRequest("No SOP template configured for product " + order.getProductCode())));
        }

        Machine machine = order.getMachine();
        if (machine == null && request.getMachineId() != null) {
            machine = machineRepository.findByIdAndIsDeletedFalse(request.getMachineId()).orElse(null);
        }

        Plant plant = order.getPlant() != null ? order.getPlant() :
                (machine != null ? machine.getPlant() : null);

        if (plant == null) {
            plant = plantRepository.findAllByIsDeletedFalseOrderByCodeAsc().stream().findFirst()
                    .orElseThrow(() -> AppException.badRequest("No active plant found to bind SOP session"));
        }

        SopExecutionSession session = new SopExecutionSession();
        session.setSop(sop);
        session.setProductionOrder(order);
        session.setMachine(machine);
        session.setPlant(plant);
        session.setSessionStatus(SopSessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        session.setOperatorUser(actor);
        session.setOperatorName(actor != null ? actor.getDisplayName() : "Shopfloor Operator");
        session.setTotalSteps(sop.getSteps().size());

        SopExecutionSession savedSession = sessionRepository.save(session);

        // Instantiate step records for each step
        for (SopStep step : sop.getSteps()) {
            SopStepExecutionRecord record = new SopStepExecutionRecord();
            record.setSession(savedSession);
            record.setStep(step);
            record.setStepNumber(step.getStepNumber());
            record.setStatus(StepRecordStatus.PENDING);
            stepRecordRepository.save(record);
            savedSession.addStepRecord(record);
        }

        // Initialize or update Quality Gate
        QualitySignOffGate gate = qualityGateRepository.findByProductionOrderId(order.getId())
                .orElseGet(() -> {
                    QualitySignOffGate g = new QualitySignOffGate();
                    g.setProductionOrder(order);
                    return g;
                });
        gate.setSop(sop);
        gate.setSession(savedSession);
        gate.setGateStatus(GateStatus.PENDING);
        gate.setRequiresQualityRole(sop.isRequiresQualitySignOff());
        qualityGateRepository.save(gate);

        log.info("Initialized SOP session {} for order {} (SOP={})", savedSession.getId(), order.getOrderNumber(), sop.getSopCode());

        return toSessionDto(savedSession);
    }

    public SopExecutionSessionDto getSessionById(UUID sessionId) {
        SopExecutionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("SOP Session not found with ID: " + sessionId));
        return toSessionDto(session);
    }

    public List<SopExecutionSessionDto> getSessionsForOrder(UUID productionOrderId) {
        return sessionRepository.findByProductionOrderIdOrderByStartedAtDesc(productionOrderId).stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SopStepExecutionRecordDto recordStepExecution(UUID sessionId, RecordStepExecutionRequestDto request, User actor) {
        SopExecutionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("SOP Session not found with ID: " + sessionId));

        SopStepExecutionRecord record = stepRecordRepository.findBySessionIdAndStepId(sessionId, request.getStepId())
                .orElseThrow(() -> AppException.notFound("Step record not found in session for step: " + request.getStepId()));

        SopStep step = record.getStep();
        StepRecordStatus evaluatedStatus = request.getStatus();
        Boolean isWithinTolerance = null;

        // Tolerance check for numeric measurements
        if (step.getStepType() == SopStepType.NUMERIC_MEASUREMENT && request.getNumericValue() != null) {
            double val = request.getNumericValue();
            boolean pass = true;
            if (step.getMinTolerance() != null && val < step.getMinTolerance()) {
                pass = false;
            }
            if (step.getMaxTolerance() != null && val > step.getMaxTolerance()) {
                pass = false;
            }
            isWithinTolerance = pass;
            evaluatedStatus = pass ? StepRecordStatus.PASSED : StepRecordStatus.FAILED;
        }

        record.setStatus(evaluatedStatus);
        record.setNumericValue(request.getNumericValue());
        record.setIsWithinTolerance(isWithinTolerance);
        record.setTextFeedback(request.getTextFeedback());
        record.setPhotoEvidenceUrl(request.getPhotoEvidenceUrl());
        record.setBarcodeScanned(request.getBarcodeScanned());
        record.setVerifiedByUser(actor);
        record.setVerifiedByName(actor != null ? actor.getDisplayName() : "Operator");
        record.setVerifiedAt(Instant.now());
        record.setNotes(request.getNotes());

        SopStepExecutionRecord savedRecord = stepRecordRepository.save(record);

        // Recalculate session statistics
        List<SopStepExecutionRecord> allRecords = stepRecordRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        int completedCount = 0;
        int passedCount = 0;
        int failedCount = 0;

        for (SopStepExecutionRecord r : allRecords) {
            if (r.getStatus() != StepRecordStatus.PENDING) {
                completedCount++;
            }
            if (r.getStatus() == StepRecordStatus.PASSED) {
                passedCount++;
            } else if (r.getStatus() == StepRecordStatus.FAILED) {
                failedCount++;
            }
        }

        session.setCompletedSteps(completedCount);
        session.setPassedSteps(passedCount);
        session.setFailedSteps(failedCount);
        session.setUpdatedAt(Instant.now());

        // Check if all mandatory steps completed
        boolean allMandatoryCompleted = allRecords.stream()
                .filter(r -> r.getStep().isMandatory())
                .allMatch(r -> r.getStatus() != StepRecordStatus.PENDING);

        boolean hasMandatoryFailure = allRecords.stream()
                .filter(r -> r.getStep().isMandatory())
                .anyMatch(r -> r.getStatus() == StepRecordStatus.FAILED);

        if (allMandatoryCompleted) {
            if (!hasMandatoryFailure) {
                session.setSessionStatus(SopSessionStatus.PASSED);
                session.setCompletedAt(Instant.now());
            } else {
                session.setSessionStatus(SopSessionStatus.FAILED);
            }
        }
        sessionRepository.save(session);

        return toStepRecordDto(savedRecord);
    }

    @Transactional
    public SopExecutionSessionDto completeExecutionSession(UUID sessionId, QualitySignOffRequestDto signOffRequest, User actor) {
        SopExecutionSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("SOP Session not found with ID: " + sessionId));

        List<SopStepExecutionRecord> allRecords = stepRecordRepository.findBySessionIdOrderByStepNumberAsc(sessionId);
        boolean anyMandatoryPending = allRecords.stream()
                .filter(r -> r.getStep().isMandatory())
                .anyMatch(r -> r.getStatus() == StepRecordStatus.PENDING);

        if (anyMandatoryPending) {
            throw AppException.badRequest("Cannot sign off SOP session: Not all mandatory inspection steps have been completed.");
        }

        session.setQualitySignOffBy(actor);
        session.setQualitySignOffName(actor != null ? actor.getDisplayName() : "Quality Authority");
        session.setQualitySignOffAt(Instant.now());
        session.setQualitySignOffNotes(signOffRequest.getSignOffComments());

        if (signOffRequest.getGateStatus() == GateStatus.PASSED) {
            session.setSessionStatus(SopSessionStatus.PASSED);
            session.setCompletedAt(Instant.now());
        } else {
            session.setSessionStatus(SopSessionStatus.FAILED);
        }

        SopExecutionSession saved = sessionRepository.save(session);

        // Update Quality Gate
        QualitySignOffGate gate = qualityGateRepository.findByProductionOrderId(session.getProductionOrder().getId())
                .orElseGet(() -> {
                    QualitySignOffGate g = new QualitySignOffGate();
                    g.setProductionOrder(session.getProductionOrder());
                    g.setSop(session.getSop());
                    return g;
                });
        gate.setSession(saved);
        gate.setGateStatus(signOffRequest.getGateStatus());
        gate.setSignedOffByUser(actor);
        gate.setSignedOffByName(actor != null ? actor.getDisplayName() : "Quality Authority");
        gate.setSignedOffAt(Instant.now());
        gate.setSignOffComments(signOffRequest.getSignOffComments());
        qualityGateRepository.save(gate);

        log.info("SOP execution session {} completed with status {}", saved.getId(), saved.getSessionStatus());

        return toSessionDto(saved);
    }

    public SopDto toSopDto(StandardOperatingProcedure sop) {
        if (sop == null) return null;
        SopDto dto = new SopDto();
        dto.setId(sop.getId());
        dto.setSopCode(sop.getSopCode());
        dto.setTitle(sop.getTitle());
        dto.setProductCode(sop.getProductCode());
        if (sop.getPlant() != null) {
            dto.setPlantId(sop.getPlant().getId());
            dto.setPlantName(sop.getPlant().getName());
        }
        dto.setVersion(sop.getVersion());
        dto.setCategory(sop.getCategory());
        dto.setStatus(sop.getStatus());
        dto.setDescription(sop.getDescription());
        dto.setCadDrawingUrl(sop.getCadDrawingUrl());
        dto.setSafetyPrecautions(sop.getSafetyPrecautions());
        dto.setEstimatedDurationMinutes(sop.getEstimatedDurationMinutes());
        dto.setRequiresQualitySignOff(sop.isRequiresQualitySignOff());
        dto.setCreatedAt(sop.getCreatedAt());
        dto.setUpdatedAt(sop.getUpdatedAt());

        if (sop.getSteps() != null) {
            dto.setSteps(sop.getSteps().stream().map(this::toStepDto).collect(Collectors.toList()));
        }
        return dto;
    }

    public SopStepDto toStepDto(SopStep step) {
        if (step == null) return null;
        SopStepDto dto = new SopStepDto();
        dto.setId(step.getId());
        if (step.getSop() != null) {
            dto.setSopId(step.getSop().getId());
        }
        dto.setStepNumber(step.getStepNumber());
        dto.setTitle(step.getTitle());
        dto.setInstructionText(step.getInstructionText());
        dto.setStepType(step.getStepType());
        dto.setImageUrl(step.getImageUrl());
        dto.setCadViewNode(step.getCadViewNode());
        dto.setMandatory(step.isMandatory());
        dto.setNominalValue(step.getNominalValue());
        dto.setMinTolerance(step.getMinTolerance());
        dto.setMaxTolerance(step.getMaxTolerance());
        dto.setUnitOfMeasure(step.getUnitOfMeasure());
        dto.setSafetyAlert(step.getSafetyAlert());
        return dto;
    }

    public SopExecutionSessionDto toSessionDto(SopExecutionSession session) {
        if (session == null) return null;
        SopExecutionSessionDto dto = new SopExecutionSessionDto();
        dto.setId(session.getId());
        if (session.getSop() != null) {
            dto.setSopId(session.getSop().getId());
            dto.setSopCode(session.getSop().getSopCode());
            dto.setSopTitle(session.getSop().getTitle());
            dto.setCadDrawingUrl(session.getSop().getCadDrawingUrl());
            dto.setSafetyPrecautions(session.getSop().getSafetyPrecautions());
        }
        if (session.getProductionOrder() != null) {
            dto.setProductionOrderId(session.getProductionOrder().getId());
            dto.setOrderNumber(session.getProductionOrder().getOrderNumber());
            dto.setProductCode(session.getProductionOrder().getProductCode());
        }
        if (session.getMachine() != null) {
            dto.setMachineId(session.getMachine().getId());
            dto.setMachineCode(session.getMachine().getSerialNumber());
        }
        if (session.getPlant() != null) {
            dto.setPlantId(session.getPlant().getId());
            dto.setPlantName(session.getPlant().getName());
        }
        dto.setSessionStatus(session.getSessionStatus());
        dto.setStartedAt(session.getStartedAt());
        dto.setCompletedAt(session.getCompletedAt());
        if (session.getOperatorUser() != null) {
            dto.setOperatorUserId(session.getOperatorUser().getId());
        }
        dto.setOperatorName(session.getOperatorName());
        if (session.getQualitySignOffBy() != null) {
            dto.setQualitySignOffBy(session.getQualitySignOffBy().getId());
        }
        dto.setQualitySignOffName(session.getQualitySignOffName());
        dto.setQualitySignOffAt(session.getQualitySignOffAt());
        dto.setQualitySignOffNotes(session.getQualitySignOffNotes());
        dto.setTotalSteps(session.getTotalSteps());
        dto.setCompletedSteps(session.getCompletedSteps());
        dto.setPassedSteps(session.getPassedSteps());
        dto.setFailedSteps(session.getFailedSteps());

        List<SopStepExecutionRecord> records = session.getStepRecords();
        if (records == null || records.isEmpty()) {
            records = stepRecordRepository.findBySessionIdOrderByStepNumberAsc(session.getId());
        }
        dto.setStepRecords(records.stream().map(this::toStepRecordDto).collect(Collectors.toList()));

        return dto;
    }

    public SopStepExecutionRecordDto toStepRecordDto(SopStepExecutionRecord record) {
        if (record == null) return null;
        SopStepExecutionRecordDto dto = new SopStepExecutionRecordDto();
        dto.setId(record.getId());
        if (record.getSession() != null) {
            dto.setSessionId(record.getSession().getId());
        }
        if (record.getStep() != null) {
            dto.setStepId(record.getStep().getId());
            dto.setStepTitle(record.getStep().getTitle());
            dto.setInstructionText(record.getStep().getInstructionText());
            dto.setStepType(record.getStep().getStepType());
            dto.setImageUrl(record.getStep().getImageUrl());
            dto.setCadViewNode(record.getStep().getCadViewNode());
            dto.setMandatory(record.getStep().isMandatory());
            dto.setNominalValue(record.getStep().getNominalValue());
            dto.setMinTolerance(record.getStep().getMinTolerance());
            dto.setMaxTolerance(record.getStep().getMaxTolerance());
            dto.setUnitOfMeasure(record.getStep().getUnitOfMeasure());
            dto.setSafetyAlert(record.getStep().getSafetyAlert());
        }
        dto.setStepNumber(record.getStepNumber());
        dto.setStatus(record.getStatus());
        dto.setNumericValue(record.getNumericValue());
        dto.setIsWithinTolerance(record.getIsWithinTolerance());
        dto.setTextFeedback(record.getTextFeedback());
        dto.setPhotoEvidenceUrl(record.getPhotoEvidenceUrl());
        dto.setBarcodeScanned(record.getBarcodeScanned());
        if (record.getVerifiedByUser() != null) {
            dto.setVerifiedByUserId(record.getVerifiedByUser().getId());
        }
        dto.setVerifiedByName(record.getVerifiedByName());
        dto.setVerifiedAt(record.getVerifiedAt());
        dto.setNotes(record.getNotes());
        dto.setCreatedAt(record.getCreatedAt());
        return dto;
    }
}
