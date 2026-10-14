package org.datamate.collaboration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
        org.datamate.authz.starter.config.BedrockAuthzAutoConfiguration.class
})
public class CollaborationApplication {
    public static void main(String[] args) {
        SpringApplication.run(CollaborationApplication.class, args);
    }
}
