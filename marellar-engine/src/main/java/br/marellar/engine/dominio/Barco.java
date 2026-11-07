package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Um barco em producao. Entidade com estado; so as regras do motor mudam este objeto. */
public final class Barco {

    private final String id;
    private final Linha linha;
    private final String cliente;
    private CodigoGate gateAtual;
    private StatusBarco status;
    private BigDecimal hhTotalDoGate;
    private BigDecimal hhRestantesNoGate;
    private int prioridade;
    private boolean retido;
    private BigDecimal hhProduzidas = BigDecimal.ZERO;
    private BigDecimal hhApontadas = BigDecimal.ZERO;
    private final List<CodigoGate> gatesAprovados = new ArrayList<>();

    public Barco(String id, Linha linha, String cliente, CodigoGate gateInicial, BigDecimal hhDoGate, int prioridade) {
        this.id = Objects.requireNonNull(id);
        this.linha = Objects.requireNonNull(linha);
        this.cliente = cliente;
        this.prioridade = prioridade;
        for (CodigoGate g : CodigoGate.values()) {
            if (g.ordinal() < gateInicial.ordinal()) {
                gatesAprovados.add(g);
            }
        }
        iniciarGate(gateInicial, hhDoGate);
    }

    public void iniciarGate(CodigoGate gate, BigDecimal hhDoGate) {
        this.gateAtual = gate;
        this.hhTotalDoGate = hhDoGate;
        this.hhRestantesNoGate = hhDoGate;
        this.status = StatusBarco.EM_PRODUCAO;
    }

    /**
     * Desconta as HH produzidas no gate atual. Devolve quanto foi de fato usado
     * (nunca mais que o que faltava no gate).
     */
    public BigDecimal produzir(BigDecimal hhProducao, int horasPagas) {
        if (status != StatusBarco.EM_PRODUCAO) {
            throw new IllegalStateException("Barco " + id + " nao esta em producao: " + status);
        }
        BigDecimal usado = hhProducao.min(hhRestantesNoGate);
        hhRestantesNoGate = hhRestantesNoGate.subtract(usado);
        hhProduzidas = hhProduzidas.add(usado);
        hhApontadas = hhApontadas.add(BigDecimal.valueOf(horasPagas));
        return usado;
    }

    public boolean gateTerminado() {
        return hhRestantesNoGate.signum() == 0;
    }

    public void mudarStatus(StatusBarco novo) {
        this.status = novo;
    }

    public void registrarAprovacao() {
        gatesAprovados.add(gateAtual);
    }

    public void definirPrioridade(int prioridade) {
        this.prioridade = prioridade;
    }

    public void definirRetido(boolean retido) {
        this.retido = retido;
    }

    public boolean ativo() {
        return status != StatusBarco.ENTREGUE;
    }

    public String id() {
        return id;
    }

    public Linha linha() {
        return linha;
    }

    public String cliente() {
        return cliente;
    }

    public CodigoGate gateAtual() {
        return gateAtual;
    }

    public StatusBarco status() {
        return status;
    }

    public BigDecimal hhTotalDoGate() {
        return hhTotalDoGate;
    }

    public BigDecimal hhRestantesNoGate() {
        return hhRestantesNoGate;
    }

    public int prioridade() {
        return prioridade;
    }

    public boolean retido() {
        return retido;
    }

    public BigDecimal hhProduzidas() {
        return hhProduzidas;
    }

    public BigDecimal hhApontadas() {
        return hhApontadas;
    }

    public List<CodigoGate> gatesAprovados() {
        return Collections.unmodifiableList(gatesAprovados);
    }
}
