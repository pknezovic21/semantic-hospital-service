package hr.foi.pknezovic21.hospital.service;

import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeReader;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HospitalService {

    private final HospitalKnowledgeReader knowledgeReader;

    public HospitalService(HospitalKnowledgeReader knowledgeReader) {
        this.knowledgeReader = knowledgeReader;
    }

    public List<UnitSummary> organizationUnits() {
        return knowledgeReader.organizationUnits();
    }

    public List<EquipmentSummary> equipment() {
        return knowledgeReader.equipment();
    }
}
