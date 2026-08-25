package hr.foi.pknezovic21.hospital.config;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalKnowledgeBase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.text.EntityDefinition;
import org.apache.jena.query.text.TextDatasetFactory;
import org.apache.jena.query.text.TextIndexConfig;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.tdb2.TDB2Factory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
    public Dataset rdfDataset(
            @Value("${hospital.rdf.storage-path}") String storagePath,
            @Value("${hospital.rdf.text-index-path}") String textIndexPath,
            @Value("${hospital.rdf.base-uri}") String baseUri
    ) throws Exception {
        Path storage = Path.of(storagePath);
        Path textIndex = Path.of(textIndexPath);
        Files.createDirectories(storage);
        Files.createDirectories(textIndex);

        Dataset dataset = TDB2Factory.connectDataset(storage.toString());
        Directory directory = FSDirectory.open(textIndex);
        EntityDefinition definition = new EntityDefinition(
                "uri",
                "text",
                ResourceFactory.createProperty(baseUri + "name")
        );
        return TextDatasetFactory.createLucene(dataset, directory, new TextIndexConfig(definition));
    }

    @Bean
    public ApplicationRunner knowledgeBaseApplicationRunner(HospitalKnowledgeBase knowledgeBase) {
        return args -> {
            knowledgeBase.initialize();
            log.info("Graph initialized.");
        };
    }
}
