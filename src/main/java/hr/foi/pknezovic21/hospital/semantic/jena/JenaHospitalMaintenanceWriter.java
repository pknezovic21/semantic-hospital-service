package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalMaintenanceWriter;
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
public class JenaHospitalMaintenanceWriter implements HospitalMaintenanceWriter {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalMaintenanceWriter(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void addMaintenanceRecord(String id, String maintenanceNumber, String reportedAt, MaintenanceRecordForm form) {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            Resource maintenanceRecord = model.createResource(uri(id));
            Resource equipment = resource(form.equipmentId());
            Resource inMaintenance = resource("InMaintenance");
            Property hasEquipmentStatus = property("hasEquipmentStatus");

            requireType(model, equipment, "Equipment", "Equipment was not found.");
            requireNoActiveLoan(model, equipment);
            requireNoOpenMaintenance(model, equipment);
            requireAvailable(model, equipment);
            Resource reportedFor = requiredResource(
                    model,
                    equipment,
                    property("assignedTo"),
                    "Equipment assignment is incomplete."
            );

            maintenanceRecord.addProperty(RDF.type, resource("MaintenanceRecord"))
                    .addLiteral(property("name"), maintenanceNumber)
                    .addLiteral(property("maintenanceNumber"), maintenanceNumber)
                    .addProperty(property("maintenanceFor"), equipment)
                    .addProperty(property("maintenanceReportedFor"), reportedFor)
                    .addLiteral(property("maintenanceReason"), form.reason())
                    .addLiteral(property("reportedAt"), model.createTypedLiteral(reportedAt, XSDDatatype.XSDdateTime));

            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, inMaintenance);
        });
    }

    private void requireNoActiveLoan(Model model, Resource equipment) {
        ParameterizedSparqlString query = queryForEquipment("""
                ASK {
                  ?loan rdf:type hospital:EquipmentLoan ;
                        hospital:loanedEquipment ?equipment .
                  FILTER NOT EXISTS { ?loan hospital:returnedAt ?returnedAt . }
                }
                """, equipment);
        if (ask(model, query)) {
            throw new IllegalArgumentException("Equipment is currently loaned.");
        }
    }

    private void requireNoOpenMaintenance(Model model, Resource equipment) {
        ParameterizedSparqlString query = queryForEquipment("""
                ASK {
                  ?record rdf:type hospital:MaintenanceRecord ;
                          hospital:maintenanceFor ?equipment .
                  FILTER NOT EXISTS { ?record hospital:completedAt ?completedAt . }
                }
                """, equipment);
        if (ask(model, query)) {
            throw new IllegalArgumentException("Equipment already has open maintenance.");
        }
    }

    private void requireAvailable(Model model, Resource equipment) {
        if (!model.contains(equipment, property("hasEquipmentStatus"), resource("Available"))) {
            throw new IllegalArgumentException("Equipment is not available for maintenance.");
        }
    }

    private ParameterizedSparqlString queryForEquipment(String body, Resource equipment) {
        ParameterizedSparqlString query = new ParameterizedSparqlString("""
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                %s
                """.formatted(baseUri, body));
        query.setIri("equipment", equipment.getURI());
        return query;
    }

    private boolean ask(Model model, ParameterizedSparqlString query) {
        try (QueryExecution execution = QueryExecution.model(model).query(query.toString()).build()) {
            return execution.execAsk();
        }
    }

    private Resource requiredResource(Model model, Resource subject, Property predicate, String message) {
        if (!model.contains(subject, predicate)) {
            throw new IllegalArgumentException(message);
        }
        return model.getProperty(subject, predicate).getResource();
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
