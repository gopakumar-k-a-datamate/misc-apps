package org.datamate.pharmacy.shared.config.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration.
 * Provides the current authenticated user for @CreatedBy / @LastModifiedBy fields.
 * Aligned with the dental project's AuditorConfig.
 */
@Configuration
public class AuditorConfig {

    private static final String SYSTEM_USER = "SYSTEM";
    private static final String ANONYMOUS_USER = "anonymousUser";

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || ANONYMOUS_USER.equals(auth.getName())) {
                return Optional.of(SYSTEM_USER);
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
