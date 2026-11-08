package br.marellar.engine.dados;

import br.marellar.engine.dominio.Funcao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Equipe inicial e regras de pessoas (arquivo data/pessoas.json). */
public record ParametrosPessoas(
        Map<Integer, BigDecimal> fatorNivel,
        Map<Integer, BigDecimal> salarioPorNivel,
        BigDecimal salarioInspetor,
        int diasChegadaContratacao,
        int diasParaSubirNivel,
        int nivelMaximoPorTempo,
        int diasTreinamento,
        int ganhoMoralUmAUm,
        BigDecimal horasLiderUmAUm,
        int diasSemUmAUmAntesDeCair,
        int quedaMoralDiariaSemUmAUm,
        int diasEntreUmAUm,
        int moralInicialContratado,
        List<String> nomesParaContratar,
        List<PessoaInicial> equipe) {

    public ParametrosPessoas {
        fatorNivel = fatorNivel == null ? Map.of() : Map.copyOf(fatorNivel);
        salarioPorNivel = salarioPorNivel == null ? Map.of() : Map.copyOf(salarioPorNivel);
        nomesParaContratar = nomesParaContratar == null ? List.of() : List.copyOf(nomesParaContratar);
        equipe = equipe == null ? List.of() : List.copyOf(equipe);
    }

    /** Personalidade do agente (0 a 100 em cada atributo). So o inspetor tem no MVP. */
    public record Personalidade(int rigor, int velocidade, int paciencia) {}

    /** {@code treinoSegurancaEmDia} ausente no JSON = em dia. */
    public record PessoaInicial(String id, String nome, Funcao funcao, int nivel, int moral,
                                Personalidade personalidade, Boolean treinoSegurancaEmDia) {

        public PessoaInicial(String id, String nome, Funcao funcao, int nivel, int moral, Personalidade personalidade) {
            this(id, nome, funcao, nivel, moral, personalidade, null);
        }

        public boolean segurancaEmDia() {
            return treinoSegurancaEmDia == null || treinoSegurancaEmDia;
        }
    }

    public BigDecimal fatorDoNivel(int nivel) {
        BigDecimal fator = fatorNivel.get(nivel);
        if (fator == null) {
            throw new IllegalArgumentException("Nivel sem fator: " + nivel);
        }
        return fator;
    }

    public ParametrosPessoas comEquipe(List<PessoaInicial> nova) {
        return new ParametrosPessoas(fatorNivel, salarioPorNivel, salarioInspetor, diasChegadaContratacao,
                diasParaSubirNivel, nivelMaximoPorTempo, diasTreinamento, ganhoMoralUmAUm, horasLiderUmAUm,
                diasSemUmAUmAntesDeCair, quedaMoralDiariaSemUmAUm, diasEntreUmAUm, moralInicialContratado,
                nomesParaContratar, nova);
    }
}
