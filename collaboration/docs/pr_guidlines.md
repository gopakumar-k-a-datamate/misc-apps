# Pull Request Guidelines

> Every pull request to this service is reviewed against these guidelines.
> This document defines what a well-structured PR looks like, what coding standards
> we enforce, and how the architecture principles map to day-to-day code decisions.

---

## 1. PR Description Template

Every PR description must include:

```markdown
## What
Brief description of what this PR does.

## Why
Business or architectural reason for this change.

## How
High-level approach. Which layers were touched and why.

## Checklist
- [ ] Domain models have zero framework annotations
- [ ] New use cases have dedicated inbound port interfaces
- [ ] Outbound ports return domain objects, not JPA entities
- [ ] Controllers depend only on port interfaces
- [ ] JPA entities are separate from domain entities
- [ ] Flyway migration added for schema changes (no ddl-auto)
- [ ] ArchUnit test passes
- [ ] Unit tests added for new use cases
- [ ] Error codes added to the centralized enum (if new errors introduced)
- [ ] No hardcoded secrets or credentials
```

---

## 2. Coding Standards

### 2.1 Separation of Concerns

Each class must have a **single, clear reason to exist**. If a class handles more than one concern, it must be split.

| Layer | Concern | NOT Its Concern |
|---|---|---|
| Domain Entity | Business invariants, self-validation | Persistence, serialization, HTTP |
| Application Service | Use case orchestration, transaction management | Business rules, SQL queries, HTTP status codes |
| Inbound Port | Defining what the application can do | How it's invoked (REST, WebSocket, CLI) |
| Outbound Port | Defining what the application needs | How it's provided (JPA, JDBC, S3, HTTP client) |
| REST Controller | HTTP request/response handling, status codes | Business logic, direct DB access |
| Persistence Adapter | Domain ↔ JPA entity mapping, query execution | Business rules, response formatting |
| JPA Entity | ORM mapping, table/column definitions | Business validation, domain logic |
| Exception Handler | Exception → HTTP response mapping | Business logic, persistence |
| Mapper | Data transformation between models | Business logic, persistence, HTTP |

**Anti-patterns to reject in review:**

```java
// BAD: Controller contains business logic
@PostMapping
public void send(@RequestBody Request req) {
    if (!threadRepo.existsById(req.threadId())) {   // ← Business logic leaked
        threadRepo.save(new Thread(req.threadId())); // ← Direct repo access
    }
    messageRepo.save(...);
}

// GOOD: Controller delegates to use case
@PostMapping
public void send(@RequestBody Request req, Principal principal) {
    sendMessageUseCase.sendMessage(threadId, principal.getName(), req);
}
```

```java
// BAD: Domain model imports JPA
import jakarta.persistence.Entity;   // ← NEVER in domain layer

@Entity
public class Message { ... }

// GOOD: Domain model is pure
public class Message {
    private final UUID id;
    // ... pure Java, no annotations
}
```

```java
// BAD: Application service returns JPA entity
public MessageJpaEntity sendMessage(...) { ... }  // ← Infrastructure leak

// GOOD: Application service returns domain object or DTO
public void sendMessage(...) { ... }
```

---

### 2.2 Single Responsibility Principle (SRP)

Every class should have **one reason to change**.

| If you need to... | Create... | Don't... |
|---|---|---|
| Add a new use case | A new inbound port + new service class | Add a method to an existing service |
| Add a new query | A new query use case | Overload the command use case |
| Add a new entity | A new domain model + persistence adapter | Add fields to an existing entity |
| Support a new protocol (e.g., WebSocket) | A new primary adapter | Modify the REST controller |
| Change how data is stored | A new/modified secondary adapter | Touch the application service |
| Add error handling for a new case | A new error code + exception handler mapping | Add try-catch in the controller |

