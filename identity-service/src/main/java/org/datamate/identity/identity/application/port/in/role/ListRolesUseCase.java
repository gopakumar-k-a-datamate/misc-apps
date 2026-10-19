package org.datamate.identity.identity.application.port.in.role;

import org.datamate.identity.identity.application.dto.role.RoleDto;
import org.datamate.identity.identity.application.query.role.RoleSearchCriteria;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;

public interface ListRolesUseCase {
    Paged<RoleDto> listRoles(RoleSearchCriteria criteria, PageQuery pageQuery);
}
