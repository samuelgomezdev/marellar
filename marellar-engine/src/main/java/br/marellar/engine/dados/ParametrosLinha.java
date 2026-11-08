package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Linha;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

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
        int estoqueInicialKits,
        int estoqueMinimoKits,
        int pecasPorKit,
        List<Gate> gates,
        List<BarcoInicial> barcosIniciais) {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    public ParametrosLinha {
        gates = gates == null ? List.of() : List.copyOf(gates);
        barcosIniciais = barcosIniciais == null ? List.of() : List.copyOf(barcosIniciais);
    }

    /**
     * Um gate da linha. {@code item} e o material que o gate consome ao comecar (1 kit por barco);
     * {@code materialPercentual} e a parte do material do barco que esse kit representa.
     */
    public record Gate(CodigoGate codigo, String nome, BigDecimal pesoHhPercentual, BigDecimal cotaPercentual,
                       String item, BigDecimal materialPercentual) {}

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

    /** Material que o gate consome ao comecar, se houver. */
    public Optional<String> itemDoGate(CodigoGate codigo) {
        return Optional.ofNullable(gate(codigo).item());
    }

    /** Itens de material usados pela linha, na ordem dos gates. */
    public List<String> itens() {
        return gates.stream().map(Gate::item).filter(i -> i != null).distinct().toList();
    }

    /** Valor de 1 kit do item, a preco de tabela: materialPorBarco x materialPercentual / 100. */
    public BigDecimal valorDoKit(String item) {
        return gates.stream()
                .filter(g -> item.equals(g.item()))
                .map(g -> materialPorBarco.multiply(g.materialPercentual()).divide(CEM, 2, RoundingMode.HALF_EVEN))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item sem gate: " + item));
    }

    public ParametrosLinha comBarcosIniciais(List<BarcoInicial> novos) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticksInspecao, estoqueInicialKits, estoqueMinimoKits, pecasPorKit, gates, novos);
    }

    public ParametrosLinha comTicksInspecao(int ticks) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticks, estoqueInicialKits, estoqueMinimoKits, pecasPorKit, gates, barcosIniciais);
    }

    public ParametrosLinha comEstoque(int inicial, int minimo) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticksInspecao, inicial, minimo, pecasPorKit, gates, barcosIniciais);
    }

    public ParametrosLinha comGates(List<Gate> novos) {
        return new ParametrosLinha(linha, nome, hhPorBarco, custoHh, materialPorBarco, precoVenda,
                percentualConsumiveis, percentualImportados, prazoRecebimentoDias, cotaContratoPercentual,
                maxPessoasPorBarco, ticksInspecao, estoqueInicialKits, estoqueMinimoKits, pecasPorKit, novos, barcosIniciais);
    }
}
