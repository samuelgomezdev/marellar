package br.marellar.web;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.DadosInvalidosException;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.dados.Textos;
import br.marellar.engine.dados.ValidadorDados;
import br.marellar.web.json.CodecGerado;
import br.marellar.web.json.Json;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Carrega os JSON de data/ sem Jackson (no navegador eles chegam como texto). */
public final class DadosWeb {

    private DadosWeb() {
    }

    /** {@code arquivos}: nome do arquivo (ex.: "linha-33.json") para o conteudo. */
    public static ParametrosJogo carregar(Map<String, String> arquivos) {
        Function<String, Object> arvore = nome -> {
            String conteudo = arquivos.get(nome);
            if (conteudo == null) {
                throw new DadosInvalidosException(List.of(nome + ": arquivo nao encontrado"));
            }
            try {
                return Json.ler(conteudo);
            } catch (RuntimeException e) {
                throw new DadosInvalidosException(nome + ": " + e.getMessage(), e);
            }
        };
        ParametrosJogo p = new ParametrosJogo(
                CodecGerado.lerParametrosLinha(arvore.apply(CarregadorDados.LINHA_33)),
                CodecGerado.lerCatalogoNc(arvore.apply(CarregadorDados.NC_CATALOGO)),
                CodecGerado.lerParametrosPessoas(arvore.apply(CarregadorDados.PESSOAS)),
                CodecGerado.lerParametrosFornecedores(arvore.apply(CarregadorDados.FORNECEDORES)),
                CodecGerado.lerParametrosEventos(arvore.apply(CarregadorDados.EVENTOS)),
                CodecGerado.lerParametrosFinanceiro(arvore.apply(CarregadorDados.FINANCEIRO)),
                new Textos(Json.objeto(arvore.apply(CarregadorDados.TEXTOS), CarregadorDados.TEXTOS)));
        ValidadorDados.validar(p);
        return p;
    }

    public static final List<String> ARQUIVOS = List.of(CarregadorDados.LINHA_33, CarregadorDados.NC_CATALOGO,
            CarregadorDados.PESSOAS, CarregadorDados.FORNECEDORES, CarregadorDados.EVENTOS,
            CarregadorDados.FINANCEIRO, CarregadorDados.TEXTOS);
}
