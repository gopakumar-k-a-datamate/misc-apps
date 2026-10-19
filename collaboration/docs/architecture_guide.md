# Architecture Guide — Hexagonal + DDD

> This document describes **how** our architecture works and **why** each rule exists.
> It is framework-agnostic and does not reference specific files — it is the mental model
> every developer must internalize before contributing to this service.

---

## 1. The Architecture at a Glance

We follow **Hexagonal Architecture** (also known as Ports & Adapters) combined with
**Domain-Driven Design (DDD)** tactical patterns. The core idea is simple:

> **Business logic must never depend on infrastructure. Infrastructure depends on business logic.**

This is achieved through **Dependency Inversion** — the domain and application layers define
interfaces (ports) that the infrastructure layer implements (adapters).

```
    ┌──────────────────────────────────────────────────────────────┐
    │                     OUTSIDE WORLD                            │
    │                                                              │
    │   REST APIs    WebSocket    Message Queues    Scheduled Jobs  │
    │       │            │              │                │          │
    │       ▼            ▼              ▼                ▼          │
    │   ┌────────────────────────────────────────────────────┐      │
    │   │            PRIMARY ADAPTERS (Driving)              │      │
    │   │     (translate external input → use case calls)    │      │
    │   └────────────────────────┬───────────────────────────┘      │
    │                            │                                  │
    │                            ▼                                  │
    │   ┌────────────────────────────────────────────────────┐      │
    │   │              INBOUND PORTS                         │      │
    │   │       (use case interfaces the app exposes)        │      │
    │   ├────────────────────────────────────────────────────┤      │
    │   │           APPLICATION SERVICES                     │      │
    │   │    (orchestrate domain logic, enforce use cases)    │      │
    │   ├────────────────────────────────────────────────────┤      │
    │   │             DOMAIN MODEL                           │      │
    │   │   (entities, value objects, aggregate roots,       │      │
    │   │    domain events — PURE business logic)            │      │
    │   ├────────────────────────────────────────────────────┤      │
    │   │             OUTBOUND PORTS                         │      │
    │   │    (repository / service interfaces the app needs) │      │
    │   └────────────────────────┬───────────────────────────┘      │
    │                            │                                  │
    │                            ▼                                  │
    │   ┌────────────────────────────────────────────────────┐      │
    │   │           SECONDARY ADAPTERS (Driven)              │      │
    │   │    (implement outbound ports using real infra:      │      │
    │   │     databases, file storage, external APIs, etc.)   │      │
    │   └────────────────────────────────────────────────────┘      │
    │                                                              │
    └──────────────────────────────────────────────────────────────┘
```

---

## 2. The Four Layers

### 2.1 Domain Layer — The Center

**What it contains:**
- Aggregate Roots
- Domain Entities
- Value Objects
- Domain Events
- Domain-level validation (invariants)

**Rules:**
- MUST NOT import any framework annotation (`@Entity`, `@Service`, `@Component`, etc.)
- MUST NOT import any infrastructure library (JPA, Spring, HTTP, messaging)
- MUST NOT reference any class from the adapter or config layers
- MAY only use standard Java libraries (`java.util`, `java.time`, etc.) and shared domain primitives from our internal framework
- All domain validation happens inside the model itself (self-validating entities)
- Identity-based equality: `equals()` and `hashCode()` based on the entity's ID, not its fields

**Factory method convention:**
- `create(...)` — for constructing a **new** entity (generates ID, sets timestamps, enforces creation-time invariants)
- `restore(...)` — for reconstituting an entity **from persistence** (no business rules, just hydration)
- Constructors are `private` — all instantiation goes through factory methods

**Why this matters:**
If the domain layer is clean, you can swap out your entire database, switch from REST to gRPC,
or replace your message broker — and the business logic does not change at all.

---

### 2.2 Application Layer — The Orchestrator

**What it contains:**
- **Inbound Ports** — interfaces that define what the application can do (use cases)
- **Outbound Ports** — interfaces that define what the application needs from the outside world (repositories, external services, event publishers)
- **Application Services** — classes that implement inbound ports by orchestrating domain objects and outbound ports
- **DTOs** — data transfer objects for input (commands) and output (responses)
- **Mappers** — convert between domain models and DTOs

**Rules:**
- MUST depend only on the Domain layer and on outbound port interfaces
- MUST NOT import any adapter or infrastructure class
- MUST NOT contain business logic — that belongs in the domain model
- Application services orchestrate: they call domain methods, coordinate repositories, and publish events
- Each use case interface should represent a **single, cohesive action** (e.g., "send a message", "fetch message history")
- Application services are annotated with `@Service` and `@Transactional` — these are the only framework annotations allowed here

**Transaction boundaries:**
- `@Transactional` is placed on the application service method, not on repositories or domain objects
- Read-only queries use `@Transactional(readOnly = true)` for performance optimization
- One transaction = one use case execution

