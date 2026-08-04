package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalRdfExporter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.system.Txn;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalRdfExporter implements HospitalRdfExporter {

    private final Dataset dataset;

    public JenaHospitalRdfExporter(Dataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public String exportTurtle() {
        return Txn.calculateRead(dataset, () -> {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            RDFDataMgr.write(output, dataset.getDefaultModel(), RDFFormat.TURTLE_PRETTY);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        });
    }
}
