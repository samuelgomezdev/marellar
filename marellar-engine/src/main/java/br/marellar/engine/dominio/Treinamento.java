package br.marellar.engine.dominio;

public enum Treinamento {
    /** Treinamento tecnico: sobe 1 nivel (maximo 5). */
    TECNICO("técnico"),
    /** Treinamento de seguranca (NR): coloca o treino em dia e reduz a chance de acidente. */
    SEGURANCA("segurança");

    private final String nome;

    Treinamento(String nome) {
        this.nome = nome;
    }

    public String nome() {
        return nome;
    }
}
