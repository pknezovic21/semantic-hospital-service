package hr.foi.pknezovic21.hospital.semantic.api;

import java.util.List;

public interface SparqlExecutor {

    List<SparqlRow> select(String sparql);

    boolean ask(String sparql);
}
