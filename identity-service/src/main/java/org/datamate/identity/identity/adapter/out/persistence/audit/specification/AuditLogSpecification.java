package org.datamate.identity.identity.adapter.out.persistence.audit.specification;

import com.datamate.bedrock.framework.common.auditing.entity.ServiceAuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<ServiceAuditLog> withActionAndUsername(String action, String username) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(action)) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }

            if (StringUtils.hasText(username)) {
                String searchPattern = "%" + username + "%";
                predicates.add(criteriaBuilder.like(root.get("arguments"), searchPattern));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
