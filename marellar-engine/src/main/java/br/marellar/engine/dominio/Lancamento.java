package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Movimento de caixa. Valor positivo entra, negativo sai. */
public record Lancamento(LocalDate data, TipoLancamento tipo, BigDecimal valor, String descricao) {}
