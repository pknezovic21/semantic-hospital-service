package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalRequestWriter;
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
public class JenaHospitalRequestWriter implements HospitalRequestWriter {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalRequestWriter(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void addEquipmentRequest(String id, String requestNumber, EquipmentRequestForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource request = model.createResource(uri(id));
            Resource requestedFor = resource(form.requestedForUnitId());
            Resource requestedType = resource(form.requestedTypeId());

            requireUnit(model, requestedFor);
            requireType(model, requestedType, "EquipmentType");

            request.addProperty(RDF.type, resource("EquipmentRequest"))
                    .addLiteral(property("name"), requestNumber)
                    .addLiteral(property("requestNumber"), requestNumber)
                    .addProperty(property("requestedFor"), requestedFor)
                    .addProperty(property("requestsType"), requestedType);
        });
    }

    private void requireUnit(Model model, Resource resource) {
        if (!model.contains(resource, RDF.type, resource("Hospital"))
                && !model.contains(resource, RDF.type, resource("ClinicalDivision"))
                && !model.contains(resource, RDF.type, resource("Department"))) {
            throw new IllegalArgumentException("Requested unit was not found.");
        }
    }

    private void requireType(Model model, Resource resource, String type) {
        if (!model.contains(resource, RDF.type, resource(type))) {
            throw new IllegalArgumentException("Requested equipment type was not found.");
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
