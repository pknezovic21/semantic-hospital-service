package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalPurchaseRequestWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalPurchaseRequestWriter implements HospitalPurchaseRequestWriter {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalPurchaseRequestWriter(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void addPurchaseRequest(String id, String purchaseNumber, String createdAt, PurchaseRequestForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource purchaseRequest = model.createResource(uri(id));
            Resource equipmentRequest = resource(form.equipmentRequestId());

            requireType(model, equipmentRequest, "EquipmentRequest", "Equipment request was not found.");
            Resource requestedFor = requiredResource(model, equipmentRequest, property("requestedFor"));
            Resource requestedType = requiredResource(model, equipmentRequest, property("requestsType"));
            requireNoAvailableCandidate(model, requestedType);

            purchaseRequest.addProperty(RDF.type, resource("PurchaseRequest"))
                    .addLiteral(property("name"), purchaseNumber)
                    .addLiteral(property("purchaseNumber"), purchaseNumber)
                    .addProperty(property("purchaseForRequest"), equipmentRequest)
                    .addProperty(property("purchaseRequestedFor"), requestedFor)
                    .addProperty(property("purchaseRequestsType"), requestedType)
                    .addLiteral(property("purchaseReason"), form.reason())
                    .addLiteral(property("createdAt"), model.createTypedLiteral(createdAt, XSDDatatype.XSDdateTime));
        });
    }

    private void requireNoAvailableCandidate(Model model, Resource requestedType) {
        StmtIterator equipmentWithType = model.listStatements(null, property("hasEquipmentType"), requestedType);
        try {
            while (equipmentWithType.hasNext()) {
                Statement statement = equipmentWithType.nextStatement();
                if (model.contains(statement.getSubject(), property("hasEquipmentStatus"), resource("Available"))) {
                    throw new IllegalArgumentException("Requested equipment is still available.");
                }
            }
        } finally {
            equipmentWithType.close();
        }
    }

    private Resource requiredResource(Model model, Resource resource, Property property) {
        Statement statement = model.getProperty(resource, property);
        if (statement == null) {
            throw new IllegalArgumentException("Equipment request is incomplete.");
        }
        return statement.getResource();
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
