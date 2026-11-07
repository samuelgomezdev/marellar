package br.marellar.engine.ordens;

import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Velocidade;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Ordem do jogador. Em JSON, o campo {@code tipo} diz qual e a ordem:
 * {@code { "tipo": "DecidirNc", "ncId": "NC-0003", "decisao": "RETRABALHO" }}.
 * <p>
 * Contratar, Treinar, FazerUmAUm e EscolherDecisaoConselho entram nos proximos milestones.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(Ordem.DefinirPrioridade.class),
        @JsonSubTypes.Type(Ordem.LiberarGate.class),
        @JsonSubTypes.Type(Ordem.SegurarGate.class),
        @JsonSubTypes.Type(Ordem.DecidirNc.class),
        @JsonSubTypes.Type(Ordem.MudarVelocidade.class)
})
public sealed interface Ordem {

    /** Coloca o barco na posicao informada da fila de prioridade (1 = mais prioritario). */
    record DefinirPrioridade(String barcoId, int posicao) implements Ordem {}

    /** Solta um barco que estava segurado; se o gate ja foi aprovado, ele segue para o proximo. */
    record LiberarGate(String barcoId) implements Ordem {}

    /** Segura o barco no gate atual: depois de aprovado, ele nao segue ate ser liberado. */
    record SegurarGate(String barcoId) implements Ordem {}

    /** Decide uma NC detectada: retrabalho, concessao ou RNC contra o fornecedor. */
    record DecidirNc(String ncId, DecisaoNc decisao) implements Ordem {}

    record MudarVelocidade(Velocidade velocidade) implements Ordem {}
}
