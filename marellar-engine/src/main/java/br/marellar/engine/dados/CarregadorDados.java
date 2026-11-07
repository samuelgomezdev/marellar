package br.marellar.engine.dados;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carrega os arquivos de data/ e valida as regras de cada um.
 * <p>
 * No jar, os JSON ficam em {@code data/} no classpath. Em desenvolvimento tambem
 * da para apontar para uma pasta com {@link #carregarDeDiretorio(Path)}.
 */
public final class CarregadorDados {

    public static final String LINHA_33 = "linha-33.json";
    public static final String NC_CATALOGO = "nc-catalogo.json";
    public static final String PESSOAS = "pessoas.json";
    public static final String FORNECEDORES = "fornecedores.json";
    public static final String EVENTOS = "eventos.json";
    public static final String FINANCEIRO = "financeiro.json";
    public static final String TEXTOS = "textos-ptbr.json";

    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

    private CarregadorDados() {
    }

    /** Carrega os parametros embutidos no jar (pasta data/ do classpath). */
    public static ParametrosJogo carregarDoClasspath() {
        return carregar(arquivo -> {
            String recurso = "data/" + arquivo;
            String conteudo;
            try (InputStream in = CarregadorDados.class.getClassLoader().getResourceAsStream(recurso)) {
                if (in == null) {
                    throw new DadosInvalidosException(java.util.List.of(arquivo + ": arquivo nao encontrado no classpath"));
                }
                conteudo = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return conteudo;
        });
    }

    /** Carrega os parametros de uma pasta no disco (ex.: a pasta data/ do repositorio). */
    public static ParametrosJogo carregarDeDiretorio(Path diretorio) {
        return carregar(arquivo -> {
            Path caminho = diretorio.resolve(arquivo);
            if (!Files.isRegularFile(caminho)) {
                throw new DadosInvalidosException(java.util.List.of(arquivo + ": arquivo nao encontrado em " + diretorio));
            }
            try {
                return Files.readString(caminho, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private interface Fonte {
        String ler(String arquivo);
    }

    private static ParametrosJogo carregar(Fonte fonte) {
        ParametrosJogo parametros = new ParametrosJogo(
                ler(LINHA_33, fonte.ler(LINHA_33), ParametrosLinha.class),
                ler(NC_CATALOGO, fonte.ler(NC_CATALOGO), CatalogoNc.class),
                ler(PESSOAS, fonte.ler(PESSOAS), ParametrosPessoas.class),
                ler(FORNECEDORES, fonte.ler(FORNECEDORES), ParametrosFornecedores.class),
                ler(EVENTOS, fonte.ler(EVENTOS), ParametrosEventos.class),
                ler(FINANCEIRO, fonte.ler(FINANCEIRO), ParametrosFinanceiro.class),
                new Textos(ler(TEXTOS, fonte.ler(TEXTOS), JsonNode.class)));
        ValidadorDados.validar(parametros);
        return parametros;
    }

    /** Converte um JSON no tipo pedido; qualquer erro vira uma mensagem com o nome do arquivo e a posicao. */
    public static <T> T ler(String arquivo, String conteudo, Class<T> tipo) {
        try {
            T valor = MAPPER.readValue(conteudo, tipo);
            if (valor == null) {
                throw new DadosInvalidosException(java.util.List.of(arquivo + ": arquivo vazio"));
            }
            return valor;
        } catch (JsonProcessingException e) {
            String local = e.getLocation() == null ? ""
                    : " (linha " + e.getLocation().getLineNr() + ", coluna " + e.getLocation().getColumnNr() + ")";
            throw new DadosInvalidosException(arquivo + local + ": " + e.getOriginalMessage(), e);
        }
    }
}
