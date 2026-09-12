package com.factoryos.modules.sop.application;

import com.factoryos.common.exception.AppException;
import com.factoryos.modules.audit.application.AuditRecordingService;
import com.factoryos.modules.auth.domain.RoleType;
import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.production.domain.ProductionOrder;
import com.factoryos.modules.production.repository.ProductionOrderRepository;
import com.factoryos.modules.sop.domain.*;
import com.factoryos.modules.sop.dto.QualityGateStatusDto;
import com.factoryos.modules.sop.dto.QualitySignOffRequestDto;
import com.factoryos.modules.sop.repository.QualitySignOffGateRepository;
import com.factoryos.modules.sop.repository.SopExecutionSessionRepository;
import com.factoryos.modules.sop.repository.StandardOperatingProcedureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class QualityGateService {

    private static final Logger log = LoggerFactory.getLogger(QualityGateService.class);

    private final QualitySignOffGateRepository qualityGateRepository;
    private final SopExecutionSessionRepository sessionRepository;
    private final StandardOperatingProcedureRepository sopRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final AuditRecordingService auditRecordingService;

    public QualityGateService(
            QualitySignOffGateRepository qualityGateRepository,
            SopExecutionSessionRepository sessionRepository,
            StandardOperatingProcedureRepository sopRepository,
            ProductionOrderRepository productionOrderRepository,
            AuditRecordingService auditRecordingService
    ) {
        this.qualityGateRepository = qualityGateRepository;
        this.sessionRepository = sessionRepository;
        this.sopRepository = sopRepository;
        this.productionOrderRepository = productionOrderRepository;
        this.auditRecordingService = auditRecordingService;
    }

    public QualityGateStatusDto getGateStatusForOrder(UUID productionOrderId) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(productionOrderId)
                .orElseThrow(() -> AppException.notFound("Production Order not found with ID: " + productionOrderId));

        Optional<QualitySignOffGate> gateOpt = qualityGateRepository.findByProductionOrderId(productionOrderId);
        if (gateOpt.isPresent()) {
            return toDto(gateOpt.get(), order);
        }

        // Check if an applicable SOP exists for this product
        List<StandardOperatingProcedure> matchingSops = sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse(order.getProductCode());
        if (matchingSops.isEmpty()) {
            matchingSops = sopRepository.findByProductCodeAndIsDeletedFalse("*");
        }

        QualityGateStatusDto dto = new QualityGateStatusDto();
        dto.setProductionOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setProductCode(order.getProductCode());

        if (matchingSops.isEmpty()) {
            // No SOP gate defined for this product -> inherently compliant
            dto.setGateStatus(GateStatus.PASSED);
            dto.setCompliant(true);
            dto.setSignOffComments("No mandatory SOP quality gate assigned for product " + order.getProductCode());
            return dto;
        }

        StandardOperatingProcedure sop = matchingSops.get(0);
        dto.setSopId(sop.getId());
        dto.setSopCode(sop.getSopCode());
        dto.setSopTitle(sop.getTitle());
        dto.setRequiresQualityRole(sop.isRequiresQualitySignOff());
        dto.setGateStatus(GateStatus.PENDING);
        dto.setCompliant(false);
        dto.setTotalMandatorySteps((int) sop.getSteps().stream().filter(SopStep::isMandatory).count());
        dto.setBlockingReason("Quality inspection session not yet initiated for order " + order.getOrderNumber());

        return dto;
    }

    /**
     * Mandatory Invariant: Validates whether a production order meets all quality gate criteria
     * before transitioning to COMPLETED status. Throws AppException if incomplete or failing.
     */
    public void validateOrderCompletionGate(UUID productionOrderId) {
        QualityGateStatusDto status = getGateStatusForOrder(productionOrderId);
        if (status.getGateStatus() == GateStatus.BYPASSED) {
            log.info("Quality gate bypassed for order {}", status.getOrderNumber());
            return;
        }

        if (status.getGateStatus() != GateStatus.PASSED || !status.isCompliant()) {
            String reason = status.getBlockingReason() != null ? status.getBlockingReason() :
                    "Quality sign-off gate is PENDING or failed. All mandatory inspection checklist steps must be completed and signed off.";
            log.warn("Order {} completion blocked by Quality Gate: {}", status.getOrderNumber(), reason);
            throw AppException.badRequest("QUALITY_GATE_FAILED: " + reason);
        }
    }

    @Transactional
    public QualityGateStatusDto signOffGate(UUID productionOrderId, QualitySignOffRequestDto request, User actor) {
        ProductionOrder order = productionOrderRepository.findByIdAndIsDeletedFalse(productionOrderId)
                .orElseThrow(() -> AppException.notFound("Production Order not found with ID: " + productionOrderId));

        StandardOperatingProcedure sop = sopRepository.findByProductCodeIgnoreCaseAndIsDeletedFalse(order.getProductCode())
                .stream().findFirst()
                .orElseGet(() -> sopRepository.findAllByIsDeletedFalseOrderBySopCodeAsc().stream().findFirst()
                        .orElseThrow(() -> AppException.badRequest("No SOP available to bind to quality gate.")));

        // Check quality inspector role if required
        if (sop.isRequiresQualitySignOff() && actor != null) {
            boolean isQualityRole = actor.getRole() != null &&
                    (actor.getRole().getName() == RoleType.ADMIN ||
                     actor.getRole().getName() == RoleType.PRODUCTION_MANAGER ||
                     actor.getRole().getName() == RoleType.ENGINEER ||
                     actor.getRole().getName() == RoleType.TECHNICIAN);
            if (!isQualityRole) {
                throw AppException.forbidden("Quality Sign-Off requires an authorized Quality Inspector, Engineer, or Manager role.");
            }
        }

        QualitySignOffGate gate = qualityGateRepository.findByProductionOrderId(productionOrderId)
                .orElseGet(() -> {
                    QualitySignOffGate g = new QualitySignOffGate();
                    g.setProductionOrder(order);
                    g.setSop(sop);
                    return g;
                });

        Optional<SopExecutionSession> sessionOpt = sessionRepository.findFirstByProductionOrderIdOrderByStartedAtDesc(productionOrderId);
        sessionOpt.ifPresent(gate::setSession);

        gate.setGateStatus(request.getGateStatus());
        gate.setSignedOffByUser(actor);
        gate.setSignedOffByName(actor != null ? actor.getDisplayName() : "Quality Authority");
        gate.setSignedOffAt(Instant.now());
        gate.setSignOffComments(request.getSignOffComments());

        QualitySignOffGate saved = qualityGateRepository.save(gate);
        log.info("Quality Sign-Off Gate for order {} set to {}", order.getOrderNumber(), saved.getGateStatus());

        auditRecordingService.record(
                actor != null ? actor.getId() : null,
                "QUALITY_GATE_SIGNED_OFF",
                "QualitySignOffGate",
                saved.getId(),
                null,
                Map.of(
                        "orderNumber", order.getOrderNumber(),
                        "gateStatus", saved.getGateStatus().name(),
                        "signedOffBy", saved.getSignedOffByName()
                )
        );

        return toDto(saved, order);
    }

    private QualityGateStatusDto toDto(QualitySignOffGate gate, ProductionOrder order) {
        QualityGateStatusDto dto = new QualityGateStatusDto();
        dto.setGateId(gate.getId());
        dto.setProductionOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setProductCode(order.getProductCode());

        if (gate.getSop() != null) {
            dto.setSopId(gate.getSop().getId());
            dto.setSopCode(gate.getSop().getSopCode());
            dto.setSopTitle(gate.getSop().getTitle());
            dto.setRequiresQualityRole(gate.getSop().isRequiresQualitySignOff());
            dto.setTotalMandatorySteps((int) gate.getSop().getSteps().stream().filter(SopStep::isMandatory).count());
        }

        if (gate.getSession() != null) {
            dto.setSessionId(gate.getSession().getId());
            dto.setCompletedMandatorySteps(gate.getSession().getCompletedSteps());
            dto.setFailedMandatorySteps(gate.getSession().getFailedSteps());
        }

        dto.setGateStatus(gate.getGateStatus());
        dto.setCompliant(gate.getGateStatus() == GateStatus.PASSED || gate.getGateStatus() == GateStatus.BYPASSED);
        dto.setSignedOffByUserId(gate.getSignedOffByUser() != null ? gate.getSignedOffByUser().getId() : null);
        dto.setSignedOffByName(gate.getSignedOffByName());
        dto.setSignedOffAt(gate.getSignedOffAt());
        dto.setSignOffComments(gate.getSignOffComments());

        if (!dto.isCompliant()) {
            dto.setBlockingReason("Quality Gate status is " + gate.getGateStatus().name() + ". Sign-off approval required.");
        }

        return dto;
    }
}
