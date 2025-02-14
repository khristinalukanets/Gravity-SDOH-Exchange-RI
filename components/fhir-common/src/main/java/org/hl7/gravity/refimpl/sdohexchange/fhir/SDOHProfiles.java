package org.hl7.gravity.refimpl.sdohexchange.fhir;

import lombok.experimental.UtilityClass;

/**
 * SDOH profiles for affected FHIR resources. Currently we do not force EHRs to use resources with these profiles. But
 * all resources generated by this app will have one.
 */
@UtilityClass
public class SDOHProfiles {

  public String TASK = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-Task";
  public String PATIENT_TASK = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-TaskForPatient";
  public String CONDITION = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-Condition";
  public String SERVICE_REQUEST = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-ServiceRequest";
  public String GOAL = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-Goal";
  public String PROCEDURE = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-Procedure";
  public String CONSENT = "http://hl7.org/fhir/us/sdoh-clinicalcare/StructureDefinition/SDOHCC-Consent";
  public String QUESTIONNAIRE_RESPONSE = "http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaireresponse|2.7";
}
