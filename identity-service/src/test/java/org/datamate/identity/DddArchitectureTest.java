package org.datamate.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * DddArchitectureTest (Domain-Driven Design)
 *
 * Guards DDD tactical patterns:
 * - Domain models are framework-independent
 * - Proper package structure (model, event, exception, policy)
 * - Rich domain models, not anemic data structures
 */
@DisplayName("DDD Architecture Tests")
class DddArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.identity");
    }

    @Test
    @DisplayName("Domain models should not have JPA annotations")
    void domainModelsShouldNotHaveJpaAnnotations() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("jakarta.persistence.Table")
                .orShould().beAnnotatedWith("jakarta.persistence.Column")
                .orShould().beAnnotatedWith("jakarta.persistence.Id")
                .because("Domain models must be persistence-ignorant (DDD tactical pattern)");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain events should reside in domain.event package")
    void domainEventsShouldBeInEventPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Event")
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.event..")
                .because("Domain events should be organized in domain.event package");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain services should not have Spring annotations")
    void domainServicesShouldBePureJava() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.service..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                .because("Domain services should be framework-independent")
                .allowEmptyShould(true);

        rule.check(classes);
    }
    

    @Test
    @DisplayName("Domain policies should reside in domain.policy package")
    void domainPoliciesShouldBeInPolicyPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Policy")
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.policy..")
                .because("Business policies should be organized in domain.policy")
                .allowEmptyShould(true);

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain exceptions should reside in domain.exception package")
    void domainExceptionsShouldBeInExceptionPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Exception")
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.exception..")
                .because("Domain exceptions should be organized in domain.exception");

        rule.check(classes);
    }
}
