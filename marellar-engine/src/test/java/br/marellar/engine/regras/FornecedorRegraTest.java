package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dados.CatalogoNc.TipoNc;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Fornecedor;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.PerfilFornecedor;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusRnc;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.estado.EstadoPartida.FornecedorVisao;
import br.marellar.engine.estado.EstadoPartida.RncVisao;
import br.marellar.engine.ordens.Ordem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;

class FornecedorRegraTest {

    private static final ParametrosJogo TRANQUILO = Cenarios.semAcaso(Cenarios.PADRAO);
    private static final String BARCO = "33-100";

    private static FornecedorVisao fornecedor(Simulacao sim, String id) {
        return sim.estado().fornecedores().stream().filter(f -> f.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void chargebackEOCustoDocumentadoMais10PorCento() {
        BigDecimal custo = FornecedorRegra.custoDocumentado(new BigDecimal("10"), new BigDecimal("57.00"), new BigDecimal("100.00"));

        assertThat(custo).isEqualByComparingTo("670.00");
        assertThat(FornecedorRegra.chargeback(custo, new BigDecimal("0.10"))).isEqualByComparingTo("737.00");
    }

    @Test
    void ppmEPecasRejeitadasPorMilhao() {
        Fornecedor f = new Fornecedor("F", "Teste", PerfilFornecedor.BARATO_INSTAVEL, List.of("x"));

        f.registrarRecebimento(1000, 12, true);

        assertThat(f.ppm()).isEqualTo(12_000);
    }

    @Test
    void semMaterialOBarcoParaERegistraHoraImprodutiva() {
        ParametrosJogo p = Cenarios.umBarcoNoRe(TRANQUILO);
        Simulacao sim = Simulacao.novaPartida(1, p.comLinha33(p.linha33().comEstoque(0, 0)));

        avancarDias(sim, 3);

        var barco = sim.barco(BARCO);
        assertThat(barco.gateAtual()).isEqualTo(CodigoGate.DM);
        assertThat(barco.status()).isEqualTo(StatusBarco.PARADO_MATERIAL);
        assertThat(barco.horasParadoMaterial()).isPositive();
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.PARADO_MATERIAL);
    }

    @Test
    void pedidoAutomaticoChegaEOBarcoVoltaATrabalhar() {
        ParametrosJogo p = Cenarios.umBarcoNoRe(TRANQUILO);
        Simulacao sim = Simulacao.novaPartida(1, p.comLinha33(p.linha33().comEstoque(0, 1)));

        avancarDias(sim, 1);
        assertThat(sim.estado().pedidosACaminho()).isNotEmpty();

        avancarDias(sim, 15);

        var barco = sim.barco(BARCO);
        assertThat(barco.status()).isNotEqualTo(StatusBarco.PARADO_MATERIAL);
        assertThat(barco.hhProduzidas()).isGreaterThan(new BigDecimal("38"));
        assertThat(sim.eventosDesde(0)).anyMatch(e -> e.tipo() == TipoEvento.MATERIAL && e.mensagem().startsWith("Chegaram"));
        assertThat(fornecedor(sim, "F01").pedidosEntregues()).isPositive();
    }

    @Test
    void scorecardSemEntregasDependeSoDoCusto() {
        ParametrosJogo p = TRANQUILO.comLinha33(TRANQUILO.linha33().comEstoque(50, 0));
        Simulacao sim = Simulacao.novaPartida(1, p);

        avancarDias(sim, Relogio.DIAS_UTEIS_POR_MES);

        // entrega 100, qualidade 100, resposta 100; custo = 100 - (fatorPreco - 0,85) x 200
        assertThat(fornecedor(sim, "F01").pontos()).isEqualTo(88); // premium, fatorPreco 1,15 -> custo 40
        assertThat(fornecedor(sim, "F01").nota()).isEqualTo("B");
        assertThat(fornecedor(sim, "F02").pontos()).isEqualTo(99); // barato, 0,88 -> custo 94
        assertThat(fornecedor(sim, "F02").nota()).isEqualTo("A");
        assertThat(fornecedor(sim, "F03").pontos()).isEqualTo(94); // 1,00 -> custo 70
        assertThat(sim.eventosDesde(0)).filteredOn(e -> e.tipo() == TipoEvento.SCORECARD).hasSize(3);
    }

    /** NC critica de material comprado, sempre detectada, no RE. */
    private static Simulacao comNcDeFornecedor(String item) {
        TipoNc tipo = new TipoNc("T-F", "Defeito de material", Gravidade.CRITICA, Origem.FORNECEDOR,
                List.of(CodigoGate.RE), BigDecimal.ONE, new BigDecimal("10"), item, new BigDecimal("100.00"));
        CatalogoNc c = TRANQUILO.catalogoNc().comTipos(List.of(tipo))
                .comDeteccaoBase(Map.of(Gravidade.LEVE, BigDecimal.ONE, Gravidade.MEDIA, BigDecimal.ONE, Gravidade.CRITICA, BigDecimal.ONE));
        return Simulacao.novaPartida(1, Cenarios.umBarcoNoRe(TRANQUILO.comCatalogoNc(c)));
    }

    @Test
    void rncGeraChargebackQueVaiParaOCreditoDoFornecedor() {
        Simulacao sim = comNcDeFornecedor("gelcoat");
        avancarDias(sim, 2);
        String nc = sim.barco(BARCO).ncs().getFirst().id();

        sim.aplicarOrdem(new Ordem.DecidirNc(nc, DecisaoNc.RNC_FORNECEDOR));

        RncVisao rnc = sim.estado().rncs().getFirst();
        assertThat(rnc.fornecedorId()).isEqualTo("F02");
        assertThat(rnc.custoDocumentado()).isEqualByComparingTo("670.00"); // 10 HH x 57 + 100
        assertThat(rnc.chargeback()).isEqualByComparingTo("737.00");
        assertThat(fornecedor(sim, "F02").creditoChargeback()).isEqualByComparingTo("737.00");
        assertThat(fornecedor(sim, "F02").rncsAbertas()).isEqualTo(1);
    }

    @Test
    void oitoDVencidoEmNcCriticaCobraAsHorasParadas() {
        // o perfil de F02 responde o 8D em 14 dias; o prazo e 10
        Simulacao sim = comNcDeFornecedor("gelcoat");
        avancarDias(sim, 2);
        sim.aplicarOrdem(new Ordem.DecidirNc(sim.barco(BARCO).ncs().getFirst().id(), DecisaoNc.RNC_FORNECEDOR));

        // RNC aberta no dia 2: prazo no dia 12, vencida no fim do dia 13; F02 responde no dia 16
        avancarDias(sim, 12);
        RncVisao vencida = sim.estado().rncs().getFirst();
        assertThat(vencida.status()).isEqualTo(StatusRnc.OITO_D_VENCIDO);
        assertThat(vencida.chargebackHorasParadas()).isEqualByComparingTo("627.00"); // 10 HH x 57 x 1,10

        avancarDias(sim, 3);
        assertThat(sim.estado().rncs().getFirst().status()).isEqualTo(StatusRnc.RESPONDIDA_COM_ATRASO);
        assertThat(fornecedor(sim, "F02").oitoDsNoPrazo()).isZero();
    }

    @Test
    void fornecedorDePerfilRuimMultiplicaAsNcsDeMaterial() {
        var regras = Cenarios.PADRAO.fornecedores();

        assertThat(FornecedorRegra.fatorPorPpm(12_000, regras)).isEqualByComparingTo("2.4"); // barato instavel
        assertThat(FornecedorRegra.fatorPorPpm(800, regras)).isEqualByComparingTo("0.5");    // premium (minimo)
        assertThat(FornecedorRegra.fatorPorPpm(50_000, regras)).isEqualByComparingTo("3.0"); // teto
    }
}
