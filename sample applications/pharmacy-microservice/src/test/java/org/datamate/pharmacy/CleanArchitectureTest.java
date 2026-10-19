package org.datamate.pharmacy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@DisplayName("Clean Architecture Tests")
public class CleanArchitectureTest {

    private JavaClasses classes;

    @BeforeEach
    void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.datamate.pharmacy");
    }

    @Test
    @DisplayName("Domain layer should not depend on application or adapter layers")
    void domainShouldNotDependOnApplicationOrAdapter() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..adapter..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Domain models should not use framework annotations")
    void domainModelsShouldNotUseFrameworkAnnotations() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Application layer should only depend on domain layer")
    void applicationShouldOnlyDependOnDomain() {
        ArchRule rule = classes().that().resideInAPackage("..application..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        "..domain..", 
                        "..application..", 
                        "java..", 
                        "org.datamate.authz..", 
                        "com.datamate.bedrock..", 
                        "jakarta.validation..", 
                        "jakarta.annotation..", 
                        "lombok..", 
                        "com.fasterxml.jackson..",
                        "org.springframework..",
                        "org.datamate.pharmacy.shared.."
                )
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    @DisplayName("Should follow Clean Architecture layered structure")
    void shouldFollowCleanArchitectureLayeredStructure() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter")
                
                .ignoreDependency(alwaysTrue(), resideInAnyPackage(
                        "java..", 
                        "jakarta..", 
                        "org.springframework..", 
                        "lombok..", 
                        "com.fasterxml.jackson..", 
                        "org.datamate.authz..", 
                        "com.datamate.bedrock.."
                ))
                .ignoreDependency(resideInAPackage("..domain.."), resideInAnyPackage(
                        "..config..", 
                        "..messaging..", 
                        "..shared.."
                ))
                .allowEmptyShould(true);
        
        rule.check(classes);
    }

    @Test
    @DisplayName("Persistence ports (outbound ports) should be interfaces")
    void persistencePortsShouldBeInterfaces() {
        ArchRule rule = classes().that().resideInAPackage("..port.out..")
                .should().beInterfaces()
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
