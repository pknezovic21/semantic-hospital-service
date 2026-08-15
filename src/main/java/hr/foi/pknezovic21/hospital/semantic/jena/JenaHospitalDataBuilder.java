package hr.foi.pknezovic21.hospital.semantic.jena;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalDataBuilder;
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
        Resource surgery = unit(model, hospital, "SurgeryDivision", "ClinicalDivision", "Surgery");
        Resource clinicalSupport = unit(model, hospital, "ClinicalSupportDivision", "ClinicalDivision", "Clinical Support");
        Resource emergency = unit(model, medicine, "EmergencyDepartment", "Department", "Emergency Department");
        Resource criticalCare = unit(model, medicine, "CriticalCareUnit", "Department", "Critical Care Unit");
        Resource imaging = unit(model, clinicalSupport, "ImagingDepartment", "Department", "Imaging Department");
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
        Resource infusionCategory = category(
                model,
                "InfusionEquipmentCategory",
                "InfusionCategory",
                "Infusion equipment",
                "CAT-INF"
        );
        Resource monitoringCategory = category(
                model,
                "MonitoringEquipmentCategory",
                "MonitoringCategory",
                "Monitoring equipment",
                "CAT-MON"
        );
        Resource diagnosticCategory = category(
                model,
                "DiagnosticEquipmentCategory",
                "DiagnosticCategory",
                "Diagnostic equipment",
                "CAT-DIA"
        );
        Resource equipmentSupplier = supplier(
                model,
                "ClinicalEquipmentSupplier",
                "EquipmentSupplier",
                "Clinical equipment supplier",
                "SUP-EQ"
        );
        Resource maintenanceSupplier = supplier(
                model,
                "MaintenanceServiceSupplier",
                "ServiceSupplier",
                "Maintenance service supplier",
                "SUP-MNT"
        );
        Resource maintenanceContract = contract(
                model,
                "ManagedMaintenanceContract",
                "MaintenanceContract",
                "Managed maintenance contract",
                "MEC-001",
                maintenanceSupplier
        );
        Resource equipmentLibraryStore = location(
                model,
                "EquipmentLibraryStore",
                "EquipmentStore",
                "Equipment library store",
                "LOC-ELS"
        );
        Resource emergencyStore = location(
                model,
                "EmergencyStore",
                "DepartmentStore",
                "Emergency store",
                "LOC-EDS"
        );
        Resource criticalCareWard = location(
                model,
                "CriticalCareWard",
                "Ward",
                "Critical care ward",
                "LOC-CCW"
        );
        Resource imagingRoom = location(
                model,
                "ImagingRoom",
                "ProcedureRoom",
                "Imaging room",
                "LOC-IMG"
        );
        Resource surgeryStore = location(
                model,
                "SurgeryStore",
                "DepartmentStore",
                "Surgery store",
                "LOC-SUR"
        );
        equipmentLibraryStore.addProperty(property("servesUnit"), equipmentLibrary);
        emergencyStore.addProperty(property("servesUnit"), emergency);
        criticalCareWard.addProperty(property("servesUnit"), criticalCare);
        imagingRoom.addProperty(property("servesUnit"), imaging);
        surgeryStore.addProperty(property("servesUnit"), surgery);
        Resource infusionPumpType = equipmentType(
                model,
                "InfusionPumpType",
                "Infusion pump",
                infusionCategory,
                equipmentSupplier
        );
        Resource patientMonitorType = equipmentType(
                model,
                "PatientMonitorType",
                "Patient monitor",
                monitoringCategory,
                equipmentSupplier
        );
        Resource syringePumpType = equipmentType(
                model,
                "SyringePumpType",
                "Syringe pump",
                infusionCategory,
                equipmentSupplier
        );
        Resource ultrasoundScannerType = equipmentType(
                model,
                "UltrasoundScannerType",
                "Ultrasound scanner",
                diagnosticCategory,
                equipmentSupplier
        );
        Resource ecgMonitorType = equipmentType(
                model,
                "EcgMonitorType",
                "ECG monitor",
                monitoringCategory,
                equipmentSupplier
        );
        Resource bladderScannerType = equipmentType(
                model,
                "BladderScannerType",
                "Bladder scanner",
                diagnosticCategory,
                equipmentSupplier
        );
        equipment(
                model,
                "Equipment001",
                "InfusionEquipment",
                "Volumetric infusion pump",
                "INF-001",
                infusionPumpType,
                equipmentLibrary,
                equipmentLibraryStore,
                maintenanceContract,
                resource("Available")
        );
        Resource infusionPumpInMaintenance = equipment(
                model,
                "Equipment002",
                "InfusionEquipment",
                "Volumetric infusion pump",
                "INF-002",
                infusionPumpType,
                equipmentLibrary,
                equipmentLibraryStore,
                maintenanceContract,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment003",
                "MonitoringEquipment",
                "Patient monitor",
                "MON-001",
                patientMonitorType,
                medicine,
                criticalCareWard,
                maintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment004",
                "InfusionEquipment",
                "Syringe pump",
                "SYR-001",
                syringePumpType,
                criticalCare,
                criticalCareWard,
                maintenanceContract,
                resource("Available")
        );
        Resource syringePumpInMaintenance = equipment(
                model,
                "Equipment005",
                "InfusionEquipment",
                "Syringe pump",
                "SYR-002",
                syringePumpType,
                emergency,
                emergencyStore,
                maintenanceContract,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment006",
                "DiagnosticEquipment",
                "Ultrasound scanner",
                "US-001",
                ultrasoundScannerType,
                imaging,
                imagingRoom,
                maintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment007",
                "MonitoringEquipment",
                "ECG monitor",
                "ECG-001",
                ecgMonitorType,
                emergency,
                emergencyStore,
                maintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment008",
                "DiagnosticEquipment",
                "Bladder scanner",
                "BLD-001",
                bladderScannerType,
                equipmentLibrary,
                equipmentLibraryStore,
                maintenanceContract,
                resource("Available")
        );
        Resource patientMonitorInMaintenance = equipment(
                model,
                "Equipment009",
                "MonitoringEquipment",
                "Patient monitor",
                "MON-002",
                patientMonitorType,
                criticalCare,
                criticalCareWard,
                maintenanceContract,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment010",
                "InfusionEquipment",
                "Volumetric infusion pump",
                "INF-003",
                infusionPumpType,
                surgery,
                surgeryStore,
                maintenanceContract,
                resource("Available")
        );
        equipmentRequest(model, "EquipmentRequest001", "REQ-001", medicine, infusionPumpType);
        equipmentRequest(model, "EquipmentRequest002", "REQ-002", medicine, patientMonitorType);
        maintenanceRecord(
                model,
                "MaintenanceRecord001",
                "MNT-001",
                infusionPumpInMaintenance,
                "Battery replacement",
                "2026-08-01T09:30:00Z"
        );
        maintenanceRecord(
                model,
                "MaintenanceRecord002",
                "MNT-002",
                syringePumpInMaintenance,
                "Preventive check",
                "2026-08-02T11:15:00Z"
        );
        maintenanceRecord(
                model,
                "MaintenanceRecord003",
                "MNT-003",
                patientMonitorInMaintenance,
                "Display issue",
                "2026-08-03T14:00:00Z"
        );
        equipmentLibrary.addProperty(property("managedBy"), coordinator);
    }

    private Resource equipmentType(Model model, String id, String name, Resource category, Resource supplier) {
        return individual(model, id, "EquipmentType", name)
                .addProperty(property("belongsToCategory"), category)
                .addProperty(property("suppliedBy"), supplier);
    }

    private Resource category(Model model, String id, String type, String name, String code) {
        return individual(model, id, type, name)
                .addLiteral(property("categoryCode"), code);
    }

    private Resource location(Model model, String id, String type, String name, String code) {
        return individual(model, id, type, name)
                .addLiteral(property("locationCode"), code);
    }

    private Resource supplier(Model model, String id, String type, String name, String code) {
        return individual(model, id, type, name)
                .addLiteral(property("supplierCode"), code);
    }

    private Resource contract(
            Model model,
            String id,
            String type,
            String name,
            String contractNumber,
            Resource supplier
    ) {
        return individual(model, id, type, name)
                .addLiteral(property("contractNumber"), contractNumber)
                .addProperty(property("contractedSupplier"), supplier);
    }

    private Resource equipment(
            Model model,
            String id,
            String equipmentClass,
            String name,
            String assetNumber,
            Resource equipmentType,
            Resource assignedTo,
            Resource location,
            Resource contract,
            Resource status
    ) {
        return individual(model, id, "Equipment", name)
                .addProperty(RDF.type, resource(equipmentClass))
                .addLiteral(property("assetNumber"), assetNumber)
                .addProperty(property("hasEquipmentType"), equipmentType)
                .addProperty(property("assignedTo"), assignedTo)
                .addProperty(property("locatedIn"), location)
                .addProperty(property("coveredByContract"), contract)
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

    private Resource maintenanceRecord(
            Model model,
            String id,
            String maintenanceNumber,
            Resource equipment,
            String reason,
            String reportedAt
    ) {
        return individual(model, id, "MaintenanceRecord", maintenanceNumber)
                .addLiteral(property("maintenanceNumber"), maintenanceNumber)
                .addProperty(property("maintenanceFor"), equipment)
                .addLiteral(property("maintenanceReason"), reason)
                .addLiteral(property("reportedAt"), model.createTypedLiteral(reportedAt, XSDDatatype.XSDdateTime));
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
