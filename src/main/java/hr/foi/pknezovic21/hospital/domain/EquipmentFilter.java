package hr.foi.pknezovic21.hospital.domain;

public record EquipmentFilter(
        String statusId,
        String typeId,
        String unitId,
        String search
) {
}
