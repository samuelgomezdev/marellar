package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/** Uma nao conformidade sorteada ao terminar um gate. O status muda com a inspecao e a decisao do jogador. */
public final class NaoConformidade {

    private final String id;
    private final String barcoId;
    private final CodigoGate gate;
    private final String tipo;
    private final String descricao;
    private final Gravidade gravidade;
    private final Origem origem;
    private final String fornecedorId;
    private final BigDecimal hhRetrabalho;
    private final BigDecimal custoMaterial;
    private StatusNc status = StatusNc.PENDENTE_INSPECAO;
    private DecisaoNc decisao;

    public NaoConformidade(String id, String barcoId, CodigoGate gate, String tipo, String descricao,
                           Gravidade gravidade, Origem origem, String fornecedorId,
                           BigDecimal hhRetrabalho, BigDecimal custoMaterial) {
        this.id = Objects.requireNonNull(id);
        this.barcoId = Objects.requireNonNull(barcoId);
        this.gate = Objects.requireNonNull(gate);
        this.tipo = tipo;
        this.descricao = descricao;
        this.gravidade = Objects.requireNonNull(gravidade);
        this.origem = Objects.requireNonNull(origem);
        this.fornecedorId = fornecedorId;
        this.hhRetrabalho = hhRetrabalho;
        this.custoMaterial = custoMaterial;
    }

    public void detectar() {
        exigir(StatusNc.PENDENTE_INSPECAO);
        status = StatusNc.ABERTA;
    }

    public void escapar() {
        exigir(StatusNc.PENDENTE_INSPECAO);
        status = StatusNc.ESCAPADA;
    }

    public void decidir(DecisaoNc d) {
        exigir(StatusNc.ABERTA);
        decisao = d;
        status = d == DecisaoNc.CONCESSAO ? StatusNc.CONCEDIDA : StatusNc.EM_RETRABALHO;
    }

    public void resolver() {
        exigir(StatusNc.EM_RETRABALHO);
        status = StatusNc.RESOLVIDA;
    }

    private void exigir(StatusNc esperado) {
        if (status != esperado) {
            throw new IllegalStateException("NC " + id + " esta " + status + ", esperado " + esperado);
        }
    }

    /** Ainda trava o gate: esperando inspecao, decisao ou retrabalho. */
    public boolean pendente() {
        return status == StatusNc.PENDENTE_INSPECAO || status == StatusNc.ABERTA || status == StatusNc.EM_RETRABALHO;
    }

    /** O jogador ve a NC (todas, menos as que escaparam do inspetor). */
    public boolean visivel() {
        return status != StatusNc.ESCAPADA && status != StatusNc.PENDENTE_INSPECAO;
    }

    public String id() {
        return id;
    }

    public String barcoId() {
        return barcoId;
    }

    public CodigoGate gate() {
        return gate;
    }

    public String tipo() {
        return tipo;
    }

    public String descricao() {
        return descricao;
    }

    public Gravidade gravidade() {
        return gravidade;
    }

    public Origem origem() {
        return origem;
    }

    public Optional<String> fornecedorId() {
        return Optional.ofNullable(fornecedorId);
    }

    public BigDecimal hhRetrabalho() {
        return hhRetrabalho;
    }

    public BigDecimal custoMaterial() {
        return custoMaterial;
    }

    public StatusNc status() {
        return status;
    }

    public Optional<DecisaoNc> decisao() {
        return Optional.ofNullable(decisao);
    }
}
