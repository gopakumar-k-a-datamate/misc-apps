package org.datamate.identity.identity.adapter.out.persistence.user.adapter;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.adapter.out.persistence.user.entity.UserJpaEntity;
import org.datamate.identity.identity.adapter.out.persistence.user.mapper.UserPersistenceMapper;
import org.datamate.identity.identity.adapter.out.persistence.user.repository.SpringDataUserRepository;
import org.datamate.identity.identity.adapter.out.persistence.user.specification.UserSpecification;
import org.datamate.identity.identity.application.port.out.user.UserPersistencePort;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import org.datamate.identity.identity.domain.model.user.entity.User;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.datamate.identity.shared.pagination.PaginationHelperMethods.toPageable;
import static org.datamate.identity.shared.pagination.PaginationHelperMethods.toPaged;

@Component
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserPersistencePort {

    @EnableLogger
    private Logger log;

    private final SpringDataUserRepository repository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = repository.findById(user.getId())
                .orElseGet(() -> {
                    UserJpaEntity newEntity = new UserJpaEntity();
                    newEntity.setId(user.getId());
                    return newEntity;
                });
        mapper.updateJpaEntity(entity, user);
        UserJpaEntity savedEntity = repository.save(entity);
        log.debug("User '{}' persisted with id {}", savedEntity.getUserName(), savedEntity.getId());
        return mapper.mapToDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return repository.findById(id).map(mapper::mapToDomain);
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        return repository.findByUserName(userName).map(mapper::mapToDomain);
    }

    @Override
    public Optional<User> findByUserNameOrEmail(String userName, String email) {
        return repository.findByUserNameOrEmail(userName, email).map(mapper::mapToDomain);
    }

    @Override
    public boolean existsByUserName(String userName) {
        return repository.existsByUserName(userName);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, UUID id) {
        return repository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public boolean existsByUserNameAndIdNot(String userName, UUID id) {
        return repository.existsByUserNameAndIdNot(userName, id);
    }

    @Override
    public long countActiveUsersWithRoleExcept(String roleName, UUID exceptUserId) {
        return repository.countActiveUsersWithRoleExcept(roleName, exceptUserId);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")).stream().map(mapper::mapToDomain).toList();
    }

    @Override
    public Paged<User> searchUsers(UserSearchCriteria criteria, PageQuery pageQuery) {
        log.debug("Searching users with criteria {} and page query {}", criteria, pageQuery);
        Pageable pageable = toPageable(pageQuery, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<UserJpaEntity> entityPage = repository.findAll(
                UserSpecification.filterUsers(criteria),
                pageable
        );
        Paged<User> result = toPaged(entityPage.map(mapper::mapToDomain));
        log.debug("Search users query returned {} of {} users", result.content().size(), result.totalElements());
        return result;
    }
}
