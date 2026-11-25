package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pedido de um cliente trazido pelo vendedor. Os prazos sao em dias uteis contados a partir do dia
 * em que a fabrica responde: {@code prazoPedido} e o que o cliente quer; {@code prazoFabrica} e o que
 * a fabrica consegue pela fila e pela capacidade declarada (calculado na chegada).
 */
public final class Proposta {

    private final String id;
    private final String cliente;
    private final Linha linha;
    private final BigDecimal preco;
    private final long diaChegada;
    private final long validaAte;
    private final int prazoPedido;
    private final int prazoFabrica;
    private StatusProposta status = StatusProposta.ABERTA;
    private Integer prazoPrometido;

    public Proposta(String id, String cliente, Linha linha, BigDecimal preco, long diaChegada, long validaAte,
                    int prazoPedido, int prazoFabrica) {
        this.id = Objects.requireNonNull(id);
        this.cliente = Objects.requireNonNull(cliente);
        this.linha = Objects.requireNonNull(linha);
        this.preco = Objects.requireNonNull(preco);
        this.diaChegada = diaChegada;
        this.validaAte = validaAte;
        this.prazoPedido = prazoPedido;
        this.prazoFabrica = prazoFabrica;
    }

    public void encerrar(StatusProposta novo, Integer prometido) {
        this.status = novo;
        this.prazoPrometido = prometido;
    }

    public boolean aberta() {
        return status == StatusProposta.ABERTA;
    }

    public String id() {
        return id;
    }

    public String cliente() {
        return cliente;
    }

    public Linha linha() {
        return linha;
    }

    public BigDecimal preco() {
        return preco;
    }

    public long diaChegada() {
        return diaChegada;
    }

    public long validaAte() {
        return validaAte;
    }

    public int prazoPedido() {
        return prazoPedido;
    }

    public int prazoFabrica() {
        return prazoFabrica;
    }

    public StatusProposta status() {
        return status;
    }

    public Integer prazoPrometido() {
        return prazoPrometido;
    }
}
