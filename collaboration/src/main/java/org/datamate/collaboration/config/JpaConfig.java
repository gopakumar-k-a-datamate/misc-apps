package org.datamate.collaboration.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA Configuration isolated in Config layer to allow @WebMvcTest slices to run without JPA.
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.datamate.collaboration.chat.adapter.out.persistence")
@EntityScan(basePackages = "org.datamate.collaboration.chat.adapter.out.persistence")
public class JpaConfig {
}
