package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentFilter;
import hr.foi.pknezovic21.hospital.domain.EquipmentLoanSummary;
import hr.foi.pknezovic21.hospital.domain.EquipmentSummary;
import hr.foi.pknezovic21.hospital.domain.HospitalOverview;
import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordSummary;
import hr.foi.pknezovic21.hospital.domain.PurchaseRequestSummary;
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
                            optionalLocalName(row, "parent"),
                            false
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

    @Override
    public List<EquipmentLoanSummary> equipmentLoans() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

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
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
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
    public List<MaintenanceRecordSummary> maintenanceRecords() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT ?record ?maintenanceNumber ?equipment ?equipmentName ?assetNumber ?reason ?reportedAt
                WHERE {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceNumber ?maintenanceNumber ;
                          hospital:maintenanceFor ?equipment ;
                          hospital:maintenanceReason ?reason ;
                          hospital:reportedAt ?reportedAt .
                  ?equipment hospital:name ?equipmentName ;
                             hospital:assetNumber ?assetNumber .
                }
                ORDER BY DESC(?reportedAt)
                """.formatted(baseUri);
        return Txn.calculateRead(dataset, () -> {
            List<MaintenanceRecordSummary> records = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().dataset(dataset).query(query).build()) {
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
                            false
                    ));
                }
            }
            return records;
        });
    }

    @Override
    public List<PurchaseRequestSummary> purchaseRequests() {
        String query = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                SELECT ?purchaseRequest ?purchaseNumber ?equipmentRequest ?unit ?unitName ?type ?typeName ?reason ?createdAt
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
                            localName(row.getResource("equipmentRequest")),
                            localName(row.getResource("unit")),
                            literal(row, "unitName"),
                            localName(row.getResource("type")),
                            literal(row, "typeName"),
                            literal(row, "reason"),
                            literal(row, "createdAt")
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

                SELECT ?organizationUnitCount ?equipmentCount ?availableEquipmentCount
                       ?inMaintenanceEquipmentCount ?loanedEquipmentCount ?requestCount
                       ?purchaseRequestCount ?loanCount ?activeLoanCount ?maintenanceRecordCount
                WHERE {
                  {
                    SELECT (COUNT(DISTINCT ?unit) AS ?organizationUnitCount)
                    WHERE {
                      ?unit rdf:type ?type .
                      FILTER (?type IN (hospital:Hospital, hospital:ClinicalDivision, hospital:Department))
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
                        number(row, "purchaseRequestCount"),
                        number(row, "loanCount"),
                        number(row, "activeLoanCount"),
                        number(row, "maintenanceRecordCount")
                );
            }
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

    private long number(QuerySolution row, String variable) {
        return row.getLiteral(variable).getLong();
    }

    private String uri(String name) {
        return baseUri + name;
    }
}
