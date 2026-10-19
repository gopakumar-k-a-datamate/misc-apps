package org.datamate.pharmacy.application.dto;

import org.datamate.authz.annotation.PolicyField;
import org.datamate.authz.annotation.PolicyResource;
import org.datamate.authz.model.policy.enumtype.FieldType;

@PolicyResource(namespace = "pharmacy", resourceName = "dispensation", action = "execute")
public class DrugDispensationPolicyResource {

    @PolicyField(displayName = "Drug Category", type = FieldType.STRING)
    private String drugCategory;

    @PolicyField(displayName = "Patient Age", type = FieldType.NUMBER)
    private int patientAge;

    @PolicyField(displayName = "Doctor Specialty", type = FieldType.STRING)
    private String doctorSpecialty;

    @PolicyField(displayName = "Clinic ID", type = FieldType.STRING)
    private String clinicId;

    @PolicyField(displayName = "Dispense Quantity", type = FieldType.NUMBER)
    private int dispenseQuantity;

    @PolicyField(displayName = "Requires Insurance Approval", type = FieldType.BOOLEAN)
    private boolean requiresInsuranceApproval;

    public DrugDispensationPolicyResource() {
    }

    public DrugDispensationPolicyResource(String drugCategory, int patientAge, String doctorSpecialty, String clinicId, int dispenseQuantity, boolean requiresInsuranceApproval) {
        this.drugCategory = drugCategory;
        this.patientAge = patientAge;
        this.doctorSpecialty = doctorSpecialty;
        this.clinicId = clinicId;
        this.dispenseQuantity = dispenseQuantity;
        this.requiresInsuranceApproval = requiresInsuranceApproval;
    }

    public String getDrugCategory() {
        return drugCategory;
    }

    public void setDrugCategory(String drugCategory) {
        this.drugCategory = drugCategory;
    }

    public int getPatientAge() {
        return patientAge;
    }

    public void setPatientAge(int patientAge) {
        this.patientAge = patientAge;
    }

    public String getDoctorSpecialty() {
        return doctorSpecialty;
    }

    public void setDoctorSpecialty(String doctorSpecialty) {
        this.doctorSpecialty = doctorSpecialty;
    }

    public String getClinicId() {
        return clinicId;
    }

    public void setClinicId(String clinicId) {
        this.clinicId = clinicId;
    }

    public int getDispenseQuantity() {
        return dispenseQuantity;
    }

    public void setDispenseQuantity(int dispenseQuantity) {
        this.dispenseQuantity = dispenseQuantity;
    }

    public boolean isRequiresInsuranceApproval() {
        return requiresInsuranceApproval;
    }

    public void setRequiresInsuranceApproval(boolean requiresInsuranceApproval) {
        this.requiresInsuranceApproval = requiresInsuranceApproval;
    }
}
