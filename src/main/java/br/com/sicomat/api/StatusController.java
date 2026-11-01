package br.com.sicomat.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/status")
public class StatusController {

    @GetMapping
    public Map<String, String> status() {
        return Map.of("aplicacao", "SICOMAT", "status", "online");
    }
}