package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosVendas;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Evento;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VendasRegraTest {

    private static final ParametrosVendas V = Cenarios.PADRAO.vendas();
    private static final int MES = Relogio.DIAS_UTEIS_POR_MES;
    private static final Set<TipoEvento> DE_VENDAS = Set.of(TipoEvento.PROPOSTA, TipoEvento.VENDA,
            TipoEvento.DESISTENCIA, TipoEvento.CASCO_NOVO, TipoEvento.MULTA);

    /** Mercado grande: toda semana o vendedor tem cliente, e a capacidade declarada e quem limita. */
    private static ParametrosJogo mercadoAquecido(ParametrosJogo p) {
        return p.comVendas(p.vendas().comMercado(new BigDecimal("650"), new BigDecimal("20")));
    }

    // ----- multa -----

    @Test
    void multaDe3SemanasE1Virgula5PorCento() {
        assertThat(VendasRegra.percentualDaMulta(V, 3)).isEqualByComparingTo("1.5");
        assertThat(VendasRegra.multa(V, new BigDecimal("800000.00"), 3)).isEqualByComparingTo("12000.00");
    }

    @Test
    void multaDe30SemanasParaNoTetoDe10PorCento() {
        assertThat(VendasRegra.percentualDaMulta(V, 30)).isEqualByComparingTo("10");
        assertThat(VendasRegra.percentualDaMulta(V, 20)).isEqualByComparingTo("10");
        assertThat(VendasRegra.percentualDaMulta(V, 19)).isEqualByComparingTo("9.5");
    }

    @Test
    void semanaIniciadaJaConta() {
        assertThat(VendasRegra.semanasDeAtraso(V, 0)).isZero();
        assertThat(VendasRegra.semanasDeAtraso(V, -4)).isZero();
        assertThat(VendasRegra.semanasDeAtraso(V, 1)).isEqualTo(1);
        assertThat(VendasRegra.semanasDeAtraso(V, 5)).isEqualTo(1);
        assertThat(VendasRegra.semanasDeAtraso(V, 6)).isEqualTo(2);
        assertThat(VendasRegra.semanasDeAtraso(V, 15)).isEqualTo(3);
    }

    @Test
    void barcoEntregueAtrasadoPagaMultaNoCaixaNaDreENaFicha() {
        // um barco no TR com prazo de 1 dia: chega a ET atrasado
        ParametrosJogo p = Cenarios.semAcaso(Cenarios.umBarcoNoTr(Cenarios.PADRAO));
        p = p.comVendas(p.vendas().comPrazosIniciais(Map.of("33-100", 1)).comMercado(BigDecimal.ONE, BigDecimal.ZERO));
        Simulacao sim = Simulacao.novaPartida(1, p);

        avancarDias(sim, 2 * MES);

        EstadoPartida.BarcoVisao barco = sim.estado().barcos().getFirst();
        assertThat(barco.status().name()).isEqualTo("ENTREGUE");
        assertThat(barco.semanasAtraso()).isPositive();
        BigDecimal esperada = VendasRegra.multa(V, new BigDecimal("800000.00"), barco.semanasAtraso());
        assertThat(barco.multa()).isEqualByComparingTo(esperada).isPositive();
        assertThat(sim.lancamentos()).anyMatch(l -> l.tipo() == TipoLancamento.MULTA_ATRASO
                && l.valor().compareTo(esperada.negate()) == 0);
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.MULTA && "33-100".equals(e.barcoId()));
        assertThat(sim.conselhos().stream().map(c -> c.dre().multas()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(esperada);
    }

    @Test
    void barcoNoPrazoNaoPagaMulta() {
        ParametrosJogo p = Cenarios.semAcaso(Cenarios.umBarcoNoTr(Cenarios.PADRAO));
        p = p.comVendas(p.vendas().comPrazosIniciais(Map.of("33-100", 200)).comMercado(BigDecimal.ONE, BigDecimal.ZERO));
        Simulacao sim = Simulacao.novaPartida(1, p);

        avancarDias(sim, 2 * MES);

        assertThat(sim.estado().barcos().getFirst().multa()).isZero();
        assertThat(sim.lancamentos()).noneMatch(l -> l.tipo() == TipoLancamento.MULTA_ATRASO);
    }

    // ----- desistencia -----

    @Test
    void chanceDeDesistenciaCresceDe1Virgula2Ate1Virgula6() {
        assertThat(VendasRegra.chanceDesistencia(V, 100, 100)).isZero();
        assertThat(VendasRegra.chanceDesistencia(V, 120, 100)).isZero();
        assertThat(VendasRegra.chanceDesistencia(V, 140, 100)).isEqualByComparingTo("0.5");
        assertThat(VendasRegra.chanceDesistencia(V, 160, 100)).isEqualByComparingTo("1");
        assertThat(VendasRegra.chanceDesistencia(V, 200, 100)).isEqualByComparingTo("1");
    }

    @Test
    void desistenciaEDeterministaComASemente() {
        List<String> a = respostasComPrazoDaFabrica(5);
        List<String> b = respostasComPrazoDaFabrica(5);
        assertThat(a).isNotEmpty().isEqualTo(b);
    }

    /** Joga 6 meses respondendo toda proposta com o prazo da fabrica; devolve como cada uma terminou. */
    private static List<String> respostasComPrazoDaFabrica(long semente) {
        Simulacao sim = Simulacao.novaPartida(semente, mercadoAquecido(Cenarios.PADRAO));
        List<String> resultado = new ArrayList<>();
        for (int d = 0; d < 6 * MES; d++) {
            avancarDias(sim, 1);
            for (var proposta : sim.estado().vendas().propostas()) {
                sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(), proposta.prazoFabricaDias()));
            }
        }
        sim.eventosDesde(0).stream().filter(e -> e.tipo() == TipoEvento.VENDA || e.tipo() == TipoEvento.DESISTENCIA)
                .forEach(e -> resultado.add(e.tick() + " " + e.mensagem()));
        return resultado;
    }

    @Test
    void prometerOPrazoDoClienteNuncaFazDesistir() {
        Simulacao sim = Simulacao.novaPartida(3, mercadoAquecido(Cenarios.PADRAO));
        for (int d = 0; d < 4 * MES; d++) {
            avancarDias(sim, 1);
            for (var proposta : sim.estado().vendas().propostas()) {
                sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(),
                        Math.min(proposta.prazoPedidoDias(), proposta.prazoFabricaDias())));
            }
        }
        assertThat(sim.estado().vendas().vendasFechadas()).isPositive();
        assertThat(sim.estado().vendas().desistencias()).isZero();
    }

    @Test
    void prazoForaDaFaixaERecusado() {
        Simulacao sim = Simulacao.novaPartida(3, mercadoAquecido(Cenarios.PADRAO));
        var proposta = esperarProposta(sim);
        int minimo = Math.min(proposta.prazoPedidoDias(), proposta.prazoFabricaDias());

        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(), minimo - 1)))
                .isInstanceOf(OrdemInvalidaException.class);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(), proposta.prazoFabricaDias() + 1)))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    // ----- propostas e capacidade -----

    @Test
    void propostaSemRespostaExpiraEOClienteVaiEmbora() {
        Simulacao sim = Simulacao.novaPartida(3, mercadoAquecido(Cenarios.PADRAO));
        var proposta = esperarProposta(sim);

        avancarDias(sim, V.validadePropostaDiasUteis() + 1);

        assertThat(sim.estado().vendas().propostas()).noneMatch(p -> p.id().equals(proposta.id()));
        assertThat(sim.estado().vendas().propostasExpiradas()).isPositive();
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(), proposta.prazoFabricaDias())))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void capacidadeSoMudaNoFechamentoDoMes() {
        Simulacao sim = Simulacao.novaPartida(3, Cenarios.PADRAO);
        avancarDias(sim, 5);

        sim.aplicarOrdem(new Ordem.DeclararCapacidade(3));

        assertThat(sim.estado().vendas().capacidadeVigente()).isEqualTo(V.capacidadeInicial());
        assertThat(sim.estado().vendas().capacidadeDeclarada()).isEqualTo(3);
        avancarDias(sim, MES - 6);
        assertThat(sim.estado().vendas().capacidadeVigente()).isEqualTo(V.capacidadeInicial());
        avancarDias(sim, 2);   // virou o mes
        assertThat(sim.estado().vendas().capacidadeVigente()).isEqualTo(3);
    }

    @Test
    void capacidadeForaDaFaixaERecusada() {
        Simulacao sim = Simulacao.novaPartida(3, Cenarios.PADRAO);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DeclararCapacidade(0))).isInstanceOf(OrdemInvalidaException.class);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DeclararCapacidade(V.capacidadeMaxima() + 1)))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void vendedorNaoTrazMaisPropostasQueACapacidade() {
        Simulacao sim = Simulacao.novaPartida(3, mercadoAquecido(Cenarios.PADRAO));
        avancarDias(sim, 3 * MES);
        long propostas = sim.eventosDesde(0).stream()
                .filter(e -> e.tipo() == TipoEvento.PROPOSTA && e.mensagem().contains("quer um 33")).count();
        assertThat(propostas).isEqualTo(3L * V.capacidadeInicial());
    }

    @Test
    void pedidosEsperadosSeguemAPremissaEASazonalidade() {
        // 650 / 12 x 1,75% = 0,948 por mes; setembro (1,6) e o pico, fevereiro (0,5) o vale
        assertThat(VendasRegra.pedidosEsperados(V, 9)).isEqualByComparingTo("1.5167");
        assertThat(VendasRegra.pedidosEsperados(V, 2)).isEqualByComparingTo("0.4740");
    }

    @Test
    void prazoDaFabricaVemDaFilaEDaCapacidade() {
        assertThat(VendasRegra.prazoFabrica(V, 0, 1)).isEqualTo(V.cicloPlanejadoDiasUteis());
        assertThat(VendasRegra.prazoFabrica(V, 4, 1)).isEqualTo(5 * MES);
        assertThat(VendasRegra.prazoFabrica(V, 4, 2)).isEqualTo(3 * MES);
    }

    // ----- carteira e cascos novos -----

    @Test
    void numaPartidaDe3MesesEntramCascosNovosSemPassarDosMoldes() {
        ParametrosJogo p = mercadoAquecido(Cenarios.PADRAO);
        Simulacao sim = Simulacao.novaPartida(42, p);
        sim.aplicarOrdem(new Ordem.DeclararCapacidade(2));
        int maxNoDm = 0;
        for (int d = 0; d < 3 * MES; d++) {
            avancarDias(sim, 1);
            for (var proposta : sim.estado().vendas().propostas()) {
                sim.aplicarOrdem(new Ordem.AceitarProposta(proposta.id(),
                        Math.min(proposta.prazoPedidoDias(), proposta.prazoFabricaDias())));
            }
            maxNoDm = Math.max(maxNoDm, (int) sim.estado().barcos().stream()
                    .filter(b -> b.gateAtual() == CodigoGate.DM && !b.status().name().equals("ENTREGUE")).count());
        }

        EstadoPartida e = sim.estado();
        assertThat(e.barcos()).hasSizeGreaterThan(4);
        assertThat(e.barcos()).anyMatch(b -> b.id().equals("33-005") && b.prazoEntrega() != null);
        assertThat(maxNoDm).isLessThanOrEqualTo(V.moldes());
        assertThat(sim.eventosDesde(0)).anyMatch(ev -> ev.tipo() == TipoEvento.CASCO_NOVO);
        // contrato de 30% entra no caixa ao fechar a venda
        assertThat(sim.lancamentos()).anyMatch(l -> l.descricao().startsWith("Contrato de"));
    }

    @Test
    void vendasNaoMexemNosSorteiosDaProducao() {
        // sem pedidos x com pedidos (sem responder): producao, NCs e pessoas iguais no primeiro mes
        ParametrosJogo semMercado = Cenarios.PADRAO.comVendas(V.comMercado(BigDecimal.ONE, BigDecimal.ZERO));
        Simulacao a = Simulacao.novaPartida(42, semMercado);
        Simulacao b = Simulacao.novaPartida(42, mercadoAquecido(Cenarios.PADRAO));
        avancarDias(a, MES - 1);
        avancarDias(b, MES - 1);

        assertThat(a.estado().barcos()).isEqualTo(b.estado().barcos());
        assertThat(a.estado().pessoas()).isEqualTo(b.estado().pessoas());
        assertThat(semVendas(a.eventosDesde(0))).isEqualTo(semVendas(b.eventosDesde(0)));
    }

    private static List<String> semVendas(List<Evento> eventos) {
        return eventos.stream().filter(e -> !DE_VENDAS.contains(e.tipo())).map(e -> e.tick() + " " + e.mensagem()).toList();
    }

    private static EstadoPartida.PropostaVisao esperarProposta(Simulacao sim) {
        for (int d = 0; d < 3 * MES; d++) {
            avancarDias(sim, 1);
            if (!sim.estado().vendas().propostas().isEmpty()) {
                return sim.estado().vendas().propostas().getFirst();
            }
        }
        throw new AssertionError("Nenhuma proposta em 3 meses");
    }
}
