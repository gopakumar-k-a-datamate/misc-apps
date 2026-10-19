package org.datamate.collaboration.chat.application.port.out;

import org.datamate.collaboration.chat.domain.model.Message;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import java.util.UUID;

public interface MessageRepositoryPort {
    Message save(Message message);
    Paged<Message> findByThreadId(UUID threadId, PageQuery query);
}
