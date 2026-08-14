package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRiskSummary;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalInferenceReader implements HospitalInferenceReader {

    private final Dataset dataset;
    private final JenaHospitalReasoner reasoner;
    private final String prefixes;

    public JenaHospitalInferenceReader(
            Dataset dataset,
            JenaHospitalReasoner reasoner,
            @Value("${hospital.rdf.base-uri}") String baseUri
    ) {
        this.dataset = dataset;
        this.reasoner = reasoner;
        this.prefixes = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                """.formatted(baseUri);
    }

    @Override
    public List<UnitSummary> organizationUnits() {
        String query = prefixes + """

                SELECT ?unit ?name ?type ?parent ?equipmentShortage
                WHERE {
                  ?unit hospital:name ?name .
                  FILTER EXISTS {
                    ?unit rdf:type ?unitType .
                    FILTER (?unitType IN (hospital:Hospital, hospital:ClinicalDivision, hospital:Department))
                  }
                  BIND(IF(EXISTS { ?unit rdf:type hospital:Department },
                          hospital:Department,
                          IF(EXISTS { ?unit rdf:type hospital:ClinicalDivision },
                             hospital:ClinicalDivision,
                             hospital:Hospital)) AS ?type)
                  OPTIONAL { ?unit hospital:partOf ?parent . }
                  BIND(EXISTS { ?unit rdf:type hospital:EquipmentShortageUnit } AS ?equipmentShortage)
                }
                ORDER BY ?name
                """;
        return Txn.calculateRead(dataset, () -> {
            List<UnitSummary> units = new ArrayList<>();
            Model model = reasoner.create(dataset.getDefaultModel());
            try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    units.add(new UnitSummary(
                            localName(row.getResource("unit")),
                            literal(row, "name"),
                            localName(row.getResource("type")),
                            optionalLocalName(row, "parent"),
                            bool(row, "equipmentShortage")
                    ));
                }
            }
            return units;
        });
    }

    @Override
    public List<EquipmentRequestSummary> equipmentRequests() {
        String query = prefixes + """

                SELECT ?request ?requestNumber ?unit ?unitName ?type ?typeName ?candidate ?candidateName
                       ?purchaseNeeded ?highPriority
                WHERE {
                  ?request rdf:type hospital:EquipmentRequest ;
                           hospital:requestNumber ?requestNumber ;
                           hospital:requestedFor ?unit ;
                           hospital:requestsType ?type .
                  ?unit hospital:name ?unitName .
                  ?type hospital:name ?typeName .
                  OPTIONAL {
                    ?request hospital:availableCandidate ?candidate .
                    ?candidate hospital:name ?candidateName .
                  }
                  BIND(EXISTS { ?request rdf:type hospital:PurchaseNeededRequest } AS ?purchaseNeeded)
                  BIND(EXISTS { ?request rdf:type hospital:HighPriorityRequest } AS ?highPriority)
                }
                ORDER BY ?requestNumber ?candidateName
                """;
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentRequestSummary> requests = new ArrayList<>();
            Model model = reasoner.create(dataset.getDefaultModel());
            try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    requests.add(new EquipmentRequestSummary(
                            localName(row.getResource("request")),
                            literal(row, "requestNumber"),
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
                            localName(row.getResource("type")),
                            literal(row, "typeName"),
                            optionalLocalName(row, "candidate"),
                            literal(row, "candidateName"),
                            bool(row, "purchaseNeeded"),
                            bool(row, "highPriority")
                    ));
                }
            }
            return requests;
        });
    }

    @Override
    public List<MaintenanceRecordSummary> maintenanceRecords() {
        String query = prefixes + """

                SELECT ?record ?maintenanceNumber ?equipment ?equipmentName ?assetNumber ?reason ?reportedAt ?highPriority
                WHERE {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceNumber ?maintenanceNumber ;
                          hospital:maintenanceFor ?equipment ;
                          hospital:maintenanceReason ?reason ;
                          hospital:reportedAt ?reportedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                  BIND(EXISTS { ?record rdf:type hospital:HighPriorityMaintenanceRecord } AS ?highPriority)
                }
                ORDER BY DESC(?reportedAt)
                """;
        return Txn.calculateRead(dataset, () -> {
            List<MaintenanceRecordSummary> records = new ArrayList<>();
            Model model = reasoner.create(dataset.getDefaultModel());
            try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    records.add(new MaintenanceRecordSummary(
                            localName(row.getResource("record")),
                            literal(row, "maintenanceNumber"),
                            localName(row.getResource("equipment")),
                            literal(row, "equipmentName"),
                            literal(row, "assetNumber"),
                            literal(row, "reason"),
                            literal(row, "reportedAt"),
                            bool(row, "highPriority")
                    ));
                }
            }
            return records;
        });
    }

    @Override
    public List<EquipmentRiskSummary> highRiskEquipment() {
        String query = prefixes + """

                SELECT ?equipment ?name ?assetNumber ?typeName ?unit ?unitName (COUNT(DISTINCT ?record) AS ?maintenanceRecordCount)
                WHERE {
                  ?equipment rdf:type hospital:HighRiskEquipment ;
                             hospital:name ?name ;
                             hospital:assetNumber ?assetNumber ;
                             hospital:hasEquipmentType ?type ;
                             hospital:assignedTo ?unit .
                  ?type hospital:name ?typeName .
                  ?unit hospital:name ?unitName .
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceFor ?equipment .
                }
                GROUP BY ?equipment ?name ?assetNumber ?typeName ?unit ?unitName
                ORDER BY DESC(?maintenanceRecordCount) ?assetNumber
                """;
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentRiskSummary> equipment = new ArrayList<>();
            Model model = reasoner.create(dataset.getDefaultModel());
            try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    equipment.add(new EquipmentRiskSummary(
                            localName(row.getResource("equipment")),
                            literal(row, "name"),
                            literal(row, "assetNumber"),
                            literal(row, "typeName"),
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
                            row.getLiteral("maintenanceRecordCount").getLong()
                    ));
                }
            }
            return equipment;
        });
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

    private boolean bool(QuerySolution row, String variable) {
        return row.getLiteral(variable).getBoolean();
    }
}
