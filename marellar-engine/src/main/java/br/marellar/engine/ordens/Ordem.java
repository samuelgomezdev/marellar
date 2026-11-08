package br.marellar.engine.ordens;

import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Treinamento;
import br.marellar.engine.dominio.Velocidade;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Ordem do jogador. Em JSON, o campo {@code tipo} diz qual e a ordem:
 * {@code { "tipo": "DecidirNc", "ncId": "NC-0003", "decisao": "RETRABALHO" }}.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.SIMPLE_NAME, property = "tipo")
@JsonSubTypes({
        @JsonSubTypes.Type(Ordem.DefinirPrioridade.class),
        @JsonSubTypes.Type(Ordem.LiberarGate.class),
        @JsonSubTypes.Type(Ordem.SegurarGate.class),
        @JsonSubTypes.Type(Ordem.DecidirNc.class),
        @JsonSubTypes.Type(Ordem.Contratar.class),
        @JsonSubTypes.Type(Ordem.Treinar.class),
        @JsonSubTypes.Type(Ordem.FazerUmAUm.class),
        @JsonSubTypes.Type(Ordem.EscolherDecisaoConselho.class),
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

    /** Contrata uma pessoa: chega em diasChegadaContratacao dias uteis, no nivel 1. */
    record Contratar(Funcao funcao) implements Ordem {}

    /** Tira a pessoa da linha por diasTreinamento dias: TECNICO sobe 1 nivel; SEGURANCA poe o treino em dia. */
    record Treinar(String pessoaId, Treinamento treinamento) implements Ordem {}

    /** Conversa 1:1: +moral; no maximo uma a cada diasEntreUmAUm dias por pessoa. */
    record FazerUmAUm(String pessoaId) implements Ordem {}

    /** Escolhe uma das opcoes da ultima reuniao do conselho (uma por reuniao). */
    record EscolherDecisaoConselho(String opcaoId) implements Ordem {}

    record MudarVelocidade(Velocidade velocidade) implements Ordem {}
}
