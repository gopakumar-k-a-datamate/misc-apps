package org.datamate.identity.identity.application.port.out.audit;

import org.datamate.identity.identity.application.dto.user.LoginHistoryDto;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;

public interface AuditLogPersistencePort {
    Paged<LoginHistoryDto> findLoginHistory(String username, PageQuery pageQuery);
}
