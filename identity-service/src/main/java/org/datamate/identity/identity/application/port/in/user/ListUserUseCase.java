package org.datamate.identity.identity.application.port.in.user;

import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;

public interface ListUserUseCase {
    Paged<UserResponseDto> searchUsers(UserSearchCriteria criteria, PageQuery pageQuery);
}
