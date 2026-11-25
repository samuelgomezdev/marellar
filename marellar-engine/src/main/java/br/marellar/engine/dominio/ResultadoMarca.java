package br.marellar.engine.dominio;

/** O que aconteceu com cada marca da rodada. */
public enum ResultadoMarca {
    /** NC clicada: detectada. */
    ACHOU,
    /** NC nao clicada: escapou e volta na garantia. */
    ESCAPOU,
    /** Marca falsa clicada: custa tempo. */
    FALSO_ALARME,
    /** Marca falsa deixada de lado: certo. */
    IGNOROU
}
