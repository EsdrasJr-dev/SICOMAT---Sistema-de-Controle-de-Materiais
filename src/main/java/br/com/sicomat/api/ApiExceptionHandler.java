package br.com.sicomat.api;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> tratarEntradaInvalida(IllegalArgumentException erro) {
        return resposta(HttpStatus.BAD_REQUEST, erro.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> tratarEstadoInvalido(IllegalStateException erro) {
        return resposta(HttpStatus.CONFLICT, erro.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarConflitoDeDados(DataIntegrityViolationException erro) {
        return resposta(HttpStatus.CONFLICT, "O registro viola uma regra de unicidade ou relacionamento.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(MethodArgumentNotValidException erro) {
        String detalhe = erro.getBindingResult().getFieldErrors().stream()
                .map(field -> field.getField() + ": " + field.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return resposta(HttpStatus.BAD_REQUEST, detalhe);
    }

    private static ResponseEntity<ProblemDetail> resposta(HttpStatus status, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(problema);
    }
}