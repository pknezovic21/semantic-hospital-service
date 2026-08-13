package hr.foi.pknezovic21.hospital.domain;

public record PurchaseRequestSummary(
        String id,
        String purchaseNumber,
        String equipmentRequestId,
        String requestedForUnitId,
        String requestedForUnitName,
        String requestedTypeId,
        String requestedTypeName,
        String reason,
        String createdAt
) {
}
