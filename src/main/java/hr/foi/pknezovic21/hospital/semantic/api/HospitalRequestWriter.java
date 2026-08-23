package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;

public interface HospitalRequestWriter {

    void addEquipmentRequest(String id, String requestNumber, String requestedAt, EquipmentRequestForm form);

    void cancelEquipmentRequest(String id, String cancelledAt);
}
