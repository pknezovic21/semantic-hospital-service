package hr.foi.pknezovic21.hospital.presentation;

import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.service.HospitalService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
    public List<EquipmentSummary> equipment() {
        return hospitalService.equipment();
    }
}
