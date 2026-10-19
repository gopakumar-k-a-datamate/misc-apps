package org.datamate.identity.identity.adapter.out.persistence.audit.repository;

import com.datamate.bedrock.framework.common.auditing.entity.ServiceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<ServiceAuditLog, Long>, JpaSpecificationExecutor<ServiceAuditLog> {
}
