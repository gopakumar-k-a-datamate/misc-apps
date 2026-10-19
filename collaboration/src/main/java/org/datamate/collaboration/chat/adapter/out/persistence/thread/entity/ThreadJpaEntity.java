package org.datamate.collaboration.chat.adapter.out.persistence.thread.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * JPA Entity for the {@code chat_threads} table.
 * <p>
 * Used exclusively by the persistence adapter layer.
 * Must never leak into the domain or application layers.
 */
@Entity
@Table(name = "chat_threads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ThreadJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;
}
