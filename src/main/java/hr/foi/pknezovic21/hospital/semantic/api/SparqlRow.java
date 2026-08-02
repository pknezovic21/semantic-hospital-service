package hr.foi.pknezovic21.hospital.semantic.api;

import java.util.Map;

public record SparqlRow(Map<String, String> values) {

    public String get(String variable) {
        return values.get(variable);
    }
}
