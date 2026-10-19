package org.datamate.identity.adapter.in.rest.controller;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.datamate.identity.identity.adapter.in.rest.controller.UserController;
import org.datamate.identity.identity.application.dto.user.CreateUserRequest;
import org.datamate.identity.identity.application.dto.user.UserDto;
import org.datamate.identity.identity.application.port.in.user.CreateUserUseCase;
import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import org.datamate.identity.identity.application.port.in.user.ListUserUseCase;
import org.datamate.identity.identity.application.port.in.user.GetUserUseCase;
import org.datamate.identity.identity.application.port.in.user.ActivateUserUseCase;
import org.datamate.identity.identity.application.port.in.user.DeactivateUserUseCase;
import org.datamate.identity.identity.domain.exception.user.UserNotFoundException;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ListUserUseCase listUserUseCase;

    @Mock
    private CreateUserUseCase createUserUseCase;

    @Mock
    private GetUserUseCase getUserUseCase;

    @Mock
    private ActivateUserUseCase activateUserUseCase;

    @Mock
    private DeactivateUserUseCase deactivateUserUseCase;

    @Mock
    private Logger log;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() throws Exception {
        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = UserController.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(userController, log);

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @RestControllerAdvice
    static class TestExceptionHandler {
        @ExceptionHandler(UserNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public String handleUserNotFound(UserNotFoundException ex) {
            return ex.getMessage();
        }
    }

    @Test
    void shouldCreateUserSuccessfullyWhenValidRequestProvided() throws Exception {
        UUID sampleId = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "new_admin",
                "admin@example.com",
                "+1234567890",
                "Admin",
                "User",
                "securePass123",
                "ELLIDER",
                "EXT-12345",
                List.of()
        );

        UserDto responseDto = new UserDto(
                sampleId,
                "new_admin",
                "admin@example.com",
                "+1234567890",
                "Admin",
                "User",
                "ELLIDER",
                "EXT-12345",
                "SYSTEM_ADMIN",
                LocalDateTime.now(),
                UserStatus.ACTIVE,
                List.of("ADMIN", "USER"),
                true
        );

        when(createUserUseCase.createUser(any(CreateUserRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.userName").value("new_admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.firstName").value("Admin"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.referenceSystem").value("ELLIDER"))
                .andExpect(jsonPath("$.referenceValue").value("EXT-12345"));
    }

    @Test
    void shouldReturnBadRequestWhenMandatoryFieldsMissing() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "",
                "invalid-email",
                "+1234567890",
                "",
                "User",
                "123",
                null,
                null,
                List.of()
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSearchUsersSuccessfully() throws Exception {
        UUID sampleId = UUID.randomUUID();
        UserResponseDto userResponseDto = new UserResponseDto(
                sampleId,
                "john_doe",
                "john@example.com",
                "John",
                "Doe",
                UserStatus.ACTIVE,
                List.of("USER")
        );

        Paged<UserResponseDto> pagedResult = new Paged<>(
                List.of(userResponseDto),
                1,
                10,
                1L,
                1,
                false,
                false
        );

        UserSearchCriteria criteria = new UserSearchCriteria("john", "USER", UserStatus.ACTIVE);
        when(listUserUseCase.searchUsers(eq(criteria), any(PageQuery.class)))
                .thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/users")
                        .param("search", "john")
                        .param("role", "USER")
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleId.toString()))
                .andExpect(jsonPath("$.content[0].userName").value("john_doe"))
                .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("USER"))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldReturnUserDetailsSuccessfullyWhenUserExists() throws Exception {
        UUID sampleId = UUID.randomUUID();
        UserDto responseDto = new UserDto(
                sampleId,
                "john_doe",
                "john@example.com",
                "+1234567890",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                "SYSTEM_ADMIN",
                LocalDateTime.now(),
                UserStatus.ACTIVE,
                List.of("USER"),
                false
        );

        when(getUserUseCase.getUserById(eq(sampleId))).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/users/" + sampleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleId.toString()))
                .andExpect(jsonPath("$.userName").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        UUID sampleId = UUID.randomUUID();
        when(getUserUseCase.getUserById(eq(sampleId)))
                .thenThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/v1/users/" + sampleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldActivateUserSuccessfully() throws Exception {
        UUID sampleId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/" + sampleId + "/activate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(activateUserUseCase).activateUser(eq(sampleId), eq("SYSTEM"));
    }

    @Test
    void shouldDeactivateUserSuccessfully() throws Exception {
        UUID sampleId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/users/" + sampleId + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(deactivateUserUseCase).deactivateUser(eq(sampleId), eq("SYSTEM"));
    }
}
