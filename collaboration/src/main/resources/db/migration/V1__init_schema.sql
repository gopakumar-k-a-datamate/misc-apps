CREATE TABLE chat_threads (
    id UUID PRIMARY KEY
);

CREATE TABLE chat_attachments (
    id UUID PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    upload_url VARCHAR(1024),
    preview_url VARCHAR(1024)
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    text TEXT,
    is_file BOOLEAN NOT NULL DEFAULT FALSE,
    is_system_message BOOLEAN NOT NULL DEFAULT FALSE,
    attachment_id UUID,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_message_thread FOREIGN KEY (thread_id) REFERENCES chat_threads (id),
    CONSTRAINT fk_chat_message_attachment FOREIGN KEY (attachment_id) REFERENCES chat_attachments (id)
);

CREATE INDEX idx_chat_messages_thread_id ON chat_messages(thread_id);
