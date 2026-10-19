package org.datamate.pharmacy.application.dto;

import org.datamate.authz.annotation.PolicyField;
import org.datamate.authz.annotation.PolicyResource;
import org.datamate.authz.model.policy.enumtype.FieldType;

/**
 * OPA Policy Resource for dispensing medication.
 * The annotations define the namespace, resource, action, and dynamic fields
 * that OPA uses to evaluate the ABAC rules.
 */
@PolicyResource(namespace = "pharmacy", resourceName = "medication", action = "dispense", description = "Dispense a medication to a patient")
public class DispenseMedicationPolicyResource {

    @PolicyField(type = FieldType.STRING, displayName = "Drug Class", allowedValues = {"OTC", "PRESCRIPTION", "CONTROLLED"})
    private String drugClass;

    @PolicyField(type = FieldType.NUMBER, displayName = "Patient Age")
    private Integer patientAge;

    public String getDrugClass() {
        return drugClass;
    }

    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }

    public Integer getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(Integer patientAge) {
        this.patientAge = patientAge;
    }
}
