package br.marellar.engine.dominio;

/** Situacao de uma proposta trazida pelo vendedor. */
public enum StatusProposta {
    /** Esperando a resposta da fabrica ate o fim da validade. */
    ABERTA,
    /** Cliente aceitou o prazo: virou pedido na carteira. */
    VENDIDA,
    /** Cliente achou o prazo longo demais (sorteio entre desistenciaInicio e desistenciaTotal). */
    DESISTIU,
    /** A fabrica recusou. */
    RECUSADA,
    /** Ficou sem resposta ate o fim da validade: o cliente foi embora. */
    EXPIRADA
}