---

### 2.3 Adapter Layer — The Translators

Adapters are the **only classes** that know about external technologies. There are two types:

#### Primary Adapters (Driving / Inbound)

These receive input from the outside world and translate it into use case calls.

| Type | Example |
|---|---|
| REST Controller | Receives HTTP requests, calls inbound ports |
| WebSocket Handler | Receives WebSocket messages, calls inbound ports |
| Event Listener | Receives domain events from other services, calls inbound ports |
| Scheduled Job | Triggered by cron, calls inbound ports |

**Rules:**
- MUST depend only on inbound port interfaces, never on application services directly
- MUST NOT contain business logic
- Responsible for: HTTP status codes, request validation annotations, response formatting, authentication extraction
- One controller per domain concept (not per entity)

#### Secondary Adapters (Driven / Outbound)

These implement outbound port interfaces using real infrastructure.

| Type | Example |
|---|---|
| Persistence Adapter | Implements repository ports using JPA/JDBC |
| Event Publisher Adapter | Implements event port using message broker or Spring Events |
| File Storage Adapter | Implements storage port using S3, MinIO, or local filesystem |
| External API Adapter | Implements service port using HTTP client to another microservice |

**Rules:**
- MUST implement an outbound port interface
- MUST NOT be referenced by any class outside the adapter layer (Dependency Inversion)
- Contains its own private data model (e.g., JPA entities are **separate** from domain entities)
- Handles bidirectional mapping: domain model ↔ infrastructure model

**Persistence adapter structure (per aggregate/entity):**

```
adapter/out/persistence/{concept}/
├── {Concept}PersistenceAdapter   → implements the outbound port
├── {Concept}JpaEntity            → JPA entity (framework-annotated)
└── {Concept}JpaRepository        → Spring Data repository interface
```

**Critical rule: JPA entities and domain entities are SEPARATE classes.**
The JPA entity lives in the adapter layer and is annotated with `@Entity`, `@Table`, etc.
The domain entity lives in the domain layer and has zero annotations.
The persistence adapter maps between them.

---

### 2.4 Config & Exception Layer — Cross-Cutting

**What it contains:**
- Security configuration (filters, security chains)
- Exception handlers (`@RestControllerAdvice`)
- Exception classes and error code enums
- WebSocket configuration
- CORS configuration
- OpenAPI / documentation configuration

**Rules:**
- MAY depend on domain and application layers
- MUST NOT depend on the adapter layer
- Exception classes extend a shared base exception from the framework
- Error codes follow the convention: `{SERVICE}-{MODULE}-{NUMBER}` (e.g., `COLLAB-CHT-001`)
- Exception handlers map intent-based exceptions to HTTP status codes

---

## 3. Dependency Flow — The Golden Rule

```
    Adapter  ──►  Application  ──►  Domain
       │               │
       │               ├── Inbound Ports (interfaces)
       │               ├── Outbound Ports (interfaces)
       │               └── Application Services
       │
       ├── Primary Adapters (implement nothing, call inbound ports)
       └── Secondary Adapters (implement outbound ports)
```

**Arrows point inward.** Nothing in the center knows about the outside.

| From | To | Allowed? |
|---|---|---|
| Domain → Application | ❌ | Domain knows nothing about use cases |
| Domain → Adapter | ❌ | Domain knows nothing about infrastructure |
| Application → Adapter | ❌ | Application uses ports, not adapters |
| Adapter → Domain | ✅ | Adapters map to/from domain models |
| Adapter → Application | ✅ | Adapters call inbound ports / implement outbound ports |
| Application → Domain | ✅ | Services orchestrate domain objects |

These rules are **enforced automatically** by ArchUnit fitness functions at build time.

---

## 4. Aggregate Design Rules

### What Is an Aggregate?

An aggregate is a cluster of domain objects that are treated as a single unit for data consistency.
Every aggregate has exactly one **Aggregate Root** — the entry point for all interactions.

### Our Rules

1. **One repository per aggregate root** — child entities within the aggregate are saved through the root's repository. If performance demands separate repositories (e.g., paginated child queries), this is an explicit, documented trade-off.

2. **Cross-aggregate references use IDs, not object navigation** — if Entity A references Aggregate B, it stores B's UUID, not a direct object reference. This prevents lazy loading traps, N+1 queries, and accidental aggregate boundary violations.

3. **Aggregates are consistency boundaries** — all invariants within an aggregate are enforced synchronously. Cross-aggregate consistency is handled via domain events (eventually consistent).

4. **Aggregate roots extend `AggregateRoot`** from the shared DDD framework to support domain event registration and dispatching.

---

## 5. The Port & Adapter Contract

### Inbound Ports

