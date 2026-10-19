package org.datamate.pharmacy.config;

import org.datamate.authz.api.endpoint.AuthzBeans;
import org.datamate.authz.api.endpoint.EndpointAuthorization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthzEndpointConfig {

    /**
     * Exposes the GET /internal/authz/bundle/{namespace} endpoint for OPA.
     * We don't perform additional authorization here because this endpoint
     * is purely for the internal OPA sidecar to fetch its policies.
     */
    @Bean(name = AuthzBeans.BUNDLE)
    public EndpointAuthorization bundleAuthorization() {
        return context -> {
            // Unrestricted access for the OPA sidecar
        };
    }
}
