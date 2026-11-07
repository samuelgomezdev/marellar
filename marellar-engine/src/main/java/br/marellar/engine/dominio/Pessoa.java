package br.marellar.engine.dominio;

import br.marellar.engine.dados.ParametrosPessoas.Personalidade;

import java.util.Objects;
import java.util.Optional;

/** Funcionario da fabrica. No MVP: produtivos da linha de 33 pes e 1 inspetor. */
public final class Pessoa {

    private final String id;
    private final String nome;
    private final Funcao funcao;
    private int nivel;
    private int moral;
    private final Personalidade personalidade;
    private String alocadoEm;

    public Pessoa(String id, String nome, Funcao funcao, int nivel, int moral, Personalidade personalidade) {
        this.id = Objects.requireNonNull(id);
        this.nome = nome;
        this.funcao = Objects.requireNonNull(funcao);
        this.nivel = nivel;
        this.moral = moral;
        this.personalidade = personalidade;
    }

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

    public Optional<String> alocadoEm() {
        return Optional.ofNullable(alocadoEm);
    }
}
