package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosEventos;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoEvento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;

class EventoRegraTest {

    @Test
    void taxasDeFrequenciaEGravidadeSeguemANbr14280() {
        BigDecimal hht = new BigDecimal("100000");

        assertThat(EventoRegra.taxaFrequencia(2, hht)).isEqualByComparingTo("20.00");
        assertThat(EventoRegra.taxaGravidade(15, hht)).isEqualByComparingTo("150.00");
        assertThat(EventoRegra.taxaFrequencia(3, BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void treinoVencidoEBaiaLotadaAumentamAChanceDeAcidente() {
        ParametrosEventos.Acidente regras = Cenarios.PADRAO.eventos().acidente();
        BigDecimal base = EventoRegra.probabilidadeAcidente(true, false, regras);

        assertThat(EventoRegra.probabilidadeAcidente(false, false, regras)).isEqualByComparingTo(base.multiply(new BigDecimal("2")));
        assertThat(EventoRegra.probabilidadeAcidente(true, true, regras)).isEqualByComparingTo(base.multiply(new BigDecimal("1.5")));
    }

    @Test
    void acidenteAfastaAPessoaDerrubaAMoralEPausa() {
        ParametrosJogo p = Cenarios.comEventos(Cenarios.semNcs(Cenarios.PADRAO),
                BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO);
        Simulacao sim = Simulacao.novaPartida(1, p);
        int moralAntes = sim.estado().pessoas().stream().filter(x -> x.id().equals("P12")).findFirst().orElseThrow().moral();

        ResultadoAvanco r = sim.avancar(8);

        assertThat(r.pausado()).isTrue();
        assertThat(r.motivoPausa()).isEqualTo(MotivoPausa.ACIDENTE);
        var estado = sim.estado();
        assertThat(estado.seguranca().acidentes()).isPositive();
        assertThat(estado.seguranca().diasPerdidos()).isPositive();
        assertThat(estado.seguranca().taxaFrequencia()).isPositive();
        assertThat(estado.pessoas().stream().filter(x -> x.id().equals("P01")).findFirst().orElseThrow().situacao())
                .isEqualTo(SituacaoPessoa.AFASTADA);
        assertThat(estado.pessoas().stream().filter(x -> x.id().equals("P12")).findFirst().orElseThrow().moral())
                .isLessThan(moralAntes);
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.ACIDENTE);
    }

    @Test
    void quebraDePecaViraRetrabalhoSemMudarAsHhPadraoDoBarco() {
        ParametrosJogo p = Cenarios.comEventos(Cenarios.umBarcoNoRe(Cenarios.semNcs(Cenarios.PADRAO)),
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        Simulacao sim = Simulacao.novaPartida(1, p);

        for (int i = 0; i < 200 && sim.barco("33-100").status() != StatusBarco.ENTREGUE; i++) {
            avancarDias(sim, 1);
        }

        var barco = sim.barco("33-100");
        assertThat(barco.status()).isEqualTo(StatusBarco.ENTREGUE);
        assertThat(barco.hhProduzidas()).isEqualByComparingTo("1900");
        // quebra em cada um dos 9 gates depois do RE, 12 HH cada
        assertThat(barco.hhRetrabalho()).isEqualByComparingTo("108");
        assertThat(sim.eventosDesde(0)).filteredOn(e -> e.tipo() == TipoEvento.QUEBRA_PECA).hasSize(9);
    }
}
