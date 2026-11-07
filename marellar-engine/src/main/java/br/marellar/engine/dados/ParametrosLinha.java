package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Linha;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Parametros de uma linha de produto (arquivo data/linha-33.json). */
public record ParametrosLinha(
        Linha linha,
        String nome,
        BigDecimal hhPorBarco,
        BigDecimal custoHh,
        BigDecimal materialPorBarco,
        BigDecimal precoVenda,
        BigDecimal percentualConsumiveis,
        BigDecimal percentualImportados,
        int prazoRecebimentoDias,
        BigDecimal cotaContratoPercentual,
        int maxPessoasPorBarco,
        int ticksInspecao,
        List<Gate> gates,
        List<BarcoInicial> barcosIniciais) {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public ParametrosLinha {
        gates = gates == null ? List.of() : List.copyOf(gates);
        barcosIniciais = barcosIniciais == null ? List.of() : List.copyOf(barcosIniciais);
    }

    public record Gate(CodigoGate codigo, String nome, BigDecimal pesoHhPercentual, BigDecimal cotaPercentual) {}

    public record BarcoInicial(String id, String cliente, CodigoGate gateInicial) {}

    public Gate gate(CodigoGate codigo) {
        return gates.stream()
                .filter(g -> g.codigo() == codigo)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Gate sem parametro: " + codigo));
    }

    /** HH padrao de um gate: hhPorBarco x peso / 100. */
    public BigDecimal hhDoGate(CodigoGate codigo) {
        return hhPorBarco.multiply(gate(codigo).pesoHhPercentual())
                .divide(CEM, 4, RoundingMode.HALF_EVEN);
    }

    public ParametrosLinha comBarcosIniciais(List<BarcoInicial> novos) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticksInspecao, gates, novos);
    }

    public ParametrosLinha comGates(List<Gate> novos) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticksInspecao, novos, barcosIniciais);
    }
}
