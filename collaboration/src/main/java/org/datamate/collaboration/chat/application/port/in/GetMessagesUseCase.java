package org.datamate.collaboration.chat.application.port.in;

import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import org.datamate.collaboration.chat.application.dto.GetMessagesQuery;
import org.datamate.collaboration.chat.application.dto.MessageDto;

import java.util.UUID;

/**
 * Inbound Use Case Port for fetching paginated messages in a thread.
 * Clean Architecture compliant: pure interface contract without implementation logic.
 */
public interface GetMessagesUseCase {

    Paged<MessageDto> getMessages(UUID threadId, PageQuery query);

    Paged<MessageDto> getMessages(UUID threadId, GetMessagesQuery query);
}