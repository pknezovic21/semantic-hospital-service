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
        individual(model, "Equipment001", "Equipment", "Volumetric infusion pump")
                .addLiteral(property("assetNumber"), "INF-001")
                .addProperty(property("assignedTo"), equipmentLibrary)
                .addProperty(property("hasEquipmentStatus"), resource("Available"));

        equipmentLibrary.addProperty(property("managedBy"), coordinator);
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
