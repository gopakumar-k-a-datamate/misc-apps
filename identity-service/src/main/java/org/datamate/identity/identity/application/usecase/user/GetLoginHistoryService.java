package org.datamate.identity.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.user.LoginHistoryDto;
import org.datamate.identity.identity.application.port.in.user.GetLoginHistoryUseCase;
import org.datamate.identity.identity.application.port.out.audit.AuditLogPersistencePort;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLoginHistoryService implements GetLoginHistoryUseCase {

    @EnableLogger
    private Logger log;

    private final AuditLogPersistencePort auditLogPort;

    @Override
    public Paged<LoginHistoryDto> getLoginHistory(String username, PageQuery pageQuery) {
        log.info("Request usecase for login history: filter username: {}, page: {}, size: {}", 
                username, pageQuery.page(), pageQuery.size());
        return auditLogPort.findLoginHistory(username, pageQuery);
    }
}
