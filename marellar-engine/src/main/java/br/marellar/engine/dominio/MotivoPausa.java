package br.marellar.engine.dominio;

/** Por que o motor pausou sozinho. ACIDENTE, NC_CRITICA e EVENTO_COM_ESCOLHA entram nos proximos milestones. */
public enum MotivoPausa {
    FIM_DO_MES,
    ACIDENTE,
    NC_CRITICA,
    EVENTO_COM_ESCOLHA,
    /** Um gate terminou: o jogador faz a rodada de inspecao ou deixa com o inspetor. */
    RODADA_INSPECAO,
    /** A partida acabou (caixa ou conselho). */
    DERROTA
}
