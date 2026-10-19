package org.datamate.collaboration.chat.adapter.out.persistence.message.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.datamate.collaboration.chat.adapter.out.persistence.thread.entity.ThreadJpaEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for the {@code chat_messages} table.
 * <p>
 * Uses a raw {@code threadId} UUID column instead of a {@code @ManyToOne}
 * relationship to {@link ThreadJpaEntity}
 * to enforce aggregate boundaries and eliminate N+1 query problems
 * when fetching paginated message history.
 * <p>
 * Used exclusively by the persistence adapter layer.
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_file", nullable = false)
    private boolean file;

    @Column(name = "is_system_message", nullable = false)
    private boolean systemMessage;

    @Column(name = "attachment_id")
    private UUID attachmentId;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    @Column(name = "parent_id")
    private UUID parentId;
}

