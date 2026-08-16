package hr.foi.pknezovic21.hospital.domain;

import java.util.List;

public record EquipmentRequestSummary(
        String id,
        String requestNumber,
        String status,
        String requestedForUnitId,
        String requestedForUnitName,
        String requestedTypeId,
        String requestedTypeName,
        List<EquipmentCandidateSummary> candidates,
        boolean purchaseNeeded,
        boolean highPriority
) {
}
