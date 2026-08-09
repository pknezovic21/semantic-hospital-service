package hr.foi.pknezovic21.hospital.config;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeBase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.jena.query.Dataset;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JenaConfig {

    private static final Logger log = LoggerFactory.getLogger(JenaConfig.class);

    @Bean(destroyMethod = "close")
    public Dataset rdfDataset(@Value("${hospital.rdf.storage-path}") String storagePath) throws Exception {
        Path path = Path.of(storagePath);
        Files.createDirectories(path);
        return TDB2Factory.connectDataset(path.toString());
    }

    @Bean
    public ApplicationRunner knowledgeBaseApplicationRunner(HospitalKnowledgeBase knowledgeBase) {
        return args -> {
            knowledgeBase.initialize();
            log.info("Graph initialized.");
        };
    }
}
