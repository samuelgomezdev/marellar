package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private BigDecimal hhRetrabalhoPendente = BigDecimal.ZERO;
    private BigDecimal hhQuebraPendente = BigDecimal.ZERO;
    private long horasParadoMaterial;
    private int prioridade;
    private boolean retido;
    private BigDecimal hhProduzidas = BigDecimal.ZERO;
    private BigDecimal hhRetrabalho = BigDecimal.ZERO;
    private BigDecimal hhApontadas = BigDecimal.ZERO;
    private long somaNiveisNoGate;
    private long horasNoGate;
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
        this.somaNiveisNoGate = 0;
        this.horasNoGate = 0;
        this.status = StatusBarco.EM_PRODUCAO;
    }

    public void iniciarRetrabalho(BigDecimal hh) {
        this.hhRetrabalhoPendente = hh;
        this.status = StatusBarco.EM_RETRABALHO;
    }

    public boolean trabalhando() {
        return status == StatusBarco.EM_PRODUCAO || status == StatusBarco.EM_RETRABALHO;
    }

    /**
     * Desconta as HH produzidas no gate atual (ou no retrabalho). Devolve quanto foi de fato usado,
     * nunca mais que o que faltava. {@code somaNiveis} e a soma dos niveis de quem trabalhou nesta hora.
     */
    public BigDecimal produzir(BigDecimal hhProducao, int horasPagas, int somaNiveis) {
        if (!trabalhando()) {
            throw new IllegalStateException("Barco " + id + " nao esta trabalhando: " + status);
        }
        hhApontadas = hhApontadas.add(BigDecimal.valueOf(horasPagas));
        if (status == StatusBarco.EM_RETRABALHO) {
            BigDecimal usado = hhProducao.min(hhRetrabalhoPendente);
            hhRetrabalhoPendente = hhRetrabalhoPendente.subtract(usado);
            hhRetrabalho = hhRetrabalho.add(usado);
            return usado;
        }
        // peca quebrada: o conserto vem antes do resto do gate e conta como retrabalho
        BigDecimal paraQuebra = hhProducao.min(hhQuebraPendente);
        hhQuebraPendente = hhQuebraPendente.subtract(paraQuebra);
        hhRetrabalho = hhRetrabalho.add(paraQuebra);
        hhProducao = hhProducao.subtract(paraQuebra);
        somaNiveisNoGate += somaNiveis;
        horasNoGate += horasPagas;
        BigDecimal usado = hhProducao.min(hhRestantesNoGate);
        hhRestantesNoGate = hhRestantesNoGate.subtract(usado);
        hhProduzidas = hhProduzidas.add(usado);
        return usado;
    }

    public void acrescentarQuebra(BigDecimal hh) {
        hhQuebraPendente = hhQuebraPendente.add(hh);
    }

    public void somarHoraParadoMaterial() {
        horasParadoMaterial++;
    }

    public BigDecimal hhQuebraPendente() {
        return hhQuebraPendente;
    }

    public long horasParadoMaterial() {
        return horasParadoMaterial;
    }

    public boolean trabalhoTerminado() {
        return status == StatusBarco.EM_RETRABALHO
                ? hhRetrabalhoPendente.signum() == 0
                : hhRestantesNoGate.signum() == 0 && hhQuebraPendente.signum() == 0;
    }

    /** Nivel medio de quem trabalhou no gate atual, arredondado (3 se ninguem trabalhou). */
    public int nivelMedioNoGate() {
        if (horasNoGate == 0) {
            return 3;
        }
        return BigDecimal.valueOf(somaNiveisNoGate)
                .divide(BigDecimal.valueOf(horasNoGate), 0, RoundingMode.HALF_EVEN)
                .intValue();
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

    public BigDecimal hhRetrabalhoPendente() {
        return hhRetrabalhoPendente;
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

    public BigDecimal hhRetrabalho() {
        return hhRetrabalho;
    }

    public BigDecimal hhApontadas() {
        return hhApontadas;
    }

    public List<CodigoGate> gatesAprovados() {
        return Collections.unmodifiableList(gatesAprovados);
    }
}
