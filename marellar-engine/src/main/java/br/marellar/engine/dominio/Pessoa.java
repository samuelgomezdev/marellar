package br.marellar.engine.dominio;

import br.marellar.engine.dados.ParametrosPessoas.Personalidade;

import java.util.Objects;
import java.util.Optional;

/** Funcionario da fabrica. No MVP: produtivos da linha de 33 pes e 1 inspetor. */
public final class Pessoa {

    private final String id;
    private final String nome;
    private final Funcao funcao;
    private final Personalidade personalidade;
    private final boolean contratadaNaPartida;
    private int nivel;
    private int moral;
    private boolean treinoSegurancaEmDia;
    private SituacaoPessoa situacao = SituacaoPessoa.ATIVA;
    private int diasRestantesSituacao;
    private Treinamento treinamentoEmCurso;
    private long diaUltimoUmAUm;
    private boolean umAUmFeito;
    private int diasNaLinha;
    private String alocadoEm;

    public Pessoa(String id, String nome, Funcao funcao, int nivel, int moral, Personalidade personalidade) {
        this(id, nome, funcao, nivel, moral, personalidade, true, false);
    }

    public Pessoa(String id, String nome, Funcao funcao, int nivel, int moral, Personalidade personalidade,
                  boolean treinoSegurancaEmDia, boolean contratadaNaPartida) {
        this.id = Objects.requireNonNull(id);
        this.nome = nome;
        this.funcao = Objects.requireNonNull(funcao);
        this.nivel = nivel;
        this.moral = moral;
        this.personalidade = personalidade;
        this.treinoSegurancaEmDia = treinoSegurancaEmDia;
        this.contratadaNaPartida = contratadaNaPartida;
    }

    // ----- situacao -----

    public void mudarSituacao(SituacaoPessoa nova, int dias) {
        this.situacao = nova;
        this.diasRestantesSituacao = dias;
        if (nova != SituacaoPessoa.ATIVA) {
            liberar();
        }
    }

    /** Desconta 1 dia da situacao atual; devolve true quando ela acabou hoje. */
    public boolean passarDiaNaSituacao() {
        if (diasRestantesSituacao <= 0) {
            return false;
        }
        diasRestantesSituacao--;
        return diasRestantesSituacao == 0;
    }

    public void iniciarTreinamento(Treinamento t, int dias) {
        this.treinamentoEmCurso = t;
        mudarSituacao(SituacaoPessoa.EM_TREINAMENTO, dias);
    }

    public Optional<Treinamento> concluirTreinamento() {
        Treinamento t = treinamentoEmCurso;
        treinamentoEmCurso = null;
        mudarSituacao(SituacaoPessoa.ATIVA, 0);
        return Optional.ofNullable(t);
    }

    public boolean disponivel() {
        return situacao == SituacaoPessoa.ATIVA;
    }

    public boolean naEmpresa() {
        return situacao != SituacaoPessoa.DESLIGADA;
    }

    /** Conta um dia de linha para quem foi contratado na partida (sobe de nivel com o tempo). */
    public int contarDiaNaLinha() {
        return ++diasNaLinha;
    }

    // ----- moral, nivel, alocacao -----

    public void alocar(String barcoId) {
        this.alocadoEm = barcoId;
    }

    public void liberar() {
        this.alocadoEm = null;
    }

    public void definirMoral(int novaMoral) {
        this.moral = Math.max(0, Math.min(100, novaMoral));
    }

    public void definirNivel(int novoNivel) {
        this.nivel = Math.max(1, Math.min(5, novoNivel));
    }

    public void definirTreinoSegurancaEmDia(boolean emDia) {
        this.treinoSegurancaEmDia = emDia;
    }

    public void registrarUmAUm(long dia) {
        this.diaUltimoUmAUm = dia;
        this.umAUmFeito = true;
    }

    /** Comeca a contar os dias sem 1:1 a partir de {@code dia}, sem contar como 1:1 feito. */
    public void marcarInicioSemUmAUm(long dia) {
        this.diaUltimoUmAUm = dia;
    }

    /** Pode ter 1:1 se nunca teve um nesta partida ou se o ultimo foi ha pelo menos {@code intervalo} dias. */
    public boolean podeFazerUmAUm(long hoje, int intervalo) {
        return !umAUmFeito || hoje - diaUltimoUmAUm >= intervalo;
    }

    // ----- leitura -----

    public String id() {
        return id;
    }

    public String nome() {
        return nome;
    }

    public Funcao funcao() {
        return funcao;
    }

    public int nivel() {
        return nivel;
    }

    public int moral() {
        return moral;
    }

    public Optional<Personalidade> personalidade() {
        return Optional.ofNullable(personalidade);
    }

    public boolean treinoSegurancaEmDia() {
        return treinoSegurancaEmDia;
    }

    public boolean contratadaNaPartida() {
        return contratadaNaPartida;
    }

    public SituacaoPessoa situacao() {
        return situacao;
    }

    public int diasRestantesSituacao() {
        return diasRestantesSituacao;
    }

    public Optional<Treinamento> treinamentoEmCurso() {
        return Optional.ofNullable(treinamentoEmCurso);
    }

    public long diaUltimoUmAUm() {
        return diaUltimoUmAUm;
    }

    public Optional<String> alocadoEm() {
        return Optional.ofNullable(alocadoEm);
    }
}
