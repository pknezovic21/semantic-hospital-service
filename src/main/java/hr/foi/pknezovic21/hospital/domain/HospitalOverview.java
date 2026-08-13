package hr.foi.pknezovic21.hospital.domain;

public record HospitalOverview(
        long organizationUnitCount,
        long equipmentCount,
        long availableEquipmentCount,
        long inMaintenanceEquipmentCount,
        long loanedEquipmentCount,
        long requestCount,
        long purchaseRequestCount,
        long loanCount,
        long activeLoanCount,
        long maintenanceRecordCount
) {
}
