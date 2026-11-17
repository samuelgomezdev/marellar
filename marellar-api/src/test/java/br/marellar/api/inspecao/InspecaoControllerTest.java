package br.marellar.api.inspecao;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "marellar.inspecao.arquivo=target/inspecao-teste/ncs.json")
@AutoConfigureMockMvc
class InspecaoControllerTest {

    static final Path ARQUIVO = Path.of("target/inspecao-teste/ncs.json");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    InspecaoService service;

    @BeforeEach
    void limpar() {
        service.listar(null).forEach(n -> service.excluir(n.id()));
    }

    private static String nc(int linha, String defeito, String gravidade) {
        return """
                {"linha": %d, "peca": "CASCO", "posicao": {"x": 8.2, "y": 1.4, "z": 2.3},
                 "baliza": 7, "bordo": "BE", "zona": "obras mortas", "componente": "Casco", "gate": "DM",
                 "codigoDefeito": "%s", "gravidade": "%s", "observacao": " lixar e retocar "}
                """.formatted(linha, defeito, gravidade);
    }

    private String registrar(String corpo) throws Exception {
        String resp = mvc.perform(post("/api/v1/inspecao/ncs").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resp).get("id").asText();
    }

    @Test
    void catalogoEOMesmoDoMotor() throws Exception {
        mvc.perform(get("/api/v1/inspecao/defeitos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[?(@.codigo == 'LAM-SECO')].gravidade").value("CRITICA"));
    }

    @Test
    void registraEListaPorLinha() throws Exception {
        registrar(nc(59, "GEL-TRINCA", "LEVE"));
        registrar(nc(59, "LAM-BOLHA", "MEDIA"));
        registrar(nc(33, "ACA-RISCO", "LEVE"));

        mvc.perform(get("/api/v1/inspecao/ncs").param("linha", "59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[1].numero").value(2))
                .andExpect(jsonPath("$[1].descricaoDefeito").value("Bolha na laminação"))
                .andExpect(jsonPath("$[0].observacao").value("lixar e retocar"))
                .andExpect(jsonPath("$[0].componente").value("Casco"))
                .andExpect(jsonPath("$[0].status").value("ABERTA"));
        mvc.perform(get("/api/v1/inspecao/ncs").param("linha", "33"))
                .andExpect(jsonPath("$[0].numero").value(1));
    }

    @Test
    void gravidadePodeSubirMasNaoPodeBaixar() throws Exception {
        registrar(nc(47, "GEL-TRINCA", "MEDIA"));                       // leve no catalogo, inspetor subiu
        mvc.perform(post("/api/v1/inspecao/ncs").contentType(MediaType.APPLICATION_JSON)
                        .content(nc(47, "LAM-SECO", "LEVE")))                // critica no catalogo
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("NC invalida"));
    }

    @Test
    void recusaDefeitoForaDoCatalogoELinhaDesconhecida() throws Exception {
        mvc.perform(post("/api/v1/inspecao/ncs").contentType(MediaType.APPLICATION_JSON)
                        .content(nc(59, "XYZ-000", "LEVE")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/v1/inspecao/ncs").contentType(MediaType.APPLICATION_JSON)
                        .content(nc(40, "GEL-TRINCA", "LEVE")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void recusaCorpoIncompleto() throws Exception {
        mvc.perform(post("/api/v1/inspecao/ncs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"linha\": 59, \"baliza\": 20}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void fechaEExclui() throws Exception {
        String id = registrar(nc(59, "GEL-TRINCA", "LEVE"));
        mvc.perform(post("/api/v1/inspecao/ncs/{id}/fechar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FECHADA"))
                .andExpect(jsonPath("$.fechadaEm").isNotEmpty());
        mvc.perform(delete("/api/v1/inspecao/ncs/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/inspecao/ncs/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void ncsSobrevivemAReiniciarOServidor() throws Exception {
        registrar(nc(59, "LAM-BOLHA", "CRITICA"));
        assertThat(Files.readString(ARQUIVO)).contains("LAM-BOLHA");

        InspecaoService reiniciado = new InspecaoService(json, ARQUIVO, Clock.systemUTC());
        assertThat(reiniciado.listar(59)).singleElement()
                .satisfies(n -> assertThat(n.gravidade().name()).isEqualTo("CRITICA"));
    }

    private static String edicao(String defeito, String gravidade, double x) {
        return """
                {"peca": "CASARIA", "posicao": {"x": %s, "y": 2.1, "z": -1.2},
                 "baliza": 9, "bordo": "BB", "zona": "2,1 m", "componente": "Casaria", "gate": "BF",
                 "codigoDefeito": "%s", "gravidade": "%s", "observacao": "ponto corrigido"}
                """.formatted(x, defeito, gravidade);
    }

    @Test
    void editaDefeitoGravidadeEPontoMantendoNumero() throws Exception {
        registrar(nc(59, "GEL-TRINCA", "LEVE"));
        String id = registrar(nc(59, "ACA-RISCO", "LEVE"));
        mvc.perform(put("/api/v1/inspecao/ncs/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("LAM-BOLHA", "CRITICA", 10.5)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(2))
                .andExpect(jsonPath("$.linha").value(59))
                .andExpect(jsonPath("$.peca").value("CASARIA"))
                .andExpect(jsonPath("$.posicao.x").value(10.5))
                .andExpect(jsonPath("$.bordo").value("BB"))
                .andExpect(jsonPath("$.gate").value("BF"))
                .andExpect(jsonPath("$.descricaoDefeito").value("Bolha na laminação"))
                .andExpect(jsonPath("$.gravidade").value("CRITICA"))
                .andExpect(jsonPath("$.editadaEm").isNotEmpty());
        assertThat(service.listar(59)).hasSize(2);
    }

    @Test
    void edicaoSegueAMesmaRegraDeGravidade() throws Exception {
        String id = registrar(nc(59, "GEL-TRINCA", "LEVE"));
        mvc.perform(put("/api/v1/inspecao/ncs/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("LAM-SECO", "MEDIA", 3)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void ncFechadaNaoPodeSerEditadaENcInexistenteDa404() throws Exception {
        String id = registrar(nc(59, "GEL-TRINCA", "LEVE"));
        mvc.perform(post("/api/v1/inspecao/ncs/{id}/fechar", id)).andExpect(status().isOk());
        mvc.perform(put("/api/v1/inspecao/ncs/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("GEL-TRINCA", "LEVE", 3)))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(put("/api/v1/inspecao/ncs/{id}", "nao-existe").contentType(MediaType.APPLICATION_JSON)
                        .content(edicao("GEL-TRINCA", "LEVE", 3)))
                .andExpect(status().isNotFound());
    }
}
