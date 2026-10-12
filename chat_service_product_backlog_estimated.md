# Agile Product Backlog: Chat Service (Estimated)

This backlog outlines the implementation plan for the new standalone chat service. The user stories are prioritized in the order of development and include specific tasks across backend, frontend, and testing disciplines to fulfill the technical requirements.

---

## Epic 1: Messaging Foundation

### US 1.1: Core Chat Messaging (Story Points: 5)
**As a** user,
**I want** to send and receive text messages in a conversation
**So that** I can communicate with others in real-time.

*   **Tasks:**
    *   **[Backend]** Design the database models (`Thread` and `Message`) using purely UUIDs as primary keys to ensure decoupling from specific domain entities. *(Estimated: 2h)*
    *   **[Backend]** Implement REST API endpoints for fetching paginated message history (`GET`) and submitting new messages (`POST`). Note: These endpoints should not require `threadId` in the URL path or query params, as it will be extracted from the security principal. *(Estimated: 4h)*
    *   **[Backend]** Implement lazy initialization logic in the database layer to automatically create a `Thread` record upon receiving the first message, avoiding the need for a separate creation API. *(Estimated: 2h)*
    *   **[Frontend]** Verify if the new backend implementation changed the REST endpoints (e.g., URL paths) or the JSON payload keys, and update the Flutter data models/Dio clients accordingly. *(Estimated: 3h)*
    *   **[Testing]** Write backend unit tests verifying that threads are created idempotently on the first message and that history is returned correctly. *(Estimated: 3h)*

---

## Epic 2: Secure Real-Time Communication

### US 2.1: WebSocket Infrastructure & Client Synchronization (Story Points: 8)
**As a** user,
**I want** to connect to a real-time messaging stream
**So that** my chat interface can instantly receive incoming messages.

*   **Tasks:**
    *   **[Backend]** Configure the WebSocket infrastructure using the STOMP protocol. *(Estimated: 3h)*
    *   **[Backend]** Implement broadcasting logic in the message saving flow to instantly push new messages to the specific thread's WebSocket topic. *(Estimated: 3h)*
    *   **[Frontend]** Verify if the backend STOMP connection URL, STOMP broker routing, or topic nomenclature (`/topic/discussion.$threadId`) has changed, and update `WebSocketService` accordingly. *(Estimated: 2h)*
    *   **[Frontend]** Establish a persistent WebSocket connection, subscribe to the active conversation thread's topic, and dynamically render incoming messages into the chat interface. *(Estimated: 4h)*

### US 2.2: Distributed REST-to-WebSocket Fanout (Message Broadcasting) (Story Points: 8)
**As a** system architect,
**I want** REST-based message saves to trigger an internal broker event and fan out to all WebSocket nodes
**So that** real-time broadcasting scales horizontally across multiple server instances.

*   **Tasks:**
    *   **[Backend]** Configure the connection to the enterprise message broker (e.g., Kafka or RabbitMQ) to serve as the internal event bus. *(Estimated: 4h)*
    *   **[Backend]** Refactor the `POST /messages` endpoint to publish a "New Message Saved" payload to the event bus instead of attempting to broadcast locally. *(Estimated: 3h)*
    *   **[Backend]** Implement an event consumer on all server nodes that listens to the internal event bus and routes incoming messages directly to its connected STOMP topic subscribers. *(Estimated: 5h)*
    *   **[Testing]** Perform load and multi-node integration testing to ensure messages sent to a REST endpoint on Node A are successfully broadcasted to WebSocket clients connected to Node B. *(Estimated: 4h)*

---

## Epic 3: Authentication and Access Control

### US 3.1: Token-Based Delegated Access (Story Points: 5)
**As a** system administrator,
**I want** the chat service to authenticate users via security tickets issued by upstream systems
**So that** the chat service remains completely decoupled from domain-specific user and role management.

*   **Tasks:**
    *   **[Backend]** Configure stateless security middleware to parse and validate signed JWT tickets on all incoming REST requests. *(Estimated: 2h)*
    *   **[Backend]** Implement a custom `OncePerRequestFilter` specifically for collaboration, which extracts all information from the token (including `userId` and `threadId`) and stores it in the Spring `Principal`. *(Estimated: 3h)*
    *   **[Backend]** Enforce strict authorization checks ensuring the provided token explicitly grants access to the requested thread UUID. *(Estimated: 2h)*
    *   **[Frontend]** Verify if the backend expects the token in a different header format (e.g., `X-Auth-Token` instead of `Authorization: Bearer`), or requires it inside the STOMP CONNECT frame differently, and adjust the frontend handshake. *(Estimated: 2h)*
    *   **[Testing]** Write security integration tests to ensure invalid, expired, or mismatched tokens are strictly rejected with proper `401/403` error codes across both REST and WebSocket channels. *(Estimated: 3h)*

