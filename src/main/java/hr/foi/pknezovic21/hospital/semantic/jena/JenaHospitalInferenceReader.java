package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestSummary;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalInferenceReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Literal;
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
    public List<EquipmentRequestSummary> equipmentRequests() {
        String query = prefixes + """

                SELECT ?request ?requestNumber ?unit ?unitName ?type ?typeName ?candidate ?candidateName
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
                }
                ORDER BY ?requestNumber ?candidateName
                """;
        return Txn.calculateRead(dataset, () -> {
            List<EquipmentRequestSummary> requests = new ArrayList<>();
            InfModel model = reasoner.create(dataset.getDefaultModel());
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
                            literal(row, "candidateName")
                    ));
                }
            }
            return requests;
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
}
