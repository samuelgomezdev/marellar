package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.Relogio;
import br.marellar.engine.dados.ParametrosLinha;
import br.marellar.engine.dados.ParametrosVendas;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.PedidoCliente;
import br.marellar.engine.dominio.Proposta;
import br.marellar.engine.dominio.StatusProposta;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.relatorio.Dinheiro;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Vendas, prazo e multa (M14; DEC-0047 da wiki). Parametros em data/vendas.json.
 * <ul>
 *   <li>No inicio do mes vale a capacidade declarada no fechamento e o vendedor sorteia quantas propostas
 *       virao: mercado/12 x fatia x sazonalidade, no maximo a capacidade. Cada uma chega num dia util sorteado.</li>
 *   <li>Prazo da fabrica = o maior entre o ciclo planejado e ceil((barcos na frente + 1) / capacidade) meses.</li>
 *   <li>A fabrica responde dentro da validade: aceita com um prazo entre o pedido e o dela, ou recusa.
 *       Sem resposta, o cliente vai embora.</li>
 *   <li>Prazo prometido ate desistenciaInicio x o pedido: venda certa. Acima, a chance de desistir cresce
 *       em linha reta ate 100% em desistenciaTotal x.</li>
 *   <li>Venda fechada: a cota do contrato entra no caixa e o pedido vai para a carteira; vira casco quando
 *       ha molde livre (no maximo {@code moldes} barcos no DM).</li>
 *   <li>Na entrega: multa de multaPercentualPorSemana do preco por semana de atraso iniciada, ate o teto.</li>
 * </ul>
 * Os sorteios usam a sequencia propria de vendas (determinista com a semente).
 */
