package hr.foi.pknezovic21.hospital.domain;

public record EquipmentRiskSummary(
        String id,
        String name,
        String assetNumber,
        String equipmentType,
        String assignedUnitId,
        String assignedUnitName,
        long maintenanceRecordCount
) {
}
