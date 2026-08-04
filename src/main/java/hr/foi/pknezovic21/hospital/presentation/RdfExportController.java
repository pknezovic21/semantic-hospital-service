package hr.foi.pknezovic21.hospital.presentation;

import hr.foi.pknezovic21.hospital.semantic.api.HospitalRdfExporter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rdf")
public class RdfExportController {

    private final HospitalRdfExporter rdfExporter;

    public RdfExportController(HospitalRdfExporter rdfExporter) {
        this.rdfExporter = rdfExporter;
    }

    @GetMapping
    public ResponseEntity<String> rdf() {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/turtle;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"hospital.ttl\"")
                .body(rdfExporter.exportTurtle());
    }
}
