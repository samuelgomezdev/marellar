package br.marellar.engine.regras;

import br.marellar.engine.Relogio;
import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.CatalogoNc.TipoNc;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.ParametrosLinha.BarcoInicial;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.estado.EstadoPartida.BarcoVisao;
import br.marellar.engine.estado.EstadoPartida.NcVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QualidadeRegraTest {

    private static final ParametrosJogo PARAMETROS = CarregadorDados.carregarDoClasspath();
    private static final String BARCO = "33-100";

    /**
     * Partida com 1 barco no RE e um catalogo com um unico tipo de NC que sempre acontece no RE.
     * {@code deteccao} e a deteccao base para todas as gravidades (1 = sempre ve, 0 = nunca ve).
     */
    private static Simulacao partidaCom(Gravidade gravidade, Origem origem, String item, double deteccao) {
        TipoNc tipo = new TipoNc("T-1", "NC de teste", gravidade, origem, List.of(CodigoGate.RE),
                BigDecimal.ONE, new BigDecimal("10"), item, new BigDecimal("100.00"));
        BigDecimal d = BigDecimal.valueOf(deteccao);
        CatalogoNc catalogo = PARAMETROS.catalogoNc().comTipos(List.of(tipo))
                .comDeteccaoBase(Map.of(Gravidade.LEVE, d, Gravidade.MEDIA, d, Gravidade.CRITICA, d));
        ParametrosJogo p = PARAMETROS.comCatalogoNc(catalogo)
                .comLinha33(PARAMETROS.linha33().comBarcosIniciais(List.of(new BarcoInicial(BARCO, "Teste", CodigoGate.RE))));
        return Simulacao.novaPartida(7, p);
    }

    /** RE tem 38 HH: com 6 pessoas termina no 1o dia, e a inspecao leva 2 ticks. */
    private static ResultadoAvanco ateAInspecaoTerminar(Simulacao sim) {
        ResultadoAvanco r = null;
        for (int i = 0; i < 3 * Relogio.TICKS_POR_DIA; i++) {
            r = sim.avancar(1);
            StatusBarco s = sim.barco(BARCO).status();
            if (s != StatusBarco.EM_PRODUCAO && s != StatusBarco.AGUARDANDO_INSPECAO && s != StatusBarco.EM_INSPECAO) {
                return r;
            }
        }
        throw new AssertionError("A inspecao nao terminou");
    }

    private static NcVisao unicaNc(Simulacao sim) {
        List<NcVisao> ncs = sim.barco(BARCO).ncs();
        assertThat(ncs).hasSize(1);
        return ncs.getFirst();
    }

    @Test
    void mesmaSementeGeraAsMesmasNcsEDeteccoes() {
        Simulacao a = Simulacao.novaPartida(2027);
        Simulacao b = Simulacao.novaPartida(2027);

        for (Simulacao s : List.of(a, b)) {
            for (int i = 0; i < 2 * Relogio.DIAS_UTEIS_POR_MES; i++) {
                s.avancar(Relogio.TICKS_POR_DIA);
            }
        }

        assertThat(a.indicadoresQualidade().ncsGeradas()).isPositive();
        assertThat(a.indicadoresQualidade()).isEqualTo(b.indicadoresQualidade());
        assertThat(a.estado()).isEqualTo(b.estado());
        assertThat(a.eventosDesde(0)).isEqualTo(b.eventosDesde(0));
    }

    @Test
    void ncCriticaDetectadaPausaETravaOGate() {
        Simulacao sim = partidaCom(Gravidade.CRITICA, Origem.PROCESSO, null, 1.0);

        ResultadoAvanco r = ateAInspecaoTerminar(sim);

        assertThat(r.pausado()).isTrue();
        assertThat(r.motivoPausa()).isEqualTo(MotivoPausa.NC_CRITICA);
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.NC_CRITICA);

        // sem decisao, o gate nao aprova nunca
        for (int i = 0; i < 10; i++) {
            sim.avancar(Relogio.TICKS_POR_DIA);
        }
        BarcoVisao barco = sim.barco(BARCO);
        assertThat(barco.status()).isEqualTo(StatusBarco.AGUARDANDO_DECISAO);
        assertThat(barco.gateAtual()).isEqualTo(CodigoGate.RE);
        assertThat(barco.gatesAprovados()).isEmpty();
        assertThat(unicaNc(sim).status()).isEqualTo(StatusNc.ABERTA);
    }

    @Test
    void ncCriticaNaoAceitaConcessao() {
        Simulacao sim = partidaCom(Gravidade.CRITICA, Origem.PROCESSO, null, 1.0);
        ateAInspecaoTerminar(sim);
        String nc = unicaNc(sim).id();

        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.CONCESSAO)))
                .isInstanceOf(OrdemInvalidaException.class)
                .hasMessageContaining("critica");
    }

    @Test
    void retrabalhoAdicionaHhECustoEAprovaOGateAoTerminar() {
        Simulacao sim = partidaCom(Gravidade.CRITICA, Origem.PROCESSO, null, 1.0);
        ateAInspecaoTerminar(sim);
        String nc = unicaNc(sim).id();

        sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.RETRABALHO));

        BarcoVisao emRetrabalho = sim.barco(BARCO);
        assertThat(emRetrabalho.status()).isEqualTo(StatusBarco.EM_RETRABALHO);
        assertThat(emRetrabalho.hhRetrabalhoPendente()).isEqualByComparingTo("10");

        sim.avancar(Relogio.TICKS_POR_DIA);

        BarcoVisao depois = sim.barco(BARCO);
        assertThat(depois.gatesAprovados()).containsExactly(CodigoGate.RE);
        assertThat(depois.gateAtual()).isEqualTo(CodigoGate.DM);
        assertThat(depois.hhRetrabalho()).isEqualByComparingTo("10");
        assertThat(unicaNc(sim).status()).isEqualTo(StatusNc.RESOLVIDA);
        assertThat(sim.indicadoresQualidade().custoMaterialRetrabalho()).isEqualByComparingTo("100.00");
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.RETRABALHO_CONCLUIDO);
    }

    @Test
    void concessaoLiberaNaHoraEDerrubaAMoralDoInspetor() {
        Simulacao sim = partidaCom(Gravidade.LEVE, Origem.PROCESSO, null, 1.0);
        ateAInspecaoTerminar(sim);
        int moralAntes = sim.estado().inspetor().moral();
        String nc = unicaNc(sim).id();

        sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.CONCESSAO));

        assertThat(sim.barco(BARCO).gateAtual()).isEqualTo(CodigoGate.DM);
        assertThat(unicaNc(sim).status()).isEqualTo(StatusNc.CONCEDIDA);
        assertThat(sim.estado().qualidade().concessoes()).isEqualTo(1);
        // paciencia 40: queda de (100 - 40) x 0,10 = 6
        assertThat(sim.estado().inspetor().moral()).isEqualTo(moralAntes - 6);
    }

    @Test
    void rncSoValeParaNcDeFornecedor() {
        Simulacao processo = partidaCom(Gravidade.MEDIA, Origem.PROCESSO, null, 1.0);
        ateAInspecaoTerminar(processo);
        String ncProcesso = unicaNc(processo).id();
        assertThatThrownBy(() -> processo.aplicarOrdem(new Ordem.DecidirNc(ncProcesso, DecisaoNc.RNC_FORNECEDOR)))
                .isInstanceOf(OrdemInvalidaException.class);

        Simulacao fornecedor = partidaCom(Gravidade.MEDIA, Origem.FORNECEDOR, "gelcoat", 1.0);
        ateAInspecaoTerminar(fornecedor);
        NcVisao nc = unicaNc(fornecedor);
        assertThat(nc.fornecedorId()).isEqualTo("F02");

        fornecedor.aplicarOrdem(new Ordem.DecidirNc(nc.id(), DecisaoNc.RNC_FORNECEDOR));

        assertThat(fornecedor.estado().qualidade().rncs()).isEqualTo(1);
        assertThat(fornecedor.barco(BARCO).status()).isEqualTo(StatusBarco.EM_RETRABALHO);
        assertThat(fornecedor.eventosDesde(0))
                .anyMatch(e -> e.tipo() == TipoEvento.RNC_ABERTA && e.mensagem().contains("Resinas Vale Barato"));
    }

    @Test
    void ncNaoDetectadaEscapaSemOJogadorVer() {
        Simulacao sim = partidaCom(Gravidade.MEDIA, Origem.PROCESSO, null, 0.0);

        sim.avancar(2 * Relogio.TICKS_POR_DIA);

        BarcoVisao barco = sim.barco(BARCO);
        assertThat(barco.gatesAprovados()).containsExactly(CodigoGate.RE);
        assertThat(barco.ncs()).isEmpty();
        assertThat(sim.estado().qualidade().ncsDetectadas()).isZero();
        assertThat(sim.indicadoresQualidade().ncsEscapadas()).isEqualTo(1);
    }

    @Test
    void ncSoPodeSerDecididaUmaVez() {
        Simulacao sim = partidaCom(Gravidade.LEVE, Origem.PROCESSO, null, 1.0);
        ateAInspecaoTerminar(sim);
        String nc = unicaNc(sim).id();
        sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.RETRABALHO));

        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.CONCESSAO)))
                .isInstanceOf(OrdemInvalidaException.class)
                .hasMessageContaining("ja foi decidida");
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.DecidirNc("NC-9999", DecisaoNc.RETRABALHO)))
                .isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void duasNcsNoMesmoGateTemIdsDiferentes() {
        TipoNc a = new TipoNc("T-A", "A", Gravidade.LEVE, Origem.PROCESSO, List.of(CodigoGate.RE),
                BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE);
        TipoNc b = new TipoNc("T-B", "B", Gravidade.LEVE, Origem.PROCESSO, List.of(CodigoGate.RE),
                BigDecimal.ONE, BigDecimal.ONE, null, BigDecimal.ONE);
        CatalogoNc catalogo = PARAMETROS.catalogoNc().comTipos(List.of(a, b))
                .comDeteccaoBase(Map.of(Gravidade.LEVE, BigDecimal.ONE, Gravidade.MEDIA, BigDecimal.ONE,
                        Gravidade.CRITICA, BigDecimal.ONE));
        Simulacao sim = Simulacao.novaPartida(7, PARAMETROS.comCatalogoNc(catalogo)
                .comLinha33(PARAMETROS.linha33().comBarcosIniciais(List.of(new BarcoInicial(BARCO, "T", CodigoGate.RE)))));
        ateAInspecaoTerminar(sim);

        List<NcVisao> ncs = sim.barco(BARCO).ncs();
        assertThat(ncs).extracting(NcVisao::id).containsExactly("NC-0001", "NC-0002");

        sim.aplicarOrdem(new Ordem.DecidirNc("NC-0001", DecisaoNc.RETRABALHO));
        assertThat(sim.barco(BARCO).status()).isEqualTo(StatusBarco.AGUARDANDO_DECISAO); // falta a NC-0002
        sim.aplicarOrdem(new Ordem.DecidirNc("NC-0002", DecisaoNc.CONCESSAO));
        assertThat(sim.barco(BARCO).status()).isEqualTo(StatusBarco.EM_RETRABALHO);
        assertThat(sim.barco(BARCO).hhRetrabalhoPendente()).isEqualByComparingTo("1"); // so a NC-0001
    }

    @Test
    void equipeDeNivelBaixoGeraMaisNc() {
        CatalogoNc c = PARAMETROS.catalogoNc();
        assertThat(c.fatorDoNivelDaEquipe(1)).isGreaterThan(c.fatorDoNivelDaEquipe(3));
        assertThat(c.fatorDoNivelDaEquipe(5)).isLessThan(c.fatorDoNivelDaEquipe(3));
    }
}
