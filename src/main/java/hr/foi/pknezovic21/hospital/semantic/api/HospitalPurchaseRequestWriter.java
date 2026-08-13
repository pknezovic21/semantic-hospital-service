package hr.foi.pknezovic21.hospital.semantic.api;

import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;

public interface HospitalPurchaseRequestWriter {

    void addPurchaseRequest(String id, String purchaseNumber, String createdAt, PurchaseRequestForm form);
}
