package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.CatalogoNc.TipoNc;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosLinha.BarcoInicial;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.ResultadoMarca;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodadaRegraTest {

    private static final String BARCO = "33-100";
    /** Aparencia de um tipo de NC sem entrada em inspecao-rodada.json (o tipo de teste abaixo). */
    private static final String APARENCIA_DA_NC = "DEFEITO";

    /**
     * 1 barco no TR, uma NC de teste que sempre acontece (HH 10, material R$ 100), pausa da rodada ligada,
     * sem vendas. {@code deteccao}: deteccao do inspetor automatico (1 = sempre ve, 0 = nunca).
     */
    private static ParametrosJogo cenario(double deteccao) {
        ParametrosJogo base = Cenarios.semEventos(Cenarios.comPausaDaRodada(Cenarios.PADRAO));
        TipoNc tipo = new TipoNc("T-1", "NC de teste", Gravidade.MEDIA, Origem.PROCESSO, List.of(CodigoGate.TR),
                BigDecimal.ONE, new BigDecimal("10"), null, new BigDecimal("100.00"));
        BigDecimal d = BigDecimal.valueOf(deteccao);
        CatalogoNc catalogo = base.catalogoNc().comTipos(List.of(tipo))
                .comDeteccaoBase(Map.of(Gravidade.LEVE, d, Gravidade.MEDIA, d, Gravidade.CRITICA, d));
        return base.comCatalogoNc(catalogo)
                .comLinha33(base.linha33().comBarcosIniciais(List.of(new BarcoInicial(BARCO, "Teste", CodigoGate.TR))))
                .comVendas(base.vendas().comMercado(BigDecimal.ONE, BigDecimal.ZERO));
    }

    /** Avanca ate a pausa da rodada (o TR termina no 1o dia). */
    private static EstadoPartida.RodadaAbertaVisao ateARodada(Simulacao sim) {
        for (int i = 0; i < 3 * Relogio.TICKS_POR_DIA; i++) {
            ResultadoAvanco r = sim.avancar(1);
            if (r.motivoPausa() == MotivoPausa.RODADA_INSPECAO) {
                assertThat(sim.estado().barcoPausa()).isEqualTo(BARCO);
                return sim.estado().rodada().abertas().getFirst();
            }
        }
        throw new AssertionError("O gate nao terminou");
    }

    private static String marcaDaNc(EstadoPartida.RodadaAbertaVisao rodada) {
        return rodada.marcas().stream().filter(m -> m.aparencia().equals(APARENCIA_DA_NC)).findFirst().orElseThrow().id();
    }

    private static List<String> marcasFalsas(EstadoPartida.RodadaAbertaVisao rodada) {
        return rodada.marcas().stream().filter(m -> !m.aparencia().equals(APARENCIA_DA_NC)).map(m -> m.id()).toList();
    }

    /** Avanca 1 tick de cada vez enquanto o barco espera a inspecao. */
    private static void ateSairDaInspecao(Simulacao sim) {
        for (int i = 0; i < 2 * Relogio.TICKS_POR_DIA; i++) {
            StatusBarco s = sim.barco(BARCO).status();
            if (s != StatusBarco.AGUARDANDO_INSPECAO && s != StatusBarco.EM_INSPECAO) {
                return;
            }
            sim.avancar(1);
        }
        throw new AssertionError("O barco nao saiu da inspecao");
    }

    @Test
    void gateTerminadoPausaComUmaMarcaPorNcEMarcasFalsasEmPontosDiferentes() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(1));
        var rodada = ateARodada(sim);
        var r = Cenarios.PADRAO.rodada();

        assertThat(rodada.gate()).isEqualTo(CodigoGate.TR);
        assertThat(rodada.marcas()).filteredOn(m -> m.aparencia().equals(APARENCIA_DA_NC)).hasSize(1);
        assertThat(marcasFalsas(rodada)).hasSizeBetween(r.marcasFalsasMin(), r.marcasFalsasMax());
        assertThat(rodada.marcas()).extracting(EstadoPartida.MarcaVisao::pontoId).doesNotHaveDuplicates();
        assertThat(rodada.marcas()).allMatch(m -> m.aparencia().equals(APARENCIA_DA_NC) || r.aparenciasFalsas().contains(m.aparencia()));
        assertThat(sim.estado().rodada().pontos()).hasSize(r.pontos().size());
    }

    @Test
    void ncDoCatalogoVaiParaUmPontoCompativel() {
        var r = Cenarios.PADRAO.rodada();
        // o painel eletrico so pode estar no console; a vibracao do motor so na popa
        assertThat(r.tipos().get("ELE-PAINEL").pontos()).containsExactly("CONSOLE");
        assertThat(r.tipos().get("MOT-VIBRACAO").pontos()).containsExactly("POPA");
        for (var t : Cenarios.PADRAO.catalogoNc().tipos()) {
            assertThat(r.tipos()).containsKey(t.codigo());
        }
    }

    @Test
    void ncClicadaViraNcDetectada() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(0));   // o inspetor nunca veria: quem acha e o jogador
        var rodada = ateARodada(sim);

        sim.aplicarOrdem(new Ordem.InspecionarRodada(BARCO, List.of(marcaDaNc(rodada))));
        ateSairDaInspecao(sim);

        var barco = sim.barco(BARCO);
        assertThat(barco.status()).isEqualTo(StatusBarco.AGUARDANDO_DECISAO);
        assertThat(barco.ncs()).singleElement().satisfies(nc -> assertThat(nc.status()).isEqualTo(StatusNc.ABERTA));
        var resultado = sim.estado().rodada().ultimoResultado();
        assertThat(resultado.marcas()).filteredOn(m -> m.resultado() == ResultadoMarca.ACHOU).hasSize(1);
        assertThat(resultado.marcas()).filteredOn(m -> m.resultado() == ResultadoMarca.IGNOROU).hasSize(marcasFalsas(rodada).size());
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.RODADA && e.mensagem().contains("1 achada"));
    }

    @Test
    void ncNaoClicadaEscapaEVoltaNaGarantiaComCustoMaior() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(1));   // o inspetor veria, mas a rodada toma o lugar dele
        ateARodada(sim);

        sim.aplicarOrdem(new Ordem.InspecionarRodada(BARCO, List.of()));
        var resultado = sim.estado().rodada().ultimoResultado();
        // 3 x (10 HH x R$ 57 + R$ 100) = R$ 2.010
        assertThat(resultado.marcas()).filteredOn(m -> m.resultado() == ResultadoMarca.ESCAPOU).singleElement()
                .satisfies(m -> {
                    assertThat(m.descricao()).isEqualTo("NC de teste");
                    assertThat(m.custoGarantia()).isEqualByComparingTo("2010.00");
                });

        avancarDias(sim, 4 * Relogio.DIAS_UTEIS_POR_MES);   // TR, TM e ET, entrega e ate 60 dias uteis de garantia

        assertThat(sim.barco(BARCO).status()).isEqualTo(StatusBarco.ENTREGUE);
        assertThat(sim.barco(BARCO).ncs()).isEmpty();   // escapou: o jogador nao ve a NC
        assertThat(sim.lancamentos()).anyMatch(l -> l.tipo() == TipoLancamento.GARANTIA
                && l.valor().compareTo(new BigDecimal("-2010.00")) == 0);
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.GARANTIA && BARCO.equals(e.barcoId()));
        assertThat(sim.conselhos().stream().map(c -> c.dre().garantia()).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("2010.00");
    }

    @Test
    void escapeDoInspetorAutomaticoTambemVoltaNaGarantia() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(0));
        ateARodada(sim);   // o jogador deixa com o inspetor: so continua

        avancarDias(sim, 4 * Relogio.DIAS_UTEIS_POR_MES);

        assertThat(sim.estado().rodada().ultimoResultado()).isNull();
        assertThat(sim.lancamentos()).anyMatch(l -> l.tipo() == TipoLancamento.GARANTIA);
    }

    @Test
    void falsoAlarmeFazOBarcoEsperar() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(1));
        var rodada = ateARodada(sim);
        List<String> falsas = marcasFalsas(rodada);
        int horas = falsas.size() * Cenarios.PADRAO.rodada().horasPorFalsoAlarme();

        sim.aplicarOrdem(new Ordem.InspecionarRodada(BARCO, falsas));
        assertThat(sim.estado().rodada().ultimoResultado().horasDeEspera()).isEqualTo(horas);
        for (int i = 0; i < horas; i++) {
            sim.avancar(1);
            assertThat(sim.barco(BARCO).status()).isEqualTo(StatusBarco.EM_INSPECAO);
        }
        sim.avancar(1);
        assertThat(sim.barco(BARCO).status()).isNotEqualTo(StatusBarco.EM_INSPECAO);
    }

    @Test
    void mesmaSementeEMesmosCliquesDaoOMesmoResultado() {
        Simulacao a = Simulacao.novaPartida(11, Cenarios.comPausaDaRodada(Cenarios.PADRAO));
        Simulacao b = Simulacao.novaPartida(11, Cenarios.comPausaDaRodada(Cenarios.PADRAO));
        for (Simulacao sim : List.of(a, b)) {
            for (int i = 0; i < 30 * Relogio.TICKS_POR_DIA; i++) {
                ResultadoAvanco r = sim.avancar(1);
                if (r.motivoPausa() == MotivoPausa.RODADA_INSPECAO) {
                    var rodada = sim.estado().rodada().abertas().stream()
                            .filter(x -> x.barcoId().equals(sim.estado().barcoPausa())).findFirst().orElseThrow();
                    sim.aplicarOrdem(new Ordem.InspecionarRodada(rodada.barcoId(), List.of(rodada.marcas().getFirst().id())));
                }
            }
        }
        assertThat(a.estado()).isEqualTo(b.estado());
        assertThat(a.eventosDesde(0)).isEqualTo(b.eventosDesde(0));
        assertThat(a.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.RODADA);
    }

    @Test
    void semRodadaValeOInspetorEARodadaSomeDepois() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(1));
        ateARodada(sim);

        ateSairDaInspecao(sim);

        assertThat(sim.barco(BARCO).ncs()).singleElement().satisfies(nc -> assertThat(nc.status()).isEqualTo(StatusNc.ABERTA));
        assertThat(sim.estado().rodada().abertas()).isEmpty();
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.InspecionarRodada(BARCO, List.of())))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void marcaInexistenteERecusada() {
        Simulacao sim = Simulacao.novaPartida(7, cenario(1));
        ateARodada(sim);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.InspecionarRodada(BARCO, List.of("M99"))))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void rodadaNaoMexeNosSorteiosDaProducao() {
        // com pausa e sem resposta (o inspetor faz tudo) = sem pausa: mesma partida
        Simulacao a = Simulacao.novaPartida(42, Cenarios.PADRAO);
        Simulacao b = Simulacao.novaPartida(42, Cenarios.comPausaDaRodada(Cenarios.PADRAO));
        avancarDias(a, Relogio.DIAS_UTEIS_POR_MES - 1);
        avancarDias(b, Relogio.DIAS_UTEIS_POR_MES - 1);
        assertThat(a.estado().barcos()).isEqualTo(b.estado().barcos());
        assertThat(a.eventosDesde(0)).isEqualTo(b.eventosDesde(0));
    }
}
