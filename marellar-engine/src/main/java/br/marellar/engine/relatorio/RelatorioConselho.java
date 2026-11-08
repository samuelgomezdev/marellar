package br.marellar.engine.relatorio;

import br.marellar.engine.dominio.MotivoDerrota;

import java.math.BigDecimal;
import java.util.List;

/** O que o conselho ve e decide na reuniao do fim do mes. */
public record RelatorioConselho(
        int mes,
        String mesReferencia,
        List<Meta> metas,
        Dre dre,
        BigDecimal caixa,
        BigDecimal valorEmpresa,
        BigDecimal descontoValuation,
        int barcosEntregues,
        int ncsDetectadas,
        int rncs,
        int concessoes,
        int acidentes,
        BigDecimal taxaFrequencia,
        BigDecimal taxaGravidade,
        int pacienciaAntes,
        int pacienciaDepois,
        boolean alertaCaixaNegativo,
        List<Fala> falas,
        List<Opcao> opcoes,
        String decisaoEscolhida,
        MotivoDerrota derrota) {

    public record Meta(String id, String descricao, boolean cumprida, String resultado) {}

    public record Fala(String conselheiro, String papel, String texto, boolean seria) {}

    public record Opcao(String id, String titulo, String descricao, BigDecimal custo) {}

    public RelatorioConselho comDecisao(String opcaoId) {
        return new RelatorioConselho(mes, mesReferencia, metas, dre, caixa, valorEmpresa, descontoValuation,
                barcosEntregues, ncsDetectadas, rncs, concessoes, acidentes, taxaFrequencia, taxaGravidade,
                pacienciaAntes, pacienciaDepois, alertaCaixaNegativo, falas, opcoes, opcaoId, derrota);
    }
}
