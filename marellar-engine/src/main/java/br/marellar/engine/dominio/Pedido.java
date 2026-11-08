package br.marellar.engine.dominio;

import java.math.BigDecimal;

/**
 * Pedido de compra de kits de material.
 * {@code diaPrevisto} e o prazo do fornecedor; {@code diaChegada} inclui o atraso sorteado.
 */
public record Pedido(String id, String fornecedorId, String item, int kits, BigDecimal valorBruto,
                     BigDecimal descontoChargeback, long diaPedido, long diaPrevisto, long diaChegada) {

    public boolean atrasado() {
        return diaChegada > diaPrevisto;
    }

    public BigDecimal valorPago() {
        return valorBruto.subtract(descontoChargeback);
    }
}
