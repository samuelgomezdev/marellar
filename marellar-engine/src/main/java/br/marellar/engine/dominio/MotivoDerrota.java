package br.marellar.engine.dominio;

public enum MotivoDerrota {
    /** Caixa negativo por mesesCaixaNegativoParaDerrota meses seguidos, sem credito. */
    CAIXA,
    /** Meses ruins seguidos com o conselho (mes ruim = muitas metas perdidas): o CEO e demitido. */
    CONSELHO
}
