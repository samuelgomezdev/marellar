package br.marellar.engine.regras;

import br.marellar.engine.Partida;
import br.marellar.engine.dados.CatalogoInspecao;
import br.marellar.engine.dominio.Barco;
import br.marellar.engine.dominio.NcInspecao3d;
import br.marellar.engine.dominio.TipoEvento;
import br.marellar.engine.ordens.Ordem;
import br.marellar.engine.ordens.OrdemInvalidaException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Inspecao 3D dentro do jogo: o jogador marca uma NC no ponto clicado do barco. O motor confere o defeito no
 * catalogo ampliado, nao deixa a gravidade ficar abaixo da do catalogo e grava a NC no gate atual do barco.
 */
public final class Inspecao3dRegra {

    static final int MAX_TEXTO = 40;
    static final int MAX_OBSERVACAO = 300;

    private Inspecao3dRegra() {
    }

    public static NcInspecao3d registrar(Partida partida, Ordem.RegistrarNcInspecao o) {
        Barco barco = partida.barco(o.barcoId())
                .orElseThrow(() -> new OrdemInvalidaException("Barco nao encontrado: " + o.barcoId()));
        CatalogoInspecao.Defeito d = partida.parametros().catalogoInspecao().defeito(o.codigoDefeito())
                .orElseThrow(() -> new OrdemInvalidaException("Defeito fora do catalogo da inspecao: " + o.codigoDefeito()));
        if (o.gravidade() == null) {
            throw new OrdemInvalidaException("Informe a gravidade da NC");
        }
        if (o.gravidade().compareTo(d.gravidade()) < 0) {
            throw new OrdemInvalidaException("O catalogo classifica " + d.codigo() + " como " + d.gravidade()
                    + "; a gravidade pode subir, mas nao pode ficar abaixo disso");
        }
        if (o.x() == null || o.y() == null || o.z() == null) {
            throw new OrdemInvalidaException("Informe o ponto da NC (x, y, z)");
        }
        String item = texto(o.item(), MAX_TEXTO, "item");
        String compartimento = texto(o.compartimento(), MAX_TEXTO, "compartimento");
        String observacao = texto(o.observacao(), MAX_OBSERVACAO, "observacao");
        int numero = (int) partida.ncsInspecao().stream().filter(n -> n.barcoId().equals(barco.id())).count() + 1;
        NcInspecao3d nc = new NcInspecao3d(
                String.format("NI-%04d", partida.ncsInspecao().size() + 1), numero, barco.id(), barco.gateAtual(),
                d.codigo(), d.base(), d.categoria(), d.descricao(), o.gravidade(), item, compartimento,
                metros(o.x()), metros(o.y()), metros(o.z()), observacao,
                partida.relogio().data(), partida.relogio().hora());
        partida.registrarNcInspecao(nc);
        partida.registrarEvento(TipoEvento.NC_INSPECAO_3D, barco.id(), "eventos.ncInspecao3d", Map.of(
                "numero", String.valueOf(numero), "barco", barco.id(), "gate", barco.gateAtual().name(),
                "descricao", d.descricao(), "gravidade", o.gravidade().name()));
        return nc;
    }

    private static String texto(String valor, int maximo, String campo) {
        String limpo = valor == null ? "" : valor.strip();
        if (limpo.length() > maximo) {
            throw new OrdemInvalidaException("O campo " + campo + " passa de " + maximo + " caracteres");
        }
        return limpo;
    }

    /** Milimetros bastam para o ponto da NC e deixam o save estavel. */
    private static BigDecimal metros(BigDecimal valor) {
        return valor.setScale(3, RoundingMode.HALF_EVEN);
    }
}
