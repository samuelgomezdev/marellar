package br.marellar.engine;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosLinha.BarcoInicial;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.Velocidade;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulacaoTest {

    private static final ParametrosJogo PARAMETROS = CarregadorDados.carregarDoClasspath();

    /** Mesmos parametros, mas com probabilidade zero de NC (para testar so a producao). */
    static ParametrosJogo semNcs(ParametrosJogo p) {
        var c = p.catalogoNc();
        return p.comCatalogoNc(c.comTipos(c.tipos().stream()
                .map(t -> t.comProbabilidade(java.math.BigDecimal.ZERO)).toList()));
    }

    private static ParametrosJogo comBarcos(BarcoInicial... barcos) {
        return PARAMETROS.comLinha33(PARAMETROS.linha33().comBarcosIniciais(List.of(barcos)));
    }

    /** Avanca de dia em dia ate o barco ser entregue (ou estourar o limite). */
    private static void avancarAteEntregar(Simulacao sim, String barcoId, int limiteDias) {
        for (int dia = 0; dia < limiteDias; dia++) {
            if (sim.barco(barcoId).status() == StatusBarco.ENTREGUE) {
                return;
            }
            sim.avancar(Relogio.TICKS_POR_DIA);
        }
        throw new AssertionError("Barco " + barcoId + " nao foi entregue em " + limiteDias + " dias");
    }

    @Test
    void umBarcoSemNcPassaDeREaETcom1900HH() {
        Simulacao sim = Simulacao.novaPartida(42, semNcs(comBarcos(new BarcoInicial("33-100", "Teste", CodigoGate.RE))));

        avancarAteEntregar(sim, "33-100", 200);

        BarcoVisao barco = sim.barco("33-100");
        assertThat(barco.status()).isEqualTo(StatusBarco.ENTREGUE);
        assertThat(barco.hhProduzidas()).isEqualByComparingTo("1900");
        assertThat(barco.gatesAprovados()).containsExactly(CodigoGate.values());
        // horas pagas sao maiores que as HH padrao, porque ninguem rende 100%
        assertThat(barco.hhApontadas()).isGreaterThan(barco.hhProduzidas());
    }

    @Test
    void barcoPassaPelosGatesNaOrdem() {
        Simulacao sim = Simulacao.novaPartida(1, semNcs(comBarcos(new BarcoInicial("33-100", "Teste", CodigoGate.RE))));

        avancarAteEntregar(sim, "33-100", 200);

        List<CodigoGate> aprovados = sim.eventosDesde(0).stream()
                .filter(e -> e.tipo() == TipoEvento.GATE_APROVADO)
                .map(e -> CodigoGate.valueOf(e.mensagem().replaceAll(".*gate (\\w+) aprovado.*", "$1")))
                .toList();
        assertThat(aprovados).isEqualTo(Arrays.asList(CodigoGate.values()));
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.BARCO_ENTREGUE);
    }

    @Test
    void mesmaSementeMesmasOrdensMesmoEstado() {
        Simulacao a = Simulacao.novaPartida(42);
        Simulacao b = Simulacao.novaPartida(42);

        for (Simulacao s : List.of(a, b)) {
            s.avancar(37);
            s.aplicarOrdem(new Ordem.DefinirPrioridade("33-003", 1));
            s.aplicarOrdem(new Ordem.SegurarGate("33-001"));
            s.avancar(100);
        }

        assertThat(a.estado()).isEqualTo(b.estado());
        assertThat(a.eventosDesde(0)).isEqualTo(b.eventosDesde(0));
    }

    @Test
    void pessoasVaoParaOBarcoMaisPrioritarioAteOLimite() {
        Simulacao sim = Simulacao.novaPartida(42);
        sim.aplicarOrdem(new Ordem.DefinirPrioridade("33-004", 1));

        sim.avancar(1);

        EstadoPartida e = sim.estado();
        int limite = PARAMETROS.linha33().maxPessoasPorBarco();
        BarcoVisao primeiro = e.barcos().stream().filter(b -> b.id().equals("33-004")).findFirst().orElseThrow();
        assertThat(primeiro.prioridade()).isEqualTo(1);
        assertThat(primeiro.pessoasAlocadas()).hasSize(limite);
        long alocadas = e.pessoas().stream().filter(p -> p.alocadoEm() != null).count();
        assertThat(alocadas).isEqualTo(12); // 12 produtivos, 2 barcos x 6
    }

    @Test
    void definirPrioridadeReordenaAFila() {
        Simulacao sim = Simulacao.novaPartida(42);

        sim.aplicarOrdem(new Ordem.DefinirPrioridade("33-003", 1));

        assertThat(sim.estado().barcos())
                .extracting(BarcoVisao::id, BarcoVisao::prioridade)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("33-003", 1),
                        org.assertj.core.groups.Tuple.tuple("33-001", 2),
                        org.assertj.core.groups.Tuple.tuple("33-002", 3),
                        org.assertj.core.groups.Tuple.tuple("33-004", 4));
    }

    @Test
    void barcoSeguradoNaoSegueDepoisDeAprovadoAteSerLiberado() {
        Simulacao sim = Simulacao.novaPartida(42, comBarcos(new BarcoInicial("33-100", "Teste", CodigoGate.RE)));
        sim.aplicarOrdem(new Ordem.SegurarGate("33-100"));

        // RE tem 38 HH; com 6 pessoas termina no 1o dia e a inspecao leva 2 ticks
        sim.avancar(3 * Relogio.TICKS_POR_DIA);

        BarcoVisao segurado = sim.barco("33-100");
        assertThat(segurado.status()).isEqualTo(StatusBarco.APROVADO);
        assertThat(segurado.gateAtual()).isEqualTo(CodigoGate.RE);
        assertThat(segurado.gatesAprovados()).containsExactly(CodigoGate.RE);

        sim.aplicarOrdem(new Ordem.LiberarGate("33-100"));

        BarcoVisao liberado = sim.barco("33-100");
        assertThat(liberado.status()).isEqualTo(StatusBarco.EM_PRODUCAO);
        assertThat(liberado.gateAtual()).isEqualTo(CodigoGate.DM);
        assertThat(liberado.hhRestantesNoGate()).isEqualByComparingTo("532");
    }

    @Test
    void inspetorAtendeUmBarcoPorVezNaOrdemDeChegada() {
        ParametrosJogo p = comBarcos(
                new BarcoInicial("33-A", "A", CodigoGate.RE),
                new BarcoInicial("33-B", "B", CodigoGate.RE));
        // inspecao longa, para o segundo barco chegar enquanto o primeiro ainda esta sendo inspecionado
        Simulacao sim = Simulacao.novaPartida(42, p.comLinha33(p.linha33().comTicksInspecao(40)));

        sim.avancar(3 * Relogio.TICKS_POR_DIA);

        List<String> ordemDeChegada = sim.eventosDesde(0).stream()
                .filter(e -> e.tipo() == TipoEvento.GATE_CONCLUIDO)
                .map(e -> e.barcoId())
                .toList();
        EstadoPartida e = sim.estado();
        assertThat(ordemDeChegada).hasSize(2);
        assertThat(e.barcoEmInspecao()).isEqualTo(ordemDeChegada.get(0));
        assertThat(e.filaInspecao()).containsExactly(ordemDeChegada.get(1));
        assertThat(sim.barco(ordemDeChegada.get(0)).status()).isEqualTo(StatusBarco.EM_INSPECAO);
        assertThat(sim.barco(ordemDeChegada.get(1)).status()).isEqualTo(StatusBarco.AGUARDANDO_INSPECAO);
    }

    @Test
    void pausaSozinhoNoFimDoMes() {
        Simulacao sim = Simulacao.novaPartida(42);

        ResultadoAvanco r = sim.avancar(Relogio.TICKS_POR_MES + 50);

        assertThat(r.ticksExecutados()).isEqualTo(Relogio.TICKS_POR_MES);
        assertThat(r.pausado()).isTrue();
        assertThat(r.motivoPausa()).isEqualTo(MotivoPausa.FIM_DO_MES);
        assertThat(sim.estado().mes()).isEqualTo(2);
        assertThat(sim.estado().mesReferencia()).isEqualTo("abr/2027");
        var fimDoMes = sim.eventosDesde(0).getLast();
        assertThat(fimDoMes.tipo()).isEqualTo(TipoEvento.FIM_DO_MES);
        assertThat(fimDoMes.data()).isEqualTo(java.time.LocalDate.of(2027, 3, 29)); // 21o dia util
        assertThat(fimDoMes.hora()).isEqualTo(14);

        ResultadoAvanco continua = sim.avancar(8);
        assertThat(continua.pausado()).isFalse();
        assertThat(continua.ticksExecutados()).isEqualTo(8);
    }

    @Test
    void horasOciosasContamQuemFicouSemBarco() {
        Simulacao sim = Simulacao.novaPartida(42, semNcs(comBarcos(new BarcoInicial("33-100", "Teste", CodigoGate.DM))));

        sim.avancar(1);

        // 12 produtivos, limite de 6 por barco, 1 barco: 6 horas ociosas
        assertThat(sim.estado().horasOciosas()).isEqualByComparingTo("6");
    }

    @Test
    void ordensInvalidasSaoRecusadas() {
        Simulacao sim = Simulacao.novaPartida(42);

        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.SegurarGate("99-999")))
                .isInstanceOf(OrdemInvalidaException.class)
                .hasMessageContaining("99-999");
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DefinirPrioridade("33-001", 9)))
                .isInstanceOf(OrdemInvalidaException.class);
        assertThatThrownBy(() -> sim.avancar(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mudarVelocidadeApareceNoEstado() {
        Simulacao sim = Simulacao.novaPartida(42);

        sim.aplicarOrdem(new Ordem.MudarVelocidade(Velocidade.X4));

        assertThat(sim.estado().velocidade()).isEqualTo(Velocidade.X4);
    }

    @Test
    void estadoInicialTemOsBarcosEPessoasDosParametros() {
        EstadoPartida e = Simulacao.novaPartida(42).estado();

        assertThat(e.barcos()).hasSize(4);
        assertThat(e.pessoas()).hasSize(13);
        assertThat(e.data()).isEqualTo(Partida.INICIO_PADRAO);
        assertThat(e.mesReferencia()).isEqualTo("mar/2027");
        BarcoVisao bf = e.barcos().getFirst();
        assertThat(bf.gateAtual()).isEqualTo(CodigoGate.BF);
        assertThat(bf.gatesAprovados()).containsExactly(CodigoGate.RE, CodigoGate.DM, CodigoGate.BA);
        assertThat(bf.nomeGate()).isEqualTo("Fechamento do barco");
    }
}
