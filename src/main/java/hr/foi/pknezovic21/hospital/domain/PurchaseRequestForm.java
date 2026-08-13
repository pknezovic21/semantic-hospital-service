package hr.foi.pknezovic21.hospital.domain;

public record PurchaseRequestForm(
        String equipmentRequestId,
        String reason
) {
}
