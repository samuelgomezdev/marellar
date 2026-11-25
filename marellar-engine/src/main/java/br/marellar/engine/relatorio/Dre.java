package br.marellar.engine.relatorio;

import java.math.BigDecimal;

/**
 * DRE simples do mes (regime de competencia).
 * EBITDA = receita - material - consumiveis - material de retrabalho - pessoal - despesas fixas
 * + chargeback recuperado - multas por atraso - garantia (NCs que escaparam e voltaram).
 */
public record Dre(
        BigDecimal receita,
        BigDecimal custoMaterial,
        BigDecimal consumiveis,
        BigDecimal materialRetrabalho,
        BigDecimal pessoal,
        BigDecimal despesasFixas,
        BigDecimal chargebackRecuperado,
        BigDecimal multas,
        BigDecimal garantia,
        BigDecimal ebitda) {

    public static Dre calcular(BigDecimal receita, BigDecimal custoMaterial, BigDecimal consumiveis,
                               BigDecimal materialRetrabalho, BigDecimal pessoal, BigDecimal despesasFixas,
                               BigDecimal chargebackRecuperado, BigDecimal multas,
                               BigDecimal garantia) {
        BigDecimal ebitda = receita.subtract(custoMaterial).subtract(consumiveis).subtract(materialRetrabalho)
                .subtract(pessoal).subtract(despesasFixas).add(chargebackRecuperado).subtract(multas)
                .subtract(garantia);
        return new Dre(receita, custoMaterial, consumiveis, materialRetrabalho, pessoal, despesasFixas,
                chargebackRecuperado, multas, garantia, ebitda);
    }
}
