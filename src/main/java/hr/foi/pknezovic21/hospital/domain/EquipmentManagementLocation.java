package hr.foi.pknezovic21.hospital.domain;

public record EquipmentManagementLocation(
        String id,
        String name,
        String type,
        String locationCode,
        String servesUnitId,
        String servesUnitName
) {
}
