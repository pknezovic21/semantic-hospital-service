package hr.foi.pknezovic21.hospital.domain;

public record EquipmentRequestForm(
        String requestedForUnitId,
        String requestedTypeId
) {
}
