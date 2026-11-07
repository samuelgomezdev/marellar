package br.marellar.engine;

import br.marellar.engine.dominio.MotivoPausa;

/** Quantos ticks rodaram de fato e, se o motor pausou sozinho, por que. */
public record ResultadoAvanco(int ticksExecutados, boolean pausado, MotivoPausa motivoPausa) {}
