package br.marellar.web;

import br.marellar.engine.ResultadoAvanco;
import br.marellar.engine.Simulacao;
import br.marellar.engine.dados.ParametrosJogo;
import br.marellar.engine.ordens.OrdemInvalidaException;
import br.marellar.engine.save.Save;
import br.marellar.engine.save.SaveIncompativelException;
import br.marellar.web.json.CodecGerado;
import br.marellar.web.json.Json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A API (PartidaController + PartidaService) dentro do navegador: mesmas rotas, mesmos corpos e
 * mesmos erros (problem+json com "detail"). O cliente Godot so troca o HTTP por esta chamada.
 * Os saves ficam num {@link Armazenamento} (localStorage no navegador).
 */
public final class ServicoWeb {

    /** Limite por chamada: 1 mes de jogo (igual ao PartidaController). */
    static final int MAX_TICKS_POR_CHAMADA = 168;
    private static final String PREFIXO = "/api/v1/partidas";

    /** Onde os saves ficam guardados: chave = id da partida, valor = JSON do save. */
    public interface Armazenamento {
        String ler(String id);

        void gravar(String id, String conteudo);

        List<String> ids();
    }

    public record Resposta(int status, String corpo) {}

    private final ParametrosJogo parametros;
    private final Armazenamento saves;
    private final Supplier<String> novoId;
    private final Supplier<String> agora;
    private final Map<String, Simulacao> partidas = new HashMap<>();

    public ServicoWeb(ParametrosJogo parametros, Armazenamento saves, Supplier<String> novoId, Supplier<String> agora) {
        this.parametros = parametros;
        this.saves = saves;
        this.novoId = novoId;
        this.agora = agora;
    }

