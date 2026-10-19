package org.datamate.identity.identity.application.port.out.user;

import org.datamate.identity.identity.domain.model.user.entity.User;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import java.util.List;
import java.util.Optional;

import java.util.UUID;

public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUserName(String userName);
    Optional<User> findByUserNameOrEmail(String userName, String email);
    boolean existsByUserName(String userName);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    boolean existsByUserNameAndIdNot(String userName, UUID id);
    long countActiveUsersWithRoleExcept(String roleName, UUID exceptUserId);
    List<User> findAll();
    Paged<User> searchUsers(UserSearchCriteria criteria, PageQuery pageQuery);
}
