# Architecture & Authorization Guide

This document dives deeper into how Clean Architecture, Hexagonal Architecture, Domain-Driven Design (DDD), and Federated OPA Authorization interconnect in this reference project.

## 1. Architectural Concept Mapping

This table maps theoretical concepts to their concrete implementations in this codebase:

| Concept | Theory / Purpose | Implementation Example |
|---|---|---|
| **Domain Entity** | Core business object, pure Java, ignorant of databases or web frameworks | `domain.model.Doctor` |
| **Inbound Port** | Use Case interface. Defines *what* the system does | `port.in.GetDoctorsUseCase` |
| **Outbound Port** | Interface defining *what* the application needs from the outside world | `port.out.DoctorQueryPort` |
| **Inbound Adapter** | Framework-specific entry point (e.g., REST Controller) | `adapter.in.rest.DoctorController` |
| **Outbound Adapter** | Framework-specific implementation (e.g., Spring Data JPA) | `adapter.out.persistence.adapter.DoctorPersistenceAdapter` |
| **Anti-Corruption Layer** | Mapper converting between Adapter models and Domain models | `adapter.out.persistence.mapper.DoctorJpaMapper` |
| **Application Service** | Orchestrates domain models and ports to execute a use case | `application.service.GetDoctorsService` |

---

## 2. Authorization Flow Sequence

In a decentralized, federated OPA model, the authorization decision happens **locally** within the microservice. The following sequence diagram demonstrates the flow using the **Programmatic Enforcement (Pattern 1)** approach.

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Controller as REST Controller (Adapter)
    participant UseCase as Application Service (Use Case)
    participant Port as Outbound Port (Interface)
    participant DB as Persistence Adapter / DB
    participant PEP as PolicyEnforcer (Port)
    participant OPA as OPA Sidecar (Local)

    Client->>Controller: POST /api/v1/pharmacy/dispense
    Controller->>UseCase: dispense(request)
    
    %% Context Gathering
    Note over UseCase,DB: 1. Context Gathering
    UseCase->>Port: getMedication(request.medId)
    Port->>DB: query
    DB-->>Port: MedicationJpaEntity
    Port-->>UseCase: MedicationDto
    
    UseCase->>Port: getPatient(request.patientId)
    Port->>DB: query
    DB-->>Port: PatientJpaEntity
    Port-->>UseCase: PatientDto
    
    %% Authorization
    Note over UseCase,OPA: 2. Authorization Enforcement
    UseCase->>UseCase: Build DispenseMedicationPolicyResource
    UseCase->>PEP: policyEnforcer.enforce(resource)
    
    PEP->>PEP: Extract Identity (JWT) & Resource Fields (Reflection)
    PEP->>OPA: HTTP POST /v1/data/pharmacy/medication/dispense (EvaluationPayload)
    
    alt OPA Returns {"result": false}
        OPA-->>PEP: Deny
        PEP-->>UseCase: throws AccessDeniedException
        UseCase-->>Controller: bubble up
        Controller-->>Client: 403 Forbidden
    else OPA Returns {"result": true}
        OPA-->>PEP: Allow
        PEP-->>UseCase: void (return success)
        
        %% Business Logic Execution
        Note over UseCase,DB: 3. Business Execution
        UseCase->>UseCase: Verify stock levels
        UseCase->>Port: saveMedication(updatedMed)
        Port->>DB: update
        UseCase-->>Controller: "Successfully dispensed..."
        Controller-->>Client: 200 OK
    end
```

### Flow Breakdown

1. **Context Gathering:** The Application layer does NOT rely on the UI/Client to send authorization context (which could be spoofed). Instead, the Application layer queries the database via Outbound Ports to fetch the true state of the requested resources (e.g., patient age, drug class).
2. **Authorization Enforcement:** The Application layer builds a DTO annotated with `@PolicyResource` and `@PolicyField` and passes it to the `PolicyEnforcer`. The Enforcer (an implementation provided by the `authz` library) handles the heavy lifting of extracting the JWT, parsing the annotations, and communicating with the local OPA sidecar.
3. **Business Execution:** Only if `enforce()` succeeds does the application layer proceed to mutate business state.

---

## 3. Package Dependency Rules

To ensure long-term maintainability, strict dependency rules are enforced using **ArchUnit**.

```mermaid
graph TD
    adapter.in.rest --> application.port.in
    application.usecase --> application.port.in
    application.usecase --> application.port.out
    application.usecase --> domain.model
    adapter.out.persistence --> application.port.out
    adapter.out.persistence --> domain.model
    
    %% The library enforcer is technically an outbound port provided by the library
    application.usecase -. calls .-> PolicyEnforcer
```

**Golden Rules:**
1. `domain` depends on nothing.
2. `application` depends only on `domain` and library standard abstractions. It must not depend on `adapter` packages.
3. `adapter` packages depend on `application` and `domain`. They implement outbound ports or invoke inbound ports.
