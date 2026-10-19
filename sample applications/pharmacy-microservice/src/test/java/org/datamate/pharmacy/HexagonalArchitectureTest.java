package org.datamate.pharmacy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@DisplayName("Hexagonal Architecture Tests (Ports & Adapters)")
class HexagonalArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.pharmacy");
    }

    @Test
    @DisplayName("Inbound ports should follow naming convention *UseCase")
    void inboundPortsShouldFollowNamingConvention() {
        ArchRule rule = classes().that().resideInAPackage("..port.in..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("UseCase")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("UseCases should reside in port.in package")
    void useCasesShouldResideInPortInPackage() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("UseCase")
                .should().resideInAPackage("..port.in..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Outbound ports should follow naming convention *Port")
    void outboundPortsShouldFollowNamingConvention() {
        ArchRule rule = classes().that().resideInAPackage("..port.out..")
                .and().areInterfaces()
                .should().haveSimpleNameEndingWith("Port")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("REST controllers should reside in adapter.in.rest package")
    void restControllersShouldResideInAdapterInRestPackage() {
        ArchRule rule = classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage("..adapter.in.rest..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Persistence adapters should reside in adapter.out.persistence package")
    void persistenceAdaptersShouldResideInAdapterOutPersistencePackage() {
        ArchRule rule = classes().that().haveSimpleNameEndingWith("PersistenceAdapter")
                .should().resideInAPackage("..adapter.out.persistence..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Application services should depend on ports, not adapters")
    void applicationServicesShouldDependOnPortsNotAdapters() {
        ArchRule rule = noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Controllers should not access persistence layer directly")
    void controllersShouldNotAccessPersistenceLayerDirectly() {
        ArchRule rule = noClasses().that().resideInAPackage("..adapter.in.rest..")
                .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
