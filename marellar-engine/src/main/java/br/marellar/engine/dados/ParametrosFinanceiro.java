package br.marellar.engine.dados;

import java.math.BigDecimal;

/** Caixa inicial, despesas fixas e valuation (arquivo data/financeiro.json). */
public record ParametrosFinanceiro(
        BigDecimal caixaInicial,
        BigDecimal despesasFixasMensais,
        BigDecimal multiploValuation,
        BigDecimal descontoPorAcidenteComAfastamento,
        BigDecimal descontoPorNcEscapada,
        int mesesCaixaNegativoParaDerrota) {}
