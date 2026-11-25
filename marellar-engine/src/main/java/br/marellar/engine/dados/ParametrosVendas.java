package br.marellar.engine.dados;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Vendas, prazo e multa (arquivo data/vendas.json; M14, DEC-0047 da wiki).
 * <ul>
 *   <li>Pedidos esperados no mes = mercadoInternoAnual / 12 x fatia x sazonalidade do mes (PREM-0007 e PREM-0008),
 *       limitados a capacidade declarada.</li>
 *   <li>Prazos em dias uteis; uma semana = diasUteisPorSemana.</li>
 *   <li>Multa = multaPercentualPorSemana por semana de atraso iniciada, ate multaTetoPercentual do preco.</li>
 *   <li>Desistencia: nenhuma ate desistenciaInicio x o prazo pedido; certa a partir de desistenciaTotal x.</li>
 * </ul>
 */
public record ParametrosVendas(
        String vendedor,
        BigDecimal mercadoInternoAnual,
        BigDecimal fatiaMercadoPercentual,
        List<BigDecimal> sazonalidade,
        int capacidadeInicial,
        int capacidadeMaxima,
        int moldes,
        int cicloPlanejadoDiasUteis,
        int prazoPedidoMinDiasUteis,
        int prazoPedidoMaxDiasUteis,
        int validadePropostaDiasUteis,
        int diasUteisPorSemana,
        BigDecimal multaPercentualPorSemana,
        BigDecimal multaTetoPercentual,
        BigDecimal desistenciaInicio,
        BigDecimal desistenciaTotal,
        List<String> clientes,
        Map<String, Integer> prazosIniciaisDiasUteis) {

    public ParametrosVendas {
        sazonalidade = sazonalidade == null ? List.of() : List.copyOf(sazonalidade);
        clientes = clientes == null ? List.of() : List.copyOf(clientes);
        prazosIniciaisDiasUteis = prazosIniciaisDiasUteis == null ? Map.of() : Map.copyOf(prazosIniciaisDiasUteis);
    }

    public ParametrosVendas comPrazosIniciais(Map<String, Integer> prazos) {
        return new ParametrosVendas(vendedor, mercadoInternoAnual, fatiaMercadoPercentual, sazonalidade, capacidadeInicial,
                capacidadeMaxima, moldes, cicloPlanejadoDiasUteis, prazoPedidoMinDiasUteis, prazoPedidoMaxDiasUteis,
                validadePropostaDiasUteis, diasUteisPorSemana, multaPercentualPorSemana, multaTetoPercentual,
                desistenciaInicio, desistenciaTotal, clientes, prazos);
    }

    public ParametrosVendas comMercado(BigDecimal anual, BigDecimal fatia) {
        return new ParametrosVendas(vendedor, anual, fatia, sazonalidade, capacidadeInicial, capacidadeMaxima, moldes,
                cicloPlanejadoDiasUteis, prazoPedidoMinDiasUteis, prazoPedidoMaxDiasUteis, validadePropostaDiasUteis,
                diasUteisPorSemana, multaPercentualPorSemana, multaTetoPercentual, desistenciaInicio, desistenciaTotal,
                clientes, prazosIniciaisDiasUteis);
    }
}
