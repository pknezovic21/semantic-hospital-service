package hr.foi.pknezovic21.hospital.service;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentStatusForm;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentWriter;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentLoanWriter;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeReader;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalMaintenanceWriter;
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
    private final HospitalEquipmentWriter equipmentWriter;
    private final HospitalEquipmentLoanWriter equipmentLoanWriter;
    private final HospitalMaintenanceWriter maintenanceWriter;

    public HospitalService(
            HospitalKnowledgeReader knowledgeReader,
            HospitalInferenceReader inferenceReader,
            HospitalRequestWriter requestWriter,
            HospitalEquipmentWriter equipmentWriter,
            HospitalEquipmentLoanWriter equipmentLoanWriter,
            HospitalMaintenanceWriter maintenanceWriter
    ) {
        this.knowledgeReader = knowledgeReader;
        this.inferenceReader = inferenceReader;
        this.requestWriter = requestWriter;
        this.equipmentWriter = equipmentWriter;
        this.equipmentLoanWriter = equipmentLoanWriter;
        this.maintenanceWriter = maintenanceWriter;
    }

    public List<UnitSummary> organizationUnits() {
        return knowledgeReader.organizationUnits();
    }

    public List<EquipmentSummary> equipment(EquipmentFilter filter) {
        EquipmentFilter normalizedFilter = normalize(filter);
        return knowledgeReader.equipment(normalizedFilter);
    }

    public List<EquipmentRequestSummary> equipmentRequests() {
        return inferenceReader.equipmentRequests();
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

    public void changeEquipmentStatus(String equipmentId, EquipmentStatusForm form) {
        requireText(equipmentId, "Equipment is required.");
        if (form == null) {
            throw new IllegalArgumentException("Equipment status is required.");
        }
        requireText(form.statusId(), "Equipment status is required.");
        equipmentWriter.changeEquipmentStatus(equipmentId.trim(), form.statusId().trim());
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
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String id = "EquipmentLoan-" + suffix;
        String loanNumber = "LOAN-" + suffix;
        EquipmentLoanForm cleanForm = new EquipmentLoanForm(
                form.equipmentId().trim(),
                form.loanedToUnitId().trim(),
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
        return knowledgeReader.maintenanceRecords();
    }

    public HospitalOverview hospitalOverview() {
        return knowledgeReader.hospitalOverview();
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
