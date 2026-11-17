package br.marellar.api.inspecao;

import br.marellar.api.inspecao.CatalogoInspecao.Defeito;
import br.marellar.engine.dados.CarregadorDados;
import br.marellar.engine.dados.CatalogoNc;
import br.marellar.engine.dominio.Gravidade;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NCs da inspecao 3D. Ficam em memoria e num arquivo JSON ({@code inspecao/ncs.json} por padrao),
 * gravado a cada mudanca por troca de arquivo temporario, como os saves.
 *
 * <p>O defeito vem do catalogo ampliado ({@link CatalogoInspecao}); o servidor preenche descricao, categoria e o
 * defeito-base do motor. Regra de gravidade: o inspetor pode subir a gravidade do catalogo (uma trinca leve que ele
 * julga media), mas nao pode baixar. Assim um defeito que o catalogo classifica como critico nunca entra como leve.</p>
 *
 * <p>Migracao: NCs gravadas antes dos campos proprios levavam o defeito detalhado na observacao
 * ({@code [TEC-INFILTRA] ...}). Ao carregar, o codigo sai da observacao e vai para {@code codigoDefeito};
 * a gravidade gravada nao muda.</p>
 */
@Service
public class InspecaoService {

    static final Set<Integer> LINHAS = Set.of(33, 47, 59);

    /** Defeito detalhado no comeco da observacao (ponte usada antes dos campos proprios). */
    private static final Pattern PREFIXO_ANTIGO = Pattern.compile("^\\[([A-Z0-9-]+)]\\s?(.*)$", Pattern.DOTALL);

    private final CatalogoInspecao catalogo = CatalogoInspecao.carregarDoClasspath();
    private final ObjectMapper json;
    private final Path arquivo;
    private final Clock relogio;
    private final List<NcInspecao> ncs = new ArrayList<>();

    @Autowired
    public InspecaoService(ObjectMapper json,
                           @Value("${marellar.inspecao.arquivo:inspecao/ncs.json}") String arquivo) {
        this(json, Path.of(arquivo), Clock.systemUTC());
    }

    InspecaoService(ObjectMapper json, Path arquivo, Clock relogio) {
        conferirBases(CarregadorDados.carregarDoClasspath().catalogoNc());
        this.json = json;
        this.arquivo = arquivo;
        this.relogio = relogio;
        carregar();
    }

    public Collection<Defeito> defeitos() {
        return catalogo.defeitos();
    }

    /** Todo defeito-base do catalogo ampliado tem de existir no catalogo do motor. */
    private void conferirBases(CatalogoNc motor) {
        Set<String> codigosMotor = new HashSet<>();
        motor.tipos().forEach(t -> codigosMotor.add(t.codigo()));
        for (Defeito d : catalogo.defeitos()) {
            if (!codigosMotor.contains(d.base())) {
                throw new IllegalStateException("Defeito " + d.codigo() + " aponta para um defeito-base fora do motor: " + d.base());
            }
        }
    }

    public synchronized List<NcInspecao> listar(Integer linha) {
        return ncs.stream()
                .filter(n -> linha == null || n.linha() == linha)
                .sorted(Comparator.comparingInt(NcInspecao::linha).thenComparingInt(NcInspecao::numero))
                .toList();
    }

    public synchronized NcInspecao registrar(NovaNcRequest req) {
        if (!LINHAS.contains(req.linha())) {
            throw new NcInvalidaException("Linha desconhecida: " + req.linha() + " (use 33, 47 ou 59)");
        }
        Defeito tipo = defeitoValido(req.codigoDefeito(), req.gravidade());
        String compartimento = compartimentoValido(req.linha(), req.compartimento());
        int numero = ncs.stream().filter(n -> n.linha() == req.linha()).mapToInt(NcInspecao::numero).max().orElse(0) + 1;
        NcInspecao nc = new NcInspecao(UUID.randomUUID().toString(), numero, req.linha(), req.peca(), req.posicao(),
                req.baliza(), req.bordo(), req.zona(), compartimento, req.componente(), req.gate(),
                tipo.codigo(), tipo.base(), tipo.categoria(), tipo.descricao(), req.gravidade(),
                req.observacao() == null ? "" : req.observacao().strip(), NcInspecao.Status.ABERTA,
                Instant.now(relogio), null, null);
        ncs.add(nc);
        gravar();
        return nc;
    }