    public Resposta requisitar(String metodo, String caminhoCompleto, String corpo) {
        try {
            return rotear(metodo, caminhoCompleto, corpo == null || corpo.isBlank() ? null : Json.ler(corpo));
        } catch (NaoEncontrado e) {
            return erro(404, e.titulo, e.getMessage());
        } catch (SaveIncompativelException e) {
            return erro(409, "Save incompativel", e.getMessage());
        } catch (OrdemInvalidaException e) {
            return erro(422, "Ordem invalida", e.getMessage());
        } catch (RuntimeException e) {
            return erro(400, "Requisicao invalida", e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private Resposta rotear(String metodo, String caminhoCompleto, Object corpo) {
        String caminho = caminhoCompleto;
        String consulta = "";
        int q = caminho.indexOf('?');
        if (q >= 0) {
            consulta = caminho.substring(q + 1);
            caminho = caminho.substring(0, q);
        }
        if (!caminho.startsWith(PREFIXO)) {
            throw new NaoEncontrado("Rota nao encontrada", caminhoCompleto);
        }
        String[] partes = caminho.substring(PREFIXO.length()).split("/");
        // partes[0] e "" (o caminho comeca com "/") ou o caminho e so o prefixo
        List<String> p = new ArrayList<>();
        for (String s : partes) {
            if (!s.isEmpty()) {
                p.add(s);
            }
        }
        boolean post = "POST".equalsIgnoreCase(metodo);
        StringBuilder sb = new StringBuilder();

        if (p.isEmpty() && post) {
            Object semente = campo(corpo, "semente");
            if (semente == null) {
                throw new IllegalArgumentException("semente: nao pode ser nulo");
            }
            String id = novoId.get();
            partidas.put(id, Simulacao.novaPartida(Json.longo(semente), parametros));
            CodecGerado.escreverRespostas_CriarPartida(sb, new Respostas.CriarPartida(id));
            return new Resposta(201, sb.toString());
        }
        if (p.size() == 1 && p.get(0).equals("saves") && !post) {
            Json.lista(sb, listarSaves(), CodecGerado::escreverRespostas_ResumoSave);
            return new Resposta(200, sb.toString());
        }
        if (p.size() == 1 && p.get(0).equals("carregar") && post) {
            String id = Json.texto(campo(corpo, "id"));
            if (id == null) {
                throw new IllegalArgumentException("id: nao pode ser nulo");
            }
            carregar(id);
            CodecGerado.escreverRespostas_CriarPartida(sb, new Respostas.CriarPartida(id));
            return new Resposta(200, sb.toString());
        }
        if (p.isEmpty()) {
            throw new NaoEncontrado("Rota nao encontrada", caminhoCompleto);
        }
        Simulacao sim = buscar(p.get(0));
        String acao = p.size() > 1 ? p.get(1) : "";
        if (p.size() == 1 && !post) {
            CodecGerado.escreverEstadoPartida(sb, sim.estado());
        } else if (acao.equals("avancar") && post && p.size() == 2) {
            int ticks = Json.inteiro(campo(corpo, "ticks"));
            if (ticks < 0 || ticks > MAX_TICKS_POR_CHAMADA) {
                throw new IllegalArgumentException("ticks: deve estar entre 0 e " + MAX_TICKS_POR_CHAMADA);
            }
            ResultadoAvanco r = sim.avancar(ticks);
            CodecGerado.escreverRespostas_Avancar(sb, new Respostas.Avancar(r, sim.estado()));
        } else if (acao.equals("ordens") && post && p.size() == 2) {
            sim.aplicarOrdem(CodecGerado.lerOrdem(corpo));
            CodecGerado.escreverEstadoPartida(sb, sim.estado());
        } else if (acao.equals("barcos") && !post && p.size() == 3) {
            CodecGerado.escreverEstadoPartida_BarcoVisao(sb, sim.barco(p.get(2)));
        } else if (acao.equals("conselho") && !post && p.size() == 2) {
            CodecGerado.escreverRelatorioConselho(sb, sim.conselho()
                    .orElseThrow(() -> new OrdemInvalidaException("Ainda nao houve reuniao do conselho nesta partida")));
        } else if (acao.equals("salvar") && post && p.size() == 2) {
            CodecGerado.escreverRespostas_ResumoSave(sb, salvar(p.get(0), sim));
        } else if (acao.equals("eventos") && !post && p.size() == 2) {
            long desde = 0;
            for (String par : consulta.split("&")) {
                if (par.startsWith("desde=")) {
                    desde = Long.parseLong(par.substring("desde=".length()));
                }
            }
            Json.lista(sb, sim.eventosDesde(desde), CodecGerado::escreverEvento);
        } else {
            throw new NaoEncontrado("Rota nao encontrada", caminhoCompleto);
        }
        return new Resposta(200, sb.toString());
    }

    private static Object campo(Object corpo, String nome) {
        return corpo instanceof Map<?, ?> m ? m.get(nome) : null;
    }

    private Simulacao buscar(String id) {
        Simulacao sim = partidas.get(id);
        if (sim == null) {
            throw new NaoEncontrado("Partida nao encontrada", "Partida nao encontrada: " + id);
        }
        return sim;
    }

    // ----- saves -----

    private Respostas.ResumoSave salvar(String id, Simulacao sim) {
        Save save = sim.salvar();
        String salvoEm = agora.get();
        Map<String, Object> envelope = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        CodecGerado.escreverSave(sb, save);
        envelope.put("salvoEm", salvoEm);
        envelope.put("save", Json.ler(sb.toString()));
        StringBuilder conteudo = new StringBuilder();
        Json.arvore(conteudo, envelope);
        saves.gravar(id, conteudo.toString());
        return resumo(id, save, salvoEm);
    }

    private void carregar(String id) {
        String conteudo = saves.ler(id);
        if (conteudo == null) {
            throw new NaoEncontrado("Save nao encontrado", "Save nao encontrado: " + id);
        }
        Save save;
        try {
            save = CodecGerado.lerSave(Json.objeto(Json.ler(conteudo), "save").get("save"));
        } catch (RuntimeException e) {
            throw new SaveIncompativelException("Save ilegivel: " + e.getMessage(), e);
        }
        if (save == null) {
            throw new SaveIncompativelException("Save ilegivel: vazio");
        }
        partidas.put(id, Simulacao.carregar(save, parametros));
    }

    private List<Respostas.ResumoSave> listarSaves() {
        List<Respostas.ResumoSave> lista = new ArrayList<>();
        for (String id : saves.ids()) {
            try {
                Map<String, Object> envelope = Json.objeto(Json.ler(saves.ler(id)), "save");
                Save save = CodecGerado.lerSave(envelope.get("save"));
                lista.add(resumo(id, save, Json.texto(envelope.get("salvoEm"))));
            } catch (RuntimeException ignorado) {
                // save corrompido ou de outro formato: nao aparece na lista
            }
        }
        lista.sort(Comparator.comparing(Respostas.ResumoSave::salvoEm).reversed());
        return lista;
    }

    private static Respostas.ResumoSave resumo(String id, Save save, String salvoEm) {
        var v = save.verificacao();
        return new Respostas.ResumoSave(id, save.semente(), save.tick(), v == null ? null : v.data(),
                v == null ? 0 : v.hora(), salvoEm == null ? "" : salvoEm);
    }

    // ----- erros -----

    private static Resposta erro(int status, String titulo, String detalhe) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("type", "about:blank");
        corpo.put("title", titulo);
        corpo.put("status", BigDecimal.valueOf(status));
        corpo.put("detail", detalhe);
        StringBuilder sb = new StringBuilder();
        Json.arvore(sb, corpo);
        return new Resposta(status, sb.toString());
    }

    private static final class NaoEncontrado extends RuntimeException {
        private final String titulo;

        NaoEncontrado(String titulo, String detalhe) {
            super(detalhe);
            this.titulo = titulo;
        }
    }
}
