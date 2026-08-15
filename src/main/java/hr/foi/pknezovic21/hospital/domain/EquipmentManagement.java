package hr.foi.pknezovic21.hospital.domain;

import java.util.List;

public record EquipmentManagement(
        List<EquipmentManagementLocation> locations,
        List<EquipmentManagementCategory> categories,
        List<EquipmentManagementOption> equipmentTypes,
        List<EquipmentManagementOption> equipmentStatuses,
        List<EquipmentManagementSupplier> suppliers,
        List<EquipmentManagementContract> maintenanceContracts
) {
}
