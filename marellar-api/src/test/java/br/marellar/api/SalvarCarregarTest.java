package br.marellar.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "marellar.saves.pasta=target/saves-teste")
@AutoConfigureMockMvc
class SalvarCarregarTest {

    static final Path PASTA = Path.of("target/saves-teste");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    PartidaService service;

    private String criar(long semente) throws Exception {
        String corpo = mvc.perform(post("/api/v1/partidas").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semente\": " + semente + "}"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("id").asText();
    }

    private void avancar(String id, int ticks) throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/avancar", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"ticks\": " + ticks + "}")).andExpect(status().isOk());
    }

    private void ordem(String id, String corpo) throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/ordens", id).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk());
    }

    private JsonNode estado(String id) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/partidas/{id}", id)).andReturn().getResponse().getContentAsString());
    }

    @Test
    void salvarGravaArquivoECarregarRecriaAMesmaPartida() throws Exception {
        String id = criar(42);
        ordem(id, "{\"tipo\": \"DefinirPrioridade\", \"barcoId\": \"33-004\", \"posicao\": 1}");
        avancar(id, 40);
        ordem(id, "{\"tipo\": \"Contratar\", \"funcao\": \"PRODUCAO\"}");
        avancar(id, 168); // atravessa o fim do mes (pausa)
        avancar(id, 13);
        JsonNode antes = estado(id);

        mvc.perform(post("/api/v1/partidas/{id}/salvar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tick").value(antes.get("tick").asLong()))
                .andExpect(jsonPath("$.data").value(antes.get("data").asText()));
        Path arquivo = PASTA.resolve(id + ".json");
        assertThat(arquivo).exists();
        assertThat(Files.readString(arquivo)).contains("\"tipo\" : \"Contratar\"");

        // a partida segue em memoria; carregar volta ao ponto salvo
        avancar(id, 50);
        mvc.perform(post("/api/v1/partidas/carregar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"" + id + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        JsonNode depois = estado(id);

        assertThat(depois.get("tick")).isEqualTo(antes.get("tick"));
        assertThat(depois.get("barcos")).isEqualTo(antes.get("barcos"));
        assertThat(depois.get("pessoas")).isEqualTo(antes.get("pessoas"));
        assertThat(depois.get("financeiro")).isEqualTo(antes.get("financeiro"));
        assertThat(depois.get("conselho")).isEqualTo(antes.get("conselho"));
    }

    @Test
    void listaOsSavesDaPasta() throws Exception {
        String id = criar(5);
        avancar(id, 8);
        service.salvar(id);

        mvc.perform(get("/api/v1/partidas/saves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].tick").value(8))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].semente").value(5));
    }

    @Test
    void carregarSaveInexistenteDa404() throws Exception {
        mvc.perform(post("/api/v1/partidas/carregar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void idQueNaoEUuidNaoViraCaminhoDeArquivo() throws Exception {
        mvc.perform(post("/api/v1/partidas/carregar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"../pom\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void saveAdulteradoDa409() throws Exception {
        String id = criar(9);
        avancar(id, 30);
        service.salvar(id);
        Path arquivo = PASTA.resolve(id + ".json");
        Files.writeString(arquivo, Files.readString(arquivo).replace("\"assinatura\" : \"", "\"assinatura\" : \"0"));

        mvc.perform(post("/api/v1/partidas/carregar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"" + id + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Save incompativel"));
    }

    @Test
    void salvarPartidaInexistenteDa404() throws Exception {
        mvc.perform(post("/api/v1/partidas/{id}/salvar", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
