package br.marellar.api;

import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosJogo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Guarda as partidas em memoria. Salvar e carregar em JSON entram no M7. */
@Service
public class PartidaService {

    private final ParametrosJogo parametros = CarregadorDados.carregarDoClasspath();
    private final Map<String, Simulacao> partidas = new ConcurrentHashMap<>();

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

    public static class PartidaNaoEncontradaException extends RuntimeException {
        public PartidaNaoEncontradaException(String id) {
            super("Partida nao encontrada: " + id);
        }
    }
}
