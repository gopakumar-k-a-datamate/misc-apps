package org.datamate.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.dto.user.LoginHistoryDto;
import org.datamate.identity.identity.application.port.out.audit.AuditLogPersistencePort;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.datamate.identity.identity.application.usecase.user.GetLoginHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetLoginHistoryServiceTest {

    private AuditLogPersistencePort auditLogPort;
    private Logger log;
    private GetLoginHistoryService getLoginHistoryService;

    @BeforeEach
    void setUp() throws Exception {
        auditLogPort = mock(AuditLogPersistencePort.class);
        log = mock(Logger.class);
        getLoginHistoryService = new GetLoginHistoryService(auditLogPort);

        Field logField = GetLoginHistoryService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(getLoginHistoryService, log);
    }

    @Test
    void shouldDelegateToAuditLogPersistencePortSuccessfully() {
        PageQuery pageQuery = new PageQuery(0, 10);
        LoginHistoryDto testDto = new LoginHistoryDto(
                "test@example.com", LocalDateTime.now(), "SUCCESS", "127.0.0.1", "agent"
        );
        Paged<LoginHistoryDto> expectedPaged = new Paged<>(
                List.of(testDto), 1, 10, 1L, 1, false, false
        );

        when(auditLogPort.findLoginHistory("test@example.com", pageQuery))
                .thenReturn(expectedPaged);

        Paged<LoginHistoryDto> result = getLoginHistoryService.getLoginHistory("test@example.com", pageQuery);

        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals("test@example.com", result.content().get(0).username());
        verify(auditLogPort).findLoginHistory("test@example.com", pageQuery);
    }
}
