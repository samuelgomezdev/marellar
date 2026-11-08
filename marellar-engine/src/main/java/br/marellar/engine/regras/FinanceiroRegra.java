package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosFinanceiro;
import br.marellar.engine.dados.ParametrosLinha;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.CotaAReceber;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Pedido;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.relatorio.Dinheiro;
import br.marellar.engine.relatorio.Dre;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.Map;

/**
 * Financas (README 5.8):
 * <ul>
 *   <li>Receita reconhecida por gate aprovado (preco x peso de HH do gate).</li>
 *   <li>Cota do cliente liberada pelo gate aprovado entra no caixa depois de prazoRecebimentoDias.</li>
 *   <li>Material: custo na DRE quando o kit e consumido; sai do caixa quando o pedido chega.</li>
 *   <li>Folha (salarios + encargos) e despesas fixas no ultimo dia util do mes.</li>
 * </ul>
 * Dinheiro sempre em BigDecimal, escala 2, HALF_EVEN.
 */
public final class FinanceiroRegra {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    private FinanceiroRegra() {
    }

    public static void gateAprovado(Partida partida, Barco barco) {
        ParametrosLinha linha = partida.parametros().linha33();
        CodigoGate gate = barco.gateAtual();
        ParametrosLinha.Gate g = linha.gate(gate);
        partida.mes().somarReceita(percentual(linha.precoVenda(), g.pesoHhPercentual()));
        if (gate == CodigoGate.DM) {
            partida.mes().contarGateDm();
        }
        if (gate.ultimo()) {
            partida.mes().contarEntrega();
        }
        if (g.cotaPercentual().signum() > 0) {
            partida.cotasAReceber().add(new CotaAReceber(barco.id(), gate,
                    percentual(linha.precoVenda(), g.cotaPercentual()),
                    partida.hoje() + linha.prazoRecebimentoDias()));
        }
    }

    public static void materialConsumido(Partida partida, String item) {
        partida.mes().somarCustoMaterial(partida.parametros().linha33().valorDoKit(item));
    }

    public static void pagarCompra(Partida partida, Pedido pedido, String fornecedor) {
        partida.mes().somarCompras(pedido.valorBruto());
        partida.lancar(TipoLancamento.COMPRA_MATERIAL, pedido.valorPago().negate(),
                "Pedido " + pedido.id() + " (" + pedido.kits() + " kit(s) de " + pedido.item() + ", " + fornecedor + ")");
    }

    /** Cotas vencidas entram no caixa. */
    public static void fimDoDia(Partida partida) {
        Iterator<CotaAReceber> it = partida.cotasAReceber().iterator();
        while (it.hasNext()) {
            CotaAReceber cota = it.next();
            if (cota.diaRecebimento() > partida.hoje()) {
                continue;
            }
            it.remove();
            partida.lancar(TipoLancamento.COTA_CLIENTE, cota.valor(), "Cota de " + cota.barcoId() + " (gate " + cota.gate() + ")");
            partida.registrarEvento(TipoEvento.FINANCEIRO, cota.barcoId(), "eventos.cotaRecebida", Map.of(
                    "barco", cota.barcoId(), "valor", Dinheiro.formatar(cota.valor()), "gate", cota.gate().name()));
        }
    }

    /** Paga folha e despesas fixas e fecha a DRE do mes. */
    public static Dre fecharMes(Partida partida) {
        ParametrosFinanceiro regras = partida.parametros().financeiro();
        BigDecimal folha = folhaDoMes(partida);
        partida.lancar(TipoLancamento.FOLHA, folha.negate(), "Salários e encargos");
        partida.lancar(TipoLancamento.DESPESA_FIXA, regras.despesasFixasMensais().negate(), "Despesas fixas do mês");
        partida.registrarEvento(TipoEvento.FINANCEIRO, null, "eventos.salarios", Map.of("valor", Dinheiro.formatar(folha)));

        BigDecimal consumiveis = percentual(partida.mes().custoMaterial(), partida.parametros().linha33().percentualConsumiveis());
        return Dre.calcular(partida.mes().receita(), partida.mes().custoMaterial(), consumiveis,
                partida.mes().materialRetrabalho(), folha, regras.despesasFixasMensais(),
                partida.mes().chargebackRecuperado());
    }

    /** Salarios de quem esta na empresa (menos quem ainda nao chegou) x (1 + encargos). */
    public static BigDecimal folhaDoMes(Partida partida) {
        var pessoas = partida.parametros().pessoas();
        BigDecimal salarios = BigDecimal.ZERO;
        for (Pessoa p : partida.pessoas()) {
            if (!p.naEmpresa() || p.situacao() == SituacaoPessoa.A_CAMINHO) {
                continue;
            }
            salarios = salarios.add(p.funcao() == Funcao.INSPETOR ? pessoas.salarioInspetor() : pessoas.salarioPorNivel().get(p.nivel()));
        }
        return salarios.multiply(BigDecimal.ONE.add(partida.parametros().financeiro().encargosSobreSalarios()))
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal percentual(BigDecimal valor, BigDecimal percentual) {
        return valor.multiply(percentual).divide(CEM, 2, RoundingMode.HALF_EVEN);
    }
}
