package hr.foi.pknezovic21.hospital.presentation;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentStatusForm;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.service.HospitalService;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HospitalRestController {

    private final HospitalService hospitalService;

    public HospitalRestController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping("/organization")
    public List<UnitSummary> organization() {
        return hospitalService.organizationUnits();
    }

    @GetMapping("/equipment")
    public List<EquipmentSummary> equipment(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, name = "type") String typeId,
            @RequestParam(required = false, name = "unit") String unitId
    ) {
        return hospitalService.equipment(new EquipmentFilter(status, typeId, unitId));
    }

    @PatchMapping("/equipment/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEquipmentStatus(@PathVariable String id, @RequestBody EquipmentStatusForm form) {
        hospitalService.changeEquipmentStatus(id, form);
    }

    @GetMapping("/requests")
    public List<EquipmentRequestSummary> requests() {
        return hospitalService.equipmentRequests();
    }

    @PostMapping("/requests")
    public ResponseEntity<CreatedResourceResponse> createRequest(@RequestBody EquipmentRequestForm form) {
        String id = hospitalService.createEquipmentRequest(form);
        return ResponseEntity.created(URI.create("/api/requests/" + id)).body(new CreatedResourceResponse(id));
    }

    @GetMapping("/loans")
    public List<EquipmentLoanSummary> equipmentLoans() {
        return hospitalService.equipmentLoans();
    }

    @PostMapping("/loans")
    public ResponseEntity<CreatedResourceResponse> createEquipmentLoan(@RequestBody EquipmentLoanForm form) {
        String id = hospitalService.createEquipmentLoan(form);
        return ResponseEntity.created(URI.create("/api/loans/" + id)).body(new CreatedResourceResponse(id));
    }

    @PatchMapping("/loans/{id}/return")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void returnEquipmentLoan(@PathVariable String id) {
        hospitalService.returnEquipmentLoan(id);
    }

    @GetMapping("/maintenance")
    public List<MaintenanceRecordSummary> maintenanceRecords() {
        return hospitalService.maintenanceRecords();
    }

    @PostMapping("/maintenance")
    public ResponseEntity<CreatedResourceResponse> createMaintenanceRecord(@RequestBody MaintenanceRecordForm form) {
        String id = hospitalService.createMaintenanceRecord(form);
        return ResponseEntity.created(URI.create("/api/maintenance/" + id)).body(new CreatedResourceResponse(id));
    }

    @GetMapping("/hospital/overview")
    public HospitalOverview hospitalOverview() {
        return hospitalService.hospitalOverview();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
