package br.com.sicomat.api;

import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/csrf")
public class CsrfController {

    @GetMapping
    public ResponseEntity<Map<String, String>> token(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken()));
    }
}
