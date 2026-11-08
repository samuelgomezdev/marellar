package br.marellar.engine.save;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Relogio;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.Velocidade;
import br.marellar.engine.estado.EstadoPartida;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaveTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Jogador simples: decide toda NC aberta, escolhe a decisao do conselho e mexe na fila. */
    private static Simulacao jogar(long semente, ParametrosJogo p, int dias) {
        Simulacao sim = Simulacao.novaPartida(semente, p);
        sim.aplicarOrdem(new Ordem.MudarVelocidade(Velocidade.X2));
        sim.aplicarOrdem(new Ordem.DefinirPrioridade("33-003", 1));
        for (int d = 0; d < dias; d++) {
            var r = sim.avancar(Relogio.TICKS_POR_DIA);
            if (r.motivoPausa() == MotivoPausa.FIM_DO_MES) {
                sim.avancar(0);
                sim.aplicarOrdem(new Ordem.EscolherDecisaoConselho("ENCONTRO_EQUIPE"));
                sim.avancar(Relogio.TICKS_POR_DIA - r.ticksExecutados());
            }
            if (d == 3) {
                sim.aplicarOrdem(new Ordem.Contratar(Funcao.PRODUCAO));
            }
            if (d == 5) {
                sim.aplicarOrdem(new Ordem.FazerUmAUm("P01"));
            }
            try {
                sim.aplicarOrdem(new Ordem.FazerUmAUm("P01")); // recusada quase sempre: nao entra no save
            } catch (OrdemInvalidaException ignorada) {
                // esperado
            }
            for (var barco : sim.estado().barcos()) {
                for (var nc : barco.ncs()) {
                    if (nc.status() == StatusNc.ABERTA) {
                        sim.aplicarOrdem(new Ordem.DecidirNc(nc.id(),
                                d % 2 == 0 ? DecisaoNc.RETRABALHO : DecisaoNc.CONCESSAO));
                    }
                }
            }
        }
        // termina no meio de um dia
        sim.avancar(3);
        return sim;
    }

    @Test
    void salvarECarregarReproduzOMesmoEstado() throws Exception {
        Simulacao original = jogar(42, Cenarios.PADRAO, 2 * Relogio.DIAS_UTEIS_POR_MES + 7);

        String json = JSON.writeValueAsString(original.salvar());
        Save lido = JSON.readValue(json, Save.class);
        Simulacao carregada = Simulacao.carregar(lido, Cenarios.PADRAO);

        EstadoPartida a = original.estado();
        EstadoPartida b = carregada.estado();
        assertThat(lido.ordens()).isNotEmpty();
        assertThat(json).contains("\"tipo\":\"DecidirNc\"");
        assertThat(b.tick()).isEqualTo(a.tick());
        assertThat(JSON.writeValueAsString(b.barcos())).isEqualTo(JSON.writeValueAsString(a.barcos()));
        assertThat(JSON.writeValueAsString(b.pessoas())).isEqualTo(JSON.writeValueAsString(a.pessoas()));
        assertThat(b.financeiro()).isEqualTo(a.financeiro());
        assertThat(carregada.conselhos()).isEqualTo(original.conselhos());
        assertThat(carregada.eventosDesde(0)).isEqualTo(original.eventosDesde(0));
        assertThat(carregada.lancamentos()).isEqualTo(original.lancamentos());
    }

    @Test
    void partidaCarregadaContinuaIgualAOriginal() {
        Simulacao original = jogar(7, Cenarios.PADRAO, 10);
        Simulacao carregada = Simulacao.carregar(original.salvar(), Cenarios.PADRAO);

        original.avancar(Relogio.TICKS_POR_MES);
        carregada.avancar(Relogio.TICKS_POR_MES);

        assertThat(carregada.eventosDesde(0)).isEqualTo(original.eventosDesde(0));
        assertThat(carregada.estado().financeiro()).isEqualTo(original.estado().financeiro());
        // e o save da carregada continua com o historico inteiro
        assertThat(carregada.salvar().ordens()).isEqualTo(original.salvar().ordens());
    }

    @Test
    void saveComOutrosParametrosERecusado() {
        Save save = jogar(3, Cenarios.PADRAO, 25).salvar();
        ParametrosJogo outros = Cenarios.PADRAO.comFinanceiro(
                Cenarios.PADRAO.financeiro().comDespesasFixas(new BigDecimal("250000")));

        assertThatThrownBy(() -> Simulacao.carregar(save, outros))
                .isInstanceOf(SaveIncompativelException.class)
                .hasMessageContaining("outros parametros");
    }

    @Test
    void versaoDesconhecidaERecusada() {
        Save s = Simulacao.novaPartida(1).salvar();
        Save futuro = new Save(99, s.semente(), s.inicio(), s.tick(), s.ordens(), s.verificacao());

        assertThatThrownBy(() -> Simulacao.carregar(futuro, Cenarios.PADRAO))
                .isInstanceOf(SaveIncompativelException.class)
                .hasMessageContaining("99");
    }
}
