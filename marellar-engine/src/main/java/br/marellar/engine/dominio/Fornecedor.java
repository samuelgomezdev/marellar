package br.marellar.engine.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Fornecedor sintetico. O {@link PerfilFornecedor} e escondido: o jogador so ve o comportamento
 * (PPM, atrasos, resposta ao 8D) e a nota do scorecard.
 */
public final class Fornecedor {

    private final String id;
    private final String nome;
    private final PerfilFornecedor perfil;
    private final List<String> itens;
    private long pecasRecebidas;
    private long pecasRejeitadas;
    private int pedidosEntregues;
    private int pedidosNoPrazo;
    private int oitoDsRespondidos;
    private int oitoDsNoPrazo;
    private BigDecimal credito = BigDecimal.ZERO;
    private BigDecimal totalChargeback = BigDecimal.ZERO;
    private String nota = "-";
    private int pontos;

    public Fornecedor(String id, String nome, PerfilFornecedor perfil, List<String> itens) {
        this.id = Objects.requireNonNull(id);
        this.nome = nome;
        this.perfil = Objects.requireNonNull(perfil);
        this.itens = List.copyOf(itens);
    }

    public void registrarRecebimento(long pecas, long rejeitadas, boolean noPrazo) {
        pecasRecebidas += pecas;
        pecasRejeitadas += rejeitadas;
        pedidosEntregues++;
        if (noPrazo) {
            pedidosNoPrazo++;
        }
    }

    public void registrarResposta8d(boolean noPrazo) {
        oitoDsRespondidos++;
        if (noPrazo) {
            oitoDsNoPrazo++;
        }
    }

    public void lancarChargeback(BigDecimal valor) {
        credito = credito.add(valor);
        totalChargeback = totalChargeback.add(valor);
    }

    /** Usa o credito de chargeback numa compra; devolve o desconto aplicado. */
    public BigDecimal usarCredito(BigDecimal valorCompra) {
        BigDecimal desconto = credito.min(valorCompra);
        credito = credito.subtract(desconto);
        return desconto;
    }

    public void definirNota(String nota, int pontos) {
        this.nota = nota;
        this.pontos = pontos;
    }

    /** PPM = pecas rejeitadas / pecas recebidas x 1.000.000 (0 sem recebimento). */
    public long ppm() {
        if (pecasRecebidas == 0) {
            return 0;
        }
        return BigDecimal.valueOf(pecasRejeitadas).multiply(BigDecimal.valueOf(1_000_000))
                .divide(BigDecimal.valueOf(pecasRecebidas), 0, RoundingMode.HALF_EVEN).longValue();
    }

    public String id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public PerfilFornecedor perfil() {
        return perfil;
    }

    public List<String> itens() {
        return itens;
    }

    public long pecasRecebidas() {
        return pecasRecebidas;
    }

    public long pecasRejeitadas() {
        return pecasRejeitadas;
    }

    public int pedidosEntregues() {
        return pedidosEntregues;
    }

    public int pedidosNoPrazo() {
        return pedidosNoPrazo;
    }

    public int oitoDsRespondidos() {
        return oitoDsRespondidos;
    }

    public int oitoDsNoPrazo() {
        return oitoDsNoPrazo;
    }

    public BigDecimal credito() {
        return credito;
    }

    public BigDecimal totalChargeback() {
        return totalChargeback;
    }

    public String nota() {
        return nota;
    }

    public int pontos() {
        return pontos;
    }
}
