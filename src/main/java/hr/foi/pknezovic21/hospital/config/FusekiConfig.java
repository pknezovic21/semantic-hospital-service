package hr.foi.pknezovic21.hospital.config;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "hospital.fuseki.enabled", havingValue = "true", matchIfMissing = true)
public class FusekiConfig {

    @Bean(destroyMethod = "stop")
    public FusekiServer fusekiServer(
            Dataset dataset,
            @Value("${hospital.fuseki.port}") int port,
            @Value("${hospital.fuseki.dataset-path}") String datasetPath,
            @Value("${hospital.fuseki.allow-update:false}") boolean allowUpdate
    ) {
        FusekiServer server = FusekiServer.create()
                .port(port)
                .add(datasetPath, dataset, allowUpdate)
                .build();
        server.start();
        return server;
    }
}
