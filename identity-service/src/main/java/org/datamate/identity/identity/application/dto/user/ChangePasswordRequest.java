package org.datamate.identity.identity.application.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "{user.validation.oldpassword.required}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String oldPassword,

        @NotBlank(message = "{user.validation.newpassword.required}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String newPassword
) {}
