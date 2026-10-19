package org.datamate.identity.adapter.out.persistence.user.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.datamate.identity.identity.adapter.out.persistence.user.entity.UserJpaEntity;
import org.datamate.identity.identity.adapter.out.persistence.user.specification.UserSpecification;
import org.datamate.identity.identity.application.query.user.UserSearchCriteria;
import org.datamate.identity.identity.domain.model.user.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSpecificationTest {

    @Test
    void shouldBuildSpecificationWithoutFilters() {
        Specification<UserJpaEntity> spec = UserSpecification.filterUsers(new UserSearchCriteria(null, null, null));
        assertNotNull(spec);

        Root<UserJpaEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        doReturn(UserJpaEntity.class).when(query).getResultType();
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
    }

    @Test
    void shouldBuildSpecificationWithAllFilters() {
        Specification<UserJpaEntity> spec = UserSpecification.filterUsers(new UserSearchCriteria("john", "USER", UserStatus.ACTIVE));
        assertNotNull(spec);

        Root<UserJpaEntity> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate predicate = mock(Predicate.class);

        @SuppressWarnings("rawtypes")
        Join rolesJoin = mock(Join.class);

        doReturn(UserJpaEntity.class).when(query).getResultType();
        when(root.get(anyString())).thenReturn(mock(Path.class));
        when(root.join("roles")).thenReturn(rolesJoin);
        when(rolesJoin.get("name")).thenReturn(mock(Path.class));
        when(cb.lower(any(Expression.class))).thenReturn(mock(Expression.class));
        when(cb.concat(any(Expression.class), any(Expression.class))).thenReturn(mock(Expression.class));
        when(cb.concat(any(Expression.class), anyString())).thenReturn(mock(Expression.class));
        when(cb.like(any(Expression.class), anyString())).thenReturn(predicate);
        when(cb.or(any(Predicate[].class))).thenReturn(predicate);
        when(cb.equal(any(Expression.class), any())).thenReturn(predicate);
        when(cb.and(any(Predicate[].class))).thenReturn(predicate);

        Predicate result = spec.toPredicate(root, query, cb);
        assertNotNull(result);
    }
}
