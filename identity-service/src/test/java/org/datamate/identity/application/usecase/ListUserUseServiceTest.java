package org.datamate.identity.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import org.datamate.identity.identity.application.mapper.user.UserDtoMapper;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.usecase.user.ListUserUseService;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ListUserUseServiceTest {

    private UserPersistencePort userPort;
    private UserDtoMapper userDtoMapper;
    private Logger log;
    private ListUserUseService listUserUseService;

    @BeforeEach
    void setUp() throws Exception {
        userPort = mock(UserPersistencePort.class);
        userDtoMapper = new UserDtoMapper();
        log = mock(Logger.class);

        listUserUseService = new ListUserUseService(userPort, userDtoMapper);

        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = ListUserUseService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(listUserUseService, log);
    }

    @Test
    void shouldSearchUsersSuccessfully() {
        // Arrange
        UserSearchCriteria criteria = new UserSearchCriteria("john", "USER", UserStatus.ACTIVE);
        PageQuery pageQuery = new PageQuery(1, 10);

        UUID userId = UUID.randomUUID();
        User sampleUser = User.reconstitute(
                userId,
                "john_doe",
                "john@example.com",
                "+1234567890",
                "password_hash",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                UserStatus.ACTIVE,
                List.of("USER"),
                false,
                0L,
                1L,
                "admin",
                LocalDateTime.now(),
                "admin",
                LocalDateTime.now()
        );

        Paged<User> userPaged = new Paged<>(
                List.of(sampleUser),
                1,
                10,
                1L,
                1,
                true,
                true
        );

        when(userPort.searchUsers(eq(criteria), any(PageQuery.class))).thenReturn(userPaged);

        // Act
        Paged<UserResponseDto> result = listUserUseService.searchUsers(criteria, pageQuery);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.content().size());
        
        UserResponseDto dto = result.content().get(0);
        assertEquals(userId, dto.id());
        assertEquals("john_doe", dto.userName());
        assertEquals("john@example.com", dto.email());
        assertEquals("John", dto.firstName());
        assertEquals("Doe", dto.lastName());
        assertEquals(UserStatus.ACTIVE, dto.status());
        assertEquals(List.of("USER"), dto.roles());

        verify(userPort).searchUsers(eq(criteria), any(PageQuery.class));
    }
}
