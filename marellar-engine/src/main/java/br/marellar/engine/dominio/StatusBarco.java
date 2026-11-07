package br.marellar.engine.dominio;

public enum StatusBarco {
    /** Equipe trabalhando no gate atual. */
    EM_PRODUCAO,
    /** HH do gate concluidas; esperando o inspetor. */
    AGUARDANDO_INSPECAO,
    /** O inspetor esta inspecionando o gate. */
    EM_INSPECAO,
    /** Gate aprovado, mas o jogador segurou o barco antes do proximo gate. */
    APROVADO,
    /** Sem material para comecar o gate (entra no M4, com fornecedores). */
    PARADO_MATERIAL,
    /** Entrega tecnica aprovada. */
    ENTREGUE
}
