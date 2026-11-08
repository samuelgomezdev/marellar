package br.marellar.engine.dados;

import br.marellar.engine.dominio.PerfilFornecedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Fornecedores sinteticos e a escada de cobranca (arquivo data/fornecedores.json). */
public record ParametrosFornecedores(
        PesosScorecard pesosScorecard,
        BigDecimal taxaAdministrativaChargeback,
        int prazo8dDiasUteis,
        int diasAtrasoMin,
        int diasAtrasoMax,
        int ppmReferencia,
        BigDecimal fatorFornecedorMin,
        BigDecimal fatorFornecedorMax,
        Map<String, Integer> notas,
        Map<PerfilFornecedor, ParametrosPerfil> perfis,
        List<FornecedorInicial> fornecedores) {

    public ParametrosFornecedores {
        notas = notas == null ? Map.of() : Map.copyOf(notas);
        perfis = perfis == null ? Map.of() : Map.copyOf(perfis);
        fornecedores = fornecedores == null ? List.of() : List.copyOf(fornecedores);
    }

    public record PesosScorecard(int entrega, int qualidade, int custo, int resposta) {
        public int soma() {
            return entrega + qualidade + custo + resposta;
        }
    }

    public record ParametrosPerfil(int ppmBase, BigDecimal probabilidadeAtraso, int diasEntrega,
                                   BigDecimal fatorPreco, int diasResposta8d) {}

    public record FornecedorInicial(String id, String nome, PerfilFornecedor perfil, List<String> itens) {
        public FornecedorInicial {
            itens = itens == null ? List.of() : List.copyOf(itens);
        }
    }

    public ParametrosPerfil perfil(PerfilFornecedor perfil) {
        return perfis.get(perfil);
    }

    public Optional<FornecedorInicial> fornecedorDoItem(String item) {
        return fornecedores.stream().filter(f -> f.itens().contains(item)).findFirst();
    }

    /** Nota A a F a partir dos pontos (0 a 100), pelos cortes de {@code notas}. */
    public String nota(int pontos) {
        for (String letra : List.of("A", "B", "C", "D", "E")) {
            Integer corte = notas.get(letra);
            if (corte != null && pontos >= corte) {
                return letra;
            }
        }
        return "F";
    }
}
