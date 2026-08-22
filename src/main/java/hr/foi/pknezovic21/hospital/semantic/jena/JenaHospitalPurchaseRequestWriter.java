package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.PurchaseRequestForm;
import hr.foi.pknezovic21.hospital.domain.PurchaseReceiptForm;
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
            Resource supplier = resource(form.supplierId());
            Property hasRequestStatus = property("hasRequestStatus");

            requireType(model, equipmentRequest, "EquipmentRequest", "Equipment request was not found.");
            requireOpenRequest(model, equipmentRequest);
            requireNoExistingPurchaseRequest(model, equipmentRequest);
            Resource requestedFor = requiredResource(
                    model,
                    equipmentRequest,
                    property("requestedFor"),
                    "Equipment request is incomplete."
            );
            Resource requestedType = requiredResource(
                    model,
                    equipmentRequest,
                    property("requestsType"),
                    "Equipment request is incomplete."
            );
            requirePurchaseNeeded(model, equipmentRequest);
            requireSupplierForType(model, supplier, requestedType);

            purchaseRequest.addProperty(RDF.type, resource("PurchaseRequest"))
                    .addLiteral(property("name"), purchaseNumber)
                    .addLiteral(property("purchaseNumber"), purchaseNumber)
                    .addProperty(property("purchaseForRequest"), equipmentRequest)
                    .addProperty(property("purchaseRequestedFor"), requestedFor)
                    .addProperty(property("purchaseRequestsType"), requestedType)
                    .addProperty(property("selectedSupplier"), supplier)
                    .addLiteral(property("purchaseReason"), form.reason())
                    .addLiteral(property("createdAt"), model.createTypedLiteral(createdAt, XSDDatatype.XSDdateTime));

            model.removeAll(equipmentRequest, hasRequestStatus, null);
            model.add(equipmentRequest, hasRequestStatus, resource("PurchasePending"));
        });
    }

    @Override
    public void receivePurchaseRequest(
            String purchaseRequestId,
            String equipmentId,
            String assetNumber,
            String receivedAt,
            PurchaseReceiptForm form
    ) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource purchaseRequest = model.createResource(uri(purchaseRequestId));
            Resource equipment = model.createResource(uri(equipmentId));
            Resource location = resource(form.locationId());
            Property hasRequestStatus = property("hasRequestStatus");

            requireType(model, purchaseRequest, "PurchaseRequest", "Purchase request was not found.");
            requireActivePurchase(model, purchaseRequest);
            Resource equipmentRequest = requiredResource(
                    model,
                    purchaseRequest,
                    property("purchaseForRequest"),
                    "Purchase request is incomplete."
            );
            Resource requestedFor = requiredResource(
                    model,
                    purchaseRequest,
                    property("purchaseRequestedFor"),
                    "Purchase request is incomplete."
            );
            Resource requestedType = requiredResource(
                    model,
                    purchaseRequest,
                    property("purchaseRequestsType"),
                    "Purchase request is incomplete."
            );
            Resource supplier = requiredResource(
                    model,
                    purchaseRequest,
                    property("selectedSupplier"),
                    "Purchase request is incomplete."
            );
            requirePendingRequest(model, equipmentRequest);
            requireLocationForUnit(model, location, requestedFor);

            String equipmentName = requiredLiteral(
                    model,
                    requestedType,
                    property("name"),
                    "Equipment type is incomplete."
            );
            equipment.addProperty(RDF.type, resource("Equipment"))
                    .addLiteral(property("name"), equipmentName)
                    .addLiteral(property("assetNumber"), assetNumber)
                    .addProperty(property("hasEquipmentType"), requestedType)
                    .addProperty(property("hasEquipmentStatus"), resource("Available"))
                    .addProperty(property("assignedTo"), requestedFor)
                    .addProperty(property("locatedIn"), location)
                    .addProperty(property("providedBy"), supplier);

            purchaseRequest.addProperty(property("receivedEquipment"), equipment)
                    .addLiteral(property("receivedAt"), model.createTypedLiteral(receivedAt, XSDDatatype.XSDdateTime));
            model.removeAll(equipmentRequest, hasRequestStatus, null);
            model.add(equipmentRequest, hasRequestStatus, resource("Fulfilled"));
        });
    }

    @Override
    public void cancelPurchaseRequest(String purchaseRequestId, String cancelledAt) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource purchaseRequest = model.createResource(uri(purchaseRequestId));
            Property hasRequestStatus = property("hasRequestStatus");

            requireType(model, purchaseRequest, "PurchaseRequest", "Purchase request was not found.");
            requireActivePurchase(model, purchaseRequest);
            Resource equipmentRequest = requiredResource(
                    model,
                    purchaseRequest,
                    property("purchaseForRequest"),
                    "Purchase request is incomplete."
            );
            requirePendingRequest(model, equipmentRequest);

            purchaseRequest.addLiteral(
                    property("cancelledAt"),
                    model.createTypedLiteral(cancelledAt, XSDDatatype.XSDdateTime)
            );
            model.removeAll(equipmentRequest, hasRequestStatus, null);
            model.add(equipmentRequest, hasRequestStatus, resource("Open"));
        });
    }

    private void requireOpenRequest(Model model, Resource request) {
        if (!model.contains(request, property("hasRequestStatus"), resource("Open"))) {
            throw new IllegalArgumentException("Equipment request is not open.");
        }
    }

    private void requireNoExistingPurchaseRequest(Model model, Resource equipmentRequest) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>

                ASK {
                  ?purchaseRequest hospital:purchaseForRequest ?equipmentRequest .
                  FILTER NOT EXISTS { ?purchaseRequest hospital:receivedAt ?receivedAt . }
                  FILTER NOT EXISTS { ?purchaseRequest hospital:cancelledAt ?cancelledAt . }
                }
                """.formatted(baseUri));
        query.setIri("equipmentRequest", equipmentRequest.getURI());
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            if (execution.execAsk()) {
                throw new IllegalArgumentException("Purchase request already exists for this equipment request.");
            }
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

    private void requireSupplierForType(Model model, Resource supplier, Resource equipmentType) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>

                ASK {
                  ?type hospital:suppliedBy ?supplier .
                }
                """.formatted(baseUri));
        query.setIri("type", equipmentType.getURI());
        query.setIri("supplier", supplier.getURI());
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Supplier does not provide the requested equipment type.");
            }
        }
    }

    private void requireActivePurchase(Model model, Resource purchaseRequest) {
        if (model.contains(purchaseRequest, property("receivedEquipment"), (Resource) null)
                || model.contains(purchaseRequest, property("receivedAt"))) {
            throw new IllegalArgumentException("Purchase request is already received.");
        }
        if (model.contains(purchaseRequest, property("cancelledAt"))) {
            throw new IllegalArgumentException("Purchase request is already cancelled.");
        }
    }

    private void requirePendingRequest(Model model, Resource equipmentRequest) {
        if (!model.contains(equipmentRequest, property("hasRequestStatus"), resource("PurchasePending"))) {
            throw new IllegalArgumentException("Equipment request is not pending purchase.");
        }
    }

    private void requireLocationForUnit(Model model, Resource location, Resource unit) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>

                ASK {
                  ?location hospital:servesUnit/hospital:partOf* ?unit .
                }
                """.formatted(baseUri));
        query.setIri("location", location.getURI());
        query.setIri("unit", unit.getURI());
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Receipt location does not belong to the requested unit.");
            }
        }
    }

    private Resource requiredResource(Model model, Resource resource, Property property, String message) {
        Statement statement = model.getProperty(resource, property);
        if (statement == null) {
            throw new IllegalArgumentException(message);
        }
        return statement.getResource();
    }

    private String requiredLiteral(Model model, Resource resource, Property property, String message) {
        Statement statement = model.getProperty(resource, property);
        if (statement == null || !statement.getObject().isLiteral()) {
            throw new IllegalArgumentException(message);
        }
        return statement.getString();
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
