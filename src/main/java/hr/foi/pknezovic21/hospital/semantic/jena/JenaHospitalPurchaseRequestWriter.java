package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalPurchaseRequestWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.RDF;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalPurchaseRequestWriter implements HospitalPurchaseRequestWriter {

    private final Dataset dataset;
    private final JenaHospitalReasoner reasoner;
    private final String baseUri;

    public JenaHospitalPurchaseRequestWriter(
            Dataset dataset,
            JenaHospitalReasoner reasoner,
            @Value("${hospital.rdf.base-uri}") String baseUri
    ) {
        this.dataset = dataset;
        this.reasoner = reasoner;
        this.baseUri = baseUri;
    }

    @Override
    public void addPurchaseRequest(String id, String purchaseNumber, String createdAt, PurchaseRequestForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource purchaseRequest = model.createResource(uri(id));
            Resource equipmentRequest = resource(form.equipmentRequestId());
            Property hasRequestStatus = property("hasRequestStatus");

            requireType(model, equipmentRequest, "EquipmentRequest", "Equipment request was not found.");
            requireOpenRequest(model, equipmentRequest);
            requireNoExistingPurchaseRequest(model, equipmentRequest);
            Resource requestedFor = requiredResource(model, equipmentRequest, property("requestedFor"));
            Resource requestedType = requiredResource(model, equipmentRequest, property("requestsType"));
            requirePurchaseNeeded(model, equipmentRequest);

            purchaseRequest.addProperty(RDF.type, resource("PurchaseRequest"))
                    .addLiteral(property("name"), purchaseNumber)
                    .addLiteral(property("purchaseNumber"), purchaseNumber)
                    .addProperty(property("purchaseForRequest"), equipmentRequest)
                    .addProperty(property("purchaseRequestedFor"), requestedFor)
                    .addProperty(property("purchaseRequestsType"), requestedType)
                    .addLiteral(property("purchaseReason"), form.reason())
                    .addLiteral(property("createdAt"), model.createTypedLiteral(createdAt, XSDDatatype.XSDdateTime));

            model.removeAll(equipmentRequest, hasRequestStatus, null);
            model.add(equipmentRequest, hasRequestStatus, resource("PurchasePending"));
        });
    }

    private void requireOpenRequest(Model model, Resource request) {
        if (!model.contains(request, property("hasRequestStatus"), resource("Open"))) {
            throw new IllegalArgumentException("Equipment request is not open.");
        }
    }

    private void requireNoExistingPurchaseRequest(Model model, Resource equipmentRequest) {
        if (model.contains(null, property("purchaseForRequest"), equipmentRequest)) {
            throw new IllegalArgumentException("Purchase request already exists for this equipment request.");
        }
    }

    private void requirePurchaseNeeded(Model model, Resource equipmentRequest) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                ASK {
                  ?request rdf:type hospital:PurchaseNeededRequest .
                }
                """.formatted(baseUri));
        query.setIri("request", equipmentRequest.getURI());
        try (QueryExecution execution = QueryExecution.model(reasoner.create(model)).query(query.toString()).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Requested equipment is still available.");
            }
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
