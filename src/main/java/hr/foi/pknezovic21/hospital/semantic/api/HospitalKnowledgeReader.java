package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import java.util.List;

public interface HospitalKnowledgeReader {

    List<UnitSummary> organizationUnits();

    List<EquipmentSummary> equipment(EquipmentFilter filter);

    List<EquipmentLoanSummary> equipmentLoans();

    List<MaintenanceRecordSummary> maintenanceRecords();

    List<PurchaseRequestSummary> purchaseRequests();

    HospitalOverview hospitalOverview();
}
