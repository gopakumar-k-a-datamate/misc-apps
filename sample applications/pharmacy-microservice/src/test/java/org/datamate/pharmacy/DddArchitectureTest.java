package org.datamate.pharmacy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;

@DisplayName("DDD Architecture Tests")
class DddArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.pharmacy");
    }

    @Test
    @DisplayName("Domain models should not have JPA annotations")
    void domainModelsShouldNotHaveJpaAnnotations() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "javax.persistence..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain events should reside in domain.event package")
    void domainEventsShouldResideInDomainEventPackage() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("Event")
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.event..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain services should not have Spring annotations")
    void domainServicesShouldNotHaveSpringAnnotations() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain.service..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain policies should reside in domain.policy package")
    void domainPoliciesShouldResideInDomainPolicyPackage() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("Policy")
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.policy..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain exceptions should reside in domain.exception package")
    void domainExceptionsShouldResideInDomainExceptionPackage() {
        ArchRule rule = classes().that().areAssignableTo(Exception.class)
                .and().resideInAPackage("..domain..")
                .should().resideInAPackage("..domain.exception..")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
