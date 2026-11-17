package br.marellar.api.inspecao;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;

import java.time.Instant;

/**
 * Nao conformidade registrada pelo inspetor no modelo 3D do barco.
 * {@code componente} detalha a parte clicada dentro da peca (ex.: Vidros, Hardtop, Guarda-mancebo).
 * A posicao (x, y, z) e em metros, no sistema da peca: x da popa para a proa, y para cima, z para boreste.
 */
public record NcInspecao(
        String id,
        int numero,
        int linha,
        Peca peca,
        Posicao posicao,
        int baliza,
        Bordo bordo,
        String zona,
        String componente,
        CodigoGate gate,
        String codigoDefeito,
        String descricaoDefeito,
        Gravidade gravidade,
        String observacao,
        Status status,
        Instant registradaEm,
        Instant editadaEm,
        Instant fechadaEm) {

    public enum Peca { CASCO, CONVES, CASARIA, INTERNOS, ACABAMENTO }

    /** BB = bombordo, BE = boreste, LC = linha de centro. */
    public enum Bordo { BB, BE, LC }

    public enum Status { ABERTA, FECHADA }

    public record Posicao(double x, double y, double z) {}

    NcInspecao fechar(Instant quando) {
        return new NcInspecao(id, numero, linha, peca, posicao, baliza, bordo, zona, componente, gate, codigoDefeito,
                descricaoDefeito, gravidade, observacao, Status.FECHADA, registradaEm, editadaEm, quando);
    }
}
