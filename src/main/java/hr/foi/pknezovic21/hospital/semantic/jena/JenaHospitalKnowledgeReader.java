package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagement;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagementCategory;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagementContract;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagementLocation;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagementOption;
import hr.foi.pknezovic21.hospital.domain.EquipmentManagementSupplier;
import hr.foi.pknezovic21.hospital.domain.EquipmentReport;
import hr.foi.pknezovic21.hospital.domain.EquipmentReportItem;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeReader;
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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.apache.lucene.queryparser.classic.QueryParserBase;
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
                            localName(row.getResource("equipmentType")),
                            literal(row, "equipmentTypeName"),
                            localName(row.getResource("status")),
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
                            optionalLocalName(row, "location"),
                            literal(row, "locationName"),
                            optionalLocalName(row, "category"),
                            literal(row, "categoryName")
                    ));
                }
            }
            return equipment;
        });
    }

    @Override
    public EquipmentManagement equipmentManagement() {
        return Txn.calculateRead(dataset, () -> new EquipmentManagement(
                equipmentLocations("""
                        PREFIX hospital: <%s>
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                        SELECT ?location ?name ?type ?locationCode ?unit ?unitName
                        WHERE {
                          ?location rdf:type ?type ;
                                    hospital:name ?name ;
                                    hospital:locationCode ?locationCode .
                          ?type rdfs:subClassOf* hospital:Location .
                          OPTIONAL {
                            ?location hospital:servesUnit ?unit .
                            ?unit hospital:name ?unitName .
                          }
                        }
                        ORDER BY ?name
                        """.formatted(baseUri)),
                equipmentCategories("""
                        PREFIX hospital: <%s>
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                        SELECT ?category ?name ?type ?categoryCode
                        WHERE {
                          ?category rdf:type ?type ;
                                    hospital:name ?name ;
                                    hospital:categoryCode ?categoryCode .
                          ?type rdfs:subClassOf* hospital:EquipmentCategory .
                        }
                        ORDER BY ?name
                        """.formatted(baseUri)),
                equipmentOptions(equipmentOptionQuery("EquipmentType")),
                equipmentOptions(equipmentOptionQuery("EquipmentStatus")),
                equipmentSuppliers("""
                        PREFIX hospital: <%s>
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                        SELECT ?supplier ?name ?type ?supplierCode ?equipmentType
                        WHERE {
                          ?supplier rdf:type ?type ;
                                    hospital:name ?name ;
                                    hospital:supplierCode ?supplierCode .
                          ?type rdfs:subClassOf* hospital:Supplier .
                          OPTIONAL { ?equipmentType hospital:suppliedBy ?supplier . }
                        }
                        ORDER BY ?name ?equipmentType
                        """.formatted(baseUri)),
                maintenanceContracts("""
                        PREFIX hospital: <%s>
                        PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                        SELECT ?contract ?name ?type ?contractNumber ?supplier ?supplierName
                        WHERE {
                          ?contract rdf:type ?type ;
                                    hospital:name ?name ;
                                    hospital:contractNumber ?contractNumber .
                          ?type rdfs:subClassOf* hospital:Contract .
                          OPTIONAL {
                            ?contract hospital:contractedSupplier ?supplier .
                            ?supplier hospital:name ?supplierName .
                          }
                        }
                        ORDER BY ?name
                        """.formatted(baseUri))
        ));
    }

    @Override
    public List<EquipmentLoanSummary> equipmentLoans() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

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
                }
                ORDER BY DESC(?loanedAt)
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentLoanSummary> loans = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
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
        });
    }

    @Override
    public List<PurchaseRequestSummary> purchaseRequests() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT ?purchaseRequest ?purchaseNumber ?status ?equipmentRequest ?unit ?unitName ?type ?typeName
                       ?supplier ?supplierName ?reason ?createdAt ?receivedEquipment ?receivedEquipmentName
                       ?receivedAssetNumber ?receivedAt ?cancelledAt
                WHERE {
                  ?purchaseRequest rdf:type hospital:PurchaseRequest ;
                                   hospital:purchaseNumber ?purchaseNumber ;
                                   hospital:purchaseForRequest ?equipmentRequest ;
                                   hospital:purchaseRequestedFor ?unit ;
                                   hospital:purchaseRequestsType ?type ;
                                   hospital:purchaseReason ?reason ;
                                   hospital:createdAt ?createdAt .
                  ?unit hospital:name ?unitName .
                  ?type hospital:name ?typeName .
                  OPTIONAL {
                    ?purchaseRequest hospital:selectedSupplier ?supplier .
                    ?supplier hospital:name ?supplierName .
                  }
                  OPTIONAL {
                    ?purchaseRequest hospital:receivedEquipment ?receivedEquipment ;
                                     hospital:receivedAt ?receivedAt .
                    ?receivedEquipment hospital:name ?receivedEquipmentName ;
                                       hospital:assetNumber ?receivedAssetNumber .
                  }
                  OPTIONAL { ?purchaseRequest hospital:cancelledAt ?cancelledAt . }
                  BIND(
                    IF(BOUND(?receivedAt), "Received", IF(BOUND(?cancelledAt), "Cancelled", "Pending"))
                    AS ?status
                  )
                }
                ORDER BY DESC(?createdAt)
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            List<PurchaseRequestSummary> requests = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    requests.add(new PurchaseRequestSummary(
                            localName(row.getResource("purchaseRequest")),
                            literal(row, "purchaseNumber"),
                            literal(row, "status"),
                            localName(row.getResource("equipmentRequest")),
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
                            localName(row.getResource("type")),
                            literal(row, "typeName"),
                            optionalLocalName(row, "supplier"),
                            literal(row, "supplierName"),
                            literal(row, "reason"),
                            literal(row, "createdAt"),
                            optionalLocalName(row, "receivedEquipment"),
                            literal(row, "receivedEquipmentName"),
                            literal(row, "receivedAssetNumber"),
                            literal(row, "receivedAt"),
                            literal(row, "cancelledAt")
                    ));
                }
            }
            return requests;
        });
    }

    @Override
    public HospitalOverview hospitalOverview() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?organizationUnitCount ?equipmentCount ?availableEquipmentCount
                       ?inMaintenanceEquipmentCount ?loanedEquipmentCount ?requestCount
                       ?openRequestCount ?purchaseRequestCount ?loanCount ?activeLoanCount ?maintenanceRecordCount
                WHERE {
                  {
                    SELECT (COUNT(DISTINCT ?unit) AS ?organizationUnitCount)
                    WHERE {
                      ?unit rdf:type/rdfs:subClassOf* hospital:OrganizationComponent .
                    }
                  }
                  { SELECT (COUNT(DISTINCT ?equipment) AS ?equipmentCount) WHERE { ?equipment rdf:type hospital:Equipment . } }
                  {
                    SELECT (COUNT(DISTINCT ?equipment) AS ?availableEquipmentCount)
                    WHERE { ?equipment hospital:hasEquipmentStatus hospital:Available . }
                  }
                  {
                    SELECT (COUNT(DISTINCT ?equipment) AS ?inMaintenanceEquipmentCount)
                    WHERE { ?equipment hospital:hasEquipmentStatus hospital:InMaintenance . }
                  }
                  {
                    SELECT (COUNT(DISTINCT ?equipment) AS ?loanedEquipmentCount)
                    WHERE { ?equipment hospital:hasEquipmentStatus hospital:Loaned . }
                  }
                  { SELECT (COUNT(DISTINCT ?request) AS ?requestCount) WHERE { ?request rdf:type hospital:EquipmentRequest . } }
                  {
                    SELECT (COUNT(DISTINCT ?request) AS ?openRequestCount)
                    WHERE {
                      ?request rdf:type hospital:EquipmentRequest ;
                               hospital:hasRequestStatus hospital:Open .
                    }
                  }
                  {
                    SELECT (COUNT(DISTINCT ?purchaseRequest) AS ?purchaseRequestCount)
                    WHERE { ?purchaseRequest rdf:type hospital:PurchaseRequest . }
                  }
                  { SELECT (COUNT(DISTINCT ?loan) AS ?loanCount) WHERE { ?loan rdf:type hospital:EquipmentLoan . } }
                  {
                    SELECT (COUNT(DISTINCT ?loan) AS ?activeLoanCount)
                    WHERE {
                      ?loan rdf:type hospital:EquipmentLoan .
                      FILTER NOT EXISTS { ?loan hospital:returnedAt ?returnedAt . }
                    }
                  }
                  {
                    SELECT (COUNT(DISTINCT ?record) AS ?maintenanceRecordCount)
                    WHERE { ?record rdf:type hospital:MaintenanceRecord . }
                  }
                }
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
                QuerySolution row = execution.execSelect().next();
                return new HospitalOverview(
                        number(row, "organizationUnitCount"),
                        number(row, "equipmentCount"),
                        number(row, "availableEquipmentCount"),
                        number(row, "inMaintenanceEquipmentCount"),
                        number(row, "loanedEquipmentCount"),
                        number(row, "requestCount"),
                        number(row, "openRequestCount"),
                        number(row, "purchaseRequestCount"),
                        number(row, "loanCount"),
                        number(row, "activeLoanCount"),
                        number(row, "maintenanceRecordCount")
                );
            }
        });
    }

    @Override
    public EquipmentReport equipmentReport() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?dimension ?item ?name (COUNT(DISTINCT ?equipment) AS ?count)
                WHERE {
                  ?equipment rdf:type hospital:Equipment .
                  {
                    VALUES (?dimension ?property) {
                      ("status" hospital:hasEquipmentStatus)
                      ("type" hospital:hasEquipmentType)
                      ("unit" hospital:assignedTo)
                    }
                    ?equipment ?property ?item .
                  }
                  UNION
                  {
                    BIND("category" AS ?dimension)
                    ?equipment hospital:hasEquipmentType/hospital:belongsToCategory ?item .
                  }
                  OPTIONAL { ?item hospital:name ?domainName . }
                  OPTIONAL { ?item rdfs:label ?label . }
                  BIND(COALESCE(?domainName, ?label, STRAFTER(STR(?item), "#")) AS ?name)
                }
                GROUP BY ?dimension ?item ?name
                ORDER BY ?dimension ?name
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentReportItem> byStatus = new ArrayList<>();
            List<EquipmentReportItem> byType = new ArrayList<>();
            List<EquipmentReportItem> byCategory = new ArrayList<>();
            List<EquipmentReportItem> byUnit = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
                ResultSet results = execution.execSelect();
                while (results.hasNext()) {
                    QuerySolution row = results.next();
                    EquipmentReportItem item = new EquipmentReportItem(
                            localName(row.getResource("item")),
                            literal(row, "name"),
                            number(row, "count")
                    );
                    switch (literal(row, "dimension")) {
                        case "status" -> byStatus.add(item);
                        case "type" -> byType.add(item);
                        case "category" -> byCategory.add(item);
                        case "unit" -> byUnit.add(item);
                        default -> throw new IllegalStateException("Unsupported equipment report dimension.");
                    }
                }
            }
            return new EquipmentReport(byStatus, byType, byCategory, byUnit);
        });
    }

    private String equipmentQuery(EquipmentFilter filter) {
        String filters = equipmentFilters(filter);
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX text: <http://jena.apache.org/text#>

                SELECT ?equipment ?name ?assetNumber ?equipmentType ?equipmentTypeName ?status ?unit ?unitName
                       ?location ?locationName ?category ?categoryName
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
                    ?equipment hospital:locatedIn ?location .
                    ?location hospital:name ?locationName .
                  }
                  OPTIONAL {
                    ?equipmentType hospital:belongsToCategory ?category .
                    ?category hospital:name ?categoryName .
                  }
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
        if (filter.search() != null) {
            query.setLiteral("searchFilter", QueryParserBase.escape(filter.search()));
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
        if (filter.search() != null) {
            filters.append("  ?equipment text:query (hospital:name ?searchFilter) .\n");
        }
        return filters.toString();
    }

    private List<EquipmentManagementLocation> equipmentLocations(String query) {
        List<EquipmentManagementLocation> items = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                items.add(new EquipmentManagementLocation(
                        localName(row.getResource("location")),
                        literal(row, "name"),
                        localName(row.getResource("type")),
                        literal(row, "locationCode"),
                        optionalLocalName(row, "unit"),
                        literal(row, "unitName")
                ));
            }
        }
        return items;
    }

    private List<EquipmentManagementCategory> equipmentCategories(String query) {
        List<EquipmentManagementCategory> items = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                items.add(new EquipmentManagementCategory(
                        localName(row.getResource("category")),
                        literal(row, "name"),
                        localName(row.getResource("type")),
                        literal(row, "categoryCode")
                ));
            }
        }
        return items;
    }

    private List<EquipmentManagementSupplier> equipmentSuppliers(String query) {
        Map<String, EquipmentManagementSupplier> items = new LinkedHashMap<>();
        try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                String supplierId = localName(row.getResource("supplier"));
                EquipmentManagementSupplier supplier = items.computeIfAbsent(supplierId, ignored -> new EquipmentManagementSupplier(
                        supplierId,
                        literal(row, "name"),
                        localName(row.getResource("type")),
                        literal(row, "supplierCode"),
                        new ArrayList<>()
                ));
                if (row.contains("equipmentType")) {
                    supplier.supportedTypeIds().add(localName(row.getResource("equipmentType")));
                }
            }
        }
        return new ArrayList<>(items.values());
    }

    private List<EquipmentManagementOption> equipmentOptions(String query) {
        List<EquipmentManagementOption> items = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                items.add(new EquipmentManagementOption(
                        localName(row.getResource("option")),
                        literal(row, "name")
                ));
            }
        }
        return items;
    }

    private String equipmentOptionQuery(String optionClass) {
        return """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?option ?name
                WHERE {
                  ?option rdf:type ?type .
                  ?type rdfs:subClassOf* hospital:%s .
                  OPTIONAL { ?option hospital:name ?domainName . }
                  OPTIONAL { ?option rdfs:label ?label . }
                  BIND(COALESCE(?domainName, ?label) AS ?name)
                }
                ORDER BY ?name
                """.formatted(baseUri, optionClass);
    }

    private List<EquipmentManagementContract> maintenanceContracts(String query) {
        List<EquipmentManagementContract> items = new ArrayList<>();
        try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
            ResultSet results = execution.execSelect();
            while (results.hasNext()) {
                QuerySolution row = results.next();
                items.add(new EquipmentManagementContract(
                        localName(row.getResource("contract")),
                        literal(row, "name"),
                        localName(row.getResource("type")),
                        literal(row, "contractNumber"),
                        optionalLocalName(row, "supplier"),
                        literal(row, "supplierName")
                ));
            }
        }
        return items;
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

    private long number(QuerySolution row, String variable) {
        return row.getLiteral(variable).getLong();
    }

    private String uri(String name) {
        return baseUri + name;
    }
}
