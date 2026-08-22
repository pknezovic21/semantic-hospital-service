package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;
import hr.foi.pknezovic21.hospital.domain.PurchaseReceiptForm;

public interface HospitalPurchaseRequestWriter {

    void addPurchaseRequest(String id, String purchaseNumber, String createdAt, PurchaseRequestForm form);

    void receivePurchaseRequest(
            String purchaseRequestId,
            String equipmentId,
            String assetNumber,
            String receivedAt,
            PurchaseReceiptForm form
    );

    void cancelPurchaseRequest(String purchaseRequestId, String cancelledAt);
}
