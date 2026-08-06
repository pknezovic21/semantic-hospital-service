package hr.foi.pknezovic21.hospital.domain;

public record EquipmentRequestSummary(
        String id,
        String requestNumber,
        String requestedForUnitId,
        String requestedForUnitName,
        String requestedTypeId,
        String requestedTypeName,
        String candidateEquipmentId,
        String candidateEquipmentName
) {
}
