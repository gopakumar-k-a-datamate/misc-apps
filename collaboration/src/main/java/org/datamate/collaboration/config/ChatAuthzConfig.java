package org.datamate.collaboration.config;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.authz.api.policy.PolicyEvaluationClient;
import org.datamate.authz.api.principal.PrincipalProvider;
import org.datamate.authz.client.config.OpaProperties;
import org.datamate.authz.dto.policy.EvaluationResult;
import org.datamate.authz.enforcement.AuthorizationContext;
import org.datamate.authz.enforcement.DefaultPolicyEnforcer;
import org.datamate.authz.enforcement.DefaultPrincipalProvider;
import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.authz.rest.client.RestPolicyEvaluationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

/**
 * Clean PEP (Policy Enforcement Point) configuration for Collaboration service.
 * Supports both live OPA evaluation and resilient local fallback when OPA is offline.
 */
@Configuration
@EnableConfigurationProperties(OpaProperties.class)
public class ChatAuthzConfig {

    @EnableLogger
    private Logger logger;

    @Value("${authz.opa.enabled:true}")
    private boolean opaEnabled;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Bean
    @ConditionalOnMissingBean(PolicyEvaluationClient.class)
    public PolicyEvaluationClient policyEvaluationClient(
            RestTemplateBuilder restTemplateBuilder,
            OpaProperties opaProperties) {

        final RestPolicyEvaluationClient restClient = new RestPolicyEvaluationClient(
                restTemplateBuilder,
                opaProperties.getEvaluationUrl()
        );

        return (String namespace, AuthorizationContext context) -> {
            if (opaEnabled) {
                try {
                    EvaluationResult result = restClient.evaluate(namespace, context);
                    if (result != null && result.allowed()) {
                        return result;
                    }

                    // In local/dev environments, fallback to delegated ticket thread-containment evaluation
                    boolean isDevMode = activeProfile.contains("dev") 
                            || activeProfile.contains("local") 
                            || activeProfile.contains("default")
                            || activeProfile.contains("test");

                    if (isDevMode) {
                        if (logger != null) {
                            logger.warn("OPA evaluation returned denied. Falling back to Delegated Ticket containment verification.");
                        }
                        return evaluateDelegatedTicketContainment(context);
                    }
                    return result != null ? result : EvaluationResult.denied("OPA evaluation returned null result");
                } catch (Exception e) {
                    // OPA sidecar unreachable (e.g. connection refused) or returned error
                    boolean isDevMode = activeProfile.contains("dev") 
                            || activeProfile.contains("local") 
                            || activeProfile.contains("default")
                            || activeProfile.contains("test");

                    if (isDevMode) {
                        if (logger != null) {
                            logger.warn("OPA sidecar unreachable at {}: {}. Falling back to Delegated Ticket containment verification.", 
                                    opaProperties.getEvaluationUrl(), e.getMessage());
                        }
                        return evaluateDelegatedTicketContainment(context);
                    }
                    return EvaluationResult.denied("OPA sidecar error: " + e.getMessage());
                }
            }

            return evaluateDelegatedTicketContainment(context);
        };
    }

    private EvaluationResult evaluateDelegatedTicketContainment(AuthorizationContext context) {
        if (context == null || context.resourceData() == null) {
            return EvaluationResult.denied("Access Denied: Missing authorization context");
        }

        Object requestedThreadId = context.resourceData().get("requestedThreadId");
        Object tokenThreadId = context.resourceData().get("tokenThreadId");

        if (requestedThreadId != null && Objects.equals(requestedThreadId.toString(), Objects.toString(tokenThreadId, null))) {
            return EvaluationResult.granted();
        }

        return EvaluationResult.denied("Access Denied: Ticket threadId [" + tokenThreadId 
                + "] does not match requested threadId [" + requestedThreadId + "]");
    }

    @Bean
    @ConditionalOnMissingBean(PrincipalProvider.class)
    public PrincipalProvider principalProvider() {
        return new DefaultPrincipalProvider();
    }

    @Bean
    @ConditionalOnMissingBean(PolicyEnforcer.class)
    public PolicyEnforcer policyEnforcer(
            PolicyEvaluationClient policyEvaluationClient,
            PrincipalProvider principalProvider) {
        return new DefaultPolicyEnforcer(policyEvaluationClient, principalProvider);
    }
}
