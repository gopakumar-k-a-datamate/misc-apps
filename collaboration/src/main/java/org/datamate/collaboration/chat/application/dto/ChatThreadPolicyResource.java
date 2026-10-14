package org.datamate.collaboration.chat.application.dto;

import org.datamate.authz.annotation.PolicyField;
import org.datamate.authz.annotation.PolicyResource;
import org.datamate.authz.model.policy.enumtype.FieldType;

/**
 * OPA Policy Resource for accessing a chat thread.
 */
@PolicyResource(namespace = "collaboration", resourceName = "chat_thread", action = "access", description = "Access a chat thread via Delegated Ticket")
public class ChatThreadPolicyResource {

    @PolicyField(type = FieldType.STRING, displayName = "Requested Thread ID")
    private String requestedThreadId;

    @PolicyField(type = FieldType.STRING, displayName = "Token Thread ID")
    private String tokenThreadId;

    @PolicyField(type = FieldType.STRING, displayName = "Requested Action")
    private String action;

    public String getRequestedThreadId() {
        return requestedThreadId;
    }

    public void setRequestedThreadId(String requestedThreadId) {
        this.requestedThreadId = requestedThreadId;
    }

    public String getTokenThreadId() {
        return tokenThreadId;
    }

    public void setTokenThreadId(String tokenThreadId) {
        this.tokenThreadId = tokenThreadId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
