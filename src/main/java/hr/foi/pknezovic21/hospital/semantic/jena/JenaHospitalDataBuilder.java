package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalDataBuilder;
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
public class JenaHospitalDataBuilder implements HospitalDataBuilder {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalDataBuilder(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void build() {
        Txn.executeWrite(dataset, () -> {
            Model model = dataset.getDefaultModel();
            if (model.contains(resource("CentralTeachingHospital"), RDF.type, resource("Hospital"))) {
                return;
            }
            seed(model);
        });
    }

    private void seed(Model model) {
        Resource hospital = individual(model, "CentralTeachingHospital", "Hospital", "Central Teaching Hospital");
        Resource medicine = unit(model, hospital, "MedicineDivision", "ClinicalDivision", "Medicine");
        unit(model, medicine, "EmergencyDepartment", "Department", "Emergency Department");
        Resource equipmentLibrary = unit(
                model,
                hospital,
                "MedicalEquipmentLibrary",
                "Department",
                "Medical Equipment Library"
        );
        Resource coordinator = individual(
                model,
                "Staff001",
                "StaffMember",
                "Equipment library coordinator"
        );
        Resource infusionPumpType = equipmentType(model, "InfusionPumpType", "Infusion pump");
        Resource patientMonitorType = equipmentType(model, "PatientMonitorType", "Patient monitor");
        equipment(
                model,
                "Equipment001",
                "Volumetric infusion pump",
                "INF-001",
                infusionPumpType,
                equipmentLibrary,
                resource("Available")
        );
        equipment(
                model,
                "Equipment002",
                "Volumetric infusion pump",
                "INF-002",
                infusionPumpType,
                equipmentLibrary,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment003",
                "Patient monitor",
                "MON-001",
                patientMonitorType,
                medicine,
                resource("Available")
        );
        equipmentRequest(model, "EquipmentRequest001", "REQ-001", medicine, infusionPumpType);
        equipmentRequest(model, "EquipmentRequest002", "REQ-002", medicine, patientMonitorType);

        equipmentLibrary.addProperty(property("managedBy"), coordinator);
    }

    private Resource equipmentType(Model model, String id, String name) {
        return individual(model, id, "EquipmentType", name);
    }

    private Resource equipment(
            Model model,
            String id,
            String name,
            String assetNumber,
            Resource equipmentType,
            Resource assignedTo,
            Resource status
    ) {
        return individual(model, id, "Equipment", name)
                .addLiteral(property("assetNumber"), assetNumber)
                .addProperty(property("hasEquipmentType"), equipmentType)
                .addProperty(property("assignedTo"), assignedTo)
                .addProperty(property("hasEquipmentStatus"), status);
    }

    private Resource equipmentRequest(
            Model model,
            String id,
            String requestNumber,
            Resource requestedFor,
            Resource requestsType
    ) {
        return individual(model, id, "EquipmentRequest", requestNumber)
                .addLiteral(property("requestNumber"), requestNumber)
                .addProperty(property("requestedFor"), requestedFor)
                .addProperty(property("requestsType"), requestsType);
    }

    private Resource unit(Model model, Resource parent, String id, String type, String name) {
        Resource unit = individual(model, id, type, name);
        parent.addProperty(property("hasPart"), unit);
        unit.addProperty(property("partOf"), parent);
        return unit;
    }

    private Resource individual(Model model, String id, String type, String name) {
        return model.createResource(uri(id))
                .addProperty(RDF.type, resource(type))
                .addLiteral(property("name"), name);
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
