package org.datamate.identity.shared.config;

import org.datamate.authz.api.endpoint.AuthzBeans;
import org.datamate.authz.api.endpoint.EndpointAuthorization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.datamate.identity.identity.domain.constant.IdentityConstants;

@Configuration
public class AuthzEndpointConfig {

    public static final String POLICY_ADMIN_ROLE_ID = IdentityConstants.POLICY_ADMIN_ROLE_ID_STRING;

    /**
     * Common authorization rule: only POLICY_ADMIN (by UUID or name) can manage policies.
     * Reused across all activated endpoints.
     */

    //move to seperate class
    private final EndpointAuthorization policyAdminAuth = context -> {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean hasAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals(POLICY_ADMIN_ROLE_ID) ||
                                   a.getAuthority().equals("ROLE_" + POLICY_ADMIN_ROLE_ID));
            if (!hasAdmin) {
                throw new AccessDeniedException("Access Denied: POLICY_ADMIN authority required");
            }
        }
    };

    @Bean(AuthzBeans.FIELDS)
    public EndpointAuthorization fieldsAuth()       { return policyAdminAuth; }

    @Bean(AuthzBeans.POLICIES)
    public EndpointAuthorization policiesAuth()      { return policyAdminAuth; }

    @Bean(AuthzBeans.SAVE_POLICIES)
    public EndpointAuthorization savePoliciesAuth()  { return policyAdminAuth; }

    @Bean(AuthzBeans.NAMESPACES)
    public EndpointAuthorization namespacesAuth()    { return policyAdminAuth; }

    // BUNDLE — auto-configured by the starter (open or API-key protected)
    // SUBJECTS — auto-configured by the starter (open by default)
}
