package hr.foi.pknezovic21.hospital.domain;

public record EquipmentManagementContract(
        String id,
        String name,
        String type,
        String contractNumber,
        String supplierId,
        String supplierName
) {
}
