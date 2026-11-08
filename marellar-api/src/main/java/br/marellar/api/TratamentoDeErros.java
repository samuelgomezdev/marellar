package br.marellar.api;

import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.save.SaveIncompativelException;
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

    @ExceptionHandler(PartidaService.SaveNaoEncontradoException.class)
    public ProblemDetail saveNaoEncontrado(PartidaService.SaveNaoEncontradoException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        p.setTitle("Save nao encontrado");
        return p;
    }

    @ExceptionHandler(SaveIncompativelException.class)
    public ProblemDetail saveIncompativel(SaveIncompativelException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        p.setTitle("Save incompativel");
        return p;
    }

    @ExceptionHandler(OrdemInvalidaException.class)
    public ProblemDetail ordemInvalida(OrdemInvalidaException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        p.setTitle("Ordem invalida");
        return p;
    }
}
