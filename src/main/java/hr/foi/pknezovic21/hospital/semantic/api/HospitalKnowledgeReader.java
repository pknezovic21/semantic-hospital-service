package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagement;
import hr.foi.pknezovic21.hospital.domain.EquipmentReport;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestSummary;
import java.util.List;

public interface HospitalKnowledgeReader {

    List<EquipmentSummary> equipment(EquipmentFilter filter);

    EquipmentManagement equipmentManagement();

    List<EquipmentLoanSummary> equipmentLoans();

    List<PurchaseRequestSummary> purchaseRequests();

    HospitalOverview hospitalOverview();

    EquipmentReport equipmentReport();
}
