package br.marellar.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PartidaControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    String id;

    @BeforeEach
    void criarPartida() throws Exception {
        String corpo = mvc.perform(post("/api/v1/partidas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semente\": 42}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        id = json.readTree(corpo).get("id").asText();
    }

    @Test
    void estadoInicial() throws Exception {
        mvc.perform(get("/api/v1/partidas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semente").value(42))
                .andExpect(jsonPath("$.tick").value(0))
                .andExpect(jsonPath("$.data").value("2027-03-01"))
                .andExpect(jsonPath("$.mesReferencia").value("mar/2027"))
                .andExpect(jsonPath("$.barcos.length()").value(4))
                .andExpect(jsonPath("$.pessoas.length()").value(13));
    }

    @Test
    void avancarUmDia() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/avancar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticks\": 8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado.ticksExecutados").value(8))
                .andExpect(jsonPath("$.resultado.pausado").value(false))
                .andExpect(jsonPath("$.estado.tick").value(8))
                .andExpect(jsonPath("$.estado.data").value("2027-03-02"));
    }

    @Test
    void ordemDeDefinirPrioridade() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"DefinirPrioridade\", \"barcoId\": \"33-004\", \"posicao\": 1}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/partidas/{id}/barcos/{b}", id, "33-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value(1))
                .andExpect(jsonPath("$.nomeGate").value("Recebimento de materiais"));
    }

    @Test
    void ordemDeVelocidade() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"MudarVelocidade\", \"velocidade\": \"X2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.velocidade").value("X2"));
    }

    @Test
    void eventosDesdeUmId() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/avancar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticks\": 168}"))
                .andExpect(jsonPath("$.resultado.pausado").value(true))
                .andExpect(jsonPath("$.resultado.motivoPausa").value("FIM_DO_MES"));

        String todos = mvc.perform(get("/api/v1/partidas/{id}/eventos", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode lista = json.readTree(todos);
        assertThat(lista.size()).isGreaterThan(1);
        long penultimo = lista.get(lista.size() - 2).get("id").asLong();

        mvc.perform(get("/api/v1/partidas/{id}/eventos", id).param("desde", String.valueOf(penultimo)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("FIM_DO_MES"));
    }

    @Test
    void partidaInexistenteDa404EmProblemJson() throws Exception {
        mvc.perform(get("/api/v1/partidas/{id}", "nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Partida nao encontrada: nao-existe"));
    }

    @Test
    void ordemComBarcoInexistenteDa422() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"SegurarGate\", \"barcoId\": \"99-999\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void ordemDecidirNcComNcInexistenteDa422() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"DecidirNc\", \"ncId\": \"NC-9999\", \"decisao\": \"RETRABALHO\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("NC nao encontrada: NC-9999"));
    }

    @Test
    void estadoTrazInspetorEQualidade() throws Exception {
        mvc.perform(get("/api/v1/partidas/{id}", id))
                .andExpect(jsonPath("$.inspetor.nome").value("Marta Quintela"))
                .andExpect(jsonPath("$.inspetor.rigor").value(75))
                .andExpect(jsonPath("$.qualidade.ncsDetectadas").value(0))
                .andExpect(jsonPath("$.barcos[0].ncs").isArray());
    }

    @Test
    void ticksForaDoLimiteDa400() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/avancar", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticks\": 100000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordemDeTipoDesconhecidoDa400() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"ExplodirFabrica\"}"))
                .andExpect(status().isBadRequest());
    }
}
