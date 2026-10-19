package org.datamate.pharmacy.adapter.in.rest;

import org.datamate.pharmacy.application.dto.DispenseMedicationRequest;
import org.datamate.pharmacy.application.usecase.DispenseMedicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pharmacy")
public class MedicineController {

    private final DispenseMedicationService dispenseMedicationService;

    public MedicineController(DispenseMedicationService dispenseMedicationService) {
        this.dispenseMedicationService = dispenseMedicationService;
    }

    @PostMapping("/dispense")
    public ResponseEntity<String> dispenseMedication(@RequestBody DispenseMedicationRequest request) {

        String result = dispenseMedicationService.dispense(request);
        return ResponseEntity.ok(result);
    }
}
