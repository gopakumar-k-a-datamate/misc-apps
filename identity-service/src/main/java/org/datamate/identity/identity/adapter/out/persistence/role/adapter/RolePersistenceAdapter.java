package org.datamate.identity.identity.adapter.out.persistence.role.adapter;

import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import lombok.RequiredArgsConstructor;
import org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity;
import org.datamate.identity.identity.adapter.out.persistence.role.mapper.RolePersistenceMapper;
import org.datamate.identity.identity.adapter.out.persistence.role.repository.SpringDataRoleRepository;
import org.datamate.identity.identity.adapter.out.persistence.role.specification.RoleSpecification;
import org.datamate.identity.identity.application.port.out.role.RolePersistencePort;
import org.datamate.identity.identity.application.query.role.RoleSearchCriteria;
import org.datamate.identity.identity.domain.model.role.entity.Role;
import org.datamate.identity.identity.domain.model.role.enums.RoleStatus;
import com.datamate.bedrock.framework.common.pagination.Paged;
import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.datamate.identity.shared.pagination.PaginationHelperMethods.toPageable;
import static org.datamate.identity.shared.pagination.PaginationHelperMethods.toPaged;

@Component
@RequiredArgsConstructor
public class RolePersistenceAdapter implements RolePersistencePort {

    @EnableLogger
    private Logger log;

    private final SpringDataRoleRepository repository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.mapToJpaEntity(role);
        RoleJpaEntity saved = repository.save(entity);
        log.debug("Role '{}' persisted with id {}", saved.getName(), saved.getId());
        return mapper.mapToDomain(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return repository.findById(id).map(mapper::mapToDomain);
    }

    @Override
    public List<Role> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdDate")).stream().map(mapper::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<Role> findAllByNameIn(Collection<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return repository.findAllByNameIn(names).stream()
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Paged<Role> searchRoles(RoleSearchCriteria criteria, PageQuery pageQuery) {
        log.debug("Searching roles with criteria: {} and page query: {}", criteria, pageQuery);
        Pageable pageable = toPageable(pageQuery, Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<RoleJpaEntity> entityPage = repository.findAll(
                RoleSpecification.filterRoles(criteria),
                pageable
        );
        Paged<Role> result = toPaged(entityPage.map(mapper::mapToDomain));
        log.debug("Search roles query returned {} of {} roles", result.content().size(), result.totalElements());
        return result;
    }

    @Override
    public List<Role> findActiveRoles(String search) {
        log.debug("Finding active roles with search query: '{}'", search);
        RoleSearchCriteria criteria = new RoleSearchCriteria(search, RoleStatus.ACTIVE);
        return repository.findAll(RoleSpecification.filterRoles(criteria), Sort.by(Sort.Direction.DESC, "createdDate"))
                .stream()
                .map(mapper::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        log.debug("Deleting role with id {}", id);
        repository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return repository.existsByName(name);
    }

    @Override
    public boolean existsByNameIgnoreCase(String name) {
        return repository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id) {
        return repository.existsByNameIgnoreCaseAndIdNot(name, id);
    }

    @Override
    public List<String> findRoleNamesByUserId(UUID userId) {
        return repository.findRoleNamesByUserId(userId);
    }
}
