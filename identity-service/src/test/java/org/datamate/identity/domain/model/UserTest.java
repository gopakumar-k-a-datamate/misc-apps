package org.datamate.identity.domain.model;

import com.datamate.bedrock.framework.common.ddd.event.DomainEvent;
import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.domain.event.user.UserCreatedEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordResetByAdminEvent;
import org.datamate.identity.identity.domain.event.user.UserPasswordChangedEvent;
import org.datamate.identity.identity.domain.exception.user.InvalidUserDataException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserAndRegisterUserCreatedEvent() {
        User user = User.create(
                "john_doe",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        );

        assertNotNull(user.getId());
        assertEquals("john_doe", user.getUserName());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("+1234567890", user.getPhoneNumber());
        assertEquals("hashedPassword", user.getPasswordHash());
        assertEquals("John", user.getFirstName());
        assertEquals("Doe", user.getLastName());
        assertEquals("ELLIDER", user.getReferenceSystem());
        assertEquals("EXT-12345", user.getReferenceValue());
        assertEquals("admin", user.getCreatedBy());
        assertTrue(user.isPasswordTemporary());

        List<DomainEvent> events = user.pullEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof UserCreatedEvent);

        UserCreatedEvent createdEvent = (UserCreatedEvent) events.get(0);
        assertNotNull(createdEvent.aggregateId());
        assertEquals(user.getId(), createdEvent.aggregateId());
        assertEquals("john_doe", createdEvent.userName());
        assertEquals("john@example.com", createdEvent.email());
        assertEquals("admin", createdEvent.createdBy());
    }

    @Test
    void shouldThrowExceptionWhenUserNameIsBlank() {
        assertThrows(InvalidUserDataException.class, () -> User.create(
                "",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        ));
    }

    @Test
    void shouldThrowExceptionWhenEmailIsInvalid() {
        assertThrows(InvalidUserDataException.class, () -> User.create(
                "john_doe",
                "invalid-email-format",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        ));
    }

    @Test
    void shouldThrowExceptionWhenFirstNameIsBlank() {
        assertThrows(InvalidUserDataException.class, () -> User.create(
                "john_doe",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        ));
    }

    @Test
    void shouldThrowExceptionWhenCreatedByIsBlank() {
        assertThrows(InvalidUserDataException.class, () -> User.create(
                "john_doe",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                " "
        ));
    }

    @Test
    void shouldResetPasswordAndRegisterEvent() {
        User user = User.create(
                "john_doe",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        );

        User resetUser = user.resetPassword("newHashedPassword", "admin_resetter");

        assertEquals("newHashedPassword", resetUser.getPasswordHash());
        assertTrue(resetUser.isPasswordTemporary());
        assertEquals("admin_resetter", resetUser.getLastModifiedBy());
        assertNotNull(resetUser.getLastModifiedDate());
        assertEquals(user.getDomainVersion() + 1, resetUser.getDomainVersion());

        List<DomainEvent> events = resetUser.pullEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof UserPasswordResetByAdminEvent);
        UserPasswordResetByAdminEvent event = (UserPasswordResetByAdminEvent) events.get(0);
        assertEquals(user.getId(), event.aggregateId());
        assertEquals("john_doe", event.userName());
        assertEquals("john@example.com", event.email());
        assertEquals("+1234567890", event.phoneNumber());
        assertEquals("admin_resetter", event.resetBy());
    }

    @Test
    void shouldChangePasswordAndRegisterEvent() {
        User user = User.create(
                "john_doe",
                "john@example.com",
                "+1234567890",
                "hashedPassword",
                "John",
                "Doe",
                "ELLIDER",
                "EXT-12345",
                List.of(),
                "admin"
        );

        User changedUser = user.changePassword("changedHashedPassword", "john_doe");

        assertEquals("changedHashedPassword", changedUser.getPasswordHash());
        assertFalse(changedUser.isPasswordTemporary());
        assertEquals("john_doe", changedUser.getLastModifiedBy());
        assertNotNull(changedUser.getLastModifiedDate());
        assertEquals(user.getDomainVersion() + 1, changedUser.getDomainVersion());

        List<DomainEvent> events = changedUser.pullEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof UserPasswordChangedEvent);
        UserPasswordChangedEvent event = (UserPasswordChangedEvent) events.get(0);
        assertEquals(user.getId(), event.aggregateId());
        assertEquals("john_doe", event.userName());
        assertEquals("john@example.com", event.email());
        assertEquals("+1234567890", event.phoneNumber());
        assertEquals("john_doe", event.changedBy());
    }
}
