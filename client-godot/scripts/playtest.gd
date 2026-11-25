extends Node
## Log de playtest (autoload "Playtest"; sprint 2, etapa 5). Sempre ligado, só local: grava em
## user://playtest/<data>_<id>.jsonl uma linha por acontecimento e nunca envia nada pela rede.
## Sem dado pessoal: a sessão é um id aleatório. O testador devolve o arquivo com F8 (ou o botão do painel
## de gestão): no navegador o arquivo é baixado; no desktop a pasta abre.
## Não decide nenhuma regra: só observa o estado, as ordens e os eventos que a fábrica já recebe.

const PASTA := "user://playtest"
const ORDENS_SEM_DECISAO := ["MudarVelocidade"]   # mexer na velocidade não conta como decisão

var _caminho := ""
var _id := ""
var _inicio_ms := 0
var _cliques := 0
var _estado: Dictionary = {}
var _oportunidades := {}          # chave -> {cliques, t}: o que o jogador pode responder agora
var _primeira_decisao := -1.0     # segundos até a 1ª ordem que é decisão
var _cliques_por_acao: Array[int] = []
var _rodadas: Array[Dictionary] = []
var _fechamentos: Array[Dictionary] = []
var _continuou_depois_do_fechamento := 0
var _multas := 0
var _desistencias := 0
var _expiradas := 0
var _garantias := 0
var _tela := "fabrica"


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	_id = "%08x" % (randi() & 0xffffffff)
	_inicio_ms = Time.get_ticks_msec()
	DirAccess.make_dir_recursive_absolute(PASTA)
	var agora := Time.get_datetime_string_from_system().replace(":", "").replace("-", "").replace("T", "_")
	_caminho = "%s/%s_%s.jsonl" % [PASTA, agora, _id]
	registrar("sessao_inicio", {"plataforma": OS.get_name(), "versao": ProjectSettings.get_setting("application/config/version", "")})


func _notification(o: int) -> void:
	if o == NOTIFICATION_WM_CLOSE_REQUEST:
		registrar("sessao_fim", {"tela": _tela})


## Cada clique do mouse e cada confirmação (Enter, A do controle) conta como clique.
func _input(event: InputEvent) -> void:
	if (event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT) \
			or (event.is_action_pressed("ui_accept") and not event.is_echo()):
		_cliques += 1


func segundos() -> float:
	return (Time.get_ticks_msec() - _inicio_ms) / 1000.0


## Uma linha no arquivo: o que aconteceu, quando (tempo real e do jogo) e os detalhes.
func registrar(tipo: String, dados: Dictionary = {}) -> void:
	var linha := {"t": snappedf(segundos(), 0.1), "tipo": tipo, "sessao": _id, "cliques": _cliques}
	if not _estado.is_empty():
		linha["data"] = _estado.get("data")
		linha["hora"] = _estado.get("hora")
		linha["tick"] = _estado.get("tick")
	linha.merge(dados)
	var arquivo := FileAccess.open(_caminho, FileAccess.READ_WRITE if FileAccess.file_exists(_caminho) else FileAccess.WRITE)
	if arquivo == null:
		return
	arquivo.seek_end()
	arquivo.store_line(JSON.stringify(linha))
	arquivo.close()


# ---------------------------------------------------------------- o que a fábrica informa

## Estado novo: abre as oportunidades que apareceram (NC esperando decisão, proposta, rodada).
func observar(estado: Dictionary) -> void:
	_estado = estado
	var atuais := {}
	for b in estado.get("barcos", []):
		for nc in b["ncs"]:
			if nc["status"] == "ABERTA":
				atuais["NC:" + str(nc["id"])] = {"barco": b["id"], "gravidade": nc["gravidade"]}
	var vendas = estado.get("vendas")
	if vendas != null:
		for p in vendas["propostas"]:
			atuais["PROPOSTA:" + str(p["id"])] = {"cliente": p["cliente"]}
	var rodada = estado.get("rodada")
	if rodada != null:
		for r in rodada["abertas"]:
			atuais["RODADA:" + str(r["barcoId"])] = {"gate": r["gate"], "marcas": (r["marcas"] as Array).size()}
	for chave in atuais:
		if not _oportunidades.has(chave):
			_oportunidades[chave] = {"cliques": _cliques, "t": segundos()}
			registrar("oportunidade", {"chave": chave, "detalhe": atuais[chave]})
	for chave in _oportunidades.keys():
		if not atuais.has(chave):
			if str(chave).begins_with("RODADA:") and not _oportunidades[chave].get("respondida", false):
				registrar("rodada_com_inspetor", {"chave": chave})
			_oportunidades.erase(chave)


