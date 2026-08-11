package hr.foi.pknezovic21.hospital.domain;

public record EquipmentLoanSummary(
        String id,
        String loanNumber,
        String equipmentId,
        String equipmentName,
        String assetNumber,
        String loanedToUnitId,
        String loanedToUnitName,
        String requestId,
        String loanedAt
) {
}
