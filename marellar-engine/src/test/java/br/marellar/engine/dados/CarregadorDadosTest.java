package br.marellar.engine.dados;

import br.marellar.engine.dominio.CodigoGate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarregadorDadosTest {

    @Test
    void carregaOsParametrosDoClasspath() {
        ParametrosJogo p = CarregadorDados.carregarDoClasspath();

        assertThat(p.linha33().gates()).hasSize(10);
        assertThat(p.linha33().hhPorBarco()).isEqualByComparingTo("1900");
        assertThat(p.linha33().custoHh()).isEqualByComparingTo("57.00");
        assertThat(p.pessoas().equipe()).hasSize(13);
        assertThat(p.fornecedores().fornecedores()).hasSize(3);
        assertThat(p.catalogoNc().tipos()).isNotEmpty();
        assertThat(p.financeiro().caixaInicial()).isEqualByComparingTo("3000000.00");
    }

    @Test
    void carregaOsParametrosDaPastaDataDoRepositorio() {
        Path data = Path.of("..", "data");
        ParametrosJogo p = CarregadorDados.carregarDeDiretorio(data);

        assertThat(p.linha33().nome()).isEqualTo("Linha de 33 pés");
    }

    @Test
    void hhDoGateUsaOPesoDoGate() {
        ParametrosLinha l = CarregadorDados.carregarDoClasspath().linha33();

        assertThat(l.hhDoGate(CodigoGate.DM)).isEqualByComparingTo("532"); // 1900 x 28%
        assertThat(l.hhDoGate(CodigoGate.RE)).isEqualByComparingTo("38");  // 1900 x 2%
    }

    @Test
    void cotasSomam100PorCentoDoPreco() {
        ParametrosLinha l = CarregadorDados.carregarDoClasspath().linha33();

        BigDecimal soma = l.gates().stream().map(ParametrosLinha.Gate::cotaPercentual)
                .reduce(l.cotaContratoPercentual(), BigDecimal::add);

        assertThat(soma).isEqualByComparingTo("100");
    }

    @Test
    void campoInteiroAusenteGeraErro() {
        assertThatThrownBy(() -> CarregadorDados.ler("financeiro.json", "{ \"caixaInicial\": 10 }", ParametrosFinanceiro.class))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("financeiro.json");
    }

    @Test
    void jsonMalFormadoGeraErroComArquivoELinha() {
        assertThatThrownBy(() -> CarregadorDados.ler("linha-33.json", "{ \"linha\": ", ParametrosLinha.class))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("linha-33.json")
                .hasMessageContaining("linha 1");
    }

    @Test
    void campoDesconhecidoGeraErro() {
        assertThatThrownBy(() -> CarregadorDados.ler("financeiro.json",
                """
                { "caixaInicial": 1, "despesasFixasMensais": 1, "multiploValuation": 6,
                  "descontoPorAcidenteComAfastamento": 0.05, "descontoPorNcEscapada": 0.03,
                  "mesesCaixaNegativoParaDerrota": 3, "caixaInicialErrado": 10 }
                """, ParametrosFinanceiro.class))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("financeiro.json")
                .hasMessageContaining("caixaInicialErrado");
    }

    @Test
    void pesosQueNaoSomam100SaoRejeitados() {
        ParametrosJogo p = CarregadorDados.carregarDoClasspath();
        List<ParametrosLinha.Gate> gates = new ArrayList<>(p.linha33().gates());
        ParametrosLinha.Gate re = gates.getFirst();
        gates.set(0, new ParametrosLinha.Gate(re.codigo(), re.nome(), new BigDecimal("5"), re.cotaPercentual()));

        ParametrosJogo invalido = p.comLinha33(p.linha33().comGates(gates));

        assertThatThrownBy(() -> ValidadorDados.validar(invalido))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("pesoHhPercentual")
                .hasMessageContaining("103");
    }

    @Test
    void gatesForaDeOrdemSaoRejeitados() {
        ParametrosJogo p = CarregadorDados.carregarDoClasspath();
        List<ParametrosLinha.Gate> gates = new ArrayList<>(p.linha33().gates());
        java.util.Collections.swap(gates, 0, 1);

        ParametrosJogo invalido = p.comLinha33(p.linha33().comGates(gates));

        assertThatThrownBy(() -> ValidadorDados.validar(invalido))
                .hasMessageContaining("nesta ordem");
    }

    @Test
    void juntaTodosOsErrosNumaSoMensagem() {
        ParametrosJogo p = CarregadorDados.carregarDoClasspath();
        List<ParametrosPessoas.PessoaInicial> equipe = new ArrayList<>(p.pessoas().equipe());
        ParametrosPessoas.PessoaInicial primeira = equipe.getFirst();
        equipe.set(0, new ParametrosPessoas.PessoaInicial(primeira.id(), primeira.nome(), primeira.funcao(),
                9, 150, null));

        ParametrosJogo invalido = p.comPessoas(p.pessoas().comEquipe(equipe));

        assertThatThrownBy(() -> ValidadorDados.validar(invalido))
                .isInstanceOfSatisfying(DadosInvalidosException.class, e ->
                        assertThat(e.erros()).hasSize(2)
                                .anyMatch(m -> m.contains("nivel"))
                                .anyMatch(m -> m.contains("moral")));
    }

    @Test
    void arquivoAusenteNaPastaGeraErroClaro(@org.junit.jupiter.api.io.TempDir Path vazia) {
        assertThatThrownBy(() -> CarregadorDados.carregarDeDiretorio(vazia))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("linha-33.json")
                .hasMessageContaining("nao encontrado");
    }

    @Test
    void textosTrocamAsChaves() {
        Textos t = CarregadorDados.carregarDoClasspath().textos();

        String texto = t.formatar("eventos.gateAprovado", Map.of("barco", "33-001", "gate", "DM"));

        assertThat(texto).isEqualTo("33-001: gate DM aprovado.");
        assertThat(t.fala("inspetor.gateAprovado", 0)).isNotBlank();
    }
}
