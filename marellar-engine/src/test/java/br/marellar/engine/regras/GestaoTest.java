package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.relatorio.RelatorioConselho.Meta;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;

class GestaoTest {

    private static final int MES = Relogio.DIAS_UTEIS_POR_MES;

    @Test
    void metasDoMesAparecemDesdeOPrimeiroDiaComAtualEAlvo() {
        Simulacao sim = Simulacao.novaPartida(42, Cenarios.PADRAO);
        EstadoPartida.GestaoVisao g = sim.estado().gestao();
        var c = Cenarios.PADRAO.financeiro().conselho();

        assertThat(g.metasDoMes()).extracting(Meta::id).containsExactly("M-DM2", "M-CAIXA", "M-ACID", "M-RNC");
        Meta dm = g.metasDoMes().get(0);
        assertThat(dm.atual()).isEqualByComparingTo("0");
        assertThat(dm.alvo()).isEqualByComparingTo(BigDecimal.valueOf(c.metaCascosDm()));
        assertThat(dm.limite()).isFalse();
        assertThat(dm.cumprida()).isFalse();
        assertThat(g.metasDoMes().get(2).limite()).isTrue();
        assertThat(g.metasDoMes().get(2).cumprida()).isTrue();
        assertThat(g.metasPerdidas()).isEqualTo(1);
        assertThat(g.metasPerdidasParaMesRuim()).isEqualTo(c.metasPerdidasParaMesRuim());
        assertThat(g.ebitdaMesPassado()).isNull();
        assertThat(g.diaUtilDoMes()).isEqualTo(1);
    }

    @Test
    void metaDeCascosNoDmAcompanhaOMes() {
        Simulacao sim = Simulacao.novaPartida(42, Cenarios.PADRAO);
        for (int d = 0; d < MES - 1 && sim.estado().gestao().metasDoMes().get(0).atual().signum() == 0; d++) {
            avancarDias(sim, 1);
        }
        Meta dm = sim.estado().gestao().metasDoMes().get(0);
        assertThat(dm.atual()).isPositive();
        assertThat(dm.resultado()).startsWith(dm.atual().toPlainString() + " casco");
    }

    @Test
    void metasDaReuniaoTrazemAtualEAlvoEOMesSeguinteComecaDoZero() {
        Simulacao sim = Simulacao.novaPartida(42, Cenarios.PADRAO);
        avancarDias(sim, MES);

        var reuniao = sim.conselho().orElseThrow();
        for (Meta m : reuniao.metas()) {
            assertThat(m.atual()).isNotNull();
            assertThat(m.alvo()).isNotNull();
        }
        Meta acidentes = reuniao.metas().get(2);
        assertThat(acidentes.cumprida()).isEqualTo(acidentes.atual().compareTo(acidentes.alvo()) <= 0);

        EstadoPartida.GestaoVisao g = sim.estado().gestao();
        assertThat(g.ebitdaMesPassado()).isEqualByComparingTo(reuniao.dre().ebitda());
        assertThat(g.metasDoMes().get(0).atual()).isEqualByComparingTo("0");
        assertThat(g.acidentesDoMes()).isZero();
        assertThat(g.multasDoMes()).isZero();
        assertThat(g.diaUtilDoMes()).isEqualTo(1);
    }

    @Test
    void mesRuimDaReuniaoSegueAContagemDeMetasPerdidas() {
        Simulacao sim = Simulacao.novaPartida(42, Cenarios.PADRAO);
        int limite = Cenarios.PADRAO.financeiro().conselho().metasPerdidasParaMesRuim();
        for (int mes = 0; mes < 3; mes++) {
            avancarDias(sim, MES);
            var reuniao = sim.conselho().orElseThrow();
            long perdidas = reuniao.metas().stream().filter(m -> !m.cumprida()).count();
            assertThat(reuniao.mesRuim()).isEqualTo(perdidas >= limite);
        }
    }
}
