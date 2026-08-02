package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.SparqlExecutor;
import hr.foi.pknezovic21.hospital.semantic.api.SparqlRow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.system.Txn;
import org.springframework.stereotype.Component;

@Component
public class JenaSparqlExecutor implements SparqlExecutor {

    private final Dataset dataset;

    public JenaSparqlExecutor(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public List<SparqlRow> select(String sparql) {
        Query query = QueryFactory.create(sparql);
        return Txn.calculateRead(dataset, () -> {
            List<SparqlRow> rows = new ArrayList<>();
            try (QueryExecution execution = QueryExecution.create().query(query).dataset(dataset).build()) {
                ResultSet results = execution.execSelect();
                List<String> variables = results.getResultVars();
                while (results.hasNext()) {
                    QuerySolution solution = results.next();
                    Map<String, String> bindings = new LinkedHashMap<>();
                    for (String variable : variables) {
                        RDFNode node = solution.get(variable);
                        bindings.put(variable, node == null ? null : node.toString());
                    }
                    rows.add(new SparqlRow(bindings));
                }
            }
            return rows;
        });
    }

    @Override
    public boolean ask(String sparql) {
        Query query = QueryFactory.create(sparql);
        return Txn.calculateRead(dataset, () -> {
            try (QueryExecution execution = QueryExecution.create().query(query).dataset(dataset).build()) {
                return execution.execAsk();
            }
        });
    }
}
