package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosFinanceiro;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.MotivoDerrota;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.TipoLancamento;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.relatorio.Dre;
import br.marellar.engine.relatorio.RelatorioConselho;
import br.marellar.engine.relatorio.Valuation;
import br.marellar.engine.dominio.Lancamento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinanceiroEConselhoTest {

    private static final ParametrosJogo TRANQUILO = Cenarios.semAcaso(Cenarios.PADRAO);
    private static final int MES = Relogio.DIAS_UTEIS_POR_MES;

    private static ParametrosJogo comConselho(ParametrosJogo p, ParametrosFinanceiro.Conselho c) {
        return p.comFinanceiro(p.financeiro().comConselho(c));
    }

    private static BigDecimal soma(Simulacao sim, TipoLancamento tipo) {
        return sim.lancamentos().stream().filter(l -> l.tipo() == tipo).map(Lancamento::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void umBarcoDoReAoEtGeraReceitaDoPrecoInteiroE70PorCentoEmCotas() {
        Simulacao sim = Simulacao.novaPartida(1, Cenarios.umBarcoNoRe(TRANQUILO));
        for (int i = 0; i < 200 && sim.barco("33-100").status() != StatusBarco.ENTREGUE; i++) {
            avancarDias(sim, 1);
        }
        avancarDias(sim, 6); // prazo de recebimento da ultima cota

        BigDecimal receita = sim.conselhos().stream().map(r -> r.dre().receita()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(sim.estado().financeiro().receitaDoMes());
        assertThat(receita).isEqualByComparingTo("800000.00");
        // contrato (30%) ja estava no caixa inicial; DM, BF, ST, PI e ET somam 70%
        assertThat(soma(sim, TipoLancamento.COTA_CLIENTE)).isEqualByComparingTo("560000.00");
        assertThat(sim.estado().financeiro().cotasAReceber()).isEqualByComparingTo("0");
    }

    @Test
    void dreEValorDaEmpresaBatemComOsLancamentosDoMes() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        ResultadoAvanco r = sim.avancar(Relogio.TICKS_POR_MES);

        assertThat(r.motivoPausa()).isEqualTo(MotivoPausa.FIM_DO_MES);
        RelatorioConselho conselho = sim.conselho().orElseThrow();
        Dre dre = conselho.dre();
        assertThat(dre.ebitda()).isEqualByComparingTo(dre.receita().subtract(dre.custoMaterial()).subtract(dre.consumiveis())
                .subtract(dre.materialRetrabalho()).subtract(dre.pessoal()).subtract(dre.despesasFixas())
                .add(dre.chargebackRecuperado()));
        // sem acidente nem NC escapada: valor = EBITDA x 12 x 6
        BigDecimal esperado = dre.ebitda().multiply(BigDecimal.valueOf(72)).max(BigDecimal.ZERO);
        assertThat(conselho.valorEmpresa()).isEqualByComparingTo(esperado);
        assertThat(sim.estado().financeiro().valorEmpresa()).isEqualByComparingTo(esperado);
        // folha paga = pessoal da DRE; despesas fixas pagas
        assertThat(soma(sim, TipoLancamento.FOLHA)).isEqualByComparingTo(dre.pessoal().negate());
        assertThat(soma(sim, TipoLancamento.DESPESA_FIXA)).isEqualByComparingTo("-180000.00");
        assertThat(conselho.caixa()).isEqualByComparingTo(sim.estado().financeiro().caixa());
    }

    @Test
    void folhaESalariosMaisEncargos() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);
        sim.avancar(Relogio.TICKS_POR_MES);

        // niveis da equipe inicial: 4,3,3,2,5,2,3,1,4,2,3,1 + inspetora
        BigDecimal salarios = new BigDecimal("4500").multiply(BigDecimal.valueOf(2))
                .add(new BigDecimal("3800").multiply(BigDecimal.valueOf(4)))
                .add(new BigDecimal("3300").multiply(BigDecimal.valueOf(3)))
                .add(new BigDecimal("2900").multiply(BigDecimal.valueOf(2)))
                .add(new BigDecimal("5300"))
                .add(new BigDecimal("5200"));
        assertThat(sim.conselho().orElseThrow().dre().pessoal()).isEqualByComparingTo(salarios.multiply(new BigDecimal("1.70")));
    }

    @Test
    void valorDaEmpresaComDescontos() {
        ParametrosFinanceiro regras = Cenarios.PADRAO.financeiro();

        Valuation v = Valuation.calcular(new BigDecimal("100000.00"), 1, 2, regras);

        assertThat(v.bruto()).isEqualByComparingTo("7200000.00");  // 100 mil x 12 x 6
        assertThat(v.descontoPercentual()).isEqualByComparingTo("0.11"); // 5% + 2 x 3%
        assertThat(v.valor()).isEqualByComparingTo("6408000.00");
        assertThat(Valuation.calcular(new BigDecimal("-50000"), 0, 0, regras).valor()).isEqualByComparingTo("0");
    }

    @Test
    void pacienciaSobeComMetaCumpridaECaiComMetaPerdida() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);
        sim.avancar(Relogio.TICKS_POR_MES);

        RelatorioConselho r = sim.conselho().orElseThrow();
        long cumpridas = r.metas().stream().filter(RelatorioConselho.Meta::cumprida).count();
        int esperado = (int) Math.max(0, Math.min(5, r.pacienciaAntes() + cumpridas - (4 - cumpridas)));
        assertThat(r.metas()).hasSize(4);
        assertThat(r.pacienciaDepois()).isEqualTo(esperado);
        assertThat(r.falas()).hasSize(2);
        assertThat(r.opcoes()).extracting(RelatorioConselho.Opcao::id)
                .containsExactly("TREINAR_NR", "CONTRATAR_2", "ANTECIPAR_COMPRA", "ENCONTRO_EQUIPE");
    }

    @Test
    void tresMesesDeCaixaNegativoEncerramAPartida() {
        // metas faceis, para a paciencia nao acabar antes; despesas fixas enormes derrubam o caixa
        ParametrosJogo p = comConselho(TRANQUILO, new ParametrosFinanceiro.Conselho(5, 5, 0,
                new BigDecimal("-1000000000"), 100, new BigDecimal("1.5")));
        p = p.comFinanceiro(p.financeiro().comDespesasFixas(new BigDecimal("5000000")));
        Simulacao sim = Simulacao.novaPartida(1, p);

        avancarDias(sim, 2 * MES);
        assertThat(sim.estado().derrota()).isNull();
        assertThat(sim.estado().financeiro().mesesCaixaNegativo()).isEqualTo(2);
        assertThat(sim.conselho().orElseThrow().alertaCaixaNegativo()).isTrue();

        sim.avancar(Relogio.TICKS_POR_MES);

        assertThat(sim.estado().derrota()).isEqualTo(MotivoDerrota.CAIXA);
        assertThat(sim.estado().motivoPausa()).isEqualTo(MotivoPausa.DERROTA);
        assertThat(sim.avancar(8).ticksExecutados()).isZero();
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.FazerUmAUm("P01"))).isInstanceOf(OrdemInvalidaException.class);
    }

    @Test
    void conselhoSemPacienciaDemiteOCeo() {
        ParametrosJogo p = comConselho(TRANQUILO, new ParametrosFinanceiro.Conselho(1, 5, 99,
                new BigDecimal("1000000000000"), 0, new BigDecimal("1.5")));
        Simulacao sim = Simulacao.novaPartida(1, p);

        sim.avancar(Relogio.TICKS_POR_MES);

        assertThat(sim.estado().derrota()).isEqualTo(MotivoDerrota.CONSELHO);
        assertThat(sim.conselho().orElseThrow().pacienciaDepois()).isZero();
        assertThat(sim.conselho().orElseThrow().opcoes()).isEmpty();
    }

    @Test
    void decisaoDoConselhoAplicaEfeitoECusto() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.EscolherDecisaoConselho("TREINAR_NR")))
                .isInstanceOf(OrdemInvalidaException.class);
        sim.avancar(Relogio.TICKS_POR_MES);
        BigDecimal caixaAntes = sim.estado().financeiro().caixa();

        sim.aplicarOrdem(new Ordem.EscolherDecisaoConselho("TREINAR_NR"));

        assertThat(sim.estado().pessoas()).filteredOn(x -> x.funcao() == Funcao.PRODUCAO)
                .allMatch(x -> x.treinoSegurancaEmDia());
        assertThat(sim.estado().financeiro().caixa()).isEqualByComparingTo(caixaAntes.subtract(new BigDecimal("15000")));
        assertThat(sim.conselho().orElseThrow().decisaoEscolhida()).isEqualTo("TREINAR_NR");
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.EscolherDecisaoConselho("ENCONTRO_EQUIPE")))
                .hasMessageContaining("ja foi tomada");
    }

    @Test
    void outrasDecisoesDoConselho() {
        Simulacao contratar = Simulacao.novaPartida(1, TRANQUILO);
        contratar.avancar(Relogio.TICKS_POR_MES);
        contratar.aplicarOrdem(new Ordem.EscolherDecisaoConselho("CONTRATAR_2"));
        assertThat(contratar.estado().pessoas()).filteredOn(x -> x.situacao() == SituacaoPessoa.A_CAMINHO).hasSize(2);

        Simulacao comprar = Simulacao.novaPartida(1, TRANQUILO);
        comprar.avancar(Relogio.TICKS_POR_MES);
        int antes = comprar.estado().pedidosACaminho().size();
        comprar.aplicarOrdem(new Ordem.EscolherDecisaoConselho("ANTECIPAR_COMPRA"));
        assertThat(comprar.estado().pedidosACaminho()).hasSize(antes + 5);

        Simulacao encontro = Simulacao.novaPartida(1, TRANQUILO);
        encontro.avancar(Relogio.TICKS_POR_MES);
        int moralAntes = encontro.estado().pessoas().getFirst().moral();
        encontro.aplicarOrdem(new Ordem.EscolherDecisaoConselho("ENCONTRO_EQUIPE"));
        assertThat(encontro.estado().pessoas().getFirst().moral()).isEqualTo(Math.min(100, moralAntes + 8));
    }
}
