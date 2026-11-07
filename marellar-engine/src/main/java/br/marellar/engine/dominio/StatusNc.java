package br.marellar.engine.dominio;

public enum StatusNc {
    /** Sorteada ao terminar o gate; o inspetor ainda nao olhou. */
    PENDENTE_INSPECAO,
    /** Detectada pelo inspetor; esperando a decisao do jogador. */
    ABERTA,
    /** Retrabalho (ou RNC contra fornecedor) em andamento. */
    EM_RETRABALHO,
    RESOLVIDA,
    /** Liberada sem conserto, com risco de garantia depois. */
    CONCEDIDA,
    /** O inspetor nao viu. O jogador nao fica sabendo; pode virar garantia depois. */
    ESCAPADA
}
