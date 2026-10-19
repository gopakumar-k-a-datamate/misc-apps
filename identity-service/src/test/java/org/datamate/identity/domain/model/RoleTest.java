package org.datamate.identity.domain.model;

import com.datamate.bedrock.framework.common.ddd.event.DomainEvent;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.event.role.RoleActivatedEvent;
import org.datamate.identity.identity.domain.event.role.RoleDeactivatedEvent;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.domain.exception.role.InvalidRoleDataException;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleTest {

    private final UUID roleId = UUID.randomUUID();
    private final EntityReference<UUID> adminUserRef = new EntityReference<>(
            UUID.randomUUID(),
            new ResourceIdentifier("identity-service", "admin")
    );

    @Test
    void shouldActivateRoleWhenInactive() {
        Role role = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Description",
                RoleStatus.INACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        Role activatedRole = role.activate(adminUserRef);

        assertEquals(RoleStatus.ACTIVE, activatedRole.getStatus());
        assertEquals(adminUserRef, activatedRole.getLastModifiedBy());
        assertEquals(role.getDomainVersion() + 1, activatedRole.getDomainVersion());

        List<DomainEvent> events = activatedRole.pullEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof RoleActivatedEvent);

        RoleActivatedEvent event = (RoleActivatedEvent) events.get(0);
        assertEquals(roleId, event.aggregateId());
        assertEquals("TEST_ROLE", event.name());
        assertEquals(adminUserRef, event.activatedBy());
    }

    @Test
    void shouldThrowInvalidRoleDataExceptionWhenAlreadyActive() {
        Role role = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Description",
                RoleStatus.ACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        assertThrows(InvalidRoleDataException.class, () -> role.activate(adminUserRef));
    }

    @Test
    void shouldDeactivateRoleWhenActive() {
        Role role = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Description",
                RoleStatus.ACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        Role deactivatedRole = role.deactivate(adminUserRef);

        assertEquals(RoleStatus.INACTIVE, deactivatedRole.getStatus());
        assertEquals(adminUserRef, deactivatedRole.getLastModifiedBy());
        assertEquals(role.getDomainVersion() + 1, deactivatedRole.getDomainVersion());

        List<DomainEvent> events = deactivatedRole.pullEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof RoleDeactivatedEvent);

        RoleDeactivatedEvent event = (RoleDeactivatedEvent) events.get(0);
        assertEquals(roleId, event.aggregateId());
        assertEquals("TEST_ROLE", event.name());
        assertEquals(adminUserRef, event.deactivatedBy());
    }

    @Test
    void shouldThrowInvalidRoleDataExceptionWhenAlreadyInactive() {
        Role role = Role.reconstitute(
                roleId,
                "TEST_ROLE",
                "Description",
                RoleStatus.INACTIVE,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        assertThrows(InvalidRoleDataException.class, () -> role.deactivate(adminUserRef));
    }

    @Test
    void shouldThrowInvalidRoleDataExceptionWhenDeactivatingPolicyAdmin() {
        Role role = Role.reconstitute(
                roleId,
                "POLICY_ADMIN",
                "Description",
                RoleStatus.ACTIVE,
                true,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        InvalidRoleDataException ex = assertThrows(InvalidRoleDataException.class, () -> role.deactivate(adminUserRef));
        assertEquals("role.validation.system.role", ex.getErrorCode());
    }

    @Test
    void shouldThrowInvalidRoleDataExceptionWhenRenamingPolicyAdmin() {
        Role role = Role.reconstitute(
                roleId,
                "POLICY_ADMIN",
                "Description",
                RoleStatus.ACTIVE,
                true,
                null,
                null,
                1L,
                1L,
                adminUserRef,
                LocalDateTime.now(),
                adminUserRef,
                LocalDateTime.now()
        );

        InvalidRoleDataException ex = assertThrows(InvalidRoleDataException.class, () -> role.updateInformation("NEW_NAME", "New Desc", adminUserRef));
        assertEquals("role.validation.system.role", ex.getErrorCode());
    }

    @Test
    void shouldMaintainDistinctRolesInSet() {
        org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity r1 = new org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity();
        r1.setId(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        r1.setName("POLICY_ADMIN");

        org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity r2 = new org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity();
        r2.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        r2.setName("ADMIN");

        org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity r3 = new org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity();
        r3.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        r3.setName("USER");

        java.util.Set<org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity> set = new java.util.HashSet<>();
        set.add(r1);
        set.add(r2);
        set.add(r3);

        System.out.println("ROLE_SET_SIZE: " + set.size());
        assertEquals(3, set.size());
    }
}
