package hr.foi.pknezovic21.hospital.domain;

import java.util.List;

public record EquipmentReport(
        List<EquipmentReportItem> byStatus,
        List<EquipmentReportItem> byType,
        List<EquipmentReportItem> byCategory,
        List<EquipmentReportItem> byUnit
) {
}
