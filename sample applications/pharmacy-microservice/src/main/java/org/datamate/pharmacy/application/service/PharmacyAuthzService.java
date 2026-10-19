package org.datamate.pharmacy.application.service;

import com.datamate.bedrock.framework.common.pagination.PageQuery;
import org.datamate.authz.dto.policy.ConditionFieldDto;
import org.datamate.authz.dto.policy.PolicyGridItemDto;
import org.datamate.authz.dto.policy.PolicySearchQuery;
import org.datamate.authz.model.policy.enumtype.SubjectType;
import org.datamate.authz.rest.dto.SavePoliciesRequest;
import org.datamate.authz.service.policy.PolicyManagementService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PharmacyAuthzService {
	// Inject the library's consolidated service
	private final PolicyManagementService policyManagementService;

	public PharmacyAuthzService(PolicyManagementService policyManagementService) {
		this.policyManagementService = policyManagementService;
	}

	public List<ConditionFieldDto> getFields(String permissionCode) {
		// custom validations could be added here
		return policyManagementService.getConditionFields(permissionCode);
	}

	public List<String> getNamespaces() {
		// custom validations could be added here
		return policyManagementService.getNamespaces();
	}

	public List<PolicyGridItemDto> getPolicies(SubjectType subjectType, String subjectId, String namespace) {
		// custom validations could be added here
		return policyManagementService.getPolicies(subjectType, subjectId, namespace);
	}

	public Object getPolicies(SubjectType subjectType, String subjectId, String namespace, String search, Integer page,
			Integer size) {
		PolicySearchQuery criteria = new PolicySearchQuery(subjectType, subjectId, namespace, search);

		if (page != null || size != null) {
			int pageNum = (page != null) ? page : 1;
			int pageSize = (size != null) ? size : 10;
			PageQuery pageQuery = new PageQuery(pageNum, pageSize);
			return policyManagementService.getPolicies(criteria, pageQuery);
		}

		return policyManagementService.getPolicies(criteria);
	}

	public void savePolicies(SavePoliciesRequest request) {
		// custom validations could be added here
		// e.g., verifying if the subject exists in the Pharmacy database

		policyManagementService.savePolicies(request);
	}
}
