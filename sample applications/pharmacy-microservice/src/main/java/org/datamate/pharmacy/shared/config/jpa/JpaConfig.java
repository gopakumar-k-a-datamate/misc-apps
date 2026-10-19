package org.datamate.pharmacy.shared.config.jpa;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration — entity scanning and repository registration.
 * Extracted from PharmacyApplication to follow dental project's convention.
 */
@Configuration
@EnableJpaRepositories(basePackages = "org.datamate.pharmacy")
@EntityScan(basePackages = "org.datamate.pharmacy")
public class JpaConfig {
}
