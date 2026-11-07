package br.marellar.api;

import br.marellar.engine.ordens.OrdemInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros no formato application/problem+json (RFC 9457). */
@RestControllerAdvice
public class TratamentoDeErros {

    @ExceptionHandler(PartidaService.PartidaNaoEncontradaException.class)
    public ProblemDetail partidaNaoEncontrada(PartidaService.PartidaNaoEncontradaException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        p.setTitle("Partida nao encontrada");
        return p;
    }

    @ExceptionHandler(OrdemInvalidaException.class)
    public ProblemDetail ordemInvalida(OrdemInvalidaException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        p.setTitle("Ordem invalida");
        return p;
    }
}
