package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentDetail;
import hr.foi.pknezovic21.hospital.domain.EquipmentCandidateSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentRiskSummary;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.UnitSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                """.formatted(baseUri);
    }

    @Override
    public List<UnitSummary> organizationUnits() {
        String query = prefixes + """

                SELECT ?unit ?name ?type ?parent ?equipmentShortage
                WHERE {
                  ?unit hospital:name ?name ;
                        rdf:type ?type .
                  ?type rdfs:subClassOf* hospital:OrganizationComponent .
                  FILTER (?type NOT IN (hospital:OrganizationComponent, hospital:Unit, hospital:EquipmentShortageUnit))
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

                SELECT ?request ?requestNumber ?status ?unit ?unitName ?type ?typeName ?reason ?requestedAt ?cancelledAt
                       ?candidate ?candidateName ?candidateAssetNumber ?candidateUnit ?candidateUnitName
                       ?candidateLocation ?candidateLocationName ?purchaseNeeded ?highPriority
                WHERE {
                  ?request rdf:type hospital:EquipmentRequest ;
                           hospital:requestNumber ?requestNumber ;
                           hospital:hasRequestStatus ?status ;
                           hospital:requestedFor ?unit ;
                           hospital:requestsType ?type .
                  ?unit hospital:name ?unitName .
                  ?type hospital:name ?typeName .
                  OPTIONAL { ?request hospital:requestReason ?reason . }
                  OPTIONAL { ?request hospital:requestedAt ?requestedAt . }
                  OPTIONAL { ?request hospital:cancelledAt ?cancelledAt . }
                  OPTIONAL {
                    ?request hospital:availableCandidate ?candidate .
                    ?candidate hospital:name ?candidateName ;
                               hospital:assetNumber ?candidateAssetNumber ;
                               hospital:assignedTo ?candidateUnit .
                    ?candidateUnit hospital:name ?candidateUnitName .
                    OPTIONAL {
                      ?candidate hospital:locatedIn ?candidateLocation .
                      ?candidateLocation hospital:name ?candidateLocationName .
                    }
                  }
                  BIND(EXISTS { ?request rdf:type hospital:PurchaseNeededRequest } AS ?purchaseNeeded)
                  BIND(EXISTS { ?request rdf:type hospital:HighPriorityRequest } AS ?highPriority)
                }
                ORDER BY DESC(?requestedAt) ?requestNumber ?candidateName
                """;
        return Txn.calculateRead(dataset, () -> {
            Map<String, EquipmentRequestSummary> requests = new LinkedHashMap<>();
            Model model = reasoner.create(dataset.getDefaultModel());
            try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    String requestId = localName(row.getResource("request"));
                    EquipmentRequestSummary request = requests.computeIfAbsent(requestId, ignored ->
                            new EquipmentRequestSummary(
                                    requestId,
                                    literal(row, "requestNumber"),
                                    localName(row.getResource("status")),
                                    localName(row.getResource("unit")),
                                    literal(row, "unitName"),
                                    localName(row.getResource("type")),
                                    literal(row, "typeName"),
                                    literal(row, "reason"),
                                    literal(row, "requestedAt"),
                                    literal(row, "cancelledAt"),
                                    new ArrayList<>(),
                                    bool(row, "purchaseNeeded"),
                                    bool(row, "highPriority")
                            ));
                    if (row.contains("candidate")) {
                        request.candidates().add(new EquipmentCandidateSummary(
                                localName(row.getResource("candidate")),
                                literal(row, "candidateName"),
                                literal(row, "candidateAssetNumber"),
                                localName(row.getResource("candidateUnit")),
                                literal(row, "candidateUnitName"),
                                optionalLocalName(row, "candidateLocation"),
                                literal(row, "candidateLocationName")
                        ));
                    }
                }
            }
            return requests.values().stream()
                    .map(request -> new EquipmentRequestSummary(
                            request.id(),
                            request.requestNumber(),
                            request.status(),
                            request.requestedForUnitId(),
                            request.requestedForUnitName(),
                            request.requestedTypeId(),
                            request.requestedTypeName(),
                            request.reason(),
                            request.requestedAt(),
                            request.cancelledAt(),
                            List.copyOf(request.candidates()),
                            request.purchaseNeeded(),
                            request.highPriority()
                    ))
                    .toList();
        });
    }

    @Override
    public List<MaintenanceRecordSummary> maintenanceRecords() {
        String query = prefixes + """

                SELECT ?record ?maintenanceNumber ?equipment ?equipmentName ?assetNumber
                       ?reportedFor ?reportedForName ?reason ?reportedAt ?completedAt ?highPriority
                WHERE {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceNumber ?maintenanceNumber ;
                          hospital:maintenanceFor ?equipment ;
                          hospital:maintenanceReportedFor ?reportedFor ;
                          hospital:maintenanceReason ?reason ;
                          hospital:reportedAt ?reportedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                  ?reportedFor hospital:name ?reportedForName .
                  OPTIONAL { ?record hospital:completedAt ?completedAt . }
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
                            localName(row.getResource("reportedFor")),
                            literal(row, "reportedForName"),
                            literal(row, "reason"),
                            literal(row, "reportedAt"),
                            literal(row, "completedAt"),
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
                        ?equipment hospital:providedBy ?supplier .
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

                SELECT ?record ?maintenanceNumber ?equipment ?equipmentName ?assetNumber
                       ?reportedFor ?reportedForName ?reason ?reportedAt ?completedAt ?highPriority
                WHERE {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceNumber ?maintenanceNumber ;
                          hospital:maintenanceFor ?equipment ;
                          hospital:maintenanceReportedFor ?reportedFor ;
                          hospital:maintenanceReason ?reason ;
                          hospital:reportedAt ?reportedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                  ?reportedFor hospital:name ?reportedForName .
                  OPTIONAL { ?record hospital:completedAt ?completedAt . }
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
                        localName(row.getResource("reportedFor")),
                        literal(row, "reportedForName"),
                        literal(row, "reason"),
                        literal(row, "reportedAt"),
                        literal(row, "completedAt"),
                        bool(row, "highPriority")
                ));
            }
        }
        return records;
    }

    private List<EquipmentLoanSummary> equipmentLoanHistory(Model model, String equipmentId) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(prefixes + """

                SELECT ?loan ?loanNumber ?equipment ?equipmentName ?assetNumber
                       ?fromUnit ?fromUnitName ?fromLocation ?fromLocationName
                       ?toUnit ?toUnitName ?toLocation ?toLocationName ?request ?loanedAt ?returnedAt
                WHERE {
                  ?loan rdf:type hospital:EquipmentLoan ;
                        hospital:loanNumber ?loanNumber ;
                        hospital:loanedEquipment ?equipment ;
                        hospital:loanedFrom ?fromUnit ;
                        hospital:loanedTo ?toUnit ;
                        hospital:loanedAt ?loanedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                  ?fromUnit hospital:name ?fromUnitName .
                  ?toUnit hospital:name ?toUnitName .
                  OPTIONAL {
                    ?loan hospital:loanedFromLocation ?fromLocation .
                    ?fromLocation hospital:name ?fromLocationName .
                  }
                  OPTIONAL {
                    ?loan hospital:loanedToLocation ?toLocation .
                    ?toLocation hospital:name ?toLocationName .
                  }
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
                        localName(row.getResource("fromUnit")),
                        literal(row, "fromUnitName"),
                        optionalLocalName(row, "fromLocation"),
                        literal(row, "fromLocationName"),
                        localName(row.getResource("toUnit")),
                        literal(row, "toUnitName"),
                        optionalLocalName(row, "toLocation"),
                        literal(row, "toLocationName"),
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
