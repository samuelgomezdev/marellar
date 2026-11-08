package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dados.ParametrosEventos;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Pessoa;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.dominio.Treinamento;
import br.marellar.engine.ordens.OrdemInvalidaException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * Pessoas (README 5.6): contratar, treinar, 1:1, queda de moral sem 1:1 e pedido de demissao.
 * Roda no inicio de cada dia util e quando o jogador da uma ordem.
 */
public final class PessoasRegra {

    private PessoasRegra() {
    }

    public static void fimDoDia(Partida partida) {
        ParametrosPessoas regras = partida.parametros().pessoas();
        // copia: uma demissao muda a situacao, mas nao a lista
        for (Pessoa p : new ArrayList<>(partida.pessoas())) {
            if (!p.naEmpresa()) {
                continue;
            }
            if (p.passarDiaNaSituacao()) {
                fimDaSituacao(partida, p);
            }
            if (p.situacao() == SituacaoPessoa.A_CAMINHO) {
                continue;
            }
            subirNivelComOTempo(partida, p, regras);
            caiMoralSemUmAUm(partida, p, regras);
            talvezPecaDemissao(partida, p);
        }
    }

    private static void fimDaSituacao(Partida partida, Pessoa p) {
        switch (p.situacao()) {
            case A_CAMINHO -> {
                p.mudarSituacao(SituacaoPessoa.ATIVA, 0);
                evento(partida, TipoEvento.PESSOA, "eventos.chegadaContratado",
                        Map.of("pessoa", p.nome(), "nivel", String.valueOf(p.nivel())));
            }
            case EM_TREINAMENTO -> p.concluirTreinamento().ifPresent(t -> {
                if (t == Treinamento.TECNICO) {
                    p.definirNivel(p.nivel() + 1);
                } else {
                    p.definirTreinoSegurancaEmDia(true);
                }
                evento(partida, TipoEvento.PESSOA, "eventos.treinamentoFim",
                        Map.of("pessoa", p.nome(), "treinamento", t.nome()));
            });
            case AFASTADA -> {
                p.mudarSituacao(SituacaoPessoa.ATIVA, 0);
                evento(partida, TipoEvento.PESSOA, "eventos.retornoAfastamento", Map.of("pessoa", p.nome()));
            }
            default -> {
            }
        }
    }

    /** Quem foi contratado na partida sobe 1 nivel a cada diasParaSubirNivel dias na linha, ate o teto. */
    private static void subirNivelComOTempo(Partida partida, Pessoa p, ParametrosPessoas regras) {
        if (!p.contratadaNaPartida() || p.situacao() != SituacaoPessoa.ATIVA) {
            return;
        }
        int dias = p.contarDiaNaLinha();
        if (dias % regras.diasParaSubirNivel() == 0 && p.nivel() < regras.nivelMaximoPorTempo()) {
            p.definirNivel(p.nivel() + 1);
            evento(partida, TipoEvento.PESSOA, "eventos.subiuNivel",
                    Map.of("pessoa", p.nome(), "nivel", String.valueOf(p.nivel())));
        }
    }

    private static void caiMoralSemUmAUm(Partida partida, Pessoa p, ParametrosPessoas regras) {
        if (partida.hoje() - p.diaUltimoUmAUm() > regras.diasSemUmAUmAntesDeCair()) {
            p.definirMoral(p.moral() - regras.quedaMoralDiariaSemUmAUm());
        }
    }

    /** Chance diaria de pedir demissao: maior com moral baixa. O inspetor nao pede demissao no MVP. */
    private static void talvezPecaDemissao(Partida partida, Pessoa p) {
        if (p.funcao() != Funcao.PRODUCAO) {
            return;
        }
        if (QualidadeRegra.sortear(partida, probabilidadeDemissao(p.moral(), partida.parametros().eventos().demissao()))) {
            p.mudarSituacao(SituacaoPessoa.DESLIGADA, 0);
            partida.registrarDemissao();
            evento(partida, TipoEvento.DEMISSAO, "eventos.demissao", Map.of("pessoa", p.nome()));
        }
    }

