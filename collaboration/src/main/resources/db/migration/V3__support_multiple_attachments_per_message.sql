CREATE TABLE chat_message_attachments (
    message_id UUID NOT NULL,
    attachment_id UUID NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (message_id, attachment_id),
    CONSTRAINT fk_cma_message FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_cma_attachment FOREIGN KEY (attachment_id) REFERENCES chat_attachments (id) ON DELETE CASCADE
);

CREATE INDEX idx_chat_message_attachments_msg_id ON chat_message_attachments(message_id);

-- Backfill existing single attachments into the new join table
INSERT INTO chat_message_attachments (message_id, attachment_id, sort_order)
SELECT id, attachment_id, 0 FROM chat_messages WHERE attachment_id IS NOT NULL
ON CONFLICT DO NOTHING;
