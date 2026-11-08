package br.marellar.engine.dados;

import java.math.BigDecimal;
import java.util.Map;

/** Caixa, folha, valuation, metas do conselho e custo das decisoes (arquivo data/financeiro.json). */
public record ParametrosFinanceiro(
        BigDecimal caixaInicial,
        BigDecimal despesasFixasMensais,
        BigDecimal encargosSobreSalarios,
        BigDecimal multiploValuation,
        BigDecimal descontoPorAcidenteComAfastamento,
        BigDecimal descontoPorNcEscapada,
        int mesesCaixaNegativoParaDerrota,
        Conselho conselho,
        Map<String, BigDecimal> decisoes,
        int kitsAntecipados,
        int ganhoMoralEncontro) {

    public ParametrosFinanceiro {
        decisoes = decisoes == null ? Map.of() : Map.copyOf(decisoes);
    }

    public record Conselho(int pacienciaInicial, int pacienciaMaxima, int metaCascosDm,
                           BigDecimal metaCaixaMinimo, int metaNcPorBarcoMaxima, BigDecimal fatorMaterialDemais) {}

    public ParametrosFinanceiro comCaixaInicial(BigDecimal caixa) {
        return new ParametrosFinanceiro(caixa, despesasFixasMensais, encargosSobreSalarios, multiploValuation,
                descontoPorAcidenteComAfastamento, descontoPorNcEscapada, mesesCaixaNegativoParaDerrota,
                conselho, decisoes, kitsAntecipados, ganhoMoralEncontro);
    }

    public ParametrosFinanceiro comDespesasFixas(BigDecimal despesas) {
        return new ParametrosFinanceiro(caixaInicial, despesas, encargosSobreSalarios, multiploValuation,
                descontoPorAcidenteComAfastamento, descontoPorNcEscapada, mesesCaixaNegativoParaDerrota,
                conselho, decisoes, kitsAntecipados, ganhoMoralEncontro);
    }

    public ParametrosFinanceiro comConselho(Conselho novo) {
        return new ParametrosFinanceiro(caixaInicial, despesasFixasMensais, encargosSobreSalarios, multiploValuation,
                descontoPorAcidenteComAfastamento, descontoPorNcEscapada, mesesCaixaNegativoParaDerrota,
                novo, decisoes, kitsAntecipados, ganhoMoralEncontro);
    }
}
