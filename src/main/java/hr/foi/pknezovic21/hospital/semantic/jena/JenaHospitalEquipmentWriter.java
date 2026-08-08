package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentWriter;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalEquipmentWriter implements HospitalEquipmentWriter {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalEquipmentWriter(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void changeEquipmentStatus(String equipmentId, String statusId) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource equipment = resource(equipmentId);
            Resource status = resource(statusId);
            Property hasEquipmentStatus = property("hasEquipmentStatus");

            requireType(model, equipment, "Equipment", "Equipment was not found.");
            requireType(model, status, "EquipmentStatus", "Equipment status was not found.");

            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, status);
        });
    }

    private void requireType(Model model, Resource resource, String type, String message) {
        if (!model.contains(resource, RDF.type, resource(type))) {
            throw new IllegalArgumentException(message);
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
