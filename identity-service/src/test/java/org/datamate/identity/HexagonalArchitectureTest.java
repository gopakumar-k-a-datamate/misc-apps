package org.datamate.identity;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * HexagonalArchitectureTest (Ports and Adapters)
 *
 * Guards the Ports & Adapters pattern:
 * - Ports are interfaces
 * - Adapters implement ports
 * - Application depends on ports, not adapters
 */
@DisplayName("Hexagonal Architecture Tests (Ports & Adapters)")
class HexagonalArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.identity");
    }

    @Test
    @DisplayName("Inbound ports should follow naming convention *UseCase")
    void inboundPortsShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..port.in..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("UseCase")
                .because("Inbound ports represent use cases and should be named accordingly");

        rule.check(classes);
    }

    @Test
    @DisplayName("UseCases should reside in port.in package")
    void useCasesShouldResideInPortInPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("UseCase")
                .should().resideInAPackage("..port.in..")
                .because("UseCases are inbound ports and should reside in port.in packages");

        rule.check(classes);
    }

    @Test
    @DisplayName("Outbound ports should follow naming convention *Port")
    void outboundPortsShouldFollowNamingConvention() {
        ArchRule rule = classes()
                .that().resideInAPackage("..port.out..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Port")
                .because("Outbound ports define contracts for infrastructure");

        rule.check(classes);
    }

    @Test
    @DisplayName("REST controllers should reside in adapter.in.rest package")
    void controllersShouldBeInInboundAdapter() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .should().resideInAPackage("..adapter.in.rest..")
                .because("Controllers are inbound adapters");

        rule.check(classes);
    }

    @Test
    @DisplayName("Persistence adapters should reside in adapter.out.persistence package")
    void persistenceAdaptersShouldBeInOutboundAdapter() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("PersistenceAdapter")
                .should().resideInAPackage("..adapter.out.persistence..")
                .because("Persistence adapters are outbound adapters");

        rule.check(classes);
    }

    @Test
    @DisplayName("Application services should depend on ports, not adapters")
    void applicationShouldDependOnPortsNotAdapters() {
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..application..", "..api..")
                .should().dependOnClassesThat()
                .resideInAPackage("..adapter..")
                .because("Application should depend on port interfaces, not adapter implementations");

        rule.check(classes);
    }

    @Test
    @DisplayName("Controllers should not access persistence layer directly")
    void controllersShouldNotAccessPersistenceDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..adapter.in.rest..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..adapter.out.persistence..",
                        "..infrastructure.."
                )
                .because("Controllers should only depend on use cases (inbound ports)");

        rule.check(classes);
    }
}
