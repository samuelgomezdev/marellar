package br.marellar.engine.dominio;

import java.math.BigDecimal;

/** Cota do cliente liberada pela aprovacao de um gate; entra no caixa no dia {@code diaRecebimento}. */
public record CotaAReceber(String barcoId, CodigoGate gate, BigDecimal valor, long diaRecebimento) {}
