package br.marellar.engine.dominio;

import java.math.BigDecimal;

/** Venda fechada esperando molde livre para virar casco ({@code diaPrometido} = dia util da entrega). */
public record PedidoCliente(String propostaId, String cliente, Linha linha, BigDecimal preco, long diaPrometido) {}
