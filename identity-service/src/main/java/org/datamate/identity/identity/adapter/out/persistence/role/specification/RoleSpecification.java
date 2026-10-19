package org.datamate.identity.identity.adapter.out.persistence.role.specification;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import com.datamate.bedrock.framework.common.logging.util.LoggerManager;
import jakarta.persistence.criteria.Predicate;
import org.datamate.identity.identity.adapter.out.persistence.role.entity.RoleJpaEntity;
import org.datamate.identity.identity.application.query.role.RoleSearchCriteria;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoleSpecification {

    private static Logger log;

    private static Logger logger() {
        if (log == null && LoggerManager.isInitialized()) {
            log = LoggerManager.getLogger(RoleSpecification.class);
        }
        return log;
    }

    public static Specification<RoleJpaEntity> filterRoles(RoleSearchCriteria criteria) {
        Logger logger = logger();
        if (logger != null) {
            logger.debug("Building role search specification with criteria {}", criteria);
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.search() != null && !criteria.search().isBlank()) {
                String searchPattern = "%" + criteria.search().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("name")), searchPattern));
            }

            if (criteria.status() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
