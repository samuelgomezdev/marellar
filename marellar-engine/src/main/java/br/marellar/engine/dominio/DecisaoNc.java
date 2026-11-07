package br.marellar.engine.dominio;

public enum DecisaoNc {
    /** HH extra no barco e custo de material. */
    RETRABALHO,
    /** Libera sem consertar; nao vale para NC critica. */
    CONCESSAO,
    /** Retrabalho + RNC contra o fornecedor (so para NC de origem FORNECEDOR). 8D e chargeback no M4. */
    RNC_FORNECEDOR
}
