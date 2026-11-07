package br.marellar.engine.dados;

import java.math.BigDecimal;

/** Probabilidades e efeitos dos eventos do MVP (arquivo data/eventos.json). */
public record ParametrosEventos(QuebraPeca quebraPeca, Acidente acidente, Demissao demissao) {

    public record QuebraPeca(BigDecimal probabilidadePorGate, BigDecimal multiplicadorPressa,
                             BigDecimal multiplicadorMoralBaixa, BigDecimal hhRetrabalho,
                             BigDecimal custoMaterial) {}

    public record Acidente(BigDecimal probabilidadePorHh, BigDecimal multiplicadorTreinoVencido,
                           BigDecimal multiplicadorSobrecarga, int diasPerdidosMin, int diasPerdidosMax,
                           int quedaMoralEquipe) {}

    public record Demissao(BigDecimal probabilidadeDiariaBase, BigDecimal probabilidadeDiariaMoralBaixa,
                           int limiarMoralBaixa) {}
}
