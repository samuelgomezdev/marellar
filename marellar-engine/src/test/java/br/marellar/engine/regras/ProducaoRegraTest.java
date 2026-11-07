package br.marellar.engine.regras;

import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.ParametrosPessoas;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Pessoa;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ProducaoRegraTest {

    private static final ParametrosPessoas REGRAS = CarregadorDados.carregarDoClasspath().pessoas();

    @ParameterizedTest
    @CsvSource({
            // nivel, moral, esperado = fatorNivel x (0,7 + 0,3 x moral/100)
            "1,   0, 0.4200",
            "1, 100, 0.6000",
            "3,  50, 0.7650",
            "4, 100, 1.0000",
            "5, 100, 1.1000",
            "5,   0, 0.7700"
    })
    void produtividadeSegueAFormulaDaSpec(int nivel, int moral, String esperado) {
        Pessoa p = new Pessoa("X", "Teste", Funcao.PRODUCAO, nivel, moral, null);

        assertThat(ProducaoRegra.produtividade(p, REGRAS)).isEqualByComparingTo(esperado);
    }
}
