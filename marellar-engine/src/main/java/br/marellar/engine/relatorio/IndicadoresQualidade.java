package br.marellar.engine.relatorio;

import java.math.BigDecimal;

/**
 * Indicadores completos de qualidade, incluindo o que o jogador nao ve (NCs escapadas).
 * Usado pela CLI, pelos testes e, no M5, pelo conselho e pelo valor da empresa.
 */
public record IndicadoresQualidade(
        int ncsGeradas,
        int ncsDetectadas,
        int ncsEscapadas,
        int ncsAbertas,
        int concessoes,
        int rncs,
        BigDecimal hhRetrabalho,
        BigDecimal custoMaterialRetrabalho) {}
