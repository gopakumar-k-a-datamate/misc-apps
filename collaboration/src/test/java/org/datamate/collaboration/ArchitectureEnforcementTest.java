package org.datamate.collaboration;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Hexagonal Architecture & DDD Fitness Functions.
 * This test enforces the Ports & Adapters architectural boundaries 
 * which is standard for enterprise-grade microservices.
 */
@AnalyzeClasses(packages = "org.datamate.collaboration", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureEnforcementTest {

    @ArchTest
    static final ArchRule hexagonalArchitectureIsRespected = layeredArchitecture()
            .consideringAllDependencies()
            // 1. The Core (Pure Business Logic)
            .layer("Domain").definedBy("..domain..")
            // 2. The Orchestrator (Use Cases & Ports)
            .layer("Application").definedBy("..application..")
            // 3. The Infrastructure (REST, JPA, external APIs)
            .layer("Adapter").definedBy("..adapter..")
            // 4. Global Configurations & Security Infrastructure
            .layer("Config").definedBy("..config..", "..exception..", "..security..")
            
            // Hexagonal Dependency Rules:
            // Domain is the center; it cannot depend on anything outside itself.
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
            // Application depends only on Domain. It defines ports for the outside world.
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
            // Adapters depend on Application and Domain, but nothing depends on Adapters!
            .whereLayer("Adapter").mayNotBeAccessedByAnyLayer();
}
