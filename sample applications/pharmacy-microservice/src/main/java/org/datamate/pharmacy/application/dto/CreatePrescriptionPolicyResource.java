package org.datamate.pharmacy.application.dto;

import org.datamate.authz.model.policy.enumtype.FieldType;
import org.datamate.authz.annotation.PolicyField;
import org.datamate.authz.annotation.PolicyResource;

@PolicyResource(namespace = "pharmacy", resourceName = "prescription", action = "create", description = "Create Prescription")
public class CreatePrescriptionPolicyResource {

    @PolicyField(
            type = FieldType.STRING, 
            displayName = "Doctor Level", 
            optionsEndpoint = "/api/v1/pharmacy/doctors"
    )
    private String doctorLevel;

    @PolicyField(type = FieldType.BOOLEAN, displayName = "Same Ward")
    private Boolean isSameWard;

    public void setDoctorLevel(String doctorLevel) {this.doctorLevel =doctorLevel;}
    public void setIsSameWard(Boolean isSameWard) {this.isSameWard = isSameWard;}
}
