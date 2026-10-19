package org.datamate.identity.identity.adapter.out.persistence.audit.adapter;

import com.datamate.bedrock.framework.common.auditing.entity.ServiceAuditLog;
import org.datamate.identity.identity.adapter.out.persistence.audit.repository.AuditLogRepository;
import org.datamate.identity.identity.adapter.out.persistence.audit.specification.AuditLogSpecification;
import org.datamate.identity.identity.application.dto.user.LoginHistoryDto;
import org.datamate.identity.identity.application.port.out.audit.AuditLogPersistencePort;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogPersistencePort {

    private final AuditLogRepository auditLogRepository;

    @Override
    public Paged<LoginHistoryDto> findLoginHistory(String username, PageQuery pageQuery) {
        Pageable pageable = PageRequest.of(
                pageQuery.page(),
                pageQuery.size(),
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Specification<ServiceAuditLog> spec = AuditLogSpecification.withActionAndUsername("USER_LOGIN", username);
        Page<ServiceAuditLog> pageResult = auditLogRepository.findAll(spec, pageable);

        List<LoginHistoryDto> dtos = pageResult.getContent().stream()
                .map(log -> new LoginHistoryDto(
                        extractUsernameFromArguments(log.getArguments(), log.getUsername()),
                        log.getCreatedDate(),
                        log.getStatus() != null ? log.getStatus().name() : "SUCCESS",
                        log.getClientIp(),
                        log.getUserAgent()
                ))
                .toList();

        return new Paged<>(
                dtos,
                pageResult.getNumber() + 1, // Return 1-based page index to client
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.hasNext(),
                pageResult.hasPrevious()
        );
    }

    private String extractUsernameFromArguments(String argumentsJson, String defaultUsername) {
        if (argumentsJson == null) {
            return defaultUsername;
        }
        try {
            Matcher matcher = Pattern.compile("\"user[nN]ame\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(argumentsJson);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Fallback to default if parsing fails
        }
        return defaultUsername;
    }
}
