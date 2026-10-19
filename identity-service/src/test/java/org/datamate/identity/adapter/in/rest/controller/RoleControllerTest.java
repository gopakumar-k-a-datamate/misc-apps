package org.datamate.identity.adapter.in.rest.controller;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.datamate.identity.identity.adapter.in.rest.controller.RoleController;
import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.dto.role.RoleRequest;
import org.datamate.identity.identity.application.dto.role.RoleSelectDto;
import org.datamate.identity.identity.application.port.in.role.CreateRoleUseCase;
import org.datamate.identity.identity.application.port.in.role.GetRoleUseCase;
import org.datamate.identity.identity.application.port.in.role.ListRolesUseCase;
import org.datamate.identity.identity.application.port.in.role.RoleManagementUseCase;
import org.datamate.identity.identity.application.port.in.role.SelectRolesUseCase;
import org.datamate.identity.identity.application.query.role.RoleSearchCriteria;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import org.datamate.identity.identity.application.port.in.role.UpdateRoleUseCase;
import org.datamate.identity.identity.application.port.in.role.ActivateRoleUseCase;
import org.datamate.identity.identity.application.port.in.role.DeactivateRoleUseCase;
import org.datamate.identity.identity.application.service.role.AuditActorResolver;
import com.datamate.bedrock.framework.common.ddd.datatype.EntityReference;
import com.datamate.bedrock.framework.common.ddd.datatype.ResourceIdentifier;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID roleId = UUID.randomUUID();

    @Mock
    private RoleManagementUseCase roleManagementUseCase;

    @Mock
    private CreateRoleUseCase createRoleUseCase;

    @Mock
    private ListRolesUseCase listRolesUseCase;

    @Mock
    private SelectRolesUseCase selectRolesUseCase;

    @Mock
    private GetRoleUseCase getRoleUseCase;

    @Mock
    private UpdateRoleUseCase updateRoleUseCase;

    @Mock
    private ActivateRoleUseCase activateRoleUseCase;
 
    @Mock
    private DeactivateRoleUseCase deactivateRoleUseCase;

    @Mock
    private AuditActorResolver auditActorResolver;

    @Mock
    private Logger log;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() throws Exception {
        // Manually inject the logger mock because it's a non-constructor private field (@EnableLogger)
        Field logField = RoleController.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(roleController, log);

        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .build();
    }

    @Test
    void shouldCreateRoleSuccessfullyWhenValidRequestProvided() throws Exception {
        RoleRequest request = new RoleRequest("DENTIST", "Clinical Dentist Role");
        RoleDto responseDto = new RoleDto(roleId, "DENTIST", "Clinical Dentist Role", RoleStatus.INACTIVE, false);

        when(createRoleUseCase.createRole(any(RoleRequest.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roleId.toString()))
                .andExpect(jsonPath("$.name").value("DENTIST"))
                .andExpect(jsonPath("$.description").value("Clinical Dentist Role"))
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void shouldReturnBadRequestWhenMandatoryFieldsMissing() throws Exception {
        RoleRequest request = new RoleRequest("", "No Name Role");

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetRoleSuccessfully() throws Exception {
        RoleDto responseDto = new RoleDto(roleId, "DENTIST", "Clinical Dentist Role", RoleStatus.ACTIVE, false);
        when(getRoleUseCase.getRoleById(eq(roleId))).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/roles/" + roleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleId.toString()))
                .andExpect(jsonPath("$.name").value("DENTIST"));
    }

    @Test
    void shouldListRolesSuccessfully() throws Exception {
        RoleDto responseDto = new RoleDto(roleId, "DENTIST", "Clinical Dentist Role", RoleStatus.ACTIVE, false);
        Paged<RoleDto> pagedResult = new Paged<>(List.of(responseDto), 1, 10, 1L, 1, false, false);
        when(listRolesUseCase.listRoles(any(RoleSearchCriteria.class), any(PageQuery.class))).thenReturn(pagedResult);

        mockMvc.perform(get("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(roleId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("DENTIST"))
                .andExpect(jsonPath("$.pageNumber").value(1))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldDeleteRoleSuccessfully() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/" + roleId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(roleManagementUseCase).deleteRole(eq(roleId));
    }

    @Test
    void shouldGetActiveRolesSelectSuccessfully() throws Exception {
        RoleSelectDto selectDto = new RoleSelectDto(roleId, "DENTIST");
        when(selectRolesUseCase.selectRoles(null)).thenReturn(List.of(selectDto));

        mockMvc.perform(get("/api/v1/roles/select")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roleId.toString()))
                .andExpect(jsonPath("$[0].name").value("DENTIST"));
    }

    @Test
    void shouldGetActiveRolesSelectWithSearchSuccessfully() throws Exception {
        RoleSelectDto selectDto = new RoleSelectDto(roleId, "DENTIST");
        when(selectRolesUseCase.selectRoles("DEN")).thenReturn(List.of(selectDto));

        mockMvc.perform(get("/api/v1/roles/select")
                        .param("search", "DEN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(roleId.toString()))
                .andExpect(jsonPath("$[0].name").value("DENTIST"));
    }

    @Test
    void shouldActivateRoleSuccessfully() throws Exception {
        EntityReference<UUID> adminUserRef = new EntityReference<>(
                UUID.randomUUID(),
                new ResourceIdentifier("identity-service", "admin")
        );
        when(auditActorResolver.resolve(any(String.class))).thenReturn(adminUserRef);

        mockMvc.perform(post("/api/v1/roles/" + roleId + "/activate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(activateRoleUseCase).activateRole(eq(roleId), eq(adminUserRef));
    }

    @Test
    void shouldDeactivateRoleSuccessfully() throws Exception {
        EntityReference<UUID> adminUserRef = new EntityReference<>(
                UUID.randomUUID(),
                new ResourceIdentifier("identity-service", "admin")
        );
        when(auditActorResolver.resolve(any(String.class))).thenReturn(adminUserRef);

        mockMvc.perform(post("/api/v1/roles/" + roleId + "/deactivate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(deactivateRoleUseCase).deactivateRole(eq(roleId), eq(adminUserRef));
    }
}
