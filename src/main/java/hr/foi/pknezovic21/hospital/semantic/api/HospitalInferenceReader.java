package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import java.util.List;

public interface HospitalInferenceReader {

    List<EquipmentRequestSummary> equipmentRequests();
}
