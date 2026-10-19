package org.datamate.identity.identity.adapter.in.rest.controller;

import org.datamate.authz.dto.policy.ConditionFieldDto;
import org.datamate.authz.dto.policy.PolicyGridItemDto;
import org.datamate.authz.model.policy.enumtype.SubjectType;
import org.datamate.authz.rest.dto.SavePoliciesRequest;
import org.datamate.authz.service.policy.PolicyManagementService;
import org.datamate.identity.identity.application.dto.role.RoleSelectDto;
import org.datamate.identity.identity.application.dto.user.UserResponseDto;
import org.datamate.identity.identity.application.port.in.role.SelectRolesUseCase;
import org.datamate.identity.identity.application.port.in.user.ListUserUseCase;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import com.datamate.bedrock.framework.common.pagination.Paged;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/authz")
@CrossOrigin(origins = "*")
public class AuthzPolicyController {

    private final PolicyManagementService policyManagementService;
    private final SelectRolesUseCase selectRolesUseCase;
    private final ListUserUseCase listUserUseCase;

    public AuthzPolicyController(
            PolicyManagementService policyManagementService,
            SelectRolesUseCase selectRolesUseCase,
            ListUserUseCase listUserUseCase) {
        this.policyManagementService = policyManagementService;
        this.selectRolesUseCase = selectRolesUseCase;
        this.listUserUseCase = listUserUseCase;
    }

    @GetMapping("/permissions/{permissionCode}/fields")
    public ResponseEntity<List<ConditionFieldDto>> getFields(
            @PathVariable String permissionCode) {
        return ResponseEntity.ok(policyManagementService.getConditionFields(permissionCode));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RoleSelectDto>> getRoles() {
        return ResponseEntity.ok(selectRolesUseCase.selectRoles(null));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getUsers() {
        Paged<UserResponseDto> paged = listUserUseCase.searchUsers(
                new UserSearchCriteria(null, null, null),
                new PageQuery(1, 1000)
        );
        return ResponseEntity.ok(paged.content());
    }
}
