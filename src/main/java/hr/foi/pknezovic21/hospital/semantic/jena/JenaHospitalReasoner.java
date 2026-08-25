package hr.foi.pknezovic21.hospital.semantic.jena;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalReasoner {

    private final Reasoner reasoner;
    private final String purchaseNeededQuery;
    private final String equipmentShortageQuery;

    public JenaHospitalReasoner(@Value("${hospital.rdf.base-uri}") String baseUri) {
        String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String rulesText = """
                [availableCandidate:
                  (?request <%2$s> <%1$sEquipmentRequest>)
                  (?request <%1$shasRequestStatus> <%1$sOpen>)
                  (?request <%1$srequestsType> ?type)
                  (?request <%1$srequestedFor> ?requestedFor)
                  (?equipment <%1$shasEquipmentType> ?type)
                  (?equipment <%1$shasEquipmentStatus> <%1$sAvailable>)
                  (?equipment <%1$sassignedTo> ?assignedTo)
                  notEqual(?assignedTo, ?requestedFor)
                  ->
                  (?request <%1$savailableCandidate> ?equipment)
                ]
                [highRiskEquipment:
                  (?firstRecord <%2$s> <%1$sMaintenanceRecord>)
                  (?secondRecord <%2$s> <%1$sMaintenanceRecord>)
                  (?firstRecord <%1$smaintenanceFor> ?equipment)
                  (?secondRecord <%1$smaintenanceFor> ?equipment)
                  notEqual(?firstRecord, ?secondRecord)
                  ->
                  (?equipment <%2$s> <%1$sHighRiskEquipment>)
                ]
                [highPriorityEmergencyRequest:
                  (?request <%2$s> <%1$sEquipmentRequest>)
                  (?request <%1$srequestedFor> <%1$sEmergencyDepartment>)
                  ->
                  (?request <%2$s> <%1$sHighPriorityRequest>)
                ]
                [highPriorityCriticalCareRequest:
                  (?request <%2$s> <%1$sEquipmentRequest>)
                  (?request <%1$srequestedFor> <%1$sCriticalCareDepartment>)
                  ->
                  (?request <%2$s> <%1$sHighPriorityRequest>)
                ]
                [highPriorityEmergencyMaintenance:
                  (?record <%2$s> <%1$sMaintenanceRecord>)
                  (?record <%1$smaintenanceReportedFor> <%1$sEmergencyDepartment>)
                  ->
                  (?record <%2$s> <%1$sHighPriorityMaintenanceRecord>)
                ]
                [highPriorityCriticalCareMaintenance:
                  (?record <%2$s> <%1$sMaintenanceRecord>)
                  (?record <%1$smaintenanceReportedFor> <%1$sCriticalCareDepartment>)
                  ->
                  (?record <%2$s> <%1$sHighPriorityMaintenanceRecord>)
                ]
                """.formatted(baseUri, rdfType);
        GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(Rule.parseRules(rulesText));
        ruleReasoner.setMode(GenericRuleReasoner.FORWARD_RETE);
        this.reasoner = ruleReasoner;
        this.purchaseNeededQuery = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                CONSTRUCT {
                  ?request rdf:type hospital:PurchaseNeededRequest .
                }
                WHERE {
                  ?request rdf:type hospital:EquipmentRequest ;
                           hospital:hasRequestStatus hospital:Open .
                  FILTER NOT EXISTS { ?request hospital:availableCandidate ?candidate . }
                }
                """.formatted(baseUri);
        this.equipmentShortageQuery = """
                PREFIX hospital: <%s>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

                CONSTRUCT {
                  ?unit rdf:type hospital:EquipmentShortageUnit .
                }
                WHERE {
                  ?request hospital:requestedFor ?unit .
                  {
                    ?request rdf:type hospital:PurchaseNeededRequest .
                  }
                  UNION
                  {
                    ?request rdf:type hospital:EquipmentRequest ;
                             hospital:hasRequestStatus hospital:PurchasePending .
                  }
                }
                """.formatted(baseUri);
    }

    public Model create(Model baseModel) {
        InfModel rdfsModel = ModelFactory.createRDFSModel(baseModel);
        InfModel inferenceModel = ModelFactory.createInfModel(reasoner, rdfsModel);
        inferenceModel.prepare();
        Model purchaseNeededModel;
        try (QueryExecution execution = QueryExecution.model(inferenceModel).query(purchaseNeededQuery).build()) {
            purchaseNeededModel = execution.execConstruct();
        }
        Model requestInferenceModel = ModelFactory.createUnion(inferenceModel, purchaseNeededModel);
        Model equipmentShortageModel;
        try (QueryExecution execution = QueryExecution.model(requestInferenceModel).query(equipmentShortageQuery).build()) {
            equipmentShortageModel = execution.execConstruct();
        }
        return ModelFactory.createUnion(requestInferenceModel, equipmentShortageModel);
    }
}
