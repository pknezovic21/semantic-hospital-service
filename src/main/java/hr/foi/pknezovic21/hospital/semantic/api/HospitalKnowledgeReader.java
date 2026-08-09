package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import java.util.List;

public interface HospitalKnowledgeReader {

    List<UnitSummary> organizationUnits();

    List<EquipmentSummary> equipment(EquipmentFilter filter);

    List<MaintenanceRecordSummary> maintenanceRecords();
}
