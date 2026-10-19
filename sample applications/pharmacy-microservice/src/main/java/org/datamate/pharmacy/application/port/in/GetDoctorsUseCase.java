package org.datamate.pharmacy.application.port.in;

import com.datamate.bedrock.framework.common.pagination.PaginatedResponse;
import org.datamate.authz.rest.dto.AllowedValueResponse;

/**
 * Inbound Port (Use Case Interface) for getting doctors.
 * Defines the operations the outside world (like a REST controller)
 * can trigger within the application layer.
 */
public interface GetDoctorsUseCase {
    PaginatedResponse<AllowedValueResponse> execute(int page, int size, String search);
}
