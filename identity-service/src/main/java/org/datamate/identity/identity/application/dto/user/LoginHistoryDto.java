package org.datamate.identity.identity.application.dto.user;

import java.time.LocalDateTime;

public record LoginHistoryDto(
        String username,
        LocalDateTime loginTime,
        String status,
        String clientIp,
        String userAgent
) {}
