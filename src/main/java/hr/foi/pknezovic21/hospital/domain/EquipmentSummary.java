package hr.foi.pknezovic21.hospital.domain;

public record EquipmentSummary(
        String id,
        String name,
        String assetNumber,
        String equipmentType,
        String status,
        String assignedUnitId,
        String assignedUnitName
) {
}
