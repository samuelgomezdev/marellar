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
    FIM_DO_MES,
    /** O vendedor trouxe uma proposta, ou ela expirou ou foi recusada. */
    PROPOSTA,
    /** Venda fechada (contrato assinado). */
    VENDA,
    /** O cliente desistiu do prazo oferecido. */
    DESISTENCIA,
    /** Pedido da carteira entrou num molde livre. */
    CASCO_NOVO,
    /** Multa por atraso na entrega. */
    MULTA,
    /** Resultado da rodada de inspecao feita pelo jogador. */
    RODADA,
    /** NC que escapou voltou no pos-venda. */
    GARANTIA
}
