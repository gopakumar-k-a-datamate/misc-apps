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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.datamate.collaboration.chat.adapter.in.rest.ChatController;
import org.datamate.collaboration.chat.application.port.in.GetMessagesUseCase;
import org.datamate.collaboration.chat.application.port.in.SendMessageUseCase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.datamate.collaboration.config.CollaborationExceptionHandler;

@WebMvcTest(controllers = ChatController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = CollaborationExceptionHandler.class))
@Import({org.datamate.collaboration.security.ChatThreadAccessPreAuthorize.class, 
         org.datamate.collaboration.config.ChatHttpSecurityConfig.class,
         org.datamate.collaboration.config.ChatAuthenticationEntryPoint.class,
         org.datamate.collaboration.security.JwtTicketAuthFilter.class})
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
                .andExpect(status().isUnauthorized());
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

        // Even with a generic @WithMockUser, the ChatThreadAccessPreAuthorize expects a TicketPrincipal
        // So this should fail the PreAuthorize check and throw AccessDeniedException (403)
        mockMvc.perform(post("/api/v1/collaboration/threads/" + threadId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden());
    }
}