---

## Epic 4: Secure File Sharing

### US 4.1: Message Attachments (Story Points: 5)
**As a** user,
**I want** to attach files to my messages
**So that** I can share documents and media with other participants.

*   **Tasks:**
    *   **[Backend]** Design the `Attachment` database model with a 1-to-1 relationship to the `Message` model, allowing the message text to serve as a caption. *(Estimated: 2h)*
    *   **[Backend]** Implement REST APIs for secure file upload (`POST`) and download (`GET`) integrated with the storage service. *(Estimated: 4h)*
    *   **[Frontend]** Verify if the backend requires `multipart/form-data` with specific field names (e.g., `file` vs `attachment`), and confirm how the attachment URL/ID should be sent in the final message payload. Update the upload flow accordingly. *(Estimated: 3h)*
    *   **[Testing]** Verify successful file upload, secure download, and accurate database linking between messages and attachments. *(Estimated: 2h)*

### US 4.2: Antivirus and Upload Restrictions (Story Points: 8)
**As a** security administrator,
**I want** uploads to be strictly restricted and scanned for threats
**So that** the enterprise storage system remains secure from malicious payloads.

*   **Tasks:**
    *   **[Backend]** Configure gateway and application limits to strictly reject files exceeding 5MB. *(Estimated: 1h)*
    *   **[Backend]** Implement an explicit block-list utility to reject executable and script file extensions (e.g., `.xml`, `.bpmn`, `.exe`, `.bat`). *(Estimated: 1h)*
    *   **[Backend]** Integrate a ClamAV TCP client to stream and scan files in real-time during the upload process, immediately aborting if a virus is detected. *(Estimated: 5h)*
    *   **[Frontend]** Verify the specific HTTP error codes and JSON error message structure the new backend returns for antivirus rejections, and map them correctly to the UI snackbars. *(Estimated: 2h)*
    *   **[Testing]** Perform security testing using safe test files (e.g., EICAR standard antivirus test file) to ensure the ClamAV integration successfully intercepts and blocks threats. *(Estimated: 3h)*

---

## Epic 5: System Integrations

### US 5.1: Automated Audit Logs (Story Points: 3)
**As an** integrated system,
**I want** to publish background status events into a chat thread
**So that** users can view a unified audit trail of automated actions.

*   **Tasks:**
    *   **[Backend]** Utilize the message broker infrastructure (established in US 2.2) to implement a consumer that receives incoming events containing the `threadId`, allowing it to store the event as a message in the appropriate thread. *(Estimated: 3h)*
    *   **[Backend]** Parse incoming events and save them into the database as read-only system messages (flagged via an `isSystemMessage` boolean). *(Estimated: 2h)*
    *   **[Backend]** Broadcast the saved system messages to the active WebSocket topic. *(Estimated: 1h)*
    *   **[Frontend]** Verify if the backend changed the flag from `isSystemMessage` to something else like `type: 'AUDIT'` or `system_message`, and update the JSON serialization mappings. *(Estimated: 2h)*
    *   **[Testing]** Write integration tests verifying that simulated broker events are successfully consumed, saved to the database, and broadcasted to connected WebSocket clients. *(Estimated: 2h)*

---

## Epic 6: Advanced File Processing

### US 6.1: Automatic Document Previews (Story Points: 8)
**As a** user,
**I want** uploaded documents to automatically generate preview images or PDFs
**So that** I can view their contents directly in the chat without downloading the original files.

*   **Tasks:**
    *   **[Backend]** Intercept non-PDF document uploads (e.g., DOCX, XLSX) in the asynchronous processing pipeline. *(Estimated: 2h)*
    *   **[Backend]** Integrate a background conversion utility (like LibreOffice or JODConverter) to process and generate preview PDFs from the intercepted documents. *(Estimated: 6h)*
    *   **[Backend]** Update the corresponding `Attachment` database record with the newly generated `previewUrl`. *(Estimated: 1h)*
    *   **[Frontend]** Verify if the backend sends `previewUrl` directly on the message JSON or inside a nested `attachments` object array, and update the parsing logic to display the preview inline. *(Estimated: 2h)*
    *   **[Testing]** Test the conversion pipeline with various document formats to ensure previews are generated accurately and URLs are updated correctly. *(Estimated: 3h)*
