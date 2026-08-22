package hr.foi.pknezovic21.hospital.service;

import hr.foi.pknezovic21.hospital.domain.EquipmentDetail;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagement;
import hr.foi.pknezovic21.hospital.domain.EquipmentReport;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRiskSummary;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;
import hr.foi.pknezovic21.hospital.domain.PurchaseReceiptForm;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentLoanWriter;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeReader;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalMaintenanceWriter;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalPurchaseRequestWriter;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalRequestWriter;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class HospitalService {

    private final HospitalKnowledgeReader knowledgeReader;
    private final HospitalInferenceReader inferenceReader;
    private final HospitalRequestWriter requestWriter;
    private final HospitalEquipmentLoanWriter equipmentLoanWriter;
    private final HospitalMaintenanceWriter maintenanceWriter;
    private final HospitalPurchaseRequestWriter purchaseRequestWriter;

    public HospitalService(
            HospitalKnowledgeReader knowledgeReader,
            HospitalInferenceReader inferenceReader,
            HospitalRequestWriter requestWriter,
            HospitalEquipmentLoanWriter equipmentLoanWriter,
            HospitalMaintenanceWriter maintenanceWriter,
            HospitalPurchaseRequestWriter purchaseRequestWriter
    ) {
        this.knowledgeReader = knowledgeReader;
        this.inferenceReader = inferenceReader;
        this.requestWriter = requestWriter;
        this.equipmentLoanWriter = equipmentLoanWriter;
        this.maintenanceWriter = maintenanceWriter;
        this.purchaseRequestWriter = purchaseRequestWriter;
    }

    public List<UnitSummary> organizationUnits() {
        return inferenceReader.organizationUnits();
    }

    public List<EquipmentSummary> equipment(EquipmentFilter filter) {
        EquipmentFilter normalizedFilter = normalize(filter);
        return knowledgeReader.equipment(normalizedFilter);
    }

    public EquipmentDetail equipmentDetail(String equipmentId) {
        requireText(equipmentId, "Equipment is required.");
        EquipmentDetail detail = inferenceReader.equipmentDetail(equipmentId.trim());
        if (detail == null) {
            throw new IllegalArgumentException("Equipment was not found.");
        }
        return detail;
    }

    public EquipmentManagement equipmentManagement() {
        return knowledgeReader.equipmentManagement();
    }

    public List<EquipmentRequestSummary> equipmentRequests() {
        return inferenceReader.equipmentRequests();
    }

    public List<EquipmentRiskSummary> highRiskEquipment() {
        return inferenceReader.highRiskEquipment();
    }

    public String createEquipmentRequest(EquipmentRequestForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Equipment request is required.");
        }
        requireText(form.requestedForUnitId(), "Requested unit is required.");
        requireText(form.requestedTypeId(), "Requested equipment type is required.");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String id = "EquipmentRequest-" + suffix;
        String requestNumber = "REQ-" + suffix;
        requestWriter.addEquipmentRequest(id, requestNumber, form);
        return id;
    }

    public List<EquipmentLoanSummary> equipmentLoans() {
        return knowledgeReader.equipmentLoans();
    }

    public String createEquipmentLoan(EquipmentLoanForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Equipment loan is required.");
        }
        requireText(form.equipmentId(), "Equipment is required.");
        requireText(form.loanedToUnitId(), "Loan unit is required.");
        requireText(form.loanedToLocationId(), "Loan location is required.");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String id = "EquipmentLoan-" + suffix;
        String loanNumber = "LOAN-" + suffix;
        EquipmentLoanForm cleanForm = new EquipmentLoanForm(
                form.equipmentId().trim(),
                form.loanedToUnitId().trim(),
                form.loanedToLocationId().trim(),
                clean(form.requestId())
        );
        equipmentLoanWriter.addEquipmentLoan(id, loanNumber, Instant.now().toString(), cleanForm);
        return id;
    }

    public void returnEquipmentLoan(String loanId) {
        requireText(loanId, "Equipment loan is required.");
        equipmentLoanWriter.returnEquipmentLoan(loanId.trim(), Instant.now().toString());
    }

    public List<MaintenanceRecordSummary> maintenanceRecords() {
        return inferenceReader.maintenanceRecords();
    }

    public List<PurchaseRequestSummary> purchaseRequests() {
        return knowledgeReader.purchaseRequests();
    }

    public HospitalOverview hospitalOverview() {
        return knowledgeReader.hospitalOverview();
    }

    public EquipmentReport equipmentReport() {
        return knowledgeReader.equipmentReport();
    }

    public String createMaintenanceRecord(MaintenanceRecordForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Maintenance record is required.");
        }
        requireText(form.equipmentId(), "Equipment is required.");
        requireText(form.reason(), "Maintenance reason is required.");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String id = "MaintenanceRecord-" + suffix;
        String maintenanceNumber = "MNT-" + suffix;
        MaintenanceRecordForm cleanForm = new MaintenanceRecordForm(form.equipmentId().trim(), form.reason().trim());
        maintenanceWriter.addMaintenanceRecord(id, maintenanceNumber, Instant.now().toString(), cleanForm);
        return id;
    }

    public void completeMaintenanceRecord(String maintenanceId) {
        requireText(maintenanceId, "Maintenance record is required.");
        maintenanceWriter.completeMaintenanceRecord(maintenanceId.trim(), Instant.now().toString());
    }

    public String createPurchaseRequest(PurchaseRequestForm form) {
        if (form == null) {
            throw new IllegalArgumentException("Purchase request is required.");
        }
        requireText(form.equipmentRequestId(), "Equipment request is required.");
        requireText(form.supplierId(), "Supplier is required.");
        requireText(form.reason(), "Purchase reason is required.");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String id = "PurchaseRequest-" + suffix;
        String purchaseNumber = "PUR-" + suffix;
        PurchaseRequestForm cleanForm = new PurchaseRequestForm(
                form.equipmentRequestId().trim(),
                form.supplierId().trim(),
                form.reason().trim()
        );
        purchaseRequestWriter.addPurchaseRequest(id, purchaseNumber, Instant.now().toString(), cleanForm);
        return id;
    }

    public String receivePurchaseRequest(String purchaseRequestId, PurchaseReceiptForm form) {
        requireText(purchaseRequestId, "Purchase request is required.");
        if (form == null) {
            throw new IllegalArgumentException("Purchase receipt is required.");
        }
        requireText(form.locationId(), "Receipt location is required.");
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String equipmentId = "Equipment-" + suffix;
        String assetNumber = "AST-" + suffix;
        purchaseRequestWriter.receivePurchaseRequest(
                purchaseRequestId.trim(),
                equipmentId,
                assetNumber,
                Instant.now().toString(),
                new PurchaseReceiptForm(form.locationId().trim())
        );
        return equipmentId;
    }

    public void cancelPurchaseRequest(String purchaseRequestId) {
        requireText(purchaseRequestId, "Purchase request is required.");
        purchaseRequestWriter.cancelPurchaseRequest(purchaseRequestId.trim(), Instant.now().toString());
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private EquipmentFilter normalize(EquipmentFilter filter) {
        if (filter == null) {
            return new EquipmentFilter(null, null, null);
        }
        return new EquipmentFilter(
                clean(filter.statusId()),
                clean(filter.typeId()),
                clean(filter.unitId())
        );
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
