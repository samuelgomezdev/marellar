package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosEventos;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.TipoEvento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Eventos do MVP (README 5.9). Atraso de fornecedor fica na {@link FornecedorRegra} e
 * pedido de demissao na {@link PessoasRegra}; aqui ficam quebra de peca e acidente de trabalho.
 */
public final class EventoRegra {

    private static final BigDecimal MILHAO = BigDecimal.valueOf(1_000_000);

    private EventoRegra() {
    }

    /**
     * Quebra de peca, sorteada quando o gate comeca: 3% (x pressa se o barco e a prioridade 1,
     * x moral baixa se a moral media da equipe esta abaixo do limite). Soma HH de retrabalho e material.
     */
    public static void talvezQuebrePeca(Partida partida, Barco barco) {
        ParametrosEventos.QuebraPeca regras = partida.parametros().eventos().quebraPeca();
        BigDecimal prob = regras.probabilidadePorGate();
        if (barco.prioridade() == 1) {
            prob = prob.multiply(regras.multiplicadorPressa());
        }
        if (moralMedia(partida) < partida.parametros().eventos().demissao().limiarMoralBaixa()) {
            prob = prob.multiply(regras.multiplicadorMoralBaixa());
        }
        if (QualidadeRegra.sortear(partida, prob.min(BigDecimal.ONE))) {
            barco.acrescentarQuebra(regras.hhRetrabalho());
            partida.somarCustoMaterialRetrabalho(regras.custoMaterial());
            partida.registrarEvento(TipoEvento.QUEBRA_PECA, barco.id(), "eventos.quebraPeca", Map.of(
                    "barco", barco.id(), "gate", barco.gateAtual().name(),
                    "hh", regras.hhRetrabalho().stripTrailingZeros().toPlainString()));
        }
    }

    /**
     * Acidente, sorteado por hora trabalhada: probabilidadePorHh x 2 com treino de seguranca vencido
     * x 1,5 com a baia lotada (sobrecarga). Afasta a pessoa, derruba a moral da equipe e pausa o jogo.
     */
    public static void talvezAcidente(Partida partida, Pessoa p, Barco barco, boolean baiaLotada) {
        ParametrosEventos.Acidente regras = partida.parametros().eventos().acidente();
        if (!QualidadeRegra.sortear(partida, probabilidadeAcidente(p.treinoSegurancaEmDia(), baiaLotada, regras))) {
            return;
        }
        int dias = partida.aleatorio().proximoInt(regras.diasPerdidosMin(), regras.diasPerdidosMax() + 1);
        p.mudarSituacao(SituacaoPessoa.AFASTADA, dias);
        partida.registrarAcidente(dias);
        for (Pessoa colega : partida.pessoas()) {
            if (colega.naEmpresa() && colega.funcao() == Funcao.PRODUCAO) {
                colega.definirMoral(colega.moral() - regras.quedaMoralEquipe());
            }
        }
        partida.registrarEvento(TipoEvento.ACIDENTE, barco.id(), "eventos.acidente", Map.of(
                "pessoa", p.nome(), "barco", barco.id(), "dias", String.valueOf(dias)));
        partida.pausar(MotivoPausa.ACIDENTE);
    }

    public static BigDecimal probabilidadeAcidente(boolean treinoEmDia, boolean baiaLotada, ParametrosEventos.Acidente regras) {
        BigDecimal prob = regras.probabilidadePorHh();
        if (!treinoEmDia) {
            prob = prob.multiply(regras.multiplicadorTreinoVencido());
        }
        if (baiaLotada) {
            prob = prob.multiply(regras.multiplicadorSobrecarga());
        }
        return prob.min(BigDecimal.ONE);
    }

    /** Taxa de frequencia (NBR 14280): acidentes x 1.000.000 / HHT. */
    public static BigDecimal taxaFrequencia(int acidentes, BigDecimal hht) {
        return taxa(BigDecimal.valueOf(acidentes), hht);
    }

    /** Taxa de gravidade (NBR 14280): (dias perdidos + debitados) x 1.000.000 / HHT. */
    public static BigDecimal taxaGravidade(int diasPerdidosEDebitados, BigDecimal hht) {
        return taxa(BigDecimal.valueOf(diasPerdidosEDebitados), hht);
    }

    private static BigDecimal taxa(BigDecimal numerador, BigDecimal hht) {
        if (hht.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return numerador.multiply(MILHAO).divide(hht, 2, RoundingMode.HALF_EVEN);
    }

    private static int moralMedia(Partida partida) {
        int soma = 0;
        int n = 0;
        for (Pessoa p : partida.pessoas()) {
            if (p.disponivel() && p.funcao() == Funcao.PRODUCAO) {
                soma += p.moral();
                n++;
            }
        }
        return n == 0 ? 100 : soma / n;
    }
}
