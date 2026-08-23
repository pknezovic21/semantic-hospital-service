package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentLocationForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentLocationWriter;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalEquipmentLocationWriter implements HospitalEquipmentLocationWriter {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalEquipmentLocationWriter(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void updateEquipmentLocation(String equipmentId, EquipmentLocationForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource equipment = resource(equipmentId);
            Resource location = resource(form.locationId());
            Property assignedTo = property("assignedTo");
            Property locatedIn = property("locatedIn");

            if (!model.contains(equipment, RDF.type, resource("Equipment"))) {
                throw new IllegalArgumentException("Equipment was not found.");
            }
            if (!model.contains(equipment, assignedTo)) {
                throw new IllegalArgumentException("Equipment assignment is incomplete.");
            }
            Resource unit = model.getProperty(equipment, assignedTo).getResource();
            requireLocationForUnit(model, location, unit);

            model.removeAll(equipment, locatedIn, null);
            model.add(equipment, locatedIn, location);
        });
    }

    private void requireLocationForUnit(Model model, Resource location, Resource unit) {
        Query query = new AskBuilder()
                .addPrefix("hospital", baseUri)
                .addWhere(location, "hospital:servesUnit/hospital:partOf*", unit)
                .build();
        try (QueryExecution execution = QueryExecution.model(model).query(query).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Location does not belong to the assigned unit.");
            }
        }
    }

    private Resource resource(String name) {
        return ResourceFactory.createResource(uri(name));
    }

    private Property property(String name) {
        return ResourceFactory.createProperty(uri(name));
    }

    private String uri(String name) {
        return baseUri + name;
    }
}
