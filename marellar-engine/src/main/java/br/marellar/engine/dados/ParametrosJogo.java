package br.marellar.engine.dados;

import java.util.Objects;

/** Todos os parametros de data/ ja carregados e validados. */
public record ParametrosJogo(
        ParametrosLinha linha33,
        CatalogoNc catalogoNc,
        ParametrosPessoas pessoas,
        ParametrosFornecedores fornecedores,
        ParametrosEventos eventos,
        ParametrosFinanceiro financeiro,
        Textos textos) {

    public ParametrosJogo {
        Objects.requireNonNull(linha33, "linha33");
        Objects.requireNonNull(catalogoNc, "catalogoNc");
        Objects.requireNonNull(pessoas, "pessoas");
        Objects.requireNonNull(fornecedores, "fornecedores");
        Objects.requireNonNull(eventos, "eventos");
        Objects.requireNonNull(financeiro, "financeiro");
        Objects.requireNonNull(textos, "textos");
    }

    public ParametrosJogo comLinha33(ParametrosLinha nova) {
        return new ParametrosJogo(nova, catalogoNc, pessoas, fornecedores, eventos, financeiro, textos);
    }

    public ParametrosJogo comCatalogoNc(CatalogoNc novo) {
        return new ParametrosJogo(linha33, novo, pessoas, fornecedores, eventos, financeiro, textos);
    }

    public ParametrosJogo comPessoas(ParametrosPessoas novas) {
        return new ParametrosJogo(linha33, catalogoNc, novas, fornecedores, eventos, financeiro, textos);
    }
}
