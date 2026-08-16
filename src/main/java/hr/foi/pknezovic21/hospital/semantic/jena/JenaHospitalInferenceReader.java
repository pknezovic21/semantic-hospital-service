package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentDetail;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRiskSummary;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
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
    private final String baseUri;
    private final String prefixes;

    public JenaHospitalInferenceReader(
            Dataset dataset,
            JenaHospitalReasoner reasoner,
            @Value("${hospital.rdf.base-uri}") String baseUri
    ) {
        this.dataset = dataset;
        this.reasoner = reasoner;
        this.baseUri = baseUri;
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

                SELECT ?request ?requestNumber ?status ?unit ?unitName ?type ?typeName ?candidate ?candidateName
                       ?purchaseNeeded ?highPriority
                WHERE {
                  ?request rdf:type hospital:EquipmentRequest ;
                           hospital:requestNumber ?requestNumber ;
                           hospital:hasRequestStatus ?status ;
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
                            localName(row.getResource("status")),
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

    @Override
    public EquipmentDetail equipmentDetail(String equipmentId) {
        return Txn.calculateRead(dataset, () -> {
            Model model = reasoner.create(dataset.getDefaultModel());
            ParameterizedSparqlString query = new ParameterizedSparqlString(prefixes + """

                    SELECT ?equipment ?name ?assetNumber ?equipmentType ?equipmentTypeName ?category ?categoryName
                           ?status ?unit ?unitName ?location ?locationName ?supplier ?supplierName
                           ?contract ?contractName ?contractNumber ?highRisk
                    WHERE {
                      ?equipment rdf:type hospital:Equipment ;
                                 hospital:name ?name ;
                                 hospital:assetNumber ?assetNumber ;
                                 hospital:hasEquipmentType ?equipmentType ;
                                 hospital:hasEquipmentStatus ?status ;
                                 hospital:assignedTo ?unit .
                      ?equipmentType hospital:name ?equipmentTypeName .
                      ?unit hospital:name ?unitName .
                      OPTIONAL {
                        ?equipmentType hospital:belongsToCategory ?category .
                        ?category hospital:name ?categoryName .
                      }
                      OPTIONAL {
                        ?equipment hospital:locatedIn ?location .
                        ?location hospital:name ?locationName .
                      }
                      OPTIONAL {
                        ?equipmentType hospital:suppliedBy ?supplier .
                        ?supplier hospital:name ?supplierName .
                      }
                      OPTIONAL {
                        ?equipment hospital:coveredByContract ?contract .
                        ?contract hospital:name ?contractName ;
                                  hospital:contractNumber ?contractNumber .
                      }
                      BIND(EXISTS { ?equipment rdf:type hospital:HighRiskEquipment } AS ?highRisk)
                      FILTER (?equipment = ?equipmentFilter)
                    }
                    """);
            query.setIri("equipmentFilter", uri(equipmentId));
            try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
                ResultSet results = execution.execSelect();
                if (!results.hasNext()) {
                    return null;
                }
                QuerySolution row = results.next();
                return new EquipmentDetail(
                        localName(row.getResource("equipment")),
                        literal(row, "name"),
                        literal(row, "assetNumber"),
                        localName(row.getResource("equipmentType")),
                        literal(row, "equipmentTypeName"),
                        optionalLocalName(row, "category"),
                        literal(row, "categoryName"),
                        localName(row.getResource("status")),
                        localName(row.getResource("unit")),
                        literal(row, "unitName"),
                        optionalLocalName(row, "location"),
                        literal(row, "locationName"),
                        optionalLocalName(row, "supplier"),
                        literal(row, "supplierName"),
                        optionalLocalName(row, "contract"),
                        literal(row, "contractName"),
                        literal(row, "contractNumber"),
                        bool(row, "highRisk"),
                        equipmentMaintenanceHistory(model, equipmentId),
                        equipmentLoanHistory(model, equipmentId)
                );
            }
        });
    }

    private List<MaintenanceRecordSummary> equipmentMaintenanceHistory(Model model, String equipmentId) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(prefixes + """

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
                  FILTER (?equipment = ?equipmentFilter)
                }
                ORDER BY DESC(?reportedAt)
                """);
        query.setIri("equipmentFilter", uri(equipmentId));
        List<MaintenanceRecordSummary> records = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
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
    }

    private List<EquipmentLoanSummary> equipmentLoanHistory(Model model, String equipmentId) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(prefixes + """

                SELECT ?loan ?loanNumber ?equipment ?equipmentName ?assetNumber ?unit ?unitName ?request ?loanedAt ?returnedAt
                WHERE {
                  ?loan rdf:type hospital:EquipmentLoan ;
                        hospital:loanNumber ?loanNumber ;
                        hospital:loanedEquipment ?equipment ;
                        hospital:loanedTo ?unit ;
                        hospital:loanedAt ?loanedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                  ?unit hospital:name ?unitName .
                  OPTIONAL { ?loan hospital:loanedForRequest ?request . }
                  OPTIONAL { ?loan hospital:returnedAt ?returnedAt . }
                  FILTER (?equipment = ?equipmentFilter)
                }
                ORDER BY DESC(?loanedAt)
                """);
        query.setIri("equipmentFilter", uri(equipmentId));
        List<EquipmentLoanSummary> loans = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                loans.add(new EquipmentLoanSummary(
                        localName(row.getResource("loan")),
                        literal(row, "loanNumber"),
                        localName(row.getResource("equipment")),
                        literal(row, "equipmentName"),
                        literal(row, "assetNumber"),
                        localName(row.getResource("unit")),
                        literal(row, "unitName"),
                        optionalLocalName(row, "request"),
                        literal(row, "loanedAt"),
                        literal(row, "returnedAt")
                ));
            }
        }
        return loans;
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

    private String uri(String name) {
        return baseUri + name;
    }
}
