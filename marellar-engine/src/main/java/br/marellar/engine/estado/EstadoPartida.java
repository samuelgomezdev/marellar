package br.marellar.engine.estado;

import br.marellar.engine.dominio.CodigoGate;
import br.marellar.engine.dominio.DecisaoNc;
import br.marellar.engine.dominio.Funcao;
import br.marellar.engine.dominio.Gravidade;
import br.marellar.engine.dominio.Linha;
import br.marellar.engine.dominio.MotivoPausa;
import br.marellar.engine.dominio.Origem;
import br.marellar.engine.dominio.SituacaoPessoa;
import br.marellar.engine.dominio.StatusBarco;
import br.marellar.engine.dominio.StatusNc;
import br.marellar.engine.dominio.StatusRnc;
import br.marellar.engine.dominio.Treinamento;
import br.marellar.engine.dominio.Velocidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Retrato imutavel da partida num instante, como o jogador ve. E o que a CLI imprime,
 * a API devolve e o teste de determinismo compara (records tem equals por valor).
 * Nao aparecem aqui: NCs que escaparam do inspetor, perfil dos fornecedores e
 * a data real de chegada de pedidos atrasados.
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
        List<FornecedorVisao> fornecedores,
        Map<String, Integer> estoque,
        List<PedidoVisao> pedidosACaminho,
        List<RncVisao> rncs,
        SegurancaVisao seguranca,
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
            BigDecimal hhQuebraPendente,
            BigDecimal hhProduzidas,
            BigDecimal hhRetrabalho,
            BigDecimal hhApontadas,
            long horasParadoMaterial,
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

    public record PessoaVisao(
            String id,
            String nome,
            Funcao funcao,
            int nivel,
            int moral,
            SituacaoPessoa situacao,
            int diasRestantesSituacao,
            Treinamento treinamento,
            boolean treinoSegurancaEmDia,
            boolean contratadaNaPartida,
            boolean podeFazerUmAUm,
            String alocadoEm) {}

    public record InspetorVisao(String id, String nome, int moral, int rigor, int velocidade, int paciencia) {}

    /** Indicadores que o jogador ve. As NCs escapadas so aparecem depois, como garantia. */
    public record QualidadeVisao(int ncsDetectadas, int ncsAbertas, int concessoes, int rncs,
                                 BigDecimal hhRetrabalho, BigDecimal custoMaterialRetrabalho) {}

    /** Scorecard visivel do fornecedor (sem o perfil escondido). */
    public record FornecedorVisao(
            String id,
            String nome,
            List<String> itens,
            String nota,
            int pontos,
            long ppm,
            int pedidosEntregues,
            int pedidosNoPrazo,
            int oitoDsRespondidos,
            int oitoDsNoPrazo,
            BigDecimal creditoChargeback,
            BigDecimal totalChargeback,
            int rncsAbertas) {}

    /** Pedido a caminho, com a data prometida pelo fornecedor (o atraso so se descobre na hora). */
    public record PedidoVisao(String id, String fornecedor, String item, int kits, LocalDate dataPrevista,
                              BigDecimal valorPago) {}

    public record RncVisao(String id, String ncId, String barcoId, String fornecedorId, Gravidade gravidade,
                           BigDecimal custoDocumentado, BigDecimal chargeback, LocalDate prazo8d, StatusRnc status,
                           BigDecimal chargebackHorasParadas) {}

    /** Indicadores de seguranca (NBR 14280) e de rotatividade. */
    public record SegurancaVisao(int acidentes, int diasPerdidos, BigDecimal horasTrabalhadas,
                                 BigDecimal taxaFrequencia, BigDecimal taxaGravidade, int demissoes,
                                 BigDecimal horasLiderUmAUm) {}
}
