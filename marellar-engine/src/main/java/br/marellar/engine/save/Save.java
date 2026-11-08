package br.marellar.engine.save;

import br.marellar.engine.ordens.Ordem;

import java.math.BigDecimal;
import java.util.List;

/**
 * Partida salva. O motor e deterministico, entao basta guardar a semente, a data de inicio,
 * ate que tick a partida chegou e as ordens aceitas, cada uma com o tick em que foi dada.
 * Carregar = jogar de novo, com as mesmas ordens nos mesmos ticks.
 * <p>
 * {@code verificacao} confere, depois do replay, que os parametros de {@code data/} sao os mesmos
 * de quando o save foi feito. Datas ficam como texto ISO para o motor nao depender do modulo
 * de datas do Jackson.
 */
public record Save(
        int versao,
        long semente,
        String inicio,
        long tick,
        List<OrdemNoTempo> ordens,
        Verificacao verificacao) {

    public static final int VERSAO_ATUAL = 1;

    public Save {
        ordens = List.copyOf(ordens);
    }

    /** Ordem aceita pelo motor e o tick em que ela foi aplicada. */
    public record OrdemNoTempo(long tick, Ordem ordem) {}

    /**
     * Resumo do estado no momento do save. {@code assinatura} e o SHA-256 de barcos, pessoas,
     * eventos e caixa: qualquer diferenca no replay aparece ali.
     */
    public record Verificacao(String data, int hora, BigDecimal caixa, int eventos, String assinatura) {}
}
