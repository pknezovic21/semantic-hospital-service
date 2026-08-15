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
        ontClass(model, "EquipmentShortageUnit", "Equipment shortage unit", unit);
        OntClass.Named equipment = ontClass(model, "Equipment", "Equipment");
        OntClass.Named medicalEquipment = ontClass(model, "MedicalEquipment", "Medical equipment", equipment);
        ontClass(model, "InfusionEquipment", "Infusion equipment", medicalEquipment);
        ontClass(model, "MonitoringEquipment", "Monitoring equipment", medicalEquipment);
        ontClass(model, "DiagnosticEquipment", "Diagnostic equipment", medicalEquipment);
        ontClass(model, "MobilityEquipment", "Mobility equipment", medicalEquipment);
        ontClass(model, "HighRiskEquipment", "High risk equipment", equipment);
        OntClass.Named equipmentType = ontClass(model, "EquipmentType", "Equipment type");
        OntClass.Named equipmentCategory = ontClass(model, "EquipmentCategory", "Equipment category");
        OntClass.Named medicalDeviceCategory = ontClass(model, "MedicalDeviceCategory", "Medical device category", equipmentCategory);
        ontClass(model, "InfusionCategory", "Infusion category", medicalDeviceCategory);
        ontClass(model, "MonitoringCategory", "Monitoring category", medicalDeviceCategory);
        ontClass(model, "DiagnosticCategory", "Diagnostic category", medicalDeviceCategory);
        OntClass.Named location = ontClass(model, "Location", "Location");
        OntClass.Named clinicalLocation = ontClass(model, "ClinicalLocation", "Clinical location", location);
        ontClass(model, "Ward", "Ward", clinicalLocation);
        ontClass(model, "ProcedureRoom", "Procedure room", clinicalLocation);
        OntClass.Named storageLocation = ontClass(model, "StorageLocation", "Storage location", location);
        ontClass(model, "EquipmentStore", "Equipment store", storageLocation);
        ontClass(model, "DepartmentStore", "Department store", storageLocation);
        OntClass.Named supplier = ontClass(model, "Supplier", "Supplier");
        ontClass(model, "EquipmentSupplier", "Equipment supplier", supplier);
        ontClass(model, "ServiceSupplier", "Service supplier", supplier);
        OntClass.Named contract = ontClass(model, "Contract", "Contract");
        ontClass(model, "MaintenanceContract", "Maintenance contract", contract);
        ontClass(model, "SupplyContract", "Supply contract", contract);
        OntClass.Named staffMember = ontClass(model, "StaffMember", "Staff member");
        OntClass.Named equipmentRequest = ontClass(model, "EquipmentRequest", "Equipment request");
        ontClass(model, "PurchaseNeededRequest", "Purchase needed request", equipmentRequest);
        ontClass(model, "HighPriorityRequest", "High priority request", equipmentRequest);
        OntClass.Named purchaseRequest = ontClass(model, "PurchaseRequest", "Purchase request");
        OntClass.Named equipmentLoan = ontClass(model, "EquipmentLoan", "Equipment loan");
        OntClass.Named maintenanceRecord = ontClass(model, "MaintenanceRecord", "Maintenance record");
        ontClass(model, "HighPriorityMaintenanceRecord", "High priority maintenance record", maintenanceRecord);
        OntClass.Named equipmentStatus = ontClass(model, "EquipmentStatus", "Equipment status");

        hospital.addDisjointClass(unit);
        unit.addDisjointClass(equipment);

        objectProperty(model, "hasPart", "Has part", hospital, unit);
        objectProperty(model, "partOf", "Part of", unit, hospital);
        objectProperty(model, "managedBy", "Managed by", unit, staffMember);
        objectProperty(model, "assignedTo", "Assigned to", equipment, unit);
        objectProperty(model, "locatedIn", "Located in", equipment, location);
        objectProperty(model, "hasEquipmentStatus", "Has equipment status", equipment, equipmentStatus);
        objectProperty(model, "hasEquipmentType", "Has equipment type", equipment, equipmentType);
        objectProperty(model, "servesUnit", "Serves unit", location, unit);
        objectProperty(model, "belongsToCategory", "Belongs to category", equipmentType, equipmentCategory);
        objectProperty(model, "suppliedBy", "Supplied by", equipmentType, supplier);
        objectProperty(model, "coveredByContract", "Covered by contract", equipment, contract);
        objectProperty(model, "contractedSupplier", "Contracted supplier", contract, supplier);
        objectProperty(model, "requestsType", "Requests type", equipmentRequest, equipmentType);
        objectProperty(model, "requestedFor", "Requested for", equipmentRequest, unit);
        objectProperty(model, "availableCandidate", "Available candidate", equipmentRequest, equipment);
        objectProperty(model, "purchaseForRequest", "Purchase for request", purchaseRequest, equipmentRequest);
        objectProperty(model, "purchaseRequestedFor", "Purchase requested for", purchaseRequest, unit);
        objectProperty(model, "purchaseRequestsType", "Purchase requests type", purchaseRequest, equipmentType);
        objectProperty(model, "loanedEquipment", "Loaned equipment", equipmentLoan, equipment);
        objectProperty(model, "loanedTo", "Loaned to", equipmentLoan, unit);
        objectProperty(model, "loanedForRequest", "Loaned for request", equipmentLoan, equipmentRequest);
        objectProperty(model, "maintenanceFor", "Maintenance for", maintenanceRecord, equipment);

        OntDataRange.Named string = model.createDatatype(XSD.xstring.getURI());
        OntDataRange.Named dateTime = model.createDatatype(XSD.dateTime.getURI());
        dataProperty(model, "name", "Name", model.getOWLThing(), string);
        dataProperty(model, "assetNumber", "Asset number", equipment, string);
        dataProperty(model, "locationCode", "Location code", location, string);
        dataProperty(model, "categoryCode", "Category code", equipmentCategory, string);
        dataProperty(model, "supplierCode", "Supplier code", supplier, string);
        dataProperty(model, "contractNumber", "Contract number", contract, string);
        dataProperty(model, "requestNumber", "Request number", equipmentRequest, string);
        dataProperty(model, "purchaseNumber", "Purchase number", purchaseRequest, string);
        dataProperty(model, "purchaseReason", "Purchase reason", purchaseRequest, string);
        dataProperty(model, "createdAt", "Created at", purchaseRequest, dateTime);
        dataProperty(model, "loanNumber", "Loan number", equipmentLoan, string);
        dataProperty(model, "loanedAt", "Loaned at", equipmentLoan, dateTime);
        dataProperty(model, "returnedAt", "Returned at", equipmentLoan, dateTime);
        dataProperty(model, "maintenanceNumber", "Maintenance number", maintenanceRecord, string);
        dataProperty(model, "maintenanceReason", "Maintenance reason", maintenanceRecord, string);
        dataProperty(model, "reportedAt", "Reported at", maintenanceRecord, dateTime);

        individual(model, "Available", "Available", equipmentStatus);
        individual(model, "InMaintenance", "In maintenance", equipmentStatus);
        individual(model, "Loaned", "Loaned", equipmentStatus);
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
