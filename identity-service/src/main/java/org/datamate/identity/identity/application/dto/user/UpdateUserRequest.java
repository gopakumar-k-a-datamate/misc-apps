package org.datamate.identity.identity.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = "{user.validation.username.required}")
        @Size(min = 3, max = 50, message = "{user.validation.username.size}")
        String userName,

        @NotBlank(message = "{user.validation.email.required}")
        @Email(message = "{user.validation.email.invalid}")
        String email,

        String phoneNumber,

        @NotBlank(message = "{user.validation.firstName.required}")
        String firstName,

        @NotBlank(message = "{user.validation.lastName.required}")
        String lastName,

        String referenceSystem,
        String referenceValue
) {}
