package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.Origem;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Catalogo de tipos de nao conformidade (arquivo data/nc-catalogo.json). */
public record CatalogoNc(Map<Gravidade, BigDecimal> deteccaoBase, int maxNcsPorGate, List<TipoNc> tipos) {

    public CatalogoNc {
        deteccaoBase = deteccaoBase == null ? Map.of() : Map.copyOf(deteccaoBase);
        tipos = tipos == null ? List.of() : List.copyOf(tipos);
    }

    public record TipoNc(
            String codigo,
            String descricao,
            Gravidade gravidade,
            Origem origem,
            List<CodigoGate> gates,
            BigDecimal probabilidade,
            BigDecimal hhRetrabalho,
            BigDecimal custoMaterial) {

        public TipoNc {
            gates = gates == null ? List.of() : List.copyOf(gates);
        }
    }

    public List<TipoNc> tiposDoGate(CodigoGate gate) {
        return tipos.stream().filter(t -> t.gates().contains(gate)).toList();
    }
}
