package br.marellar.engine;

import java.math.BigDecimal;

/** Acumula o que aconteceu no mes de jogo corrente; zera depois da reuniao do conselho. */
public final class MesContabil {

    BigDecimal receita = BigDecimal.ZERO;
    BigDecimal custoMaterial = BigDecimal.ZERO;
    BigDecimal materialRetrabalho = BigDecimal.ZERO;
    BigDecimal chargebackRecuperado = BigDecimal.ZERO;
    BigDecimal compras = BigDecimal.ZERO;
    int gatesDmAprovados;
    int ncsDetectadas;
    int ncsEscapadas;
    int acidentes;
    int barcosEntregues;
    int rncs;
    int concessoes;

    public void somarReceita(BigDecimal v) {
        receita = receita.add(v);
    }

    public void somarCustoMaterial(BigDecimal v) {
        custoMaterial = custoMaterial.add(v);
    }

    public void somarMaterialRetrabalho(BigDecimal v) {
        materialRetrabalho = materialRetrabalho.add(v);
    }

    public void somarChargeback(BigDecimal v) {
        chargebackRecuperado = chargebackRecuperado.add(v);
    }

    public void somarCompras(BigDecimal v) {
        compras = compras.add(v);
    }

    public void contarGateDm() {
        gatesDmAprovados++;
    }

    public void contarNcDetectada() {
        ncsDetectadas++;
    }

    public void contarNcEscapada() {
        ncsEscapadas++;
    }

    public void contarAcidente() {
        acidentes++;
    }

    public void contarEntrega() {
        barcosEntregues++;
    }

    public void contarRnc() {
        rncs++;
    }

    public void contarConcessao() {
        concessoes++;
    }

    public BigDecimal receita() {
        return receita;
    }

    public BigDecimal custoMaterial() {
        return custoMaterial;
    }

    public BigDecimal materialRetrabalho() {
        return materialRetrabalho;
    }

    public BigDecimal chargebackRecuperado() {
        return chargebackRecuperado;
    }

    public BigDecimal compras() {
        return compras;
    }

    public int gatesDmAprovados() {
        return gatesDmAprovados;
    }

    public int ncsDetectadas() {
        return ncsDetectadas;
    }

    public int ncsEscapadas() {
        return ncsEscapadas;
    }

    public int acidentes() {
        return acidentes;
    }

    public int barcosEntregues() {
        return barcosEntregues;
    }

    public int rncs() {
        return rncs;
    }

    public int concessoes() {
        return concessoes;
    }
}
