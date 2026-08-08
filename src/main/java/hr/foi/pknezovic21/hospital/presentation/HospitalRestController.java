package hr.foi.pknezovic21.hospital.presentation;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentStatusForm;
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

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalidRequest(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
