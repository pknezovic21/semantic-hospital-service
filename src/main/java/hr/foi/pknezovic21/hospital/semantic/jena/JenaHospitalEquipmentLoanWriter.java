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
            Resource request = form.requestId() == null ? null : resource(form.requestId());
            Resource loaned = resource("Loaned");
            Resource fulfilled = resource("Fulfilled");
            Property hasEquipmentStatus = property("hasEquipmentStatus");
            Property hasRequestStatus = property("hasRequestStatus");
            Property assignedTo = property("assignedTo");

            requireType(model, equipment, "Equipment", "Equipment was not found.");
            requireUnit(model, loanedTo);
            if (request == null) {
                requireAvailable(model, equipment);
            } else {
                requireRequest(model, request, equipment, loanedTo);
            }

            loan.addProperty(RDF.type, resource("EquipmentLoan"))
                    .addLiteral(property("name"), loanNumber)
                    .addLiteral(property("loanNumber"), loanNumber)
                    .addProperty(property("loanedEquipment"), equipment)
                    .addProperty(property("loanedTo"), loanedTo)
                    .addLiteral(property("loanedAt"), model.createTypedLiteral(loanedAt, XSDDatatype.XSDdateTime));

            if (request != null) {
                loan.addProperty(property("loanedForRequest"), request);
            }

            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, loaned);
            model.removeAll(equipment, assignedTo, null);
            model.add(equipment, assignedTo, loanedTo);
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
            Resource equipment = model.getProperty(loan, property("loanedEquipment")).getResource();
            Resource available = resource("Available");
            Resource equipmentLibrary = resource("MedicalEquipmentLibrary");
            Property hasEquipmentStatus = property("hasEquipmentStatus");
            Property assignedTo = property("assignedTo");

            requireType(model, loan, "EquipmentLoan", "Equipment loan was not found.");
            requireOpenLoan(model, loan);

            model.add(loan, property("returnedAt"), model.createTypedLiteral(returnedAt, XSDDatatype.XSDdateTime));
            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, available);
            model.removeAll(equipment, assignedTo, null);
            model.add(equipment, assignedTo, equipmentLibrary);
        });
    }

    private void requireType(Model model, Resource resource, String type, String message) {
        if (!model.contains(resource, RDF.type, resource(type))) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireUnit(Model model, Resource resource) {
        if (!model.contains(resource, RDF.type, resource("Hospital"))
                && !model.contains(resource, RDF.type, resource("ClinicalDivision"))
                && !model.contains(resource, RDF.type, resource("Department"))) {
            throw new IllegalArgumentException("Loan unit was not found.");
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
