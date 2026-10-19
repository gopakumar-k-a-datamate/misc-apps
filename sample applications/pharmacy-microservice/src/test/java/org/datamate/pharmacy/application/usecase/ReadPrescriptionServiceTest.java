package org.datamate.pharmacy.application.usecase;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.ReadPrescriptionPolicyResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReadPrescriptionService - Unit Tests")
class ReadPrescriptionServiceTest {

    @Mock
    private PolicyEnforcer policyEnforcer;

    @InjectMocks
    private ReadPrescriptionService readPrescriptionService;

    @Test
    @DisplayName("Should read prescriptions and enforce policy")
    void shouldReadPrescriptionsSuccessfully() {
        List<String> results = readPrescriptionService.readPrescriptions();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Prescription #101: Aspirin for PAT-001", results.get(0));

        // Verify OPA policy enforcement was triggered
        ArgumentCaptor<ReadPrescriptionPolicyResource> captor = ArgumentCaptor.forClass(ReadPrescriptionPolicyResource.class);
        verify(policyEnforcer).enforce(captor.capture());
        assertNotNull(captor.getValue());
    }
}
