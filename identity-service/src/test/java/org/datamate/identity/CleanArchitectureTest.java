package org.datamate.identity;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Clean Architecture Tests")
public class CleanArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.identity");
    }

    @Test
    @DisplayName("Domain layer should not depend on application or adapter layers")
    void domainShouldNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..application..",
                        "..adapter..",
                        "..infrastructure.."
                )
                .because("Domain must not depend on application/adapter layers, but can use shared cross-cutting utilities");

        rule.check(classes);
    }

    @Test
    @DisplayName("Domain models should not use framework annotations")
    void domainModelsShouldBePureJava() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain.model..")
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                .orShould().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("jakarta.persistence.Table")
                .because("Domain models must be pure Java without framework dependencies");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application layer should only depend on domain layer")
    void applicationShouldOnlyDependOnDomain() {
        DescribedPredicate<JavaClass> allowedPackages = resideInAnyPackage(
                "..domain..",
                "..application..",
                "..api..",
                "java..",
                "com.datamate.bedrock..",
                "jakarta.validation..",
                "jakarta.annotation..",
                "lombok..",
                "com.fasterxml.jackson..",
                "org.datamate.identity.shared..",
                "io.swagger.v3.oas.annotations..",
                "org.springframework.boot.context.properties..",
                "org.springframework.core.io..",
                "org.springframework.web.multipart..",
                "org.springframework.transaction.support.."
        );

        DescribedPredicate<JavaClass> allowedSpringClasses =
                type(org.springframework.stereotype.Service.class)
                .or(type(org.springframework.stereotype.Component.class))
                .or(type(org.springframework.modulith.NamedInterface.class))
                .or(type(org.springframework.context.ApplicationEventPublisher.class))
                .or(type(org.springframework.transaction.annotation.Transactional.class))
                .or(type(org.springframework.transaction.annotation.Isolation.class))
                .or(type(org.springframework.transaction.annotation.Propagation.class))
                .or(type(org.springframework.beans.factory.annotation.Autowired.class))
                .or(type(org.springframework.beans.factory.annotation.Value.class))
                .or(type(Configuration.class))
                .or(type(Bean.class))
                .or(type(org.springframework.context.annotation.PropertySource.class))
                .or(type(Bean.Bootstrap.class));

        ArchRule rule = classes()
                .that().resideInAnyPackage("..application..", "..api..")
                .should().onlyDependOnClassesThat(allowedPackages.or(allowedSpringClasses))
                .because("Application should only depend on Domain, cross-cutting concerns, and strictly whitelisted Spring framework features");

        rule.check(classes);
    }

    @Test
    @DisplayName("Should follow Clean Architecture layered structure")
    void shouldFollowCleanArchitectureLayers() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..", "..api..")
                .layer("Adapter").definedBy("..adapter..")

                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Adapter").mayOnlyAccessLayers("Application", "Domain")

                // Allow Domain → Shared dependencies
                // The `shared` module contains cross-cutting, framework-agnostic utilities
                // (e.g., base exceptions) used across layers.
                //
                // It is not considered an outer layer (like application or adapter),
                // and does not violate the dependency rule as long as it remains
                // independent of frameworks and infrastructure.
                .ignoreDependency(
                        resideInAnyPackage("..domain.."),
                        resideInAnyPackage("..shared..")
                )

                // 1. Ignore standard Java & Third-party dependencies for ALL layers
                // This is cleaner than repeating ignoreDependency for every layer
                .ignoreDependency(alwaysTrue(), resideInAnyPackage(
                        "java..",
                        "jakarta..",
                        "org.springframework..",
                        "lombok..",
                        "com.fasterxml.jackson..",
                        "io.swagger.v3.oas.annotations..", // Fixes the Swagger violations
                        "org.hibernate.envers..",          // Fixes Envers violations
                        "org.hibernate.annotations..",     // Allows Hibernate-specific annotations in Adapter layer
                        "org.hibernate.type..",            // Required for SqlTypes
                        "org.hibernate.jdbc..",            // Allows Hibernate JDBC Expectation annotations in Adapter layer
                        "org.springdoc..",
                        "com.datamate.bedrock..",
                        "org.datamate.authz..",
                        "org.datamate.sharedkernel..",
                        "org.datamate.identity.shared..",
                        "net.coobird.thumbnailator..",
                        "javax..",
                        "io.jsonwebtoken.."
                ));

        rule.check(classes);
    }


    @Test
    @DisplayName("Persistence ports (outbound ports) should be interfaces")
    void persistencePortsShouldBeInterfaces() {
        ArchRule rule = classes()
                .that().resideInAPackage("..port.out..")
                .and().haveSimpleNameEndingWith("Port")
                .should().beInterfaces()
                .because("Ports define contracts for infrastructure adapters");

        rule.check(classes);
    }

}
