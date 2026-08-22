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
            if (model.contains(resource("CentralHospital"), RDF.type, resource("Hospital"))) {
                return;
            }
            seed(model);
        });
    }

    private void seed(Model model) {
        Resource hospital = individual(model, "CentralHospital", "Hospital", "Central Hospital");
        Resource medicine = unit(model, hospital, "MedicineDivision", "ClinicalDivision", "Medicine");
        Resource surgery = unit(model, hospital, "SurgeryDivision", "ClinicalDivision", "Surgery");
        Resource clinicalSupport = unit(model, hospital, "ClinicalSupportDivision", "ClinicalDivision", "Clinical Support");
        Resource emergency = unit(model, medicine, "EmergencyDepartment", "Department", "Emergency Department");
        Resource emergencyCare = unit(model, emergency, "EmergencyCareUnit", "ClinicalServiceUnit", "Emergency Care Unit");
        Resource criticalCareDepartment = unit(
                model,
                medicine,
                "CriticalCareDepartment",
                "Department",
                "Critical Care Department"
        );
        Resource criticalCare = unit(
                model,
                criticalCareDepartment,
                "CriticalCareUnit",
                "ClinicalServiceUnit",
                "Critical Care Unit"
        );
        Resource cardiology = unit(model, medicine, "CardiologyDepartment", "Department", "Cardiology Department");
        Resource cardiacCare = unit(model, cardiology, "CardiacCareUnit", "ClinicalServiceUnit", "Cardiac Care Unit");
        Resource surgeryDepartment = unit(model, surgery, "SurgeryDepartment", "Department", "Surgery Department");
        Resource surgeryCare = unit(model, surgeryDepartment, "SurgeryCareUnit", "ClinicalServiceUnit", "Surgery Care Unit");
        Resource orthopaedics = unit(model, surgery, "OrthopaedicsDepartment", "Department", "Orthopaedics Department");
        Resource orthopaedicCare = unit(
                model,
                orthopaedics,
                "OrthopaedicCareUnit",
                "ClinicalServiceUnit",
                "Orthopaedic Care Unit"
        );
        Resource imaging = unit(model, clinicalSupport, "ImagingDepartment", "Department", "Imaging Department");
        Resource ultrasound = unit(model, imaging, "UltrasoundUnit", "ClinicalServiceUnit", "Ultrasound Unit");
        Resource rehabilitation = unit(
                model,
                clinicalSupport,
                "RehabilitationDepartment",
                "Department",
                "Rehabilitation Department"
        );
        Resource mobilityCare = unit(
                model,
                rehabilitation,
                "MobilityCareUnit",
                "ClinicalServiceUnit",
                "Mobility Care Unit"
        );
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
        Resource mobilityCategory = category(
                model,
                "MobilityEquipmentCategory",
                "MobilityCategory",
                "Mobility equipment",
                "CAT-MOB"
        );
        Resource respiratoryCategory = category(
                model,
                "RespiratoryEquipmentCategory",
                "RespiratoryCategory",
                "Respiratory equipment",
                "CAT-RES"
        );
        Resource emergencyCategory = category(
                model,
                "EmergencyEquipmentCategory",
                "EmergencyCategory",
                "Emergency equipment",
                "CAT-EMG"
        );
        Resource infusionSupplier = supplier(
                model,
                "InfusionEquipmentSupplier",
                "EquipmentSupplier",
                "Infusion Equipment Supply",
                "SUP-INF"
        );
        Resource monitoringSupplier = supplier(
                model,
                "MonitoringEquipmentSupplier",
                "EquipmentSupplier",
                "Monitoring Equipment Supply",
                "SUP-MON"
        );
        Resource diagnosticSupplier = supplier(
                model,
                "DiagnosticEquipmentSupplier",
                "EquipmentSupplier",
                "Diagnostic Equipment Supply",
                "SUP-DIA"
        );
        Resource generalSupplier = supplier(
                model,
                "GeneralEquipmentSupplier",
                "EquipmentSupplier",
                "General Equipment Supply",
                "SUP-GEN"
        );
        Resource generalMaintenanceSupplier = supplier(
                model,
                "GeneralMaintenanceSupplier",
                "ServiceSupplier",
                "General Maintenance Service",
                "SUP-MNT"
        );
        Resource diagnosticMaintenanceSupplier = supplier(
                model,
                "DiagnosticMaintenanceSupplier",
                "ServiceSupplier",
                "Diagnostic Maintenance Service",
                "SUP-DMT"
        );
        Resource generalMaintenanceContract = contract(
                model,
                "GeneralMaintenanceContract",
                "MaintenanceContract",
                "General maintenance contract",
                "MEC-GEN-001",
                generalMaintenanceSupplier
        );
        Resource diagnosticMaintenanceContract = contract(
                model,
                "DiagnosticMaintenanceContract",
                "MaintenanceContract",
                "Diagnostic maintenance contract",
                "MEC-DIA-001",
                diagnosticMaintenanceSupplier
        );
        Resource equipmentLibraryStore = location(
                model,
                "EquipmentLibraryStore",
                "EquipmentStore",
                "Equipment library store",
                "LOC-ELS"
        );
        Resource emergencyRoom = location(
                model,
                "EmergencyRoom",
                "ProcedureRoom",
                "Emergency room",
                "LOC-EMR"
        );
        Resource criticalCareWard = location(
                model,
                "CriticalCareWard",
                "Ward",
                "Critical care ward",
                "LOC-CCW"
        );
        Resource cardiacWard = location(
                model,
                "CardiacWard",
                "Ward",
                "Cardiac ward",
                "LOC-CDW"
        );
        Resource cardiacRoom = location(
                model,
                "CardiacRoom",
                "ProcedureRoom",
                "Cardiac room",
                "LOC-CDR"
        );
        Resource imagingRoom = location(
                model,
                "ImagingRoom",
                "ProcedureRoom",
                "Imaging room",
                "LOC-IMG"
        );
        Resource surgeryRoom = location(
                model,
                "SurgeryRoom",
                "ProcedureRoom",
                "Surgery room",
                "LOC-SUR"
        );
        Resource orthopaedicWard = location(
                model,
                "OrthopaedicWard",
                "Ward",
                "Orthopaedic ward",
                "LOC-ORW"
        );
        Resource mobilityRoom = location(
                model,
                "MobilityRoom",
                "ProcedureRoom",
                "Mobility room",
                "LOC-MOB"
        );
        equipmentLibraryStore.addProperty(property("servesUnit"), equipmentLibrary);
        emergencyRoom.addProperty(property("servesUnit"), emergencyCare);
        criticalCareWard.addProperty(property("servesUnit"), criticalCare);
        cardiacWard.addProperty(property("servesUnit"), cardiacCare);
        cardiacRoom.addProperty(property("servesUnit"), cardiacCare);
        imagingRoom.addProperty(property("servesUnit"), ultrasound);
        surgeryRoom.addProperty(property("servesUnit"), surgeryCare);
        orthopaedicWard.addProperty(property("servesUnit"), orthopaedicCare);
        mobilityRoom.addProperty(property("servesUnit"), mobilityCare);
        Resource infusionPumpType = equipmentType(
                model,
                "InfusionPumpType",
                "Infusion pump",
                infusionCategory,
                infusionSupplier,
                generalSupplier
        );
        Resource patientMonitorType = equipmentType(
                model,
                "PatientMonitorType",
                "Patient monitor",
                monitoringCategory,
                monitoringSupplier,
                generalSupplier
        );
        Resource syringePumpType = equipmentType(
                model,
                "SyringePumpType",
                "Syringe pump",
                infusionCategory,
                infusionSupplier,
                generalSupplier
        );
        Resource ultrasoundScannerType = equipmentType(
                model,
                "UltrasoundScannerType",
                "Ultrasound scanner",
                diagnosticCategory,
                diagnosticSupplier,
                generalSupplier
        );
        Resource ecgMonitorType = equipmentType(
                model,
                "EcgMonitorType",
                "ECG monitor",
                monitoringCategory,
                monitoringSupplier,
                generalSupplier
        );
        Resource bladderScannerType = equipmentType(
                model,
                "BladderScannerType",
                "Bladder scanner",
                diagnosticCategory,
                diagnosticSupplier,
                generalSupplier
        );
        Resource defibrillatorType = equipmentType(
                model,
                "DefibrillatorType",
                "Defibrillator",
                emergencyCategory,
                monitoringSupplier,
                generalSupplier
        );
        Resource pulseOximeterType = equipmentType(
                model,
                "PulseOximeterType",
                "Pulse oximeter",
                monitoringCategory,
                monitoringSupplier,
                generalSupplier
        );
        Resource ventilatorType = equipmentType(
                model,
                "VentilatorType",
                "Ventilator",
                respiratoryCategory,
                generalSupplier,
                monitoringSupplier
        );
        Resource wheelchairType = equipmentType(
                model,
                "WheelchairType",
                "Wheelchair",
                mobilityCategory,
                generalSupplier
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
                infusionSupplier,
                generalMaintenanceContract,
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
                infusionSupplier,
                generalMaintenanceContract,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment003",
                "MonitoringEquipment",
                "Patient monitor",
                "MON-001",
                patientMonitorType,
                criticalCare,
                criticalCareWard,
                monitoringSupplier,
                generalMaintenanceContract,
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
                infusionSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        Resource syringePumpInMaintenance = equipment(
                model,
                "Equipment005",
                "InfusionEquipment",
                "Syringe pump",
                "SYR-002",
                syringePumpType,
                emergencyCare,
                emergencyRoom,
                infusionSupplier,
                generalMaintenanceContract,
                resource("InMaintenance")
        );
        Resource ultrasoundScanner = equipment(
                model,
                "Equipment006",
                "DiagnosticEquipment",
                "Ultrasound scanner",
                "US-001",
                ultrasoundScannerType,
                ultrasound,
                imagingRoom,
                diagnosticSupplier,
                diagnosticMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment007",
                "MonitoringEquipment",
                "ECG monitor",
                "ECG-001",
                ecgMonitorType,
                cardiacCare,
                cardiacWard,
                monitoringSupplier,
                generalMaintenanceContract,
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
                diagnosticSupplier,
                diagnosticMaintenanceContract,
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
                monitoringSupplier,
                generalMaintenanceContract,
                resource("InMaintenance")
        );
        equipment(
                model,
                "Equipment010",
                "InfusionEquipment",
                "Volumetric infusion pump",
                "INF-003",
                infusionPumpType,
                surgeryCare,
                surgeryRoom,
                infusionSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment011",
                "ResuscitationEquipment",
                "Defibrillator",
                "DEF-001",
                defibrillatorType,
                emergencyCare,
                emergencyRoom,
                monitoringSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment012",
                "MonitoringEquipment",
                "Pulse oximeter",
                "OXI-001",
                pulseOximeterType,
                cardiacCare,
                cardiacWard,
                monitoringSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment013",
                "RespiratoryEquipment",
                "Ventilator",
                "VEN-001",
                ventilatorType,
                criticalCare,
                criticalCareWard,
                generalSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment014",
                "MobilityEquipment",
                "Wheelchair",
                "MOB-001",
                wheelchairType,
                mobilityCare,
                mobilityRoom,
                generalSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipment(
                model,
                "Equipment015",
                "MonitoringEquipment",
                "Patient monitor",
                "MON-003",
                patientMonitorType,
                orthopaedicCare,
                orthopaedicWard,
                monitoringSupplier,
                generalMaintenanceContract,
                resource("Available")
        );
        equipmentRequest(
                model,
                "EquipmentRequest001",
                "REQ-001",
                emergencyCare,
                infusionPumpType,
                "Additional infusion capacity is required.",
                "2026-08-10T08:30:00Z"
        );
        equipmentRequest(
                model,
                "EquipmentRequest002",
                "REQ-002",
                criticalCare,
                patientMonitorType,
                "Additional patient monitoring capacity is required.",
                "2026-08-11T09:15:00Z"
        );
        equipmentRequest(
                model,
                "EquipmentRequest003",
                "REQ-003",
                mobilityCare,
                wheelchairType,
                "An additional wheelchair is required for patient mobility.",
                "2026-08-12T10:00:00Z"
        );
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
        maintenanceRecord(
                model,
                "MaintenanceRecord004",
                "MNT-004",
                ultrasoundScanner,
                "Probe replacement",
                "2026-06-10T08:00:00Z"
        ).addLiteral(property("completedAt"), model.createTypedLiteral("2026-06-11T12:00:00Z", XSDDatatype.XSDdateTime));
        maintenanceRecord(
                model,
                "MaintenanceRecord005",
                "MNT-005",
                ultrasoundScanner,
                "Image quality check",
                "2026-07-15T09:00:00Z"
        ).addLiteral(property("completedAt"), model.createTypedLiteral("2026-07-15T15:00:00Z", XSDDatatype.XSDdateTime));
        equipmentLibrary.addProperty(property("managedBy"), coordinator);
    }

    private Resource equipmentType(Model model, String id, String name, Resource category, Resource... suppliers) {
        Resource equipmentType = individual(model, id, "EquipmentType", name)
                .addProperty(property("belongsToCategory"), category);
        for (Resource supplier : suppliers) {
            equipmentType.addProperty(property("suppliedBy"), supplier);
        }
        return equipmentType;
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
            Resource supplier,
            Resource contract,
            Resource status
    ) {
        return individual(model, id, "Equipment", name)
                .addProperty(RDF.type, resource(equipmentClass))
                .addLiteral(property("assetNumber"), assetNumber)
                .addProperty(property("hasEquipmentType"), equipmentType)
                .addProperty(property("assignedTo"), assignedTo)
                .addProperty(property("locatedIn"), location)
                .addProperty(property("providedBy"), supplier)
                .addProperty(property("coveredByContract"), contract)
                .addProperty(property("hasEquipmentStatus"), status);
    }

    private Resource equipmentRequest(
            Model model,
            String id,
            String requestNumber,
            Resource requestedFor,
            Resource requestsType,
            String reason,
            String requestedAt
    ) {
        return individual(model, id, "EquipmentRequest", requestNumber)
                .addLiteral(property("requestNumber"), requestNumber)
                .addLiteral(property("requestReason"), reason)
                .addLiteral(property("requestedAt"), model.createTypedLiteral(requestedAt, XSDDatatype.XSDdateTime))
                .addProperty(property("requestedFor"), requestedFor)
                .addProperty(property("requestsType"), requestsType)
                .addProperty(property("hasRequestStatus"), resource("Open"));
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
                .addProperty(property("maintenanceReportedFor"), equipment.getProperty(property("assignedTo")).getResource())
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
