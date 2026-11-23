package br.marellar.engine.dominio;

public enum TipoEvento {
    GATE_CONCLUIDO,
    GATE_APROVADO,
    NC_DETECTADA,
    NC_CRITICA,
    RETRABALHO_CONCLUIDO,
    CONCESSAO,
    RNC_ABERTA,
    BARCO_ENTREGUE,
    PARADO_MATERIAL,
    MATERIAL,
    QUEBRA_PECA,
    ACIDENTE,
    PESSOA,
    DEMISSAO,
    SCORECARD,
    FINANCEIRO,
    DERROTA,
    /** NC marcada pelo jogador no modelo 3D do barco. */
    NC_INSPECAO_3D,
    FIM_DO_MES
}
