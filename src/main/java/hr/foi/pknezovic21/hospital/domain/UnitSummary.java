package hr.foi.pknezovic21.hospital.domain;

public record UnitSummary(
        String id,
        String name,
        String type,
        String parentId
) {
}
