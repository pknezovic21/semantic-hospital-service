package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentLocationForm;

public interface HospitalEquipmentLocationWriter {

    void updateEquipmentLocation(String equipmentId, EquipmentLocationForm form);
}
