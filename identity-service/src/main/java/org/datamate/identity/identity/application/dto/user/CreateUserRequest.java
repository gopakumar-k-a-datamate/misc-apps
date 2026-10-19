package org.datamate.identity.identity.application.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.List;

public record CreateUserRequest(
        @NotBlank(message = "{user.validation.username.required}")
        @Size(min = 3, max = 50, message = "{user.validation.username.size}")
        String userName,

        @NotBlank(message = "{user.validation.email.required}")
        @Email(message = "{user.validation.email.invalid}")
        String email,

        @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        String phoneNumber,

        @NotBlank(message = "{user.validation.firstName.required}")
        String firstName,

        @NotBlank(message = "{user.validation.lastName.required}")
        String lastName,

        @NotBlank(message = "{user.validation.password.required}")
        @Size(min = 6, message = "{user.validation.password.size}")
        String password,

        @Size(max = 50, message = "Reference system must be at most 50 characters")
        String referenceSystem,

        @Size(max = 255, message = "Reference value must be at most 255 characters")
        String referenceValue,

        List<String> roles
) {}
