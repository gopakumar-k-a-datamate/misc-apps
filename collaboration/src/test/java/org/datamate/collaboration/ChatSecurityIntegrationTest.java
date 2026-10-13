package org.datamate.collaboration;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.collaboration.chat.application.dto.SendMessageCommand;
import org.datamate.collaboration.security.JwtVerifier;
import org.datamate.collaboration.security.TicketPrincipal;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.datamate.collaboration.chat.adapter.in.rest.ChatController;
import org.datamate.collaboration.chat.application.port.in.GetMessagesUseCase;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.datamate.collaboration.config.CollaborationExceptionHandler;
import com.datamate.bedrock.framework.common.exception.config.ExceptionProperties;
import com.datamate.bedrock.framework.common.exception.service.MessageResolver;

@WebMvcTest(controllers = ChatController.class)
@Import({org.datamate.collaboration.security.ChatThreadAccessPreAuthorize.class, 
         org.datamate.collaboration.config.ChatHttpSecurityConfig.class,
         org.datamate.collaboration.config.ChatAuthenticationEntryPoint.class,
         org.datamate.collaboration.security.JwtTicketAuthFilter.class,
         CollaborationExceptionHandler.class})
public class ChatSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtVerifier jwtVerifier;

    @MockBean
    private PolicyEnforcer policyEnforcer;

    @MockBean
    private SendMessageUseCase sendMessageUseCase;

    @MockBean
    private GetMessagesUseCase getMessagesUseCase;

    @MockBean
    private MessageResolver messageResolver;

    @MockBean
    private ExceptionProperties exceptionProperties;

    @Test
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
    @WithMockUser // Sets up a default user without the TicketPrincipal
    public void testSendMessage_InvalidPrincipal_ShouldReturn403() throws Exception {
        UUID threadId = UUID.randomUUID();
        
        String jsonPayload = """
                {
                    "content": "Hello World"
                }
                """;

        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN"));
    }

    @Test
    public void testGetMessages_NoAuth_ShouldReturn401() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.properties.errorCode").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    @WithMockUser
    public void testGetMessages_InvalidPrincipal_ShouldReturn403() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("AUTH_FORBIDDEN"));
    }

    @Test
    public void testGetMessages_WithValidTicketQueryParam_ShouldSucceed() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        io.jsonwebtoken.Claims claims = Mockito.mock(io.jsonwebtoken.Claims.class);
        Mockito.when(claims.getSubject()).thenReturn("user-1");
        Mockito.when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        Mockito.when(jwtVerifier.verify("valid-ticket")).thenReturn(claims);
        Mockito.when(getMessagesUseCase.getMessages(org.mockito.ArgumentMatchers.eq(threadId), org.mockito.ArgumentMatchers.any(org.datamate.collaboration.chat.application.dto.GetMessagesQuery.class)))
                .thenReturn(new com.datamate.bedrock.framework.common.pagination.Paged<>(java.util.List.of(), 1, 10, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10&ticket=valid-ticket"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetMessages_WithValidTokenQueryParam_ShouldSucceed() throws Exception {
        UUID threadId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        io.jsonwebtoken.Claims claims = Mockito.mock(io.jsonwebtoken.Claims.class);
        Mockito.when(claims.getSubject()).thenReturn("user-2");
        Mockito.when(claims.get("threadId", String.class)).thenReturn(threadId.toString());
        Mockito.when(jwtVerifier.verify("valid-token")).thenReturn(claims);
        Mockito.when(getMessagesUseCase.getMessages(org.mockito.ArgumentMatchers.eq(threadId), org.mockito.ArgumentMatchers.any(org.datamate.collaboration.chat.application.dto.GetMessagesQuery.class)))
                .thenReturn(new com.datamate.bedrock.framework.common.pagination.Paged<>(java.util.List.of(), 1, 10, 0, 0, false, false));

        mockMvc.perform(get("/api/v1/collaboration/threads/" + threadId + "/messages?page=1&size=10&token=valid-token"))
                .andExpect(status().isOk());
    }
}
