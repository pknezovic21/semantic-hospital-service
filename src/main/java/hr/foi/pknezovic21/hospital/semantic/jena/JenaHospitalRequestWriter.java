package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentRequestForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalRequestWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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
    public void addEquipmentRequest(String id, String requestNumber, String requestedAt, EquipmentRequestForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource request = model.createResource(uri(id));
            Resource requestedFor = resource(form.requestedForUnitId());
            Resource requestedType = resource(form.requestedTypeId());

            requireDepartment(model, requestedFor);
            requireType(model, requestedType, "EquipmentType", "Requested equipment type was not found.");

            request.addProperty(RDF.type, resource("EquipmentRequest"))
                    .addLiteral(property("name"), requestNumber)
                    .addLiteral(property("requestNumber"), requestNumber)
                    .addLiteral(property("requestReason"), form.reason())
                    .addLiteral(property("requestedAt"), model.createTypedLiteral(requestedAt, XSDDatatype.XSDdateTime))
                    .addProperty(property("requestedFor"), requestedFor)
                    .addProperty(property("requestsType"), requestedType)
                    .addProperty(property("hasRequestStatus"), resource("Open"));
        });
    }

    @Override
    public void cancelEquipmentRequest(String id, String cancelledAt) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource request = resource(id);
            Property hasRequestStatus = property("hasRequestStatus");

            requireType(model, request, "EquipmentRequest", "Equipment request was not found.");
            if (!model.contains(request, hasRequestStatus, resource("Open"))) {
                throw new IllegalArgumentException("Only an open equipment request can be cancelled.");
            }
            model.add(
                    request,
                    property("cancelledAt"),
                    model.createTypedLiteral(cancelledAt, XSDDatatype.XSDdateTime)
            );
            model.removeAll(request, hasRequestStatus, null);
            model.add(request, hasRequestStatus, resource("Cancelled"));
        });
    }

    private void requireDepartment(Model model, Resource resource) {
        InfModel rdfsModel = ModelFactory.createRDFSModel(model);
        if (!rdfsModel.contains(resource, RDF.type, resource("Department"))) {
            throw new IllegalArgumentException("Requested department was not found.");
        }
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
