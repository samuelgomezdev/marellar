package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Catalogo ampliado da inspecao 3D ({@code data/nc-catalogo-inspecao.json}): defeitos detalhados por categoria,
 * cada um ligado a um defeito-base do motor ({@code base}, em {@code nc-catalogo.json}). Gerado por
 * {@code docs/ferramentas/gerar_catalogo_inspecao.py}. Separado do catalogo do motor para nao mudar o sorteio de NCs.
 */
public record CatalogoInspecao(int versao, String observacao, Map<String, String> categorias, List<Defeito> tipos) {

    public CatalogoInspecao {
        categorias = categorias == null ? Map.of() : Map.copyOf(categorias);
        tipos = tipos == null ? List.of() : List.copyOf(tipos);
    }

    /** {@code aplica}: tipos de item da estrutura de produto em que o defeito e sugerido (MOLDE, FRP, TECA...). */
    public record Defeito(String codigo, String descricao, String categoria, Gravidade gravidade,
                          List<CodigoGate> gates, String base, List<String> aplica) {
        public Defeito {
            gates = gates == null ? List.of() : List.copyOf(gates);
            aplica = aplica == null ? List.of() : List.copyOf(aplica);
        }
    }

    public Optional<Defeito> defeito(String codigo) {
        return tipos.stream().filter(t -> t.codigo().equals(codigo)).findFirst();
    }
}
