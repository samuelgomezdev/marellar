package br.marellar.engine.dados;

import java.util.List;

/** Erro claro quando um arquivo de data/ esta mal formado ou fora das regras. */
public class DadosInvalidosException extends RuntimeException {

    private final List<String> erros;

    public DadosInvalidosException(List<String> erros) {
        super("Parametros invalidos em data/:\n - " + String.join("\n - ", erros));
        this.erros = List.copyOf(erros);
    }

    public DadosInvalidosException(String erro, Throwable causa) {
        super("Parametros invalidos em data/:\n - " + erro, causa);
        this.erros = List.of(erro);
    }

    public List<String> erros() {
        return erros;
    }
}
