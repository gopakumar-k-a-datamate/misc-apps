package org.datamate.collaboration.config;

import org.datamate.authz.api.policy.PolicyEvaluationClient;
import org.datamate.authz.api.principal.PrincipalProvider;
import org.datamate.authz.client.config.OpaProperties;
import org.datamate.authz.enforcement.DefaultPolicyEnforcer;
import org.datamate.authz.enforcement.DefaultPrincipalProvider;
import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.authz.rest.client.RestPolicyEvaluationClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clean PEP (Policy Enforcement Point) configuration for Collaboration service.
 * Connects OPA Policy Evaluation client and PolicyEnforcer without bootstrapping
 * Authz JPA schema/repositories or Admin bundle endpoints.
 */
@Configuration
@EnableConfigurationProperties(OpaProperties.class)
public class ChatAuthzConfig {

    @Bean
    @ConditionalOnMissingBean(PolicyEvaluationClient.class)
    public PolicyEvaluationClient restPolicyEvaluationClient(
            RestTemplateBuilder restTemplateBuilder,
            OpaProperties opaProperties) {
        return new RestPolicyEvaluationClient(restTemplateBuilder, opaProperties.getEvaluationUrl());
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
