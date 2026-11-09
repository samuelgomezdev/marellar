package br.marellar.web;

import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Treinamento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.save.Save;
import br.marellar.web.json.CodecGerado;
import br.marellar.web.json.Json;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O que o navegador le e escreve tem que ser igual ao que a API (Jackson do Spring) le e escreve:
 * mesmos parametros carregados de data/ e o mesmo JSON de estado, eventos, conselho e save.
 */
class ParidadeJacksonTest {

    /** Como o ObjectMapper do Spring Boot: datas ISO, sem timestamps. */
    private static final ObjectMapper JACKSON = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .build();

    /** Numeros iguais em valor (1.50 == 1.5), o resto igual em tudo. */
    private static final Comparator<JsonNode> VALOR = (a, b) -> {
        if (a.isNumber() && b.isNumber()) {
            return a.decimalValue().compareTo(b.decimalValue());
        }
        return a.equals(b) ? 0 : 1;
    };

    private static ParametrosJogo jackson;
    private static ParametrosJogo web;

    @BeforeAll
    static void carregar() throws IOException {
        Path pasta = Path.of("../data");
        jackson = CarregadorDados.carregarDeDiretorio(pasta);
        Map<String, String> arquivos = new HashMap<>();
        for (String nome : DadosWeb.ARQUIVOS) {
            arquivos.put(nome, Files.readString(pasta.resolve(nome), StandardCharsets.UTF_8));
        }
        web = DadosWeb.carregar(arquivos);
    }

    @Test
    void parametrosIguais() {
        assertThat(web.linha33()).isEqualTo(jackson.linha33());
        assertThat(web.catalogoNc()).isEqualTo(jackson.catalogoNc());
        assertThat(web.pessoas()).isEqualTo(jackson.pessoas());
        assertThat(web.fornecedores()).isEqualTo(jackson.fornecedores());
        assertThat(web.eventos()).isEqualTo(jackson.eventos());
        assertThat(web.financeiro()).isEqualTo(jackson.financeiro());
        for (String caminho : new String[]{"eventos.gateAprovado", "conselho.humor.tudoBem", "conselho.serio.acidente"}) {
            assertThat(web.textos().formatar(caminho, Map.of())).isEqualTo(jackson.textos().formatar(caminho, Map.of()));
        }
        assertThat(web.textos().campo("conselheiros", 1, "nome")).isEqualTo(jackson.textos().campo("conselheiros", 1, "nome"));
    }

    @Test
    void mesmoJsonQueAApiDuranteUmaPartida() throws Exception {
        Simulacao sim = Simulacao.novaPartida(42, web);
        Simulacao ref = Simulacao.novaPartida(42, jackson);
        boolean contratou = false;
        for (int passo = 0; passo < 60 && sim.estado().derrota() == null; passo++) {
            sim.avancar(24);
            ref.avancar(24);
            EstadoPartida e = sim.estado();
            comparar(e, CodecGerado::escreverEstadoPartida);
            assertThat(e).isEqualTo(ref.estado());
            comparar(sim.eventosDesde(0), (sb, l) -> Json.lista(sb, l, CodecGerado::escreverEvento));
            sim.conselho().ifPresent(c -> comparar(c, CodecGerado::escreverRelatorioConselho));
            comparar(sim.barco(e.barcos().get(0).id()), CodecGerado::escreverEstadoPartida_BarcoVisao);

            // decisoes variadas, para o save ter todo tipo de ordem
            for (EstadoPartida.BarcoVisao b : e.barcos()) {
                for (EstadoPartida.NcVisao nc : b.ncs()) {
                    if (nc.status() == br.marellar.engine.dominio.StatusNc.ABERTA) {
                        DecisaoNc d = switch (passo % 3) {
                            case 0 -> DecisaoNc.RETRABALHO;
                            case 1 -> DecisaoNc.CONCESSAO;
                            default -> DecisaoNc.RNC_FORNECEDOR;
                        };
                        ordem(sim, ref, new Ordem.DecidirNc(nc.id(), d));
                        ordem(sim, ref, new Ordem.DecidirNc(nc.id(), DecisaoNc.RETRABALHO));
                    }
                }
            }
            if (!contratou) {
                ordem(sim, ref, new Ordem.Contratar(Funcao.PRODUCAO));
                ordem(sim, ref, new Ordem.DefinirPrioridade(e.barcos().get(e.barcos().size() - 1).id(), 1));
                contratou = true;
            }
            EstadoPartida.PessoaVisao p = e.pessoas().get(passo % e.pessoas().size());
            ordem(sim, ref, new Ordem.FazerUmAUm(p.id()));
            if (passo == 5) {
                ordem(sim, ref, new Ordem.Treinar(p.id(), Treinamento.SEGURANCA));
            }
            if (e.conselho() != null && e.conselho().decisaoEscolhida() == null && !e.conselho().opcoes().isEmpty()) {
                ordem(sim, ref, new Ordem.EscolherDecisaoConselho(e.conselho().opcoes().get(0).id()));
            }
        }

        // o save escrito pelo navegador e lido pelo Jackson (e vice-versa) e o mesmo
        Save save = sim.salvar();
        assertThat(save.ordens()).isNotEmpty();
        comparar(save, CodecGerado::escreverSave);
        StringBuilder sb = new StringBuilder();
        CodecGerado.escreverSave(sb, save);
        assertThat(JACKSON.readValue(sb.toString(), Save.class)).isEqualTo(save);
        assertThat(CodecGerado.lerSave(Json.ler(JACKSON.writeValueAsString(save)))).isEqualTo(save);
        assertThat(Simulacao.carregar(save, web).estado()).isEqualTo(sim.estado());
    }

    @Test
    void ordensNoFormatoDoCliente() {
        assertThat(CodecGerado.lerOrdem(Json.ler("{\"tipo\":\"DecidirNc\",\"ncId\":\"NC-0003\",\"decisao\":\"RETRABALHO\"}")))
                .isEqualTo(new Ordem.DecidirNc("NC-0003", DecisaoNc.RETRABALHO));
        assertThat(CodecGerado.lerOrdem(Json.ler("{\"barcoId\":\"B2\",\"posicao\":1,\"tipo\":\"DefinirPrioridade\"}")))
                .isEqualTo(new Ordem.DefinirPrioridade("B2", 1));
    }

    private static void ordem(Simulacao sim, Simulacao ref, Ordem o) {
        boolean ok = aplicar(sim, o);
        assertThat(aplicar(ref, o)).isEqualTo(ok);
    }

    private static boolean aplicar(Simulacao s, Ordem o) {
        try {
            s.aplicarOrdem(o);
            return true;
        } catch (OrdemInvalidaException e) {
            return false;
        }
    }

    private static <T> void comparar(T valor, BiConsumer<StringBuilder, T> escritor) {
        StringBuilder sb = new StringBuilder();
        escritor.accept(sb, valor);
        try {
            JsonNode nosso = JACKSON.readTree(sb.toString());
            JsonNode deles = JACKSON.valueToTree(valor);
            assertThat(nosso.equals(VALOR, deles))
                    .as("JSON diferente do Jackson para %s%nnavegador: %s%njackson:   %s",
                            valor.getClass().getSimpleName(), nosso, deles)
                    .isTrue();
        } catch (IOException e) {
            throw new AssertionError("JSON invalido: " + sb, e);
        }
    }
}
