package org.datamate.pharmacy.application.usecase;

import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.CreatePrescriptionRequest;
import org.datamate.pharmacy.application.port.out.PatientPort;
import org.datamate.pharmacy.application.port.out.PractitionerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePrescriptionService - Unit Tests")
class CreatePrescriptionServiceTest {

    @Mock
    private PractitionerPort practitionerPort;

    @Mock
    private PatientPort patientPort;

    @Mock
    private PolicyEnforcer enforcer;

    @InjectMocks
    private CreatePrescriptionService createPrescriptionService;

    @Test
    @DisplayName("Should successfully create a prescription")
    void shouldCreatePrescriptionSuccessfully() {
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                "DOC-456", "PAT-123", "MED-789"
        );

        String result = createPrescriptionService.createPrescription(request);
        
        assertEquals("Prescription created ", result);
        
        // Note: The @PreAuthorize enforcement is handled by Spring Security aspect
        // in integration tests, not typically mockable in this direct unit test.
    }
}