**Indicators of SRP violation:**
- A class has more than ~150 lines (domain entities excluded)
- A class has more than 3-4 injected dependencies
- A class name contains "And" (e.g., `MessageAndAttachmentService`)
- A method does validation, persistence, event publishing, AND response formatting

---

### 2.3 Naming Conventions

#### Packages

```
{feature}.domain.model         → Domain entities, value objects
{feature}.domain.event         → Domain events
{feature}.application.port.in  → Inbound port interfaces (use cases)
{feature}.application.port.out → Outbound port interfaces (repositories, services)
{feature}.application.usecase  → Application service implementations
{feature}.application.dto      → Data transfer objects
{feature}.application.mapper   → Model-to-DTO mappers
{feature}.adapter.in.rest      → REST controllers
{feature}.adapter.in.websocket → WebSocket handlers
{feature}.adapter.out.persistence.{concept} → JPA adapters
{feature}.adapter.out.event    → Event publishing adapters
{feature}.adapter.out.storage  → File storage adapters
```

#### Classes

| Type | Naming Pattern | Example |
|---|---|---|
| Aggregate Root | `{Noun}` | `Thread` |
| Domain Entity | `{Noun}` | `Message`, `Attachment` |
| Domain Event | `{Noun}{Verb}Event` (past tense) | `MessageSavedEvent` |
| Inbound Port | `{Verb}{Noun}UseCase` | `SendMessageUseCase` |
| Application Service | `{Verb}{Noun}Service` | `SendMessageService` |
| Outbound Port | `{Noun}RepositoryPort`, `{Noun}Port` | `MessageRepositoryPort`, `EventPublisherPort` |
| REST Controller | `{Noun}Controller` | `ChatController` |
| Persistence Adapter | `{Noun}PersistenceAdapter` | `MessagePersistenceAdapter` |
| JPA Entity | `{Noun}JpaEntity` | `MessageJpaEntity` |
| JPA Repository | `{Noun}JpaRepository` | `MessageJpaRepository` |
| Inbound DTO | `{Verb}{Noun}Request` | `SendMessageRequest` |
| Outbound DTO | `{Noun}Dto` | `MessageDto` |
| Mapper | `{Noun}Mapper` | `MessageMapper` |
| Exception | `{Intent}Exception` | `ResourceNotFoundException`, `DomainValidationException` |
| Error Codes | `{Service}ErrorCodes` | `CollaborationErrorCodes` |

#### Methods

| Context | Pattern | Example |
|---|---|---|
| Factory (new entity) | `create(...)` | `Message.create(threadId, senderId, text, ...)` |
| Factory (from DB) | `restore(...)` | `Message.restore(id, threadId, ...)` |
| Use case methods | `{verb}{noun}(...)` | `sendMessage(...)`, `getMessages(...)` |
| Repository reads | `findBy{Field}(...)` | `findByThreadId(threadId, query)` |
| Repository writes | `save(...)` | `save(message)` |
| Mapper methods | `toDto(...)`, `toDomain(...)`, `toEntity(...)` | `messageMapper.toDto(message)` |

---

### 2.4 Exception & Error Code Standards

**When introducing a new error scenario:**

1. Add an error code to the centralized error codes enum following the pattern `{SERVICE}-{MODULE}-{NUMBER}`
2. Use an existing intent-based exception class (`DomainValidationException`, `ResourceNotFoundException`) unless a genuinely new intent is needed
3. Verify the exception handler maps it to the correct HTTP status


**Error code numbering:**

- Never reuse a retired code
- Document the code's arguments in a Javadoc comment

**Rules:**
- NEVER throw raw `RuntimeException`, `IllegalArgumentException`, or `IllegalStateException`
- NEVER catch exceptions silently (empty catch blocks)
- NEVER use exceptions for control flow
- ALWAYS include context in the exception (the ID that was not found, the field that was invalid)

---

### 2.5 Domain Model Standards

