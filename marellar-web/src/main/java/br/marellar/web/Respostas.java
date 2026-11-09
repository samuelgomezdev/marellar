package br.marellar.web;

import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.estado.EstadoPartida;

/** Corpos de resposta da API que nao sao records do motor (iguais aos do PartidaController). */
public final class Respostas {

    private Respostas() {
    }

    public record CriarPartida(String id) {}

    public record Avancar(ResultadoAvanco resultado, EstadoPartida estado) {}

    /** Como o ResumoSave da API; {@code salvoEm} e o instante ISO-8601 em UTC. */
    public record ResumoSave(String id, long semente, long tick, String data, int hora, String salvoEm) {}
}
