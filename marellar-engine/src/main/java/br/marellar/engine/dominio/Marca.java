package br.marellar.engine.dominio;

/**
 * Marca que o jogador ve na rodada de inspecao. {@code ncId} nulo = marca falsa (sujeira, reflexo...).
 * O cliente recebe so id, ponto e aparencia: nunca sabe qual marca e NC.
 */
public record Marca(String id, String pontoId, String aparencia, String ncId) {

    public boolean falsa() {
        return ncId == null;
    }
}
