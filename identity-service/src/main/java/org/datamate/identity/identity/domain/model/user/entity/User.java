package org.datamate.identity.identity.domain.model.user.entity;

import lombok.Getter;
import org.datamate.identity.identity.domain.event.user.UserCreatedEvent;
import org.datamate.identity.identity.domain.event.user.UserActivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserDeactivatedEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordResetByAdminEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordChangedEvent;
import org.datamate.identity.identity.domain.event.user.UserInformationUpdatedEvent;
import org.datamate.identity.identity.domain.event.user.UserRolesUpdatedEvent;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import com.datamate.bedrock.framework.common.ddd.domain.AggregateRoot;
import org.datamate.identity.identity.domain.exception.user.InvalidUserDataException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class User extends AggregateRoot {
    private final UUID id;
    private final String userName;
    private final String email;
    private final String phoneNumber;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final String referenceSystem;
    private final String referenceValue;
    private final UserStatus status;
    private final List<String> roles;
    private final boolean passwordTemporary;
    private final Long version;
    private final String createdBy;
    private final LocalDateTime createdDate;
    private final String lastModifiedBy;
    private final LocalDateTime lastModifiedDate;

    private User(
            UUID id,
            String userName,
            String email,
            String phoneNumber,
            String passwordHash,
            String firstName,
            String lastName,
            String referenceSystem,
            String referenceValue,
            UserStatus status,
            List<String> roles,
            boolean passwordTemporary,
            Long version,
            Long domainVersion,
            String createdBy,
            LocalDateTime createdDate,
            String lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {
        super(domainVersion);
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.referenceSystem = referenceSystem;
        this.referenceValue = referenceValue;
        this.status = status;
        this.roles = roles != null ? roles : new ArrayList<>();
        this.passwordTemporary = passwordTemporary;
        this.version = version;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedDate = lastModifiedDate;
    }

    public static User create(
            String userName,
            String email,
            String phoneNumber,
            String passwordHash,
            String firstName,
            String lastName,
            String referenceSystem,
            String referenceValue,
            List<String> roles,
            String createdBy
    ) {
        validateState(userName, email, passwordHash, firstName, lastName, createdBy);

        UUID newUserId = UUID.randomUUID();
        List<String> uniqueRoles = roles != null ? roles.stream().distinct().toList() : new ArrayList<>();

        User newUser = new User(
                newUserId,
                userName,
                email,
                phoneNumber,
                passwordHash,
                firstName,
                lastName,
                referenceSystem,
                referenceValue,
                UserStatus.INACTIVE,
                uniqueRoles,
                true,
                null,
                0L,
                createdBy,
                LocalDateTime.now(),
                createdBy,
                LocalDateTime.now()
        );

        newUser.registerEvent(new UserCreatedEvent(
                newUserId,
                newUser.getDomainVersion(),
                userName,
                email,
                phoneNumber,
                firstName,
                lastName,
                UserStatus.INACTIVE,
                uniqueRoles,
                createdBy
        ));

        return newUser;
    }

    public static User reconstitute(
            UUID id,
            String userName,
            String email,
            String phoneNumber,
            String passwordHash,
            String firstName,
            String lastName,
            String referenceSystem,
            String referenceValue,
            UserStatus status,
            List<String> roles,
            boolean passwordTemporary,
            Long version,
            Long domainVersion,
            String createdBy,
            LocalDateTime createdDate,
            String lastModifiedBy,
            LocalDateTime lastModifiedDate
    ) {
        return new User(
                id,
                userName,
                email,
                phoneNumber,
                passwordHash,
                firstName,
                lastName,
                referenceSystem,
                referenceValue,
                status,
                roles,
                passwordTemporary,
                version,
                domainVersion,
                createdBy,
                createdDate,
                lastModifiedBy,
                lastModifiedDate
        );
    }

    public User activate(String updatedBy) {
        if (this.status == UserStatus.ACTIVE) {
            return this;
        }
        User updatedUser = new User(
                this.id,
                this.userName,
                this.email,
                this.phoneNumber,
                this.passwordHash,
                this.firstName,
                this.lastName,
                this.referenceSystem,
                this.referenceValue,
                UserStatus.ACTIVE,
                this.roles,
                this.passwordTemporary,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                updatedBy,
                LocalDateTime.now()
        );
        updatedUser.registerEvent(new UserActivatedEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                this.userName,
                this.email,
                this.phoneNumber,
                this.firstName,
                this.lastName,
                updatedBy
        ));
        return updatedUser;
    }

    public User deactivate(String updatedBy) {
        if (this.status == UserStatus.INACTIVE) {
            return this;
        }
        User updatedUser = new User(
                this.id,
                this.userName,
                this.email,
                this.phoneNumber,
                this.passwordHash,
                this.firstName,
                this.lastName,
                this.referenceSystem,
                this.referenceValue,
                UserStatus.INACTIVE,
                this.roles,
                this.passwordTemporary,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                updatedBy,
                LocalDateTime.now()
        );
        updatedUser.registerEvent(new UserDeactivatedEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                this.userName,
                this.email,
                this.phoneNumber,
                this.firstName,
                this.lastName,
                updatedBy
        ));
        return updatedUser;
    }

    public User resetPassword(String newPasswordHash, String adminUsername) {
        User updatedUser = new User(
                this.id,
                this.userName,
                this.email,
                this.phoneNumber,
                newPasswordHash,
                this.firstName,
                this.lastName,
                this.referenceSystem,
                this.referenceValue,
                this.status,
                this.roles,
                true, // passwordTemporary = true
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                adminUsername,
                LocalDateTime.now()
        );
        updatedUser.registerEvent(new UserPasswordResetByAdminEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                this.userName,
                this.email,
                this.phoneNumber,
                this.firstName,
                this.lastName,
                adminUsername
        ));
        return updatedUser;
    }

    public User changePassword(String newPasswordHash, String username) {
        User updatedUser = new User(
                this.id,
                this.userName,
                this.email,
                this.phoneNumber,
                newPasswordHash,
                this.firstName,
                this.lastName,
                this.referenceSystem,
                this.referenceValue,
                this.status,
                this.roles,
                false, // passwordTemporary = false
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                username,
                LocalDateTime.now()
        );
        updatedUser.registerEvent(new UserPasswordChangedEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                this.userName,
                this.email,
                this.phoneNumber,
                this.firstName,
                this.lastName,
                username
        ));
        return updatedUser;
    }

    public User updateInformation(
            String userName,
            String email,
            String phoneNumber,
            String firstName,
            String lastName,
            String referenceSystem,
            String referenceValue,
            String adminUsername
    ) {
        validateUpdate(userName, email, firstName, lastName);

        User updatedUser = new User(
                this.id,
                userName,
                email,
                phoneNumber,
                this.passwordHash,
                firstName,
                lastName,
                referenceSystem,
                referenceValue,
                this.status,
                this.roles,
                this.passwordTemporary,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                adminUsername,
                LocalDateTime.now()
        );

        updatedUser.registerEvent(new UserInformationUpdatedEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                userName,
                email,
                phoneNumber,
                firstName,
                lastName,
                referenceSystem,
                referenceValue,
                adminUsername
        ));

        return updatedUser;
    }

    public User assignRoles(List<String> newRoles, String adminUsername) {
        List<String> uniqueRoles = newRoles.stream()
                .distinct()
                .toList();

        User updatedUser = new User(
                this.id,
                this.userName,
                this.email,
                this.phoneNumber,
                this.passwordHash,
                this.firstName,
                this.lastName,
                this.referenceSystem,
                this.referenceValue,
                this.status,
                uniqueRoles,
                this.passwordTemporary,
                this.version,
                this.getDomainVersion(),
                this.createdBy,
                this.createdDate,
                adminUsername,
                LocalDateTime.now()
        );

        updatedUser.registerEvent(new UserRolesUpdatedEvent(
                this.id,
                updatedUser.getDomainVersion() + 1,
                uniqueRoles,
                adminUsername
        ));

        return updatedUser;
    }

    private void validateUpdate(String userName, String email, String firstName, String lastName) {
        if (userName == null || userName.isBlank()) {
            throw new InvalidUserDataException("user.validation.username.required", "Username is required.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidUserDataException("user.validation.email.required", "Email is required.");
        }
        if (!email.contains("@")) {
            throw new InvalidUserDataException("user.validation.email.invalid", "Email must be valid.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidUserDataException("user.validation.firstName.required", "First name is required.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidUserDataException("user.validation.lastName.required", "Last name is required.");
        }
    }

    private static void validateState(
            String userName,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String createdBy
    ) {
        if (userName == null || userName.isBlank()) {
            throw new InvalidUserDataException("user.validation.username.required", "Username is required.");
        }
        if (email == null || email.isBlank()) {
            throw new InvalidUserDataException("user.validation.email.required", "Email is required.");
        }
        if (!email.contains("@")) {
            throw new InvalidUserDataException("user.validation.email.invalid", "Email must be valid.");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new InvalidUserDataException("user.validation.passwordHash.required", "Password hash is required.");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new InvalidUserDataException("user.validation.firstName.required", "First name is required.");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new InvalidUserDataException("user.validation.lastName.required", "Last name is required.");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new InvalidUserDataException("user.validation.createdBy.required", "Created by reference is required.");
        }
    }
}
