package br.marellar.engine.relatorio;

import java.math.BigDecimal;

/**
 * DRE simples do mes (regime de competencia).
 * EBITDA = receita - material - consumiveis - material de retrabalho - pessoal - despesas fixas + chargeback recuperado.
 */
public record Dre(
        BigDecimal receita,
        BigDecimal custoMaterial,
        BigDecimal consumiveis,
        BigDecimal materialRetrabalho,
        BigDecimal pessoal,
        BigDecimal despesasFixas,
        BigDecimal chargebackRecuperado,
        BigDecimal ebitda) {

    public static Dre calcular(BigDecimal receita, BigDecimal custoMaterial, BigDecimal consumiveis,
                               BigDecimal materialRetrabalho, BigDecimal pessoal, BigDecimal despesasFixas,
                               BigDecimal chargebackRecuperado) {
        BigDecimal ebitda = receita.subtract(custoMaterial).subtract(consumiveis).subtract(materialRetrabalho)
                .subtract(pessoal).subtract(despesasFixas).add(chargebackRecuperado);
        return new Dre(receita, custoMaterial, consumiveis, materialRetrabalho, pessoal, despesasFixas,
                chargebackRecuperado, ebitda);
    }
}
