package br.marellar.engine.dominio;

import java.math.BigDecimal;

/** NC que escapou e volta no pos-venda no dia util {@code dia}, custando {@code custo}. */
public record GarantiaAgendada(String barcoId, String ncId, String descricao, long dia, BigDecimal custo) {}
