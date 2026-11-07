package br.marellar.engine.ordens;

/** A ordem nao pode ser aplicada no estado atual (barco inexistente, posicao fora da fila etc.). */
public class OrdemInvalidaException extends IllegalArgumentException {

    public OrdemInvalidaException(String mensagem) {
        super(mensagem);
    }
}
