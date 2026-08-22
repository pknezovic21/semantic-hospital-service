package hr.foi.pknezovic21.hospital.domain;

public record PurchaseRequestSummary(
        String id,
        String purchaseNumber,
        String status,
        String equipmentRequestId,
        String requestedForUnitId,
        String requestedForUnitName,
        String requestedTypeId,
        String requestedTypeName,
        String supplierId,
        String supplierName,
        String reason,
        String createdAt,
        String receivedEquipmentId,
        String receivedEquipmentName,
        String receivedAssetNumber,
        String receivedAt,
        String cancelledAt
) {
}
