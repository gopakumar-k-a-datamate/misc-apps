package org.datamate.identity.identity.application.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "{user.validation.password.blank}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String newPassword
) {}
