package br.marellar.engine.relatorio;

import br.marellar.engine.dados.ParametrosFinanceiro;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Valor da empresa (README 5.8): EBITDA do mes x 12 x multiplo, com desconto de 5% por acidente
 * com afastamento e 3% por NC escapada no mes. Nunca abaixo de zero.
 */
public record Valuation(BigDecimal bruto, BigDecimal descontoPercentual, BigDecimal valor) {

    private static final BigDecimal DOZE = BigDecimal.valueOf(12);

    public static Valuation calcular(BigDecimal ebitdaMes, int acidentes, int ncsEscapadas, ParametrosFinanceiro regras) {
        BigDecimal bruto = ebitdaMes.multiply(DOZE).multiply(regras.multiploValuation()).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal desconto = regras.descontoPorAcidenteComAfastamento().multiply(BigDecimal.valueOf(acidentes))
                .add(regras.descontoPorNcEscapada().multiply(BigDecimal.valueOf(ncsEscapadas)))
                .min(BigDecimal.ONE);
        BigDecimal valor = bruto.multiply(BigDecimal.ONE.subtract(desconto)).setScale(2, RoundingMode.HALF_EVEN)
                .max(BigDecimal.ZERO.setScale(2));
        return new Valuation(bruto, desconto, valor);
    }
}
