package org.datamate.pharmacy.adapter.in.rest;

import org.datamate.authz.annotation.ProtectedResource;
import org.datamate.pharmacy.application.dto.DispensePayload;
import org.datamate.pharmacy.application.service.DrugDispensationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dispensation")
public class DrugDispensationController {

    private final DrugDispensationService dispensationService;

    public DrugDispensationController(DrugDispensationService dispensationService) {
        this.dispensationService = dispensationService;
    }

    // 1. Stacked Annotations Happy Path (RBAC + ABAC)
    @PostMapping("/execute")
    public ResponseEntity<String> executeDispensation(@RequestBody DispensePayload resource) {
        return ResponseEntity.ok(dispensationService.dispenseDrug(resource));
    }

    // 2. Fail-Closed Edge Case Test
    @PostMapping("/test-fail-closed")
    public ResponseEntity<String> testFailClosed() {
        // Passing an unannotated object (String) to the enforcer
        return ResponseEntity.ok(dispensationService.testUnannotatedObject("This is an unannotated string"));
    }
}