public final class VendasRegra {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);
    private static final BigDecimal DOZE = BigDecimal.valueOf(12);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Onde fica cada opcao de prazo na proposta. */
    public enum TipoOpcao { CLIENTE, SEM_DESISTENCIA, FABRICA }

    /** Uma opcao de prazo para a fabrica responder, com o risco de cada lado. */
    public record Opcao(TipoOpcao tipo, int prazoDias, BigDecimal chanceDesistencia, BigDecimal multaEstimada) {}

    private VendasRegra() {
    }

    // ----- a cada tick -----

    /** Inicio do tick: virada do mes, chegada das propostas do dia e cascos novos nos moldes livres. */
    public static void executar(Partida partida) {
        Relogio r = partida.relogio();
        if (r.tick() % Relogio.TICKS_POR_MES == 0) {
            inicioDoMes(partida);
        }
        if (r.horaDoTurno() == 0) {
            chegadas(partida);
        }
        abrirCascos(partida);
    }

    /** Fim do turno: proposta que venceu hoje sem resposta e perdida. */
    public static void fimDoDia(Partida partida) {
        for (Proposta p : partida.propostas()) {
            if (p.aberta() && partida.hoje() >= p.validaAte()) {
                p.encerrar(StatusProposta.EXPIRADA, null);
                partida.contarPropostaExpirada();
                partida.registrarEvento(TipoEvento.PROPOSTA, null, "eventos.propostaExpirada",
                        Map.of("cliente", p.cliente(), "proposta", p.id()));
            }
        }
    }

    static void inicioDoMes(Partida partida) {
        partida.aplicarCapacidadeDeclarada();
        ParametrosVendas v = partida.parametros().vendas();
        BigDecimal esperado = pedidosEsperados(v, partida.relogio().mesReferencia().getMonthValue());
        int quantos = esperado.setScale(0, RoundingMode.FLOOR).intValue();
        BigDecimal fracao = esperado.subtract(BigDecimal.valueOf(quantos));
        if (partida.aleatorioVendas().sortear(fracao)) {
            quantos++;
        }
        quantos = Math.min(quantos, partida.capacidadeVigente());
        long primeiroDia = partida.hoje();
        List<Long> dias = new ArrayList<>();
        for (int i = 0; i < quantos; i++) {
            dias.add(primeiroDia + partida.aleatorioVendas().proximoInt(Relogio.DIAS_UTEIS_POR_MES));
        }
        Collections.sort(dias);
        partida.chegadasDePropostas().clear();
        partida.chegadasDePropostas().addAll(dias);
    }

    /** Pedidos esperados no mes: mercadoInternoAnual / 12 x fatia / 100 x sazonalidade (mes de 1 a 12). */
    public static BigDecimal pedidosEsperados(ParametrosVendas v, int mesDoAno) {
        return v.mercadoInternoAnual().multiply(v.fatiaMercadoPercentual()).multiply(v.sazonalidade().get(mesDoAno - 1))
                .divide(DOZE.multiply(CEM), 4, RoundingMode.HALF_EVEN);
    }

    private static void chegadas(Partida partida) {
        Iterator<Long> it = partida.chegadasDePropostas().iterator();
        while (it.hasNext()) {
            if (it.next() != partida.hoje()) {
                continue;
            }
            it.remove();
            novaProposta(partida);
        }
    }

    private static void novaProposta(Partida partida) {
        ParametrosVendas v = partida.parametros().vendas();
        ParametrosLinha linha = partida.parametros().linha33();
        int pedido = partida.aleatorioVendas().proximoInt(v.prazoPedidoMinDiasUteis(), v.prazoPedidoMaxDiasUteis() + 1);
        long hoje = partida.hoje();
        Proposta p = new Proposta(partida.proximoIdProposta(), partida.proximoCliente(), linha.linha(), linha.precoVenda(),
                hoje, hoje + v.validadePropostaDiasUteis() - 1, pedido, prazoFabrica(partida));
        partida.propostas().add(p);
        partida.registrarEvento(TipoEvento.PROPOSTA, null, "eventos.propostaNova", Map.of(
                "vendedor", v.vendedor(), "cliente", p.cliente(), "semanas", String.valueOf(semanas(v, pedido)),
                "validade", data(partida, p.validaAte())));
    }

    /**
     * Prazo que a fabrica consegue hoje, em dias uteis: o maior entre o ciclo planejado e
     * ceil((barcos na frente + 1) / capacidade) meses. Barcos na frente = em producao + carteira.
     */
    public static int prazoFabrica(Partida partida) {
        int naFrente = partida.barcosPorPrioridade().size() + partida.carteira().size();
        return prazoFabrica(partida.parametros().vendas(), naFrente, partida.capacidadeVigente());
    }

    public static int prazoFabrica(ParametrosVendas v, int barcosNaFrente, int capacidade) {
        int meses = (barcosNaFrente + capacidade) / capacidade;   // ceil((n + 1) / c)
        return Math.max(v.cicloPlanejadoDiasUteis(), meses * Relogio.DIAS_UTEIS_POR_MES);
    }

    // ----- resposta da fabrica -----

    public static void declararCapacidade(Partida partida, int barcosPorMes) {
        int max = partida.parametros().vendas().capacidadeMaxima();
        if (barcosPorMes < 1 || barcosPorMes > max) {
            throw new OrdemInvalidaException("Capacidade fora da faixa (1 a " + max + "): " + barcosPorMes);
        }
        partida.declararCapacidade(barcosPorMes);
        partida.registrarEvento(TipoEvento.PROPOSTA, null, "eventos.capacidade",
                Map.of("capacidade", String.valueOf(barcosPorMes)));
    }

    /** Menor prazo que a fabrica pode prometer: o pedido pelo cliente (ou o dela, se for menor). */
    public static int prazoMinimo(Proposta p) {
        return Math.min(p.prazoPedido(), p.prazoFabrica());
    }

    public static void aceitar(Partida partida, String propostaId, int prazoDias) {
        Proposta p = propostaAberta(partida, propostaId);
        if (prazoDias < prazoMinimo(p) || prazoDias > p.prazoFabrica()) {
            throw new OrdemInvalidaException("Prazo fora da faixa (" + prazoMinimo(p) + " a " + p.prazoFabrica()
                    + " dias uteis): " + prazoDias);
        }
        ParametrosVendas v = partida.parametros().vendas();
        if (partida.aleatorioVendas().sortear(chanceDesistencia(v, prazoDias, p.prazoPedido()))) {
            p.encerrar(StatusProposta.DESISTIU, prazoDias);
            partida.contarDesistencia();
            partida.registrarEvento(TipoEvento.DESISTENCIA, null, "eventos.clienteDesistiu",
                    Map.of("cliente", p.cliente(), "semanas", String.valueOf(semanas(v, prazoDias))));
            return;
        }
        p.encerrar(StatusProposta.VENDIDA, prazoDias);
        long diaPrometido = partida.hoje() + prazoDias;
        partida.carteira().add(new PedidoCliente(p.id(), p.cliente(), p.linha(), p.preco(), diaPrometido));
        partida.contarVendaFechada();
        BigDecimal contrato = FinanceiroRegra.percentual(p.preco(), partida.parametros().linha33().cotaContratoPercentual());
        partida.lancar(TipoLancamento.COTA_CLIENTE, contrato, "Contrato de " + p.cliente() + " (" + p.id() + ")");
        partida.registrarEvento(TipoEvento.VENDA, null, "eventos.vendaFechada", Map.of("cliente", p.cliente(),
                "data", data(partida, diaPrometido), "valor", Dinheiro.formatar(contrato)));
    }

    public static void recusar(Partida partida, String propostaId) {
        Proposta p = propostaAberta(partida, propostaId);
        p.encerrar(StatusProposta.RECUSADA, null);
        partida.contarPropostaRecusada();
        partida.registrarEvento(TipoEvento.PROPOSTA, null, "eventos.propostaRecusada",
                Map.of("cliente", p.cliente(), "proposta", p.id()));
    }

    private static Proposta propostaAberta(Partida partida, String id) {
        Proposta p = partida.proposta(id).orElseThrow(() -> new OrdemInvalidaException("Proposta nao encontrada: " + id));
        if (!p.aberta()) {
            throw new OrdemInvalidaException("Proposta " + id + " ja foi encerrada: " + p.status());
        }
        return p;
    }

    /**
     * Chance de o cliente desistir: 0 ate desistenciaInicio x o pedido, 1 a partir de desistenciaTotal x,
     * em linha reta no meio.
     */
    public static BigDecimal chanceDesistencia(ParametrosVendas v, int prometido, int pedido) {
        BigDecimal razao = BigDecimal.valueOf(prometido).divide(BigDecimal.valueOf(pedido), 6, RoundingMode.HALF_EVEN);
        if (razao.compareTo(v.desistenciaInicio()) <= 0) {
            return BigDecimal.ZERO;
        }
        if (razao.compareTo(v.desistenciaTotal()) >= 0) {
            return BigDecimal.ONE;
        }
        return razao.subtract(v.desistenciaInicio())
                .divide(v.desistenciaTotal().subtract(v.desistenciaInicio()), 4, RoundingMode.HALF_EVEN);
    }

    /** Opcoes de prazo para a tela da proposta: o pedido, o ultimo dia sem desistencia e o da fabrica. */
    public static List<Opcao> opcoes(ParametrosVendas v, Proposta p) {
        int minimo = prazoMinimo(p);
        int semDesistencia = BigDecimal.valueOf(p.prazoPedido()).multiply(v.desistenciaInicio())
                .setScale(0, RoundingMode.FLOOR).intValue();
        List<Opcao> lista = new ArrayList<>();
        int[] prazos = {p.prazoPedido(), semDesistencia, p.prazoFabrica()};
        TipoOpcao[] tipos = TipoOpcao.values();
        for (int i = 0; i < prazos.length; i++) {
            int prazo = prazos[i];
            if (prazo < minimo || prazo > p.prazoFabrica()) {
                continue;
            }
            if (!lista.isEmpty() && lista.get(lista.size() - 1).prazoDias() == prazo) {
                lista.remove(lista.size() - 1);   // mesmo prazo: fica o tipo mais a direita (o mais seguro)
            }
            int atraso = Math.max(0, p.prazoFabrica() - prazo);
            lista.add(new Opcao(tipos[i], prazo, chanceDesistencia(v, prazo, p.prazoPedido()),
                    multa(v, p.preco(), semanasDeAtraso(v, atraso))));
        }
        return lista;
    }

    // ----- carteira e cascos novos -----

    /** Pedido da carteira entra num molde livre (no maximo {@code moldes} barcos no DM ao mesmo tempo). */
    static void abrirCascos(Partida partida) {
        int moldes = partida.parametros().vendas().moldes();
        while (!partida.carteira().isEmpty() && moldesOcupados(partida) < moldes) {
            PedidoCliente pedido = partida.carteira().remove(0);
            ParametrosLinha linha = partida.parametros().linha33();
            Barco barco = new Barco(partida.proximoIdBarco(), pedido.linha(), pedido.cliente(), CodigoGate.DM,
                    linha.hhDoGate(CodigoGate.DM), partida.ultimaPrioridade() + 1);
            barco.definirContrato(pedido.diaPrometido(), pedido.preco());
            partida.adicionarBarco(barco);
            partida.registrarEvento(TipoEvento.CASCO_NOVO, barco.id(), "eventos.cascoNovo", Map.of("barco", barco.id(),
                    "cliente", pedido.cliente(), "data", data(partida, pedido.diaPrometido())));
            FornecedorRegra.consumirMaterial(partida, barco);
        }
    }

    public static int moldesOcupados(Partida partida) {
        return (int) partida.barcosPorPrioridade().stream().filter(b -> b.gateAtual() == CodigoGate.DM).count();
    }

    // ----- entrega e multa -----

    /** Barco entregue: multa por semana de atraso iniciada, descontada do caixa. */
    public static void entregue(Partida partida, Barco barco) {
        if (barco.diaPrometido().isEmpty()) {
            barco.registrarEntrega(partida.hoje(), 0, BigDecimal.ZERO);
            return;
        }
        ParametrosVendas v = partida.parametros().vendas();
        int semanas = semanasDeAtraso(v, partida.hoje() - barco.diaPrometido().get());
        BigDecimal valor = multa(v, preco(partida, barco), semanas);
        barco.registrarEntrega(partida.hoje(), semanas, valor);
        if (valor.signum() == 0) {
            return;
        }
        partida.lancar(TipoLancamento.MULTA_ATRASO, valor.negate(), "Multa por atraso de " + barco.id());
        partida.registrarMulta(valor);
        partida.registrarEvento(TipoEvento.MULTA, barco.id(), "eventos.multa", Map.of("barco", barco.id(),
                "semanas", String.valueOf(semanas), "valor", Dinheiro.formatar(valor),
                "percentual", percentualDaMulta(v, semanas).stripTrailingZeros().toPlainString().replace('.', ',')));
    }

    /** Semanas de atraso, contando a semana iniciada (1 dia de atraso = 1 semana). */
    public static int semanasDeAtraso(ParametrosVendas v, long diasDeAtraso) {
        if (diasDeAtraso <= 0) {
            return 0;
        }
        return (int) ((diasDeAtraso + v.diasUteisPorSemana() - 1) / v.diasUteisPorSemana());
    }

    /** Percentual da multa: multaPercentualPorSemana x semanas, ate o teto. */
    public static BigDecimal percentualDaMulta(ParametrosVendas v, int semanas) {
        return v.multaPercentualPorSemana().multiply(BigDecimal.valueOf(semanas)).min(v.multaTetoPercentual());
    }

    public static BigDecimal multa(ParametrosVendas v, BigDecimal preco, int semanas) {
        return FinanceiroRegra.percentual(preco, percentualDaMulta(v, semanas));
    }

    /** Multa ate hoje, para a ficha: a final se ja foi entregue; senao a que valeria se entregasse hoje. */
    public static BigDecimal multaAteHoje(Partida partida, Barco barco) {
        if (!barco.ativo()) {
            return barco.multa();
        }
        return multa(partida.parametros().vendas(), preco(partida, barco), semanasAteHoje(partida, barco));
    }

    public static int semanasAteHoje(Partida partida, Barco barco) {
        if (!barco.ativo()) {
            return barco.semanasAtraso();
        }
        return barco.diaPrometido()
                .map(d -> semanasDeAtraso(partida.parametros().vendas(), partida.hoje() - d)).orElse(0);
    }

    private static BigDecimal preco(Partida partida, Barco barco) {
        return barco.precoVenda() != null ? barco.precoVenda() : partida.parametros().linha33().precoVenda();
    }

    private static int semanas(ParametrosVendas v, int dias) {
        return (dias + v.diasUteisPorSemana() - 1) / v.diasUteisPorSemana();
    }

    private static String data(Partida partida, long diaUtil) {
        return Relogio.somarDiasUteis(partida.relogio().inicio(), diaUtil).format(DATA);
    }
}
