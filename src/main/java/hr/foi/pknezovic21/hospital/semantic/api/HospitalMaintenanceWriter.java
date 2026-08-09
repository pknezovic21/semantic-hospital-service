package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;

public interface HospitalMaintenanceWriter {

    void addMaintenanceRecord(String id, String maintenanceNumber, String reportedAt, MaintenanceRecordForm form);
}
