# Bedrock Storage Framework — Integration & Usage Guide

This guide explains how to integrate and use the **Bedrock Framework Storage** module (`bedrock-storage-starter`) in any external Spring Boot microservice or project.

---

## 1. Overview & Architecture

`framework-storage` follows an **Orchestrated Hexagonal Architecture (Ports and Adapters)**:

```
framework-storage/
├── storage-core/              # Domain & Port interfaces (StorageService, StorageObject)
├── storage-minio/             # Outbound Adapter (MinIO implementation)
└── bedrock-storage-starter/   # Spring Boot Auto-Configuration (Infrastructure)
```

- **Consumer Microservices** depend only on `bedrock-storage-starter`.
- The starter auto-configures the `StorageService` bean into the Spring application context.
- Your application injects `StorageService` as an outbound port without depending on MinIO SDK details directly.

---

## 2. Step-by-Step Integration

### Step 1: Install / Publish Artifacts

Ensure the Bedrock Storage framework is installed in your local repository (`~/.m2/repository`) or deployed to your organization's artifact registry (Nexus/Artifactory):

```bash
# Run from the framework-storage directory
mvn clean install -DskipTests
```

---

### Step 2: Add Maven Dependency

Add `bedrock-storage-starter` to your consumer project's `pom.xml`:

```xml
<dependencies>
    <!-- Bedrock Storage Starter -->
    <dependency>
        <groupId>com.datamate.bedrock.framework.storage</groupId>
        <artifactId>bedrock-storage-starter</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

*(Note: If your project inherits from the Bedrock parent POM or uses its BOM, you can omit the `<version>` tag).*

---

### Step 3: Configure Application Properties

Provide the MinIO connection credentials in your project's configuration.

#### Option A: `application.yml` (Recommended)
```yaml
bedrock:
  storage:
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: my-service-bucket       # Default bucket name for this service
```

#### Option B: `application.properties`
```properties
bedrock.storage.minio.endpoint=http://localhost:9000
bedrock.storage.minio.access-key=minioadmin
bedrock.storage.minio.secret-key=minioadmin
bedrock.storage.minio.bucket=my-service-bucket
```

---

### Step 4: Inject and Use `StorageService` in Your Application

The framework provides the `StorageService` interface. Inject it into your domain or application service via constructor injection.

#### Example Service Implementation:
```java
package com.example.myservice.application.service;

import com.datamate.bedrock.framework.storage.application.port.StorageService;
import com.datamate.bedrock.framework.storage.domain.model.StorageObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileManagementService {

    private final StorageService storageService;

    @Value("${bedrock.storage.minio.bucket:my-service-bucket}")
    private String defaultBucket;

    /**
     * Uploads a file to MinIO.
     */
    public StorageObject uploadFile(MultipartFile file) throws IOException {
        // 1. Ensure target bucket exists
        if (!storageService.bucketExists(defaultBucket)) {
            storageService.createBucket(defaultBucket);
        }

        // 2. Generate unique object key
        String objectKey = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 3. Upload stream
        return storageService.upload(
            defaultBucket,
            objectKey,
            file.getInputStream(),
            file.getContentType(),
            file.getSize()
        );
    }

    /**
     * Downloads a file stream from MinIO.
     */
    public InputStream downloadFile(String objectKey) {
        return storageService.download(defaultBucket, objectKey);
    }

    /**
     * Downloads file as a byte array.
     */
    public byte[] downloadBytes(String objectKey) {
        return storageService.downloadAsBytes(defaultBucket, objectKey);
    }

    /**
     * Deletes a file.
     */
    public void deleteFile(String objectKey) {
        storageService.delete(defaultBucket, objectKey);
    }

    /**
     * Checks if a file exists.
     */
    public boolean fileExists(String objectKey) {
        return storageService.exists(defaultBucket, objectKey);
    }

    /**
     * Lists files with a given prefix.
     */
    public List<StorageObject> listFiles(String prefix) {
        return storageService.listObjects(defaultBucket, prefix);
    }
}
```

---

### Step 5: (Optional) Expose REST Endpoints

If external clients or frontends need to upload and download files through your service, create a controller in your application:

```java
package com.example.myservice.adapter.in.rest;

import com.example.myservice.application.service.FileManagementService;
import com.datamate.bedrock.framework.storage.domain.model.StorageObject;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileManagementService fileManagementService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<StorageObject> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        StorageObject uploadedObject = fileManagementService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedObject);
    }

    @GetMapping("/{objectKey}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String objectKey) {
        InputStream stream = fileManagementService.downloadFile(objectKey);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectKey + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(stream));
    }

    @DeleteMapping("/{objectKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable String objectKey) {
        fileManagementService.deleteFile(objectKey);
    }
}
```

---

## 3. Key Concepts & Best Practices

| Concept | Detail |
| :--- | :--- |
| **Automatic Key Prefixing (`br_`)** | `DefaultStorageService` automatically prefixes object keys with `br_` (e.g. `123_doc.pdf` -> `br_123_doc.pdf`) if not present. File retrieval and deletion methods automatically handle prefix mapping. |
| **Bucket Auto-Verification** | Always check `storageService.bucketExists(bucket)` and create it using `storageService.createBucket(bucket)` before uploads if the bucket might not exist yet. |
| **Spring Boot 3 Compliance** | Auto-configuration is declared in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Ensure the consuming project uses Spring Boot 3+. |
| **Domain Isolation** | Keep business entities (e.g., `Attachment`, `Invoice`, `Prescription`) and database records in your microservice. Use `StorageService` purely for object storage operations. |
| **Logging** | The framework uses Bedrock's `@EnableLogger`. Audit logs for uploads and deletions are automatically output through Bedrock's logging framework. |

---

## 4. Troubleshooting

1. **`StorageException: Failed to create MinIO client`**:
   - Verify that `bedrock.storage.minio.endpoint` is reachable and protocol (`http://` or `https://`) is present.
   - Verify `access-key` and `secret-key`.

2. **`Error executing upload: NoSuchBucket`**:
   - The target bucket does not exist. Ensure you call `storageService.createBucket(bucketName)` or create the bucket via the MinIO Web Console beforehand.

3. **`Field storageService in ... required a bean of type 'StorageService' that could not be found`**:
   - Verify that your application's `@SpringBootApplication` scans the starter or that the starter's auto-configuration is loaded.
   - Confirm that `bedrock-storage-starter` is included as a Maven dependency.
