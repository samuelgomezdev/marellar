package br.marellar.engine.estado;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.Linha;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.Velocidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Retrato imutavel da partida num instante, como o jogador ve. E o que a CLI imprime,
 * a API devolve e o teste de determinismo compara (records tem equals por valor).
 * NCs que escaparam do inspetor NAO aparecem aqui.
 */
public record EstadoPartida(
        long semente,
        long tick,
        LocalDate data,
        int hora,
        int mes,
        String mesReferencia,
        int diaUtilDoMes,
        Velocidade velocidade,
        boolean pausado,
        MotivoPausa motivoPausa,
        List<BarcoVisao> barcos,
        List<PessoaVisao> pessoas,
        InspetorVisao inspetor,
        String barcoEmInspecao,
        List<String> filaInspecao,
        QualidadeVisao qualidade,
        BigDecimal horasOciosas,
        long ultimoEventoId) {

    public record BarcoVisao(
            String id,
            Linha linha,
            String cliente,
            CodigoGate gateAtual,
            String nomeGate,
            StatusBarco status,
            boolean retido,
            int prioridade,
            BigDecimal hhTotalDoGate,
            BigDecimal hhRestantesNoGate,
            BigDecimal progressoGate,
            BigDecimal hhRetrabalhoPendente,
            BigDecimal hhProduzidas,
            BigDecimal hhRetrabalho,
            BigDecimal hhApontadas,
            List<CodigoGate> gatesAprovados,
            List<String> pessoasAlocadas,
            List<NcVisao> ncs) {}

    public record NcVisao(
            String id,
            CodigoGate gate,
            String tipo,
            String descricao,
            Gravidade gravidade,
            Origem origem,
            String fornecedorId,
            StatusNc status,
            DecisaoNc decisao,
            BigDecimal hhRetrabalho,
            BigDecimal custoMaterial) {}

    public record PessoaVisao(String id, String nome, Funcao funcao, int nivel, int moral, String alocadoEm) {}

    public record InspetorVisao(String id, String nome, int moral, int rigor, int velocidade, int paciencia) {}

    /** Indicadores que o jogador ve. As NCs escapadas so aparecem depois, como garantia. */
    public record QualidadeVisao(int ncsDetectadas, int ncsAbertas, int concessoes, int rncs,
                                 BigDecimal hhRetrabalho, BigDecimal custoMaterialRetrabalho) {}
}
