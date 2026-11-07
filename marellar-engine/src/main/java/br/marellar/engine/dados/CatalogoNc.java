package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.Origem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Catalogo de tipos de nao conformidade e regras de inspecao (arquivo data/nc-catalogo.json). */
public record CatalogoNc(
        Map<Gravidade, BigDecimal> deteccaoBase,
        int maxNcsPorGate,
        Map<Integer, BigDecimal> fatorNivelEquipe,
        Inspecao inspecao,
        List<TipoNc> tipos) {

    public CatalogoNc {
        deteccaoBase = deteccaoBase == null ? Map.of() : Map.copyOf(deteccaoBase);
        fatorNivelEquipe = fatorNivelEquipe == null ? Map.of() : Map.copyOf(fatorNivelEquipe);
        tipos = tipos == null ? List.of() : List.copyOf(tipos);
    }

    /**
     * Coeficientes do inspetor. Deteccao: {@code deteccaoBase + pesoRigor x rigor/100 - pesoVelocidade x velocidade/100}.
     * Tempo: {@code ticksInspecao x (1 - pesoRigor/2 + pesoRigor x rigor/100) x (1 + pesoVelocidade/2 - pesoVelocidade x velocidade/100)}.
     */
    public record Inspecao(
            BigDecimal deteccaoBase,
            BigDecimal pesoRigorNaDeteccao,
            BigDecimal pesoVelocidadeNaDeteccao,
            BigDecimal pesoRigorNoTempo,
            BigDecimal pesoVelocidadeNoTempo,
            int filaSemPerda,
            BigDecimal perdaPorBarcoNaFila,
            BigDecimal fatorCargaMinimo,
            BigDecimal quedaMoralPorConcessao,
            int concessoesParaReclamar) {}

    public record TipoNc(
            String codigo,
            String descricao,
            Gravidade gravidade,
            Origem origem,
            List<CodigoGate> gates,
            BigDecimal probabilidade,
            BigDecimal hhRetrabalho,
            String item,
            BigDecimal custoMaterial) {

        public TipoNc {
            gates = gates == null ? List.of() : List.copyOf(gates);
        }

        public TipoNc comProbabilidade(BigDecimal nova) {
            return new TipoNc(codigo, descricao, gravidade, origem, gates, nova, hhRetrabalho, item, custoMaterial);
        }
    }

    public List<TipoNc> tiposDoGate(CodigoGate gate) {
        return tipos.stream().filter(t -> t.gates().contains(gate)).toList();
    }

    public TipoNc tipo(String codigo) {
        return tipos.stream().filter(t -> t.codigo().equals(codigo)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tipo de NC desconhecido: " + codigo));
    }

    public BigDecimal fatorDoNivelDaEquipe(int nivel) {
        return fatorNivelEquipe.getOrDefault(Math.max(1, Math.min(5, nivel)), BigDecimal.ONE);
    }

    public CatalogoNc comTipos(List<TipoNc> novos) {
        return new CatalogoNc(deteccaoBase, maxNcsPorGate, fatorNivelEquipe, inspecao, novos);
    }

    public CatalogoNc comDeteccaoBase(Map<Gravidade, BigDecimal> nova) {
        return new CatalogoNc(nova, maxNcsPorGate, fatorNivelEquipe, inspecao, tipos);
    }
}
