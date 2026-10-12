ALTER TABLE chat_messages
ADD COLUMN parent_id UUID,
ADD CONSTRAINT fk_chat_message_parent FOREIGN KEY (parent_id) REFERENCES chat_messages (id);