```java
// GOOD: One use case = one interface
public interface SendMessageUseCase {
    void sendMessage(UUID threadId, String senderId, Command request);
}

// BAD: God interface with all operations
public interface ChatService {
    void sendMessage(...);
    List<Message> getMessages(...);
    void deleteMessage(...);
    void uploadAttachment(...);
}
```

Each inbound port should satisfy the **Interface Segregation Principle** — a primary adapter should only depend on the specific use cases it needs, not an entire service facade.

### Outbound Ports

```java
// GOOD: Domain-centric contract
public interface MessageRepositoryPort {
    Message save(Message message);
    Paged<Message> findByThreadId(UUID threadId, PageQuery query);
}

// BAD: Leaking infrastructure concepts
public interface MessageRepositoryPort {
    MessageJpaEntity save(MessageJpaEntity entity);         // JPA leak
    Page<MessageJpaEntity> findAll(Pageable pageable);      // Spring Data leak
}
```

Outbound ports speak in **domain language** — they accept and return domain objects, not infrastructure types.

---

## 6. Error Handling Philosophy

### Intent-Based Exceptions

We do NOT create one exception class per entity or per error. Instead, we create exceptions based on **intent**:

| Intent | Exception Class | HTTP Status |
|---|---|---|
| A resource was not found | `ResourceNotFoundException` | 404 |
| A domain invariant was violated | `DomainValidationException` | 400 |
| A security constraint was violated | (future) | 403 |

### Error Code Convention

All error codes follow: `{SERVICE_PREFIX}-{MODULE}-{NUMBER}`

| Prefix | Module |
|---|---|
| `VAL` | Reusable validation violations |
| `CHT` | Chat module |
| `ATT` | Attachment module |
| `SEC` | Security module |

Error codes are centralized in a single enum and resolved against Spring `MessageSource`
for internationalized error messages.

### Response Format

All error responses follow **RFC 9457 (Problem Detail)**:

```json
{
  "type": "about:blank",
  "title": "COLLAB-VAL-001",
  "status": 400,
  "detail": "Required field 'text' is missing",
  "errorCode": "COLLAB-VAL-001",
  "timestamp": "2026-10-04T10:30:00+05:30"
}
```

---

## 7. How a New Feature Flows Through the Architecture

When implementing a new feature, follow this order:

```
Step 1: DOMAIN
    └── Define or extend domain entities, value objects, and events
        (pure Java, no annotations, self-validating)

Step 2: APPLICATION — PORTS
    └── Define the inbound port (use case interface)
    └── Define any new outbound port (repository/service interface)

Step 3: APPLICATION — SERVICE
    └── Implement the use case by orchestrating domain objects + outbound ports
    └── Add @Service, @Transactional
    └── Define DTOs and mappers

Step 4: ADAPTER — SECONDARY (DRIVEN)
    └── Implement outbound ports (JPA adapter, event adapter, etc.)
    └── Create JPA entities, repositories, Flyway migration scripts

Step 5: ADAPTER — PRIMARY (DRIVING)
    └── Create/extend REST controller or WebSocket handler
    └── Wire to inbound ports

Step 6: CONFIG (if needed)
    └── Add exception handlers, security rules, etc.

Step 7: TEST
    └── Unit tests for domain models and application services
    └── ArchUnit test to verify boundaries
    └── Integration tests with Testcontainers
```

**Never start from the controller and work inward.** Always start from the domain and work outward.

---

## 8. Key Conventions Summary

| Convention | Rule |
|---|---|
| **Package structure** | Feature-sliced: `{feature}/domain`, `{feature}/application`, `{feature}/adapter` |
| **Domain models** | Zero framework annotations, private constructors, `create()` + `restore()` factory methods |
| **JPA entities** | Separate from domain models, live in adapter layer only |
| **Inbound ports** | One interface per use case (Interface Segregation Principle) |
| **Outbound ports** | Speak domain language, return domain objects |
| **Controllers** | Depend on port interfaces, never on services or repositories |
| **Persistence adapters** | Implement outbound ports, handle domain ↔ JPA mapping internally |
| **Transactions** | Declared on application service methods, not repositories |
| **Exceptions** | Intent-based hierarchy, centralized error codes, RFC 9457 responses |
| **Migrations** | Flyway (versioned SQL scripts), never `ddl-auto` |
| **Cross-aggregate refs** | UUID foreign keys only, no JPA `@ManyToOne` across aggregates |
| **Equality** | Identity-based (`equals`/`hashCode` on entity ID) |
| **Logging** | Framework logger (`@EnableLogger`), not raw SLF4J |
| **Pagination** | Framework's `PageQuery` → `Paged<T>` → `PaginatedResponse<T>` pipeline |

---

*This guide is the source of truth for how architecture is practiced in this service.
Every pull request is reviewed against these principles.*
