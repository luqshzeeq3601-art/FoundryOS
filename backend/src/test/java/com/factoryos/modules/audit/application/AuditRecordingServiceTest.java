package com.factoryos.modules.audit.application;

import com.factoryos.modules.audit.domain.AuditEvent;
import com.factoryos.modules.audit.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRecordingServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditRecordingService auditRecordingService;

    @BeforeEach
    void setUp() {
        auditRecordingService = new AuditRecordingService(auditEventRepository);
    }

    @Test
    void record_Success_CapturesTraceAndPayload() {
        UUID actorId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        MDC.put("traceId", "test-trace-12345");

        try {
            auditRecordingService.record(
                    actorId,
                    "MACHINE_CREATED",
                    "Machine",
                    entityId,
                    null,
                    Map.of("name", "Lathe A1")
            );

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            verify(auditEventRepository).save(captor.capture());

            AuditEvent captured = captor.getValue();
            assertEquals(actorId, captured.getActorId());
            assertEquals("MACHINE_CREATED", captured.getAction());
            assertEquals("Machine", captured.getEntityType());
            assertEquals(entityId, captured.getEntityId());
            assertEquals("test-trace-12345", captured.getTraceId());
            assertNotNull(captured.getAfterData());
            assertTrue(captured.getAfterData().contains("Lathe A1"));
        } finally {
            MDC.clear();
        }
    }

    @Test
    void record_PersistenceFailure_ThrowsRuntimeExceptionToTriggerRollback() {
        doThrow(new RuntimeException("DB Connection Timeout")).when(auditEventRepository).save(any(AuditEvent.class));

        assertThrows(RuntimeException.class, () ->
                auditRecordingService.record(
                        UUID.randomUUID(),
                        "USER_CREATED",
                        "User",
                        UUID.randomUUID(),
                        null,
                        Map.of("email", "test@factoryos.local")
                )
        );
    }
}
