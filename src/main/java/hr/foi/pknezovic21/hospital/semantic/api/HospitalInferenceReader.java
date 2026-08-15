package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentDetail;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRiskSummary;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import java.util.List;

public interface HospitalInferenceReader {

    List<UnitSummary> organizationUnits();

    List<EquipmentRequestSummary> equipmentRequests();

    List<MaintenanceRecordSummary> maintenanceRecords();

    List<EquipmentRiskSummary> highRiskEquipment();

    EquipmentDetail equipmentDetail(String equipmentId);
}
