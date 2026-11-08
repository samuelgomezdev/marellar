package br.marellar.engine.dominio;

public enum SituacaoPessoa {
    /** Na linha, pode ser alocada. */
    ATIVA,
    /** Contratada; chega depois de {@code diasChegadaContratacao} dias uteis. */
    A_CAMINHO,
    /** Fora da linha, em treinamento. */
    EM_TREINAMENTO,
    /** Afastada por acidente de trabalho. */
    AFASTADA,
    /** Pediu demissao. */
    DESLIGADA
}