    public static BigDecimal probabilidadeDemissao(int moral, ParametrosEventos.Demissao regras) {
        return moral < regras.limiarMoralBaixa() ? regras.probabilidadeDiariaMoralBaixa() : regras.probabilidadeDiariaBase();
    }

    // ----- ordens -----

    public static Pessoa contratar(Partida partida, Funcao funcao) {
        if (funcao != Funcao.PRODUCAO) {
            throw new OrdemInvalidaException("No MVP so se contrata gente de PRODUCAO");
        }
        ParametrosPessoas regras = partida.parametros().pessoas();
        Pessoa nova = new Pessoa(partida.proximoIdPessoa(), partida.proximoNomeContratacao(), funcao, 1,
                regras.moralInicialContratado(), null, true, true);
        nova.marcarInicioSemUmAUm(partida.hoje());
        nova.mudarSituacao(SituacaoPessoa.A_CAMINHO, regras.diasChegadaContratacao());
        partida.pessoas().add(nova);
        evento(partida, TipoEvento.PESSOA, "eventos.contratacao",
                Map.of("pessoa", nova.nome(), "dias", String.valueOf(regras.diasChegadaContratacao())));
        return nova;
    }

    public static void treinar(Partida partida, String pessoaId, Treinamento treinamento) {
        Pessoa p = pessoa(partida, pessoaId);
        if (p.funcao() != Funcao.PRODUCAO) {
            throw new OrdemInvalidaException("No MVP so se treina gente de PRODUCAO");
        }
        if (!p.disponivel()) {
            throw new OrdemInvalidaException(p.nome() + " nao esta na linha agora (" + p.situacao() + ")");
        }
        if (treinamento == Treinamento.TECNICO && p.nivel() >= 5) {
            throw new OrdemInvalidaException(p.nome() + " ja esta no nivel maximo");
        }
        int dias = partida.parametros().pessoas().diasTreinamento();
        p.iniciarTreinamento(treinamento, dias);
        evento(partida, TipoEvento.PESSOA, "eventos.treinamentoInicio", Map.of(
                "pessoa", p.nome(), "treinamento", treinamento.nome(), "dias", String.valueOf(dias)));
    }

    public static void fazerUmAUm(Partida partida, String pessoaId) {
        Pessoa p = pessoa(partida, pessoaId);
        ParametrosPessoas regras = partida.parametros().pessoas();
        if (p.situacao() == SituacaoPessoa.A_CAMINHO) {
            throw new OrdemInvalidaException(p.nome() + " ainda nao chegou");
        }
        if (!p.podeFazerUmAUm(partida.hoje(), regras.diasEntreUmAUm())) {
            throw new OrdemInvalidaException("O ultimo 1:1 com " + p.nome() + " foi ha menos de "
                    + regras.diasEntreUmAUm() + " dias uteis");
        }
        p.definirMoral(p.moral() + regras.ganhoMoralUmAUm());
        p.registrarUmAUm(partida.hoje());
        partida.somarHorasLider(regras.horasLiderUmAUm());
        evento(partida, TipoEvento.PESSOA, "eventos.umAUm",
                Map.of("pessoa", p.nome(), "moral", String.valueOf(p.moral())));
    }

    private static Pessoa pessoa(Partida partida, String id) {
        Pessoa p = partida.pessoa(id).orElseThrow(() -> new OrdemInvalidaException("Pessoa nao encontrada: " + id));
        if (!p.naEmpresa()) {
            throw new OrdemInvalidaException(p.nome() + " nao esta mais na empresa");
        }
        return p;
    }

    private static void evento(Partida partida, TipoEvento tipo, String texto, Map<String, String> valores) {
        partida.registrarEvento(tipo, null, texto, valores);
    }
}
