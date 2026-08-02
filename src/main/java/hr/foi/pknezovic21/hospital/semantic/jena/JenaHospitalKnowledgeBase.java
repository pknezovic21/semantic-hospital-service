package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalDataBuilder;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeBase;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalOntologyBuilder;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalKnowledgeBase implements HospitalKnowledgeBase {

    private final HospitalOntologyBuilder ontologyBuilder;
    private final HospitalDataBuilder dataBuilder;

    public JenaHospitalKnowledgeBase(
            HospitalOntologyBuilder ontologyBuilder,
            HospitalDataBuilder dataBuilder
    ) {
        this.ontologyBuilder = ontologyBuilder;
        this.dataBuilder = dataBuilder;
    }

    @Override
    public void initialize() {
        ontologyBuilder.build();
        dataBuilder.build();
    }
}
