package br.marellar.engine.dados;

import br.marellar.engine.dominio.PerfilFornecedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Fornecedores sinteticos e a escada de cobranca (arquivo data/fornecedores.json). */
public record ParametrosFornecedores(
        PesosScorecard pesosScorecard,
        BigDecimal taxaAdministrativaChargeback,
        int prazo8dDiasUteis,
        Map<PerfilFornecedor, ParametrosPerfil> perfis,
        List<FornecedorInicial> fornecedores) {

    public ParametrosFornecedores {
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
}
