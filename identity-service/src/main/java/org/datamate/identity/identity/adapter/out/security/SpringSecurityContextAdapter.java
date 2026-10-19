package org.datamate.identity.identity.adapter.out.security;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.identity.identity.application.port.out.SecurityContextPort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityContextAdapter implements SecurityContextPort {

    @EnableLogger
    private Logger log;

    @Override
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        log.debug("No authenticated user found, defaulting to SYSTEM_ADMIN");
        return "SYSTEM_ADMIN";
    }
}
