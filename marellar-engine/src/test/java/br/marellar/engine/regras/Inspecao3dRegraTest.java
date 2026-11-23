package br.marellar.engine.regras;

import br.marellar.engine.Cenarios;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.NcInspecao3d;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.save.Save;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Inspecao3dRegraTest {

    private static Ordem.RegistrarNcInspecao nc(String barco, String defeito, Gravidade gravidade) {
        return new Ordem.RegistrarNcInspecao(barco, defeito, gravidade, "VTR-33-011", "CMD",
                new BigDecimal("5.5512"), new BigDecimal("2.07"), new BigDecimal("-0.3"), "  trinca no painel ");
    }

    @Test
    void registraNoGateAtualComOsDadosDoCatalogoAmpliado() {
        Simulacao sim = Simulacao.novaPartida(1, Cenarios.PADRAO);

        sim.aplicarOrdem(nc("33-002", "TEC-INFILTRA", Gravidade.CRITICA));     // 33-002 comeca no BA

        assertThat(sim.estado().ncsInspecao()).singleElement().satisfies(n -> {
            assertThat(n.id()).isEqualTo("NI-0001");
            assertThat(n.numero()).isEqualTo(1);
            assertThat(n.barcoId()).isEqualTo("33-002");
            assertThat(n.gate()).isEqualTo(CodigoGate.BA);
            assertThat(n.codigoDefeito()).isEqualTo("TEC-INFILTRA");
            assertThat(n.codigoBase()).isEqualTo("EST-INFILTRA");
            assertThat(n.categoria()).isEqualTo("ESTANQUEIDADE");
            assertThat(n.descricao()).isEqualTo("Infiltração sob a teca (água entre a teca e a fibra)");
            assertThat(n.item()).isEqualTo("VTR-33-011");
            assertThat(n.compartimento()).isEqualTo("CMD");
            assertThat(n.x()).isEqualByComparingTo("5.551");
            assertThat(n.observacao()).isEqualTo("trinca no painel");
        });
        assertThat(sim.eventosDesde(0)).anySatisfy(e -> {
            assertThat(e.tipo()).isEqualTo(TipoEvento.NC_INSPECAO_3D);
            assertThat(e.barcoId()).isEqualTo("33-002");
            assertThat(e.mensagem()).contains("NC 1").contains("33-002").contains("gate BA");
        });
    }

    @Test
    void numeroContaPorBarco() {
        Simulacao sim = Simulacao.novaPartida(1, Cenarios.PADRAO);
        sim.aplicarOrdem(nc("33-002", "ACA-RISCO", Gravidade.LEVE));
        sim.aplicarOrdem(nc("33-001", "ACA-RISCO", Gravidade.LEVE));
        sim.aplicarOrdem(nc("33-002", "GEL-TRINCA", Gravidade.MEDIA));

        assertThat(sim.estado().ncsInspecao()).extracting(NcInspecao3d::id, NcInspecao3d::barcoId, NcInspecao3d::numero)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("NI-0001", "33-002", 1),
                        org.assertj.core.groups.Tuple.tuple("NI-0002", "33-001", 1),
                        org.assertj.core.groups.Tuple.tuple("NI-0003", "33-002", 2));
    }

    @Test
    void gravidadeNaoFicaAbaixoDoCatalogoEDefeitoTemDeExistir() {
        Simulacao sim = Simulacao.novaPartida(1, Cenarios.PADRAO);

        assertThatThrownBy(() -> sim.aplicarOrdem(nc("33-002", "LAM-SECO", Gravidade.LEVE)))
                .isInstanceOf(OrdemInvalidaException.class).hasMessageContaining("nao pode ficar abaixo");
        assertThatThrownBy(() -> sim.aplicarOrdem(nc("33-002", "XYZ-000", Gravidade.LEVE)))
                .isInstanceOf(OrdemInvalidaException.class).hasMessageContaining("fora do catalogo");
        assertThatThrownBy(() -> sim.aplicarOrdem(nc("99-999", "ACA-RISCO", Gravidade.LEVE)))
                .isInstanceOf(OrdemInvalidaException.class).hasMessageContaining("Barco nao encontrado");
        assertThat(sim.estado().ncsInspecao()).isEmpty();
    }

    @Test
    void ncDaInspecaoVaiNoSaveEVoltaAoCarregar() throws Exception {
        ObjectMapper json = new ObjectMapper();
        Simulacao sim = Simulacao.novaPartida(7, Cenarios.PADRAO);
        sim.avancar(20);
        sim.aplicarOrdem(nc("33-001", "TEC-INFILTRA", Gravidade.CRITICA));
        sim.avancar(5);

        Save lido = json.readValue(json.writeValueAsString(sim.salvar()), Save.class);
        Simulacao carregada = Simulacao.carregar(lido, Cenarios.PADRAO);

        assertThat(carregada.estado().ncsInspecao()).isEqualTo(sim.estado().ncsInspecao()).hasSize(1);
    }
}
