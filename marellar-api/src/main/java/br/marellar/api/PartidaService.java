package br.marellar.api;

import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.save.Save;
import br.marellar.engine.save.SaveIncompativelException;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Guarda as partidas em memoria e grava/le saves em {@code saves/{id}.json}.
 * O save so tem semente, ordens e ticks: carregar e jogar de novo (o motor e deterministico).
 */
@Service
public class PartidaService {

    private final ParametrosJogo parametros = CarregadorDados.carregarDoClasspath();
    private final Map<String, Simulacao> partidas = new ConcurrentHashMap<>();
    private final ObjectMapper json;
    private final Path pastaSaves;

    public PartidaService(ObjectMapper json, @Value("${marellar.saves.pasta:saves}") String pastaSaves) {
        this.json = json;
        this.pastaSaves = Path.of(pastaSaves);
    }

    public String criar(long semente) {
        String id = UUID.randomUUID().toString();
        partidas.put(id, Simulacao.novaPartida(semente, parametros));
        return id;
    }

    public Simulacao buscar(String id) {
        Simulacao sim = partidas.get(id);
        if (sim == null) {
            throw new PartidaNaoEncontradaException(id);
        }
        return sim;
    }

    // ----- saves -----

    public record ResumoSave(String id, long semente, long tick, String data, int hora, Instant salvoEm) {}

    /** Grava {@code saves/{id}.json}; um save novo da mesma partida substitui o anterior. */
    public ResumoSave salvar(String id) {
        Save save = buscar(id).salvar();
        Path arquivo = arquivo(id);
        try {
            Files.createDirectories(pastaSaves);
            // escreve num temporario e troca, para nao deixar um save pela metade se algo falhar
            Path temporario = Files.createTempFile(pastaSaves, id, ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporario.toFile(), save);
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            return resumo(id, save, Files.getLastModifiedTime(arquivo).toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar " + arquivo, e);
        }
    }

    /** Le {@code saves/{id}.json} e recria a partida em memoria com o mesmo id. */
    public String carregar(String id) {
        Path arquivo = arquivo(id);
        if (!Files.isRegularFile(arquivo)) {
            throw new SaveNaoEncontradoException(id);
        }
        Save save;
        try {
            save = json.readValue(arquivo.toFile(), Save.class);
        } catch (JacksonException e) {
            throw new SaveIncompativelException("Save ilegivel: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + arquivo, e);
        }
        partidas.put(id, Simulacao.carregar(save, parametros));
        return id;
    }

    /** Saves da pasta, do mais recente para o mais antigo. Arquivos ilegiveis ficam de fora. */
    public List<ResumoSave> listarSaves() {
        if (!Files.isDirectory(pastaSaves)) {
            return List.of();
        }
        List<ResumoSave> lista = new ArrayList<>();
        try (Stream<Path> arquivos = Files.list(pastaSaves)) {
            for (Path p : arquivos.filter(a -> a.getFileName().toString().endsWith(".json")).toList()) {
                String id = p.getFileName().toString().replaceFirst("\\.json$", "");
                if (!idValido(id)) {
                    continue;
                }
                try {
                    Save save = json.readValue(p.toFile(), Save.class);
                    lista.add(resumo(id, save, Files.getLastModifiedTime(p).toInstant()));
                } catch (IOException ignorado) {
                    // save corrompido ou de outro formato: nao aparece na lista
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel listar " + pastaSaves, e);
        }
        lista.sort(Comparator.comparing(ResumoSave::salvoEm).reversed());
        return lista;
    }

    private static ResumoSave resumo(String id, Save save, Instant salvoEm) {
        var v = save.verificacao();
        return new ResumoSave(id, save.semente(), save.tick(), v == null ? null : v.data(),
                v == null ? 0 : v.hora(), salvoEm);
    }

    /** So aceita UUID: o id vira nome de arquivo, entao nada de "../". */
    private Path arquivo(String id) {
        if (!idValido(id)) {
            throw new SaveNaoEncontradoException(id);
        }
        return pastaSaves.resolve(id + ".json");
    }

    private static boolean idValido(String id) {
        try {
            return UUID.fromString(id).toString().equals(id);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static class PartidaNaoEncontradaException extends RuntimeException {
        public PartidaNaoEncontradaException(String id) {
            super("Partida nao encontrada: " + id);
        }
    }

    public static class SaveNaoEncontradoException extends RuntimeException {
        public SaveNaoEncontradoException(String id) {
            super("Save nao encontrado: " + id);
        }
    }
}
