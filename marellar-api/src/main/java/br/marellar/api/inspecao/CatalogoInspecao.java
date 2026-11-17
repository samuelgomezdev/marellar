package br.marellar.api.inspecao;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Catalogo ampliado da inspecao 3D ({@code data/nc-catalogo-inspecao.json}) e os compartimentos de cada linha
 * ({@code data/estrutura-produto.json}). Separado do catalogo do motor para nao mudar o sorteio de NCs:
 * cada defeito detalhado aponta para um defeito-base do motor em {@code base}.
 */
public final class CatalogoInspecao {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Defeito(String codigo, String descricao, String categoria, Gravidade gravidade,
                          List<CodigoGate> gates, String base, List<String> aplica) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Arquivo(Map<String, String> categorias, List<Defeito> tipos) {}

    private final Map<String, String> categorias;
    private final Map<String, Defeito> defeitos = new LinkedHashMap<>();
    private final Map<Integer, Set<String>> compartimentos = new TreeMap<>();

    private CatalogoInspecao(Arquivo arquivo, JsonNode estrutura) {
        this.categorias = Map.copyOf(arquivo.categorias());
        for (Defeito d : arquivo.tipos()) {
            if (!categorias.containsKey(d.categoria())) {
                throw new IllegalStateException("Defeito " + d.codigo() + " com categoria desconhecida: " + d.categoria());
            }
            if (defeitos.put(d.codigo(), d) != null) {
                throw new IllegalStateException("Defeito repetido no catalogo da inspecao: " + d.codigo());
            }
        }
        for (JsonNode linha : estrutura.path("linhas")) {
            Set<String> comps = new TreeSet<>();
            for (JsonNode escopo : linha.path("escopos")) {
                for (JsonNode item : escopo.path("itens")) {
                    if (item.hasNonNull("compartimento")) {
                        comps.add(item.get("compartimento").asText());
                    }
                }
            }
            compartimentos.put(linha.path("linha").asInt(), Set.copyOf(comps));
        }
    }

    public static CatalogoInspecao carregarDoClasspath() {
        ObjectMapper json = new ObjectMapper();
        return new CatalogoInspecao(ler(json, "data/nc-catalogo-inspecao.json", Arquivo.class),
                ler(json, "data/estrutura-produto.json", JsonNode.class));
    }

    private static <T> T ler(ObjectMapper json, String recurso, Class<T> tipo) {
        try (InputStream in = CatalogoInspecao.class.getClassLoader().getResourceAsStream(recurso)) {
            if (in == null) {
                throw new IllegalStateException("Recurso nao encontrado no classpath: " + recurso);
            }
            return json.readValue(in, tipo);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + recurso, e);
        }
    }

    public Collection<Defeito> defeitos() {
        return defeitos.values();
    }

    public Optional<Defeito> defeito(String codigo) {
        return Optional.ofNullable(codigo).map(defeitos::get);
    }

    /** Compartimentos que aparecem nos itens da linha (ex.: SM, SAL, VIP na 59; CONS, PORAO na 33). */
    public Set<String> compartimentos(int linha) {
        return compartimentos.getOrDefault(linha, Set.of());
    }
}
