package hr.foi.pknezovic21.hospital.domain;

import java.util.List;

public record EquipmentManagementSupplier(
        String id,
        String name,
        String type,
        String supplierCode,
        List<String> supportedTypeIds
) {
}
