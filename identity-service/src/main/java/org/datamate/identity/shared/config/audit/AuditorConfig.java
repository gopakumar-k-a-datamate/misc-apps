package org.datamate.identity.shared.config.audit;

import org.datamate.identity.shared.constants.AppConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
public class AuditorConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || AppConstants.ANONYMOUS_USER.equals(auth.getName())) {
                return Optional.of(AppConstants.SYSTEM_USER);
            }
            return Optional.ofNullable(auth.getName());
        };
    }
}
