package org.datamate.identity.identity.application.usecase.user;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.in.user.ListUserUseCase;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.domain.model.user.entity.User;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.PaginationHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListUserUseService implements ListUserUseCase {

    @EnableLogger
    private Logger log;

    private final UserPersistencePort userPort;
    private final UserDtoMapper userDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public Paged<UserResponseDto> searchUsers(UserSearchCriteria criteria, PageQuery pageQuery) {
        log.info("Searching users with criteria {} and page query {}", criteria, pageQuery);
        int validatedPage = PaginationHelper.validatePageNumber(pageQuery.page());
        int validatedSize = PaginationHelper.validateLimit(pageQuery.size());
        PageQuery validatedPageQuery = new PageQuery(validatedPage, validatedSize);

        Paged<User> userPaged = userPort.searchUsers(criteria, validatedPageQuery);
        Paged<UserResponseDto> result = userDtoMapper.toPaged(userPaged);
        log.info("Search users completed, returned {} of {} users", result.content().size(), result.totalElements());
        return result;
    }
}
