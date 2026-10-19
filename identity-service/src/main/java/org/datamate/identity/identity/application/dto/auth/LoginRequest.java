package org.datamate.identity.identity.application.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "{user.validation.username.required}")
        String userName,

        @NotBlank(message = "{user.validation.password.required}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password
) {}
