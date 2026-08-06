package hr.foi.pknezovic21.hospital.semantic.jena;

import java.util.List;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JenaHospitalReasoner {

    private final Reasoner reasoner;

    public JenaHospitalReasoner(@Value("${hospital.rdf.base-uri}") String baseUri) {
        String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        String rulesText = """
                [availableCandidate:
                  (?request <%2$s> <%1$sEquipmentRequest>)
                  (?request <%1$srequestsType> ?type)
                  (?equipment <%1$shasEquipmentType> ?type)
                  (?equipment <%1$shasEquipmentStatus> <%1$sAvailable>)
                  ->
                  (?request <%1$savailableCandidate> ?equipment)
                ]
                """.formatted(baseUri, rdfType);
        GenericRuleReasoner ruleReasoner = new GenericRuleReasoner(Rule.parseRules(rulesText));
        ruleReasoner.setMode(GenericRuleReasoner.FORWARD_RETE);
        this.reasoner = ruleReasoner;
    }

    public InfModel create(Model baseModel) {
        InfModel rdfsModel = ModelFactory.createRDFSModel(baseModel);
        InfModel inferenceModel = ModelFactory.createInfModel(reasoner, rdfsModel);
        inferenceModel.prepare();
        return inferenceModel;
    }
}
