package hr.foi.pknezovic21.hospital.domain;

public record EquipmentSummary(
        String id,
        String name,
        String assetNumber,
        String equipmentTypeId,
        String equipmentTypeName,
        String status,
        String assignedUnitId,
        String assignedUnitName,
        String locationId,
        String locationName,
        String categoryId,
        String categoryName
) {
}
