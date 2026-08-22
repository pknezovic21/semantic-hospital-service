package hr.foi.pknezovic21.hospital.domain;

public record EquipmentLoanForm(
        String equipmentId,
        String loanedToUnitId,
        String loanedToLocationId,
        String requestId
) {
}