    /** Edita uma NC aberta. Linha, numero e data de registro nao mudam. */
    public synchronized NcInspecao editar(String id, EditarNcRequest req) {
        NcInspecao atual = buscar(id);
        if (atual.status() == NcInspecao.Status.FECHADA) {
            throw new NcInvalidaException("A NC " + atual.numero() + " esta fechada e nao pode ser editada");
        }
        Defeito tipo = defeitoValido(req.codigoDefeito(), req.gravidade());
        String compartimento = compartimentoValido(atual.linha(), req.compartimento());
        NcInspecao editada = new NcInspecao(atual.id(), atual.numero(), atual.linha(), req.peca(), req.posicao(),
                req.baliza(), req.bordo(), req.zona(), compartimento, req.componente(), req.gate(),
                tipo.codigo(), tipo.base(), tipo.categoria(), tipo.descricao(), req.gravidade(), req.observacao() == null ? "" : req.observacao().strip(), NcInspecao.Status.ABERTA,
                atual.registradaEm(), Instant.now(relogio), null);
        ncs.set(ncs.indexOf(atual), editada);
        gravar();
        return editada;
    }

    /** Defeito tem de existir no catalogo; a gravidade pode subir, mas nao pode ficar abaixo da do catalogo. */
    private Defeito defeitoValido(String codigo, Gravidade gravidade) {
        Defeito tipo = catalogo.defeito(codigo)
                .orElseThrow(() -> new NcInvalidaException("Defeito fora do catalogo da inspecao: " + codigo));
        if (gravidade.compareTo(tipo.gravidade()) < 0) {
            throw new NcInvalidaException("O catalogo classifica " + tipo.codigo() + " como " + tipo.gravidade()
                    + "; a gravidade pode subir, mas nao pode ficar abaixo disso");
        }
        return tipo;
    }

    /** Compartimento e opcional; quando vem, tem de existir nos itens da linha (data/estrutura-produto.json). */
    private String compartimentoValido(int linha, String compartimento) {
        if (compartimento == null || compartimento.isBlank()) {
            return null;
        }
        String codigo = compartimento.strip();
        if (!catalogo.compartimentos(linha).contains(codigo)) {
            throw new NcInvalidaException("Compartimento " + codigo + " nao existe na linha " + linha);
        }
        return codigo;
    }

    public synchronized NcInspecao fechar(String id) {
        NcInspecao atual = buscar(id);
        if (atual.status() == NcInspecao.Status.FECHADA) {
            return atual;
        }
        NcInspecao fechada = atual.fechar(Instant.now(relogio));
        ncs.set(ncs.indexOf(atual), fechada);
        gravar();
        return fechada;
    }

    public synchronized void excluir(String id) {
        ncs.remove(buscar(id));
        gravar();
    }

    private NcInspecao buscar(String id) {
        return ncs.stream().filter(n -> n.id().equals(id)).findFirst()
                .orElseThrow(() -> new NcNaoEncontradaException(id));
    }

    // ----- arquivo -----

    private void carregar() {
        if (!Files.isRegularFile(arquivo)) {
            return;
        }
        List<NcInspecao> lidas;
        try {
            lidas = json.readValue(arquivo.toFile(), new TypeReference<List<NcInspecao>>() {});
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel ler " + arquivo, e);
        }
        boolean migrou = false;
        for (NcInspecao lida : lidas) {
            NcInspecao nc = migrar(lida);
            migrou |= !nc.equals(lida);
            ncs.add(nc);
        }
        if (migrou) {
            gravar();
        }
    }

    /**
     * NC antiga: tira o {@code [CODIGO]} do comeco da observacao e preenche codigoDefeito, codigoBase e categoria
     * pelo catalogo ampliado. Codigo que nao esta no catalogo fica como esta.
     */
    NcInspecao migrar(NcInspecao nc) {
        String codigo = nc.codigoDefeito();
        String observacao = nc.observacao() == null ? "" : nc.observacao();
        Matcher m = PREFIXO_ANTIGO.matcher(observacao);
        if (m.matches() && catalogo.defeito(m.group(1)).isPresent()) {
            codigo = m.group(1);
            observacao = m.group(2).strip();
        }
        Defeito d = catalogo.defeito(codigo).orElse(null);
        if (d == null) {
            return nc;
        }
        boolean emDia = d.codigo().equals(nc.codigoDefeito()) && d.base().equals(nc.codigoBase())
                && d.categoria().equals(nc.categoria()) && observacao.equals(nc.observacao());
        return emDia ? nc : nc.comDefeito(d, observacao);
    }

    private void gravar() {
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            Files.createDirectories(pasta);
            Path temporario = Files.createTempFile(pasta, "ncs", ".tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporario.toFile(), ncs);
            Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Nao foi possivel gravar " + arquivo, e);
        }
    }

    public static class NcNaoEncontradaException extends RuntimeException {
        public NcNaoEncontradaException(String id) {
            super("NC nao encontrada: " + id);
        }
    }

    public static class NcInvalidaException extends RuntimeException {
        public NcInvalidaException(String mensagem) {
            super(mensagem);
        }
    }
}
