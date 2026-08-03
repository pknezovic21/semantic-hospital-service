package hr.foi.pknezovic21.hospital.domain;

public record EquipmentSummary(
        String id,
        String name,
        String assetNumber,
        String status,
        String assignedUnitId,
        String assignedUnitName
) {
}
