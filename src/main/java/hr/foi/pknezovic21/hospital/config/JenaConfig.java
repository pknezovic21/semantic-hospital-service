package hr.foi.pknezovic21.hospital.config;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeBase;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JenaConfig {

    private static final Logger log = LoggerFactory.getLogger(JenaConfig.class);

    @Bean
    public Dataset rdfDataset() {
        return DatasetFactory.createTxnMem();
    }

    @Bean
    public ApplicationRunner knowledgeBaseApplicationRunner(HospitalKnowledgeBase knowledgeBase) {
        return args -> {
            knowledgeBase.initialize();
            log.info("Graph initialized.");
        };
    }
}
