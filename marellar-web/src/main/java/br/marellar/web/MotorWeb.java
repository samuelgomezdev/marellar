package br.marellar.web;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ponto de entrada do motor no navegador (compilado para JavaScript pelo TeaVM).
 * Registra duas funcoes globais que o cliente Godot chama pelo JavaScriptBridge:
 * <ul>
 *   <li>{@code marellarIniciar(arquivos)}: recebe os JSON de data/ ({nome: conteudo}) e devolve "" ou o erro;</li>
 *   <li>{@code marellarRequisitar(metodo, caminho, corpo)}: a mesma API REST, devolvendo
 *       {@code {"status": 200, "corpo": "..."}} em texto.</li>
 * </ul>
 * Os saves ficam no localStorage do navegador.
 */
public final class MotorWeb {

    private static final String PREFIXO_SAVE = "marellar.save.";

    private static ServicoWeb servico;

    private MotorWeb() {
    }

    @JSFunctor
    public interface Iniciar extends JSObject {
        String iniciar(JSObject arquivos);
    }

    @JSFunctor
    public interface Requisitar extends JSObject {
        String requisitar(String metodo, String caminho, String corpo);
    }

    public static void main(String[] args) {
        registrar(MotorWeb::iniciar, MotorWeb::requisitar);
    }

    private static String iniciar(JSObject arquivos) {
        try {
            Map<String, String> conteudos = new HashMap<>();
            for (String nome : DadosWeb.ARQUIVOS) {
                String c = lerCampo(arquivos, nome);
                if (c != null) {
                    conteudos.put(nome, c);
                }
            }
            servico = new ServicoWeb(DadosWeb.carregar(conteudos), new Local(), MotorWeb::uuid, MotorWeb::agoraIso);
            return "";
        } catch (RuntimeException e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        }
    }

    private static String requisitar(String metodo, String caminho, String corpo) {
        ServicoWeb.Resposta r;
        if (servico == null) {
            r = new ServicoWeb.Resposta(503, "{\"detail\":\"Motor ainda nao iniciado\"}");
        } else {
            r = servico.requisitar(metodo, caminho, corpo);
        }
        StringBuilder sb = new StringBuilder("{\"status\":").append(r.status()).append(",\"corpo\":");
        br.marellar.web.json.Json.texto(sb, r.corpo());
        return sb.append('}').toString();
    }

    /** Saves no localStorage, cada um em "marellar.save.{id}". */
    private static final class Local implements ServicoWeb.Armazenamento {
        @Override
        public String ler(String id) {
            return localLer(PREFIXO_SAVE + id);
        }

        @Override
        public void gravar(String id, String conteudo) {
            String erro = localGravar(PREFIXO_SAVE + id, conteudo);
            if (erro != null) {
                throw new IllegalStateException("Nao foi possivel gravar o save no navegador: " + erro);
            }
        }

        @Override
        public List<String> ids() {
            List<String> ids = new ArrayList<>();
            String todas = localChaves();
            if (todas == null || todas.isEmpty()) {
                return ids;
            }
            for (String chave : todas.split("\n")) {
                if (chave.startsWith(PREFIXO_SAVE)) {
                    ids.add(chave.substring(PREFIXO_SAVE.length()));
                }
            }
            return ids;
        }
    }

    @JSBody(params = {"iniciar", "requisitar"}, script =
            "window.marellarIniciar = iniciar; window.marellarRequisitar = requisitar;"
            + " if (typeof window.marellarPronto === 'function') { window.marellarPronto(); }")
    private static native void registrar(Iniciar iniciar, Requisitar requisitar);

    @JSBody(params = {"o", "k"}, script = "var v = o[k]; return (typeof v === 'string') ? v : null;")
    private static native String lerCampo(JSObject o, String k);

    @JSBody(params = {"k"}, script = "try { return window.localStorage.getItem(k); } catch (e) { return null; }")
    private static native String localLer(String k);

    @JSBody(params = {"k", "v"}, script =
            "try { window.localStorage.setItem(k, v); return null; } catch (e) { return String(e); }")
    private static native String localGravar(String k, String v);

    @JSBody(script = "try { var s = window.localStorage, l = [];"
            + " for (var i = 0; i < s.length; i++) { l.push(s.key(i)); } return l.join('\\n'); }"
            + " catch (e) { return ''; }")
    private static native String localChaves();

    @JSBody(script = "if (window.crypto && window.crypto.randomUUID) { return window.crypto.randomUUID(); }"
            + " return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {"
            + " var r = Math.random() * 16 | 0; return (c === 'x' ? r : (r & 3 | 8)).toString(16); });")
    private static native String uuid();

    @JSBody(script = "return new Date().toISOString();")
    private static native String agoraIso();
}
