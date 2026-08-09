package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.domain.MaintenanceRecordForm;
import hr.foi.pknezovic21.hospital.semantic.api.HospitalMaintenanceWriter;
import org.apache.jena.datatypes.xsd.XSDDatatype;
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

            maintenanceRecord.addProperty(RDF.type, resource("MaintenanceRecord"))
                    .addLiteral(property("name"), maintenanceNumber)
                    .addLiteral(property("maintenanceNumber"), maintenanceNumber)
                    .addProperty(property("maintenanceFor"), equipment)
                    .addLiteral(property("maintenanceReason"), form.reason())
                    .addLiteral(property("reportedAt"), model.createTypedLiteral(reportedAt, XSDDatatype.XSDdateTime));

            model.removeAll(equipment, hasEquipmentStatus, null);
            model.add(equipment, hasEquipmentStatus, inMaintenance);
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
