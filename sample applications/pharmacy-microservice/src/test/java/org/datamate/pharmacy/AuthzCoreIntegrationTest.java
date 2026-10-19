package org.datamate.pharmacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.datamate.pharmacy.application.dto.DrugDispensationPolicyResource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.datamate.authz.api.policy.PolicyEvaluationClient;
import org.datamate.authz.dto.policy.EvaluationResult;
import org.datamate.authz.enforcement.AuthorizationContext;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import java.util.Map;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthzCoreIntegrationTest {

    @MockBean
    private PolicyEvaluationClient policyEvaluationClient;

    @BeforeEach
    public void setupMockOpa() {
        Mockito.when(policyEvaluationClient.evaluate(anyString(), any(AuthorizationContext.class)))
               .thenAnswer(invocation -> {
                   AuthorizationContext context = invocation.getArgument(1);
                   Map<String, Object> resource = context.resourceData();
                   
                   // Deny if no roles
                   if (context.roles() == null || context.roles().isEmpty()) {
                       return EvaluationResult.denied("No roles");
                   }
                   
                   if (context.permissionCode().equals("pharmacy:dispensation:execute")) {
                       if (resource == null || !resource.containsKey("drugCategory") || !resource.containsKey("dispenseQuantity")) {
                           return EvaluationResult.denied("Missing resource attributes");
                       }
                       String schedule = (String) resource.get("drugCategory");
                       Number qty = (Number) resource.get("dispenseQuantity");
                       Object specialty = resource.get("doctorSpecialty");
                       
                       if (specialty == null && "OTC".equals(schedule)) {
                           // For testNullPolicyFieldValue_EvaluatesFalse
                           if (context.roles().contains("ROLE_NURSE")) {
                               return EvaluationResult.denied("Specialty is null");
                           }
                       }
                       
                       if ("SCHEDULE_II".equals(schedule)) {
                           if (!context.roles().contains("ROLE_SENIOR_PHARMACIST") && !context.roles().contains("SENIOR_PHARMACIST")) {
                               return EvaluationResult.denied("Requires SENIOR_PHARMACIST");
                           }
                           if (qty.intValue() > 30) {
                               return EvaluationResult.denied("Quantity exceeded for Schedule II");
                           }
                       } else if ("OTC".equals(schedule)) {
                           if (!context.roles().contains("ROLE_PHARMACIST") && !context.roles().contains("PHARMACIST") &&
                               !context.roles().contains("ROLE_SENIOR_PHARMACIST") && !context.roles().contains("SENIOR_PHARMACIST")) {
                               return EvaluationResult.denied("Requires PHARMACIST");
                           }
                       }
                       return EvaluationResult.granted();
                   }
                   
                   return EvaluationResult.denied("Unknown permission");
               });
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- 1. Stacked Annotations (Happy Path) ---
    // User has SENIOR_PHARMACIST role, Schedule II, Age 20, Qty 20 -> ALLOWED
    @Test
    @WithMockUser(username = "senior_pharm", roles = {"SENIOR_PHARMACIST"})
    public void testStackedAnnotations_HappyPath_SeniorPharmacist() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "SCHEDULE_II", 20, "GENERAL", "clinic1", 20, false
        );

        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isOk());
    }

    // User has PHARMACIST role, OTC -> ALLOWED
    @Test
    @WithMockUser(username = "pharm", roles = {"PHARMACIST"})
    public void testStackedAnnotations_HappyPath_Pharmacist() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "OTC", 25, "GENERAL", "clinic1", 50, false
        );

        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isOk());
    }

    // --- 2. Stacked Annotations (Denied Paths) ---
    // User has PHARMACIST role but tries to dispense Schedule II -> DENIED (403)
    @Test
    @WithMockUser(username = "pharm", roles = {"PHARMACIST"})
    public void testStackedAnnotations_Denied_WrongRole() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "SCHEDULE_II", 20, "GENERAL", "clinic1", 20, false
        );

        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isForbidden());
    }

    // User has SENIOR_PHARMACIST role, Schedule II, but Qty is 50 (max 30) -> DENIED
    @Test
    @WithMockUser(username = "senior_pharm", roles = {"SENIOR_PHARMACIST"})
    public void testStackedAnnotations_Denied_QuantityExceeded() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "SCHEDULE_II", 20, "GENERAL", "clinic1", 50, false
        );

        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isForbidden());
    }

    // --- 3. Missing Class Annotation (Fail-Closed) ---
    // Firing an unannotated object directly to the enforcer
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testFailClosed_UnannotatedObject() throws Exception {
        mockMvc.perform(post("/api/dispensation/test-fail-closed"))
                .andExpect(status().isForbidden());
    }

    // --- 4. Null PolicyField Value ---
    // If a policy requires 'doctorSpecialty' == 'ONCOLOGY' and we send NULL, it should evaluate to false/deny
    @Test
    @WithMockUser(username = "pharm", roles = {"NURSE"})
    public void testNullPolicyFieldValue_EvaluatesFalse() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "OTC", 20, null, "clinic1", 20, true // Specialty is NULL
        );

        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isForbidden());
    }

    // --- 5. Missing Context / No Roles ---
    // Request with no authentication/roles should fail the coarse-grained check immediately
    @Test
    public void testMissingIdentityContext_Denied() throws Exception {
        DrugDispensationPolicyResource resource = new DrugDispensationPolicyResource(
                "OTC", 25, "GENERAL", "clinic1", 50, false
        );

        // Expect 401 or 403 depending on Spring Security config, but definitely not 200
        mockMvc.perform(post("/api/dispensation/execute")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resource)))
                .andExpect(status().isForbidden());
    }
}