## Ordem enviada: tempo até a 1ª decisão e cliques desde a oportunidade que ela responde.
func ordem(dados: Dictionary) -> void:
	var tipo := str(dados.get("tipo", ""))
	if tipo in ORDENS_SEM_DECISAO:
		return
	if _primeira_decisao < 0:
		_primeira_decisao = segundos()
		registrar("primeira_decisao", {"segundos": snappedf(_primeira_decisao, 0.1), "ordem": tipo})
	var chave := ""
	match tipo:
		"DecidirNc":
			chave = "NC:" + str(dados.get("ncId"))
		"AceitarProposta", "RecusarProposta":
			chave = "PROPOSTA:" + str(dados.get("propostaId"))
		"InspecionarRodada":
			chave = "RODADA:" + str(dados.get("barcoId"))
	var cliques := -1
	if _oportunidades.has(chave):
		cliques = _cliques - int(_oportunidades[chave]["cliques"])
		_oportunidades[chave]["respondida"] = true
		_cliques_por_acao.append(cliques)
	registrar("ordem", {"ordem": dados, "oportunidade": chave, "cliques_desde_oportunidade": cliques})


## Eventos do motor que contam como perda (multa, desistência, proposta perdida, garantia).
func evento(e: Dictionary) -> void:
	match str(e.get("tipo")):
		"MULTA":
			_multas += 1
		"DESISTENCIA":
			_desistencias += 1
		"GARANTIA":
			_garantias += 1
		"PROPOSTA":
			if not str(e.get("mensagem", "")).contains("foi embora"):
				return   # proposta nova, recusada ou capacidade: não é perda
			_expiradas += 1
		_:
			return
	registrar("perda", {"evento": e.get("tipo"), "mensagem": e.get("mensagem"), "barco": e.get("barcoId")})


func rodada(r: Dictionary) -> void:
	var contagem := {"ACHOU": 0, "ESCAPOU": 0, "FALSO_ALARME": 0, "IGNOROU": 0}
	for m in r["marcas"]:
		contagem[str(m["resultado"])] += 1
	var defeitos: int = contagem["ACHOU"] + contagem["ESCAPOU"]
	var marcas: int = (r["marcas"] as Array).size()
	# acerto = marcas classificadas certo (defeito clicado ou marca falsa deixada) / todas as marcas
	var acerto := float(contagem["ACHOU"] + contagem["IGNOROU"]) / maxf(1.0, marcas)
	var dados := {"barco": r["barcoId"], "gate": r["gate"], "defeitos": defeitos, "acerto": snappedf(acerto, 0.01)}
	dados.merge(contagem)
	_rodadas.append(dados)
	registrar("rodada", dados)


func pausa(motivo: String, estado: Dictionary) -> void:
	_estado = estado
	registrar("pausa", {"motivo": motivo})
	if motivo == "FIM_DO_MES" and _estado.get("conselho") != null:
		var c: Dictionary = _estado["conselho"]
		var fechamento := {"mes": c["mesReferencia"], "mes_ruim": c["mesRuim"],
				"metas_cumpridas": (c["metas"] as Array).filter(func(m): return m["cumprida"]).size()}
		_fechamentos.append(fechamento)
		registrar("fechamento", fechamento)


## O jogador seguiu jogando depois do fechamento do mês (o "mais um mês" que o log consegue ver).
func continuou(motivo: String) -> void:
	if motivo == "FIM_DO_MES":
		_continuou_depois_do_fechamento += 1
		registrar("continuou_depois_do_fechamento", {"vezes": _continuou_depois_do_fechamento})


func tela(nome: String) -> void:
	if nome != _tela:
		_tela = nome
		registrar("tela", {"tela": nome})


# ---------------------------------------------------------------- resumo e exportação

func resumo() -> Dictionary:
	var media := -1.0
	if not _cliques_por_acao.is_empty():
		var soma := 0
		for c in _cliques_por_acao:
			soma += c
		media = float(soma) / _cliques_por_acao.size()
	return {
		"tempo_ate_primeira_decisao_s": snappedf(_primeira_decisao, 0.1),
		"cliques_medios_ate_acao": snappedf(media, 0.1),
		"acoes_com_ate_2_cliques": _cliques_por_acao.filter(func(c): return c <= 2).size(),
		"acoes": _cliques_por_acao.size(),
		"acerto_primeira_rodada": _rodadas[0]["acerto"] if not _rodadas.is_empty() else null,
		"rodadas": _rodadas.size(),
		"meses_fechados": _fechamentos.size(),
		"continuou_depois_do_fechamento": _continuou_depois_do_fechamento,
		"multas": _multas, "desistencias": _desistencias, "propostas_expiradas": _expiradas, "garantias": _garantias,
		"tempo_jogado_s": snappedf(segundos(), 0.1),
		"data_do_jogo": _estado.get("data"),
	}


## F8: no navegador baixa o arquivo; no desktop abre a pasta. O resumo vai na última linha.
func exportar() -> String:
	registrar("resumo", {"resumo": resumo()})
	var nome := _caminho.get_file()
	if OS.has_feature("web"):
		var arquivo := FileAccess.open(_caminho, FileAccess.READ)
		if arquivo == null:
			return ""
		JavaScriptBridge.download_buffer(arquivo.get_buffer(arquivo.get_length()), nome, "application/json")
		return nome
	OS.shell_open(ProjectSettings.globalize_path(PASTA))
	return ProjectSettings.globalize_path(_caminho)
