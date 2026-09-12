package com.factoryos.modules.sop.domain;

import com.factoryos.modules.auth.domain.User;
import com.factoryos.modules.production.domain.ProductionOrder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quality_sign_off_gates")
public class QualitySignOffGate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_order_id", nullable = false, unique = true)
    private ProductionOrder productionOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sop_id", nullable = false)
    private StandardOperatingProcedure sop;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private SopExecutionSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "gate_status", nullable = false, length = 25)
    private GateStatus gateStatus = GateStatus.PENDING;

    @Column(name = "requires_quality_role", nullable = false)
    private boolean requiresQualityRole = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_off_by_user_id")
    private User signedOffByUser;

    @Column(name = "signed_off_by_name", length = 100)
    private String signedOffByName;

    @Column(name = "signed_off_at")
    private Instant signedOffAt;

    @Column(name = "sign_off_comments", columnDefinition = "TEXT")
    private String signOffComments;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public QualitySignOffGate() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionOrder getProductionOrder() {
        return productionOrder;
    }

    public void setProductionOrder(ProductionOrder productionOrder) {
        this.productionOrder = productionOrder;
    }

    public StandardOperatingProcedure getSop() {
        return sop;
    }

    public void setSop(StandardOperatingProcedure sop) {
        this.sop = sop;
    }

    public SopExecutionSession getSession() {
        return session;
    }

    public void setSession(SopExecutionSession session) {
        this.session = session;
    }

    public GateStatus getGateStatus() {
        return gateStatus;
    }

    public void setGateStatus(GateStatus gateStatus) {
        this.gateStatus = gateStatus;
    }

    public boolean isRequiresQualityRole() {
        return requiresQualityRole;
    }

    public void setRequiresQualityRole(boolean requiresQualityRole) {
        this.requiresQualityRole = requiresQualityRole;
    }

    public User getSignedOffByUser() {
        return signedOffByUser;
    }

    public void setSignedOffByUser(User signedOffByUser) {
        this.signedOffByUser = signedOffByUser;
    }

    public String getSignedOffByName() {
        return signedOffByName;
    }

    public void setSignedOffByName(String signedOffByName) {
        this.signedOffByName = signedOffByName;
    }

    public Instant getSignedOffAt() {
        return signedOffAt;
    }

    public void setSignedOffAt(Instant signedOffAt) {
        this.signedOffAt = signedOffAt;
    }

    public String getSignOffComments() {
        return signOffComments;
    }

    public void setSignOffComments(String signOffComments) {
        this.signOffComments = signOffComments;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
