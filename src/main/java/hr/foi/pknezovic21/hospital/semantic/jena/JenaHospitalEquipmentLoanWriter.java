package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.EquipmentLoanForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalEquipmentLoanWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ParameterizedSparqlString;
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
public class JenaHospitalEquipmentLoanWriter implements HospitalEquipmentLoanWriter {

    private final Dataset dataset;
    private final JenaHospitalReasoner reasoner;
    private final String baseUri;

    public JenaHospitalEquipmentLoanWriter(
            Dataset dataset,
            JenaHospitalReasoner reasoner,
            @Value("${hospital.rdf.base-uri}") String baseUri
    ) {
        this.dataset = dataset;
        this.reasoner = reasoner;
        this.baseUri = baseUri;
    }

    @Override
    public void addEquipmentLoan(String id, String loanNumber, String loanedAt, EquipmentLoanForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource loan = model.createResource(uri(id));
            Resource equipment = resource(form.equipmentId());
            Resource loanedTo = resource(form.loanedToUnitId());
            Resource loanedToLocation = resource(form.loanedToLocationId());
            Resource request = form.requestId() == null ? null : resource(form.requestId());
            Resource loaned = resource("Loaned");
            Resource fulfilled = resource("Fulfilled");
            Property hasEquipmentStatus = property("hasEquipmentStatus");
            Property hasRequestStatus = property("hasRequestStatus");
            Property assignedTo = property("assignedTo");
            Property locatedIn = property("locatedIn");

            requireType(model, equipment, "Equipment", "Equipment was not found.");
            requireUnit(model, loanedTo);
            requireLocationForUnit(model, loanedToLocation, loanedTo);
            Resource loanedFrom = requiredResource(model, equipment, assignedTo, "Equipment assignment is incomplete.");
            Resource loanedFromLocation = optionalResource(model, equipment, locatedIn);
            if (loanedFrom.equals(loanedTo)) {
                throw new IllegalArgumentException("Equipment is already assigned to the loan unit.");
            }
            if (request == null) {
                requireAvailable(model, equipment);
            } else {
                requireRequest(model, request, equipment, loanedTo);
            }

            loan.addProperty(RDF.type, resource("EquipmentLoan"))
                    .addLiteral(property("name"), loanNumber)
                    .addLiteral(property("loanNumber"), loanNumber)
                    .addProperty(property("loanedEquipment"), equipment)
                    .addProperty(property("loanedFrom"), loanedFrom)
                    .addProperty(property("loanedTo"), loanedTo)
                    .addProperty(property("loanedToLocation"), loanedToLocation)
                    .addLiteral(property("loanedAt"), model.createTypedLiteral(loanedAt, XSDDatatype.XSDdateTime));

            if (loanedFromLocation != null) {
                loan.addProperty(property("loanedFromLocation"), loanedFromLocation);
            }
            if (request != null) {
                loan.addProperty(property("loanedForRequest"), request);
            }

            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, loaned);
            model.removeAll(equipment, assignedTo, null);
            model.add(equipment, assignedTo, loanedTo);
            model.removeAll(equipment, locatedIn, null);
            model.add(equipment, locatedIn, loanedToLocation);
            if (request != null) {
                model.removeAll(request, hasRequestStatus, null);
                model.add(request, hasRequestStatus, fulfilled);
            }
        });
    }

    @Override
    public void returnEquipmentLoan(String loanId, String returnedAt) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource loan = resource(loanId);
            Property hasEquipmentStatus = property("hasEquipmentStatus");
            Property assignedTo = property("assignedTo");
            Property locatedIn = property("locatedIn");

            requireType(model, loan, "EquipmentLoan", "Equipment loan was not found.");
            requireOpenLoan(model, loan);
            Resource equipment = requiredResource(
                    model,
                    loan,
                    property("loanedEquipment"),
                    "Equipment loan is incomplete."
            );
            Resource loanedFrom = requiredResource(model, loan, property("loanedFrom"), "Equipment loan is incomplete.");
            Resource loanedFromLocation = optionalResource(model, loan, property("loanedFromLocation"));
            Resource restoredStatus = hasOpenMaintenance(model, equipment)
                    ? resource("InMaintenance")
                    : resource("Available");

            model.add(loan, property("returnedAt"), model.createTypedLiteral(returnedAt, XSDDatatype.XSDdateTime));
            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, restoredStatus);
            model.removeAll(equipment, assignedTo, null);
            model.add(equipment, assignedTo, loanedFrom);
            model.removeAll(equipment, locatedIn, null);
            if (loanedFromLocation != null) {
                model.add(equipment, locatedIn, loanedFromLocation);
            }
        });
    }

    private void requireType(Model model, Resource resource, String type, String message) {
        if (!model.contains(resource, RDF.type, resource(type))) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireUnit(Model model, Resource resource) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                ASK {
                  ?unit rdf:type/rdfs:subClassOf* hospital:Unit .
                }
                """.formatted(baseUri));
        query.setIri("unit", resource.getURI());
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Loan unit was not found.");
            }
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
                throw new IllegalArgumentException("Loan location does not belong to the loan unit.");
            }
        }
    }

    private void requireAvailable(Model model, Resource equipment) {
        if (!model.contains(equipment, property("hasEquipmentStatus"), resource("Available"))) {
            throw new IllegalArgumentException("Equipment is not available.");
        }
    }

    private void requireRequest(Model model, Resource request, Resource equipment, Resource loanedTo) {
        requireType(model, request, "EquipmentRequest", "Equipment request was not found.");
        requireOpenRequest(model, request);
        if (!model.contains(request, property("requestedFor"), loanedTo)) {
            throw new IllegalArgumentException("Equipment does not match the request.");
        }
        requireAvailableCandidate(model, request, equipment);
    }

    private void requireAvailableCandidate(Model model, Resource request, Resource equipment) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>

                ASK {
                  ?request hospital:availableCandidate ?equipment .
                }
                """.formatted(baseUri));
        query.setIri("request", request.getURI());
        query.setIri("equipment", equipment.getURI());
        try (QueryExecution execution = QueryExecution.model(reasoner.create(model)).query(query.toString()).build()) {
            if (!execution.execAsk()) {
                throw new IllegalArgumentException("Equipment is not an available candidate for the request.");
            }
        }
    }

    private void requireOpenRequest(Model model, Resource request) {
        if (!model.contains(request, property("hasRequestStatus"), resource("Open"))) {
            throw new IllegalArgumentException("Equipment request is not open.");
        }
    }

    private void requireOpenLoan(Model model, Resource loan) {
        if (model.contains(loan, property("returnedAt"))) {
            throw new IllegalArgumentException("Equipment loan is already returned.");
        }
    }

    private boolean hasOpenMaintenance(Model model, Resource equipment) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                ASK {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceFor ?equipment .
                  FILTER NOT EXISTS { ?record hospital:completedAt ?completedAt . }
                }
                """.formatted(baseUri));
        query.setIri("equipment", equipment.getURI());
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            return execution.execAsk();
        }
    }

    private Resource requiredResource(Model model, Resource subject, Property predicate, String message) {
        Resource value = optionalResource(model, subject, predicate);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private Resource optionalResource(Model model, Resource subject, Property predicate) {
        return model.contains(subject, predicate)
                ? model.getProperty(subject, predicate).getResource()
                : null;
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
