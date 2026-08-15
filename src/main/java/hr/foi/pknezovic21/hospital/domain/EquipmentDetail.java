package hr.foi.pknezovic21.hospital.domain;

import java.util.List;

public record EquipmentDetail(
        String id,
        String name,
        String assetNumber,
        String equipmentTypeId,
        String equipmentTypeName,
        String categoryId,
        String categoryName,
        String status,
        String assignedUnitId,
        String assignedUnitName,
        String locationId,
        String locationName,
        String supplierId,
        String supplierName,
        String maintenanceContractId,
        String maintenanceContractName,
        String contractNumber,
        boolean highRisk,
        List<MaintenanceRecordSummary> maintenanceHistory,
        List<EquipmentLoanSummary> loanHistory
) {
}
