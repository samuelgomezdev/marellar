package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * RNC contra fornecedor (modelo automotivo, ate o chargeback).
 * Ao abrir: chargeback do custo documentado + taxa administrativa. O fornecedor tem
 * {@code prazo8d} para responder o 8D; 8D vencido em NC critica gera chargeback das horas paradas.
 */
public final class Rnc {

    private final String id;
    private final String ncId;
    private final String barcoId;
    private final String fornecedorId;
    private final Gravidade gravidade;
    private final BigDecimal custoDocumentado;
    private final BigDecimal chargeback;
    private final long diaAbertura;
    private final long prazo8d;
    private final long diaResposta8d;
    private StatusRnc status = StatusRnc.AGUARDANDO_8D;
    private BigDecimal chargebackHorasParadas = BigDecimal.ZERO;

    public Rnc(String id, String ncId, String barcoId, String fornecedorId, Gravidade gravidade,
               BigDecimal custoDocumentado, BigDecimal chargeback, long diaAbertura, long prazo8d, long diaResposta8d) {
        this.id = Objects.requireNonNull(id);
        this.ncId = ncId;
        this.barcoId = barcoId;
        this.fornecedorId = fornecedorId;
        this.gravidade = gravidade;
        this.custoDocumentado = custoDocumentado;
        this.chargeback = chargeback;
        this.diaAbertura = diaAbertura;
        this.prazo8d = prazo8d;
        this.diaResposta8d = diaResposta8d;
    }

    public void vencer(BigDecimal chargebackExtra) {
        status = StatusRnc.OITO_D_VENCIDO;
        chargebackHorasParadas = chargebackExtra;
    }

    public void responder() {
        status = status == StatusRnc.OITO_D_VENCIDO ? StatusRnc.RESPONDIDA_COM_ATRASO : StatusRnc.RESPONDIDA;
    }

    public boolean aguardandoResposta() {
        return status == StatusRnc.AGUARDANDO_8D || status == StatusRnc.OITO_D_VENCIDO;
    }

    public String id() {
        return id;
    }

    public String ncId() {
        return ncId;
    }

    public String barcoId() {
        return barcoId;
    }

    public String fornecedorId() {
        return fornecedorId;
    }

    public Gravidade gravidade() {
        return gravidade;
    }

    public BigDecimal custoDocumentado() {
        return custoDocumentado;
    }

    public BigDecimal chargeback() {
        return chargeback;
    }

    public long diaAbertura() {
        return diaAbertura;
    }

    public long prazo8d() {
        return prazo8d;
    }

    public long diaResposta8d() {
        return diaResposta8d;
    }

    public StatusRnc status() {
        return status;
    }

    public BigDecimal chargebackHorasParadas() {
        return chargebackHorasParadas;
    }
}
