package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.Treinamento;
import br.marellar.engine.estado.EstadoPartida.PessoaVisao;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static br.marellar.engine.Cenarios.avancarDias;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PessoasRegraTest {

    private static final ParametrosJogo TRANQUILO = Cenarios.semAcaso(Cenarios.PADRAO);

    private static PessoaVisao pessoa(Simulacao sim, String id) {
        return sim.estado().pessoas().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void contratadoChegaEm10DiasUteisNoNivel1() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        sim.aplicarOrdem(new Ordem.Contratar(Funcao.PRODUCAO));

        PessoaVisao nova = pessoa(sim, "P14");
        assertThat(nova.situacao()).isEqualTo(SituacaoPessoa.A_CAMINHO);
        assertThat(nova.nivel()).isEqualTo(1);
        assertThat(nova.nome()).isEqualTo("Nádia Fontes");

        avancarDias(sim, 9);
        assertThat(pessoa(sim, "P14").situacao()).isEqualTo(SituacaoPessoa.A_CAMINHO);
        avancarDias(sim, 1);
        assertThat(pessoa(sim, "P14").situacao()).isEqualTo(SituacaoPessoa.ATIVA);
    }

    @Test
    void contratadoSobeDeNivelComOTempoAteONivel3() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);
        sim.aplicarOrdem(new Ordem.Contratar(Funcao.PRODUCAO));

        avancarDias(sim, 10 + 30);
        assertThat(pessoa(sim, "P14").nivel()).isEqualTo(2);

        avancarDias(sim, 30 * 3);
        assertThat(pessoa(sim, "P14").nivel()).isEqualTo(3);
    }

    @Test
    void treinamentoTecnicoTiraDaLinhaESobeUmNivel() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        sim.aplicarOrdem(new Ordem.Treinar("P08", Treinamento.TECNICO));

        assertThat(pessoa(sim, "P08").situacao()).isEqualTo(SituacaoPessoa.EM_TREINAMENTO);
        sim.avancar(1);
        assertThat(pessoa(sim, "P08").alocadoEm()).isNull();

        avancarDias(sim, 3);
        PessoaVisao depois = pessoa(sim, "P08");
        assertThat(depois.situacao()).isEqualTo(SituacaoPessoa.ATIVA);
        assertThat(depois.nivel()).isEqualTo(2);
    }

    @Test
    void treinamentoDeSegurancaPoeOTreinoEmDia() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);
        assertThat(pessoa(sim, "P06").treinoSegurancaEmDia()).isFalse();

        sim.aplicarOrdem(new Ordem.Treinar("P06", Treinamento.SEGURANCA));
        avancarDias(sim, 3);

        assertThat(pessoa(sim, "P06").treinoSegurancaEmDia()).isTrue();
        assertThat(pessoa(sim, "P06").nivel()).isEqualTo(2);
    }

    @Test
    void umAUmSobeAMoralESoPodeSerRepetidoDepoisDoIntervalo() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        sim.aplicarOrdem(new Ordem.FazerUmAUm("P10"));

        assertThat(pessoa(sim, "P10").moral()).isEqualTo(55);
        assertThat(pessoa(sim, "P10").podeFazerUmAUm()).isFalse();
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.FazerUmAUm("P10")))
                .isInstanceOf(OrdemInvalidaException.class);
        assertThat(sim.estado().seguranca().horasLiderUmAUm()).isEqualByComparingTo("0.5");

        avancarDias(sim, 5);
        sim.aplicarOrdem(new Ordem.FazerUmAUm("P10"));
        assertThat(pessoa(sim, "P10").moral()).isEqualTo(60);
    }

    @Test
    void semUmAUmPorMaisDe30DiasAMoralCai1PorDia() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        avancarDias(sim, 35);

        // fim dos dias 31, 32, 33 e 34: 4 quedas
        assertThat(pessoa(sim, "P01").moral()).isEqualTo(75 - 4);
    }

    @Test
    void moralBaixaAumentaOsPedidosDeDemissao() {
        ParametrosJogo base = Cenarios.semNcs(Cenarios.PADRAO);
        ParametrosJogo semAcidente = Cenarios.comEventos(base, BigDecimal.ZERO, BigDecimal.ZERO,
                base.eventos().demissao().probabilidadeDiariaBase(),
                base.eventos().demissao().probabilidadeDiariaMoralBaixa());
        Simulacao triste = Simulacao.novaPartida(5, Cenarios.comMoralDaEquipe(semAcidente, 10));
        Simulacao feliz = Simulacao.novaPartida(5, Cenarios.comMoralDaEquipe(semAcidente, 90));

        avancarDias(triste, 63);
        avancarDias(feliz, 63);

        assertThat(triste.estado().seguranca().demissoes())
                .isGreaterThan(feliz.estado().seguranca().demissoes());
        assertThat(PessoasRegra.probabilidadeDemissao(10, base.eventos().demissao()))
                .isGreaterThan(PessoasRegra.probabilidadeDemissao(90, base.eventos().demissao()));
    }

    @Test
    void ordensDePessoasInvalidasSaoRecusadas() {
        Simulacao sim = Simulacao.novaPartida(1, TRANQUILO);

        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.Contratar(Funcao.INSPETOR)))
                .isInstanceOf(OrdemInvalidaException.class);
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.Treinar("P05", Treinamento.TECNICO)))
                .hasMessageContaining("nivel maximo");
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.FazerUmAUm("X99")))
                .isInstanceOf(OrdemInvalidaException.class);

        sim.aplicarOrdem(new Ordem.Treinar("P01", Treinamento.SEGURANCA));
        assertThatThrownBy(() -> sim.aplicarOrdem(new Ordem.Treinar("P01", Treinamento.TECNICO)))
                .hasMessageContaining("nao esta na linha");
    }
}
