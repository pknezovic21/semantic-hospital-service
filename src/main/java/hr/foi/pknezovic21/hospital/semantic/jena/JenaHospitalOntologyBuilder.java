package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalOntologyBuilder;
import org.apache.jena.ontapi.OntModelFactory;
import org.apache.jena.ontapi.OntSpecification;
import org.apache.jena.ontapi.model.OntClass;
import org.apache.jena.ontapi.model.OntDataProperty;
import org.apache.jena.ontapi.model.OntDataRange;
import org.apache.jena.ontapi.model.OntIndividual;
import org.apache.jena.ontapi.model.OntModel;
import org.apache.jena.ontapi.model.OntObjectProperty;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalOntologyBuilder implements HospitalOntologyBuilder {

    private final Dataset dataset;
    private final String baseUri;

    public JenaHospitalOntologyBuilder(Dataset dataset, @Value("${hospital.rdf.base-uri}") String baseUri) {
        this.dataset = dataset;
        this.baseUri = baseUri;
    }

    @Override
    public void build() {
        Txn.executeWrite(dataset, () -> build(OntModelFactory.createModel(
                dataset.getDefaultModel().getGraph(),
                OntSpecification.OWL2_DL_MEM)));
    }

    private void build(OntModel model) {
        model.setNsPrefix("hospital", baseUri);
        model.setNsPrefix("owl", OWL.NS);
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("rdfs", RDFS.uri);
        model.setNsPrefix("xsd", XSD.NS);
        model.setID(uri("HospitalOrganizationOntology"));

        OntClass.Named hospital = ontClass(model, "Hospital", "Hospital");
        OntClass.Named unit = ontClass(model, "Unit", "Organization unit");
        ontClass(model, "ClinicalDivision", "Clinical division", unit);
        ontClass(model, "Department", "Department", unit);
        OntClass.Named staffMember = ontClass(model, "StaffMember", "Staff member");
        OntClass.Named equipment = ontClass(model, "Equipment", "Equipment");
        OntClass.Named equipmentType = ontClass(model, "EquipmentType", "Equipment type");
        OntClass.Named equipmentRequest = ontClass(model, "EquipmentRequest", "Equipment request");
        OntClass.Named maintenanceRecord = ontClass(model, "MaintenanceRecord", "Maintenance record");
        OntClass.Named equipmentStatus = ontClass(model, "EquipmentStatus", "Equipment status");

        hospital.addDisjointClass(unit);
        unit.addDisjointClass(equipment);

        objectProperty(model, "hasPart", "Has part", hospital, unit);
        objectProperty(model, "partOf", "Part of", unit, hospital);
        objectProperty(model, "managedBy", "Managed by", unit, staffMember);
        objectProperty(model, "assignedTo", "Assigned to", equipment, unit);
        objectProperty(model, "hasEquipmentStatus", "Has equipment status", equipment, equipmentStatus);
        objectProperty(model, "hasEquipmentType", "Has equipment type", equipment, equipmentType);
        objectProperty(model, "requestsType", "Requests type", equipmentRequest, equipmentType);
        objectProperty(model, "requestedFor", "Requested for", equipmentRequest, unit);
        objectProperty(model, "availableCandidate", "Available candidate", equipmentRequest, equipment);
        objectProperty(model, "maintenanceFor", "Maintenance for", maintenanceRecord, equipment);

        OntDataRange.Named string = model.createDatatype(XSD.xstring.getURI());
        OntDataRange.Named dateTime = model.createDatatype(XSD.dateTime.getURI());
        dataProperty(model, "name", "Name", model.getOWLThing(), string);
        dataProperty(model, "assetNumber", "Asset number", equipment, string);
        dataProperty(model, "requestNumber", "Request number", equipmentRequest, string);
        dataProperty(model, "maintenanceNumber", "Maintenance number", maintenanceRecord, string);
        dataProperty(model, "maintenanceReason", "Maintenance reason", maintenanceRecord, string);
        dataProperty(model, "reportedAt", "Reported at", maintenanceRecord, dateTime);

        individual(model, "Available", "Available", equipmentStatus);
        individual(model, "InMaintenance", "In maintenance", equipmentStatus);
    }

    private OntClass.Named ontClass(OntModel model, String name, String label) {
        OntClass.Named ontClass = model.createOntClass(uri(name));
        ontClass.addLabel(label, "en");
        return ontClass;
    }

    private OntClass.Named ontClass(
            OntModel model,
            String name,
            String label,
            OntClass.Named superClass
    ) {
        OntClass.Named ontClass = ontClass(model, name, label);
        ontClass.addSuperClass(superClass);
        return ontClass;
    }

    private OntObjectProperty.Named objectProperty(
            OntModel model,
            String name,
            String label,
            OntClass.Named domain,
            OntClass.Named range
    ) {
        OntObjectProperty.Named property = model.createObjectProperty(uri(name));
        property.addLabel(label, "en");
        property.addDomain(domain);
        property.addRange(range);
        return property;
    }

    private OntDataProperty dataProperty(
            OntModel model,
            String name,
            String label,
            OntClass.Named domain,
            OntDataRange.Named range
    ) {
        OntDataProperty property = model.createDataProperty(uri(name));
        property.addLabel(label, "en");
        property.addDomain(domain);
        property.addRange(range);
        return property;
    }

    private OntIndividual individual(
            OntModel model,
            String name,
            String label,
            OntClass.Named type
    ) {
        OntIndividual individual = model.createIndividual(uri(name), type);
        individual.addLabel(label, "en");
        return individual;
    }

    private String uri(String name) {
        return baseUri + name;
    }
}
