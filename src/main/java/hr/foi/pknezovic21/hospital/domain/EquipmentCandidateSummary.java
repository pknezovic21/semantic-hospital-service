package hr.foi.pknezovic21.hospital.domain;

public record EquipmentCandidateSummary(
        String equipmentId,
        String equipmentName,
        String assetNumber,
        String assignedUnitId,
        String assignedUnitName,
        String locationId,
        String locationName
) {
}