**Self-validating entities:**
```java
// GOOD: Validation inside the entity
private Message(UUID id, UUID threadId, String senderId, ...) {
    requireNonNull(id, "messageId");
    requireNonNull(threadId, "threadId");
    requireNonNull(senderId, "senderId");
    this.id = id;
    // ...
}

// BAD: Validation in the service
public void sendMessage(...) {
    if (senderId == null) throw new ValidationException(...);  // ← belongs in domain
    Message message = new Message(id, threadId, senderId, ...);
}
```

**Immutability:**
- Domain entities should be **effectively immutable** after construction
- Fields are `final` wherever possible
- State changes produce new instances or are done through explicit domain methods with clear names

**Equality:**
- Entity equality is based on **identity** (the entity's ID), not on field values
- Override both `equals()` and `hashCode()` using only the `id` field
- Two entities with the same ID are considered the same entity regardless of other field values

---

### 2.6 DTO Standards

**Use Java `record` types for DTOs:**
```java
// GOOD: Concise, immutable, readable
public record SendMessageRequest(
    @NotBlank(message = "Message text must not be blank")
    String text
) {}

// BAD: Mutable class with getters/setters
public class SendMessageRequest {
    private String text;
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
```

**Rules:**
- Inbound DTOs carry Jakarta Bean Validation annotations (`@NotBlank`, `@NotNull`, `@Size`, etc.)
- Outbound DTOs carry no validation — they represent what the system produces, not what it accepts
- DTOs must never contain domain objects or JPA entities
- DTOs must never contain business logic

---

### 2.7 Persistence Standards

**Flyway over ddl-auto:**
- All schema changes go through versioned Flyway migration scripts (`V{N}__{description}.sql`)
- Never use `spring.jpa.hibernate.ddl-auto=create` or `update` in any profile
- Each migration script must be idempotent where possible
- Include `CREATE INDEX` statements for columns used in `WHERE` or `ORDER BY` clauses

**JPA entity rules:**
- JPA entities are annotated data carriers — no business logic
- Use `@Column` annotations explicitly for all fields (don't rely on Hibernate defaults)
- Use `@Id` with application-generated UUIDs, not database sequences (domain controls identity)
- Prefer raw foreign-key columns (`UUID parentId`) over `@ManyToOne` across aggregate boundaries

**Persistence adapter rules:**
- Each adapter maps bidirectionally: `toDomain(entity)` and `toEntity(domainModel)`
- Pagination uses the framework's `PageQuery` → `Paged<T>` pipeline, converting from Spring Data's `Page<T>`
- Never return `Optional.empty()` when a resource should exist — throw `ResourceNotFoundException`

---

### 2.8 Controller Standards

**Request handling:**
- Use `@PathVariable` for resource identifiers
- Use `@RequestParam` for optional filters and pagination
- Use `@RequestBody` with `@Valid` for command payloads
- Extract the authenticated user from `Principal`, never from custom headers in production

**Response standards:**
- `201 Created` for successful resource creation (POST commands)
- `200 OK` for successful reads (GET queries)
- `204 No Content` for successful deletions (DELETE commands)
- Return `void` for commands that don't need to return data
- Return paginated responses using the framework's `PaginatedResponse<T>`

**Rules:**
- One controller per domain concept (e.g., `ChatController`, `AttachmentController`)
- Controllers depend only on inbound port interfaces
- No business logic in controllers — they are thin translation layers
- No direct repository or service class injection

---

### 2.9 Test Standards

**Every PR must include tests for:**

| Change Type | Required Tests |
|---|---|
| New domain entity / invariant | Unit test for validation rules (`create()` with invalid inputs) |
| New use case / application service | Unit test with mocked outbound ports |
| New REST endpoint | Integration test or `@WebMvcTest` with MockMvc |
| New persistence adapter | Integration test with Testcontainers |
| Schema change | Verify Flyway migration runs cleanly |
| Any change | ArchUnit test must pass (already configured) |

**Test naming:**
```java
@Test
@DisplayName("should lazily create thread when it does not exist")
void shouldCreateThread_WhenThreadDoesNotExist() { ... }
```

- Use `should{ExpectedBehavior}_When{Condition}` or `should{ExpectedBehavior}` pattern
- Always add `@DisplayName` with a human-readable sentence
- Use AssertJ for assertions (`assertThat(...)`)
- Use Mockito for mocking outbound ports in unit tests

**Test organization:**
- Test classes mirror the source package structure
- Test class name = `{ClassUnderTest}Test`
- Use `@ExtendWith(MockitoExtension.class)` for unit tests
- Use `@SpringBootTest` + Testcontainers for integration tests

---

### 2.10 General Java Standards

**Formatting:**
- 4-space indentation
- Opening brace on the same line
- One blank line between methods
- No trailing whitespace
- Maximum line length: 120 characters

**Imports:**
- No wildcard imports (`import java.util.*`)
- Organize: Java standard → third-party → project internal
- Remove unused imports

**Annotations:**
- Lombok: Use `@Getter`, `@RequiredArgsConstructor` for boilerplate reduction
- Use `@Getter` only, not `@Setter` — prefer immutability
- `@Setter` is allowed only on JPA entities (required by Hibernate)
- Use Lombok's `@RequiredArgsConstructor` for constructor injection (not `@Autowired`)

**Javadoc:**
- Required on: all public domain entities, all port interfaces, all exception classes, all error codes
- Optional on: private methods, obvious getter/setter methods, test classes
- Include `@param` and `@return` for non-obvious methods
- Document architectural decisions and trade-offs in class-level Javadoc

**Logging:**
- Use the framework's `@EnableLogger` / `Logger`, not raw SLF4J
- Log at appropriate levels: `error` for system failures, `warn` for recoverable issues, `info` for significant state changes, `debug` for diagnostic detail
- Include correlation context (entity IDs, request IDs) in log messages
- Never log sensitive data (passwords, tokens, personal information)

---

## 3. What Reviewers Look For

### Architecture Compliance

- [ ] **Domain purity**: Domain models have zero framework imports
- [ ] **Port boundaries**: Controllers use port interfaces, not concrete services
- [ ] **Adapter isolation**: No class outside the adapter layer references JPA entities or repositories
- [ ] **Dependency direction**: All dependencies point inward (adapter → application → domain)
- [ ] **ArchUnit passes**: The automated fitness function does not fail

### Code Quality

- [ ] **SRP**: Each class has one responsibility
- [ ] **No god classes**: No service with more than 3-4 dependencies
- [ ] **Immutability**: Domain entities and DTOs are immutable
- [ ] **No null returns**: Use `Optional<T>` for queries that may return nothing
- [ ] **No silent catches**: Every catch block either handles, re-throws, or logs
- [ ] **Meaningful names**: Variables, methods, and classes express intent

### Testing

- [ ] **Coverage**: New logic has corresponding unit tests
- [ ] **Isolation**: Unit tests mock outbound ports, not internal classes
- [ ] **Readability**: Tests are self-documenting with `@DisplayName`
- [ ] **Edge cases**: Null inputs, empty collections, concurrent scenarios are covered

### Security

- [ ] **No hardcoded secrets**: No passwords, API keys, or tokens in source code
- [ ] **Input validation**: All inbound DTOs use Jakarta Bean Validation
- [ ] **Auth check**: Authenticated user is extracted from the security context, not from request body

### Database

- [ ] **Flyway migration**: Schema changes have a new `V{N}__` migration script
- [ ] **Indexes**: New query patterns have corresponding indexes
- [ ] **No ddl-auto**: Hibernate does not auto-create or auto-update schema

---

*These guidelines ensure every contribution maintains the architectural integrity
and code quality standards established in this service. When in doubt, refer to
the Architecture Guide for the underlying principles.*
