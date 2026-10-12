package org.datamate.collaboration;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Standard Spring Boot application context test.
 * Verifies that the Spring IoC container can successfully start up.
 */
@SpringBootTest
@EnableAutoConfiguration(exclude = {
        org.datamate.authz.starter.config.BedrockAuthzAutoConfiguration.class
})
class CollaborationApplicationTests {

    @MockBean
    private PolicyEnforcer policyEnforcer;

    @Test
    void contextLoads() {
    }

}
