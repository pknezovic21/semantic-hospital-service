package hr.foi.pknezovic21.hospital.domain;

public record MaintenanceRecordSummary(
        String id,
        String maintenanceNumber,
        String equipmentId,
        String equipmentName,
        String assetNumber,
        String reportedForUnitId,
        String reportedForUnitName,
        String reason,
        String reportedAt,
        String completedAt,
        boolean highPriority
) {
}
