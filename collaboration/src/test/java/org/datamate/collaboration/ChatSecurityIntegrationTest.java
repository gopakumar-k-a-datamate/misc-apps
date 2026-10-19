package org.datamate.collaboration;

import io.jsonwebtoken.Claims;
import org.datamate.collaboration.chat.adapter.in.rest.ChatController;
import org.datamate.collaboration.chat.application.dto.GetMessagesQuery;
import org.datamate.collaboration.chat.application.port.in.GetMessagesUseCase;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.datamate.collaboration.config.ChatAuthenticationEntryPoint;
import org.datamate.collaboration.config.ChatHttpSecurityConfig;
import org.datamate.collaboration.config.CollaborationExceptionHandler;
import org.datamate.collaboration.security.JwtTicketAuthFilter;
import org.datamate.collaboration.security.JwtVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatController.class)
@Import({ChatHttpSecurityConfig.class,
         ChatAuthenticationEntryPoint.class,
         JwtTicketAuthFilter.class,
         CollaborationExceptionHandler.class})
public class ChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtVerifier jwtVerifier;

    @MockBean
    private SendMessageUseCase sendMessageUseCase;

    @MockBean
    private GetMessagesUseCase getMessagesUseCase;

    @MockBean
    private com.datamate.bedrock.framework.common.exception.service.MessageResolver messageResolver;

    @MockBean
    private com.datamate.bedrock.framework.common.exception.config.ExceptionProperties exceptionProperties;

    @Test
    @DisplayName("POST /messages: Request without token should return 401 Unauthorized")
    public void testSendMessage_NoAuth_ShouldReturn401() throws Exception {
        UUID threadId = UUID.randomUUID();
        
        String jsonPayload = """
                {
                    "content": "Hello World"
                }
                """;

        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /messages: Request with valid signed ticket for this thread should succeed (201 Created)")
    public void testSendMessage_WithValidTicket_ShouldSucceed() throws Exception {
        UUID threadId = UUID.randomUUID();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ", "WRITE"));
        when(jwtVerifier.verify("valid-ticket")).thenReturn(claims);

        String jsonPayload = """
                {
                    "content": "Hello from John"
                }
                """;

        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .header("Authorization", "Bearer valid-ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /messages: Request with valid ticket for a different thread should be rejected (403 Forbidden)")
    public void testSendMessage_WithMismatchedThreadTicket_ShouldReturn403() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID differentThreadId = UUID.randomUUID();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("john");
        when(claims.get("threadId", String.class)).thenReturn(differentThreadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ", "WRITE"));
        when(jwtVerifier.verify("different-thread-ticket")).thenReturn(claims);

        String jsonPayload = """
                {
                    "content": "Attacking another thread"
                }
                """;

        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .header("Authorization", "Bearer different-thread-ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /messages: Request with valid ticket but lacking WRITE permission should return 403 Forbidden")
    public void testSendMessage_WithReadOnlyTicket_ShouldReturn403() throws Exception {
        UUID threadId = UUID.randomUUID();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("reader-user");
        when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ"));
        when(jwtVerifier.verify("read-only-ticket")).thenReturn(claims);

        String jsonPayload = """
                {
                    "content": "Trying to write with read-only ticket"
                }
                """;

        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .header("Authorization", "Bearer read-only-ticket")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_FORBIDDEN"));
    }

    @Test
    @DisplayName("GET /messages: Request without token should return 401 Unauthorized")
    public void testGetMessages_NoAuth_ShouldReturn401() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @DisplayName("GET /messages: Request with valid ticket query param should succeed (200 OK)")
    public void testGetMessages_WithValidTicketQueryParam_ShouldSucceed() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-1");
        when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ"));
        when(jwtVerifier.verify("valid-ticket")).thenReturn(claims);
        when(getMessagesUseCase.getMessages(eq(threadId), any(GetMessagesQuery.class)))
                .thenReturn(new com.datamate.bedrock.framework.common.pagination.Paged<>(List.of(), 1, 10, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10&ticket=valid-ticket"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /messages: Request with valid token query param should succeed (200 OK)")
    public void testGetMessages_WithValidTokenQueryParam_ShouldSucceed() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-2");
        when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ", "WRITE"));
        when(jwtVerifier.verify("valid-token")).thenReturn(claims);
        when(getMessagesUseCase.getMessages(eq(threadId), any(GetMessagesQuery.class)))
                .thenReturn(new com.datamate.bedrock.framework.common.pagination.Paged<>(List.of(), 1, 10, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10&token=valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /messages: Request with mismatched thread ticket should return 403 Forbidden")
    public void testGetMessages_WithMismatchedThread_ShouldReturn403() throws Exception {
        UUID threadId = UUID.randomUUID();
        UUID differentThreadId = UUID.randomUUID();

        Claims claims = Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn("user-2");
        when(claims.get("threadId", String.class)).thenReturn(differentThreadId.toString());
        when(claims.get("permissions")).thenReturn(List.of("READ"));
        when(jwtVerifier.verify("mismatched-token")).thenReturn(claims);

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10&token=mismatched-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_FORBIDDEN"));
    }
}
