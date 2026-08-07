package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalKnowledgeReader implements HospitalKnowledgeReader {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalKnowledgeReader(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public List<UnitSummary> organizationUnits() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT ?unit ?name ?type ?parent
                WHERE {
                  ?unit hospital:name ?name .
                  ?unit rdf:type ?type .
                  FILTER (?type IN (hospital:Hospital, hospital:ClinicalDivision, hospital:Department))
                  OPTIONAL { ?unit hospital:partOf ?parent . }
                }
                ORDER BY ?name
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            List<UnitSummary> units = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    units.add(new UnitSummary(
                            localName(row.getResource("unit")),
                            literal(row, "name"),
                            localName(row.getResource("type")),
                            optionalLocalName(row, "parent")
                    ));
                }
            }
            return units;
        });
    }

    @Override
    public List<EquipmentSummary> equipment(EquipmentFilter filter) {
        String query = equipmentQuery(filter);
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentSummary> equipment = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    equipment.add(new EquipmentSummary(
                            localName(row.getResource("equipment")),
                            literal(row, "name"),
                            literal(row, "assetNumber"),
                            literal(row, "equipmentTypeName"),
                            localName(row.getResource("status")),
                            localName(row.getResource("unit")),
                            literal(row, "unitName")
                    ));
                }
            }
            return equipment;
        });
    }

    private String equipmentQuery(EquipmentFilter filter) {
        String filters = equipmentFilters(filter);
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT ?equipment ?name ?assetNumber ?equipmentTypeName ?status ?unit ?unitName
                WHERE {
                  ?equipment rdf:type hospital:Equipment ;
                             hospital:name ?name ;
                             hospital:assetNumber ?assetNumber ;
                             hospital:hasEquipmentType ?equipmentType ;
                             hospital:hasEquipmentStatus ?status ;
                             hospital:assignedTo ?unit .
                  ?equipmentType hospital:name ?equipmentTypeName .
                  ?unit hospital:name ?unitName .
                %s
                }
                ORDER BY ?assetNumber
                """.formatted(baseUri, filters));
        if (filter.statusId() != null) {
            query.setIri("statusFilter", uri(filter.statusId()));
        }
        if (filter.typeId() != null) {
            query.setIri("typeFilter", uri(filter.typeId()));
        }
        if (filter.unitId() != null) {
            query.setIri("unitFilter", uri(filter.unitId()));
        }
        return query.toString();
    }

    private String equipmentFilters(EquipmentFilter filter) {
        StringBuilder filters = new StringBuilder();
        if (filter.statusId() != null) {
            filters.append("  FILTER (?status = ?statusFilter)\n");
        }
        if (filter.typeId() != null) {
            filters.append("  FILTER (?equipmentType = ?typeFilter)\n");
        }
        if (filter.unitId() != null) {
            filters.append("  FILTER (?unit = ?unitFilter)\n");
        }
        return filters.toString();
    }

    private String optionalLocalName(QuerySolution row, String variable) {
        return row.contains(variable) ? localName(row.getResource(variable)) : null;
    }

    private String localName(Resource resource) {
        return resource.getLocalName();
    }

    private String literal(QuerySolution row, String variable) {
        Literal literal = row.getLiteral(variable);
        return literal == null ? null : literal.getString();
    }

    private String uri(String name) {
        return baseUri + name;
    }
}
