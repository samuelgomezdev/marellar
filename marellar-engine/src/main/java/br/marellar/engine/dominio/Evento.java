package br.marellar.engine.dominio;

import java.time.LocalDate;

/** Algo que aconteceu na partida e que o cliente mostra como alerta ou toast. */
public record Evento(long id, long tick, LocalDate data, int hora, TipoEvento tipo, String barcoId, String mensagem) {}
