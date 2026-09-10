package hr.foi.pknezovic21.hospital.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/equipment")
    public String equipment() {
        return "equipment";
    }

    @GetMapping("/requests")
    public String requests() {
        return "requests";
    }

    @GetMapping("/maintenance")
    public String maintenance() {
        return "maintenance";
    }
}
