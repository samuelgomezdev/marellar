package br.marellar.engine.dominio;

/** Velocidade escolhida pelo jogador. O cliente usa o multiplicador para chamar o avancar. */
public enum Velocidade {
    PAUSA(0), X1(1), X2(2), X4(4);

    private final int multiplicador;

    Velocidade(int multiplicador) {
        this.multiplicador = multiplicador;
    }

    public int multiplicador() {
        return multiplicador;
    }
}
