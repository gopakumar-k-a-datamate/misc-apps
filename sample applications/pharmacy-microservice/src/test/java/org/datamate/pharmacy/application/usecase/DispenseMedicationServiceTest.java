package org.datamate.pharmacy.application.usecase;

import com.datamate.bedrock.framework.common.logging.service.Logger;
import org.datamate.authz.enforcement.PolicyEnforcer;
import org.datamate.pharmacy.application.dto.DispenseMedicationPolicyResource;
import org.datamate.pharmacy.application.dto.DispenseMedicationRequest;
import org.datamate.pharmacy.application.dto.MedicationDto;
import org.datamate.pharmacy.application.dto.PatientDto;
import org.datamate.pharmacy.application.exception.InsufficientStockException;
import org.datamate.pharmacy.application.exception.MedicationNotFoundException;
import org.datamate.pharmacy.application.exception.PatientNotFoundException;
import org.datamate.pharmacy.application.port.out.InventoryAlertPort;
import org.datamate.pharmacy.application.port.out.MedicationPort;
import org.datamate.pharmacy.application.port.out.PatientPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DispenseMedicationService - Unit Tests")
class DispenseMedicationServiceTest {

    @Mock
    private PolicyEnforcer policyEnforcer;
    @Mock
    private MedicationPort medicationPort;
    @Mock
    private PatientPort patientPort;
    @Mock
    private InventoryAlertPort inventoryAlertPort;
    @Mock
    private Logger log;

    @InjectMocks
    private DispenseMedicationService dispenseMedicationService;

    @BeforeEach
    void setUp() throws Exception {
        // Inject mock logger via reflection since it's injected via @EnableLogger
        java.lang.reflect.Field logField = DispenseMedicationService.class.getDeclaredField("log");
        logField.setAccessible(true);
        logField.set(dispenseMedicationService, log);
    }

    @Test
    @DisplayName("Should throw MedicationNotFoundException when medication does not exist")
    void shouldThrowWhenMedicationNotFound() {
        DispenseMedicationRequest request = new DispenseMedicationRequest("MED-999", "PAT-123", 10);
        when(medicationPort.getMedicationById("MED-999")).thenReturn(null);

        assertThrows(MedicationNotFoundException.class, () -> dispenseMedicationService.dispense(request));
    }

    @Test
    @DisplayName("Should throw PatientNotFoundException when patient does not exist")
    void shouldThrowWhenPatientNotFound() {
        DispenseMedicationRequest request = new DispenseMedicationRequest("MED-123", "PAT-999", 10);
        when(medicationPort.getMedicationById("MED-123")).thenReturn(new MedicationDto("MED-123", "Paracetamol", "Painkiller", 100, 20));
        when(patientPort.getPatientById("PAT-999")).thenReturn(null);

        assertThrows(PatientNotFoundException.class, () -> dispenseMedicationService.dispense(request));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when requested quantity exceeds stock")
    void shouldThrowWhenInsufficientStock() {
        DispenseMedicationRequest request = new DispenseMedicationRequest("MED-123", "PAT-123", 150);
        when(medicationPort.getMedicationById("MED-123")).thenReturn(new MedicationDto("MED-123", "Paracetamol", "Painkiller", 100, 20));
        when(patientPort.getPatientById("PAT-123")).thenReturn(new PatientDto("PAT-123", "John Doe", 30, "Ward A"));

        assertThrows(InsufficientStockException.class, () -> dispenseMedicationService.dispense(request));
    }

    @Test
    @DisplayName("Should dispense medication successfully and enforce policy")
    void shouldDispenseSuccessfully() {
        DispenseMedicationRequest request = new DispenseMedicationRequest("MED-123", "PAT-123", 10);
        MedicationDto medication = new MedicationDto("MED-123", "Paracetamol", "Painkiller", 100, 20);
        PatientDto patient = new PatientDto("PAT-123", "John Doe", 30, "Ward A");

        when(medicationPort.getMedicationById("MED-123")).thenReturn(medication);
        when(patientPort.getPatientById("PAT-123")).thenReturn(patient);

        String result = dispenseMedicationService.dispense(request);

        assertEquals("Dispensed successfully! OPA approved.", result);
        
        // Verify OPA was called with correct context
        ArgumentCaptor<DispenseMedicationPolicyResource> captor = ArgumentCaptor.forClass(DispenseMedicationPolicyResource.class);
        verify(policyEnforcer).enforce(captor.capture());
        assertEquals("Painkiller", captor.getValue().getDrugClass());
        assertEquals(30, captor.getValue().getPatientAge());

        // Verify stock was reduced and saved
        verify(medicationPort).saveMedication(any(MedicationDto.class));
        assertEquals(90, medication.getCurrentStock());
        
        // Stock didn't drop below threshold (90 > 20)
        verify(inventoryAlertPort, never()).sendLowStockAlert(anyString(), anyInt());
    }

    @Test
    @DisplayName("Should trigger inventory alert when stock drops below minimum threshold")
    void shouldTriggerInventoryAlert() {
        DispenseMedicationRequest request = new DispenseMedicationRequest("MED-123", "PAT-123", 90);
        // Current stock 100, Min threshold 20. Dispense 90 -> remaining 10, which is <= 20.
        MedicationDto medication = new MedicationDto("MED-123", "Paracetamol", "Painkiller", 100, 20);
        PatientDto patient = new PatientDto("PAT-123", "John Doe", 30, "Ward A");

        when(medicationPort.getMedicationById("MED-123")).thenReturn(medication);
        when(patientPort.getPatientById("PAT-123")).thenReturn(patient);

        dispenseMedicationService.dispense(request);

        verify(medicationPort).saveMedication(any(MedicationDto.class));
        verify(inventoryAlertPort).sendLowStockAlert("MED-123", 10);
    }
}
