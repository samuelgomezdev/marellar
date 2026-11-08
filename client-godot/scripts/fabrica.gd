extends Control
## Tela da Fábrica (MVP): baias com os barcos de 33 pés, bonequinhos trabalhando,
## barra de topo, painel à direita (ficha do barco, equipe ou fornecedores), avisos e barra de ordens.
## Arte provisória desenhada em código (scripts/sprites.gd) na paleta Endesga 32.
## Nenhuma regra de jogo aqui: tudo vem do motor Java pela API.

const SEMENTE := 42
const SEGUNDOS_POR_TICK_X1 := 2.5  # 1 dia útil (8 ticks) ≈ 20 s na velocidade 1 (GDD, Loops de jogo)
const ALTURA_TOPO := 16
const ALTURA_BASE := 24
const LARGURA_PAINEL := 200
const TAMANHO_TEXTO := 16   # VT323 fica nítida em 16
const TAMANHO_TITULO := 8   # Press Start 2P fica nítida em 8
const MAX_AVISOS := 6

const FONTE_TEXTO := preload("res://assets/fonts/VT323-Regular.ttf")
const FONTE_TITULO := preload("res://assets/fonts/PressStart2P-Regular.ttf")

const NOMES_STATUS := {
	"EM_PRODUCAO": "Em produção",
	"AGUARDANDO_INSPECAO": "Na fila da inspeção",
	"EM_INSPECAO": "Em inspeção",
	"AGUARDANDO_DECISAO": "NC: decida!",
	"EM_RETRABALHO": "Em retrabalho",
	"APROVADO": "Aprovado, segurado",
	"PARADO_MATERIAL": "Parado: sem material",
	"ENTREGUE": "Entregue",
}
const NOMES_GRAVIDADE := {"LEVE": "leve", "MEDIA": "média", "CRITICA": "CRÍTICA"}
const NOMES_SITUACAO := {
	"ATIVA": "", "A_CAMINHO": "chega em %d d", "EM_TREINAMENTO": "treino %d d",
	"AFASTADA": "afastado %d d", "DESLIGADA": "saiu",
}
const PAINEL_BARCO := "barco"
const PAINEL_EQUIPE := "equipe"
const PAINEL_FORNECEDORES := "fornecedores"
const NOMES_STATUS_NC := {
	"ABERTA": "aberta", "EM_RETRABALHO": "em retrabalho", "RESOLVIDA": "resolvida",
	"CONCEDIDA": "concessão",
}

var estado: Dictionary = {}
var velocidade := 0
var acumulado := 0.0
var ocupado := false
var ultimo_evento := 0
var avisos: Array[String] = []
var erro_api := ""
var selecionado := ""
var areas_barcos := {}  # id do barco -> Rect2 clicável
var assinatura_ficha := ""
var painel := PAINEL_BARCO

var fonte: Font
var fonte_titulo: Font
var botoes_velocidade := {}
var rotulo_selecao: Label
var botao_prioridade: Button
var botao_segurar: Button
var ficha: VBoxContainer
var rolagem: ScrollContainer
var botoes_painel := {}


func _ready() -> void:
	fonte = _fonte_pixel(FONTE_TEXTO)
	fonte_titulo = _fonte_pixel(FONTE_TITULO)
	theme = Theme.new()
	theme.default_font = fonte
	theme.default_font_size = TAMANHO_TEXTO
	Api.erro.connect(_on_erro_api)
	_montar_barra_inferior()
	_montar_ficha()
	_atualizar_interface()
	if await Api.criar_partida(SEMENTE):
		await _atualizar_estado()
		_avisar("Partida criada. Espaço pausa; 1, 2 e 3 mudam a velocidade. Clique num barco.")
		_opcoes_de_desenvolvimento(OS.get_cmdline_user_args())


## Opções para testes e capturas de tela, passadas depois de "--":
##   godot --path client-godot -- --dias 12 --selecionar 33-002 --rodar
func _opcoes_de_desenvolvimento(args: PackedStringArray) -> void:
	var i := args.find("--dias")
	if i >= 0 and i + 1 < args.size():
		var faltam := int(args[i + 1]) * 8
		while faltam > 0:
			var resposta = await Api.avancar(mini(faltam, 168))
			if resposta == null:
				return
			estado = resposta["estado"]
			faltam -= int(resposta["resultado"]["ticksExecutados"])
		await _buscar_eventos()
	i = args.find("--selecionar")
	if i >= 0 and i + 1 < args.size():
		selecionado = args[i + 1]
	i = args.find("--painel")
	if i >= 0 and i + 1 < args.size():
		painel = args[i + 1]
	_atualizar_interface()
	if args.has("--rodar"):
		_mudar_velocidade(4)


func _process(delta: float) -> void:
	queue_redraw()  # bonequinhos animados
	if velocidade == 0 or ocupado or Api.partida_id.is_empty():
		return
	acumulado += delta
	if acumulado >= SEGUNDOS_POR_TICK_X1 / velocidade:
		acumulado = 0.0
		_avancar(1)


static func _fonte_pixel(origem: FontFile) -> FontFile:
	var f := origem.duplicate() as FontFile
	f.antialiasing = TextServer.FONT_ANTIALIASING_NONE
	f.hinting = TextServer.HINTING_NONE
	f.subpixel_positioning = TextServer.SUBPIXEL_POSITIONING_DISABLED
	return f


# ---------------------------------------------------------------- ordens e API

func _avancar(ticks: int) -> void:
	ocupado = true
	var resposta = await Api.avancar(ticks)
	ocupado = false
	if resposta == null:
		_mudar_velocidade(0)
		return
	estado = resposta["estado"]
	await _buscar_eventos()
	if resposta["resultado"]["pausado"]:
		_mudar_velocidade(0)
		_avisar("PAUSA: %s" % _motivo_pausa(str(resposta["resultado"]["motivoPausa"])))
	_atualizar_interface()


func _atualizar_estado() -> void:
	var novo = await Api.estado()
	if novo != null:
		estado = novo
		_atualizar_interface()


func _buscar_eventos() -> void:
	var lista = await Api.eventos(ultimo_evento)
	if lista == null:
		return
	for evento in lista:
		ultimo_evento = maxi(ultimo_evento, int(evento["id"]))
		_avisar(str(evento["mensagem"]))


func _enviar_ordem(dados: Dictionary, aviso: String) -> void:
	var novo = await Api.ordem(dados)
	if novo != null:
		estado = novo
		erro_api = ""
		if not aviso.is_empty():
			_avisar(aviso)
		await _buscar_eventos()
		_atualizar_interface()


func _mudar_velocidade(nova: int) -> void:
	velocidade = nova
	acumulado = 0.0
	for chave in botoes_velocidade:
		botoes_velocidade[chave].set_pressed_no_signal(chave == nova)
	if not Api.partida_id.is_empty():
		Api.ordem({"tipo": "MudarVelocidade", "velocidade": "PAUSA" if nova == 0 else "X%d" % nova})


func _priorizar() -> void:
	if not selecionado.is_empty():
		_enviar_ordem({"tipo": "DefinirPrioridade", "barcoId": selecionado, "posicao": 1},
				"%s agora é a prioridade 1." % selecionado)


func _alternar_segurar() -> void:
	var barco := _barco(selecionado)
	if barco.is_empty():
		return
	var liberar: bool = barco["retido"]
	_enviar_ordem({"tipo": "LiberarGate" if liberar else "SegurarGate", "barcoId": selecionado},
			"%s %s." % [selecionado, "liberado" if liberar else "segurado no gate"])


func _decidir_nc(nc_id: String, decisao: String) -> void:
	_enviar_ordem({"tipo": "DecidirNc", "ncId": nc_id, "decisao": decisao}, "")


func _on_erro_api(mensagem: String) -> void:
	erro_api = mensagem
	push_warning(mensagem)


# ---------------------------------------------------------------- entrada

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventKey and event.pressed and not event.echo:
		match event.keycode:
			KEY_SPACE:
				_mudar_velocidade(0 if velocidade != 0 else 1)
			KEY_1:
				_mudar_velocidade(1)
			KEY_2:
				_mudar_velocidade(2)
			KEY_3:
				_mudar_velocidade(4)
			KEY_F1:
				_abrir_painel(PAINEL_EQUIPE)
			KEY_F2:
				_abrir_painel(PAINEL_FORNECEDORES)
			KEY_ESCAPE:
				selecionado = ""
				painel = PAINEL_BARCO
				_atualizar_interface()


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		for id in areas_barcos:
			if areas_barcos[id].has_point(event.position):
				selecionado = id
				painel = PAINEL_BARCO
				_atualizar_interface()
				return


# ---------------------------------------------------------------- interface (Controls)

func _montar_barra_inferior() -> void:
	var barra := HBoxContainer.new()
	barra.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE)
	barra.offset_top = -ALTURA_BASE + 3
	barra.offset_bottom = -3
	barra.offset_left = 4
	barra.offset_right = -4
	barra.add_theme_constant_override("separation", 3)
	add_child(barra)

	for opcao in [[0, "Pausa"], [1, "x1"], [2, "x2"], [4, "x4"]]:
		var botao := _botao(opcao[1], _mudar_velocidade.bind(opcao[0]))
		botao.toggle_mode = true
		barra.add_child(botao)
		botoes_velocidade[opcao[0]] = botao
	botoes_velocidade[0].set_pressed_no_signal(true)

	barra.add_child(VSeparator.new())
	rotulo_selecao = Label.new()
	rotulo_selecao.add_theme_color_override("font_color", Paleta.TEXTO)
	barra.add_child(rotulo_selecao)
	botao_prioridade = _botao("Prioridade 1", _priorizar)
	barra.add_child(botao_prioridade)
	botao_segurar = _botao("Segurar gate", _alternar_segurar)
	barra.add_child(botao_segurar)
	barra.add_child(VSeparator.new())
	for opcao in [[PAINEL_EQUIPE, "Equipe F1"], [PAINEL_FORNECEDORES, "Fornec. F2"]]:
		var b := _botao(opcao[1], _abrir_painel.bind(opcao[0]))
		b.toggle_mode = true
		barra.add_child(b)
		botoes_painel[opcao[0]] = b


func _montar_ficha() -> void:
	rolagem = ScrollContainer.new()
	rolagem.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(rolagem)
	ficha = VBoxContainer.new()
	ficha.add_theme_constant_override("separation", 1)
	ficha.custom_minimum_size.x = LARGURA_PAINEL - 14
	rolagem.add_child(ficha)


func _abrir_painel(qual: String) -> void:
	painel = PAINEL_BARCO if painel == qual else qual
	_atualizar_interface()


func _botao(texto: String, acao: Callable) -> Button:
	var b := Button.new()
	b.text = texto
	b.focus_mode = Control.FOCUS_NONE
	b.pressed.connect(acao)
	return b


func _atualizar_interface() -> void:
	var barco := _barco(selecionado)
	var ativo: bool = not barco.is_empty() and barco["status"] != "ENTREGUE"
	rotulo_selecao.text = "Clique num barco" if barco.is_empty() else "Barco %s" % selecionado
	botao_prioridade.disabled = not ativo
	botao_segurar.disabled = not ativo
	botao_segurar.text = "Liberar gate" if ativo and barco["retido"] else "Segurar gate"
	for chave in botoes_painel:
		botoes_painel[chave].set_pressed_no_signal(painel == chave)
	_posicionar_rolagem()
	match painel:
		PAINEL_EQUIPE:
			_reconstruir_equipe()
		PAINEL_FORNECEDORES:
			_reconstruir_fornecedores()
		_:
			_reconstruir_ficha(barco)


## Na ficha do barco o painel divide espaço com os avisos; equipe e fornecedores usam a altura toda.
func _posicionar_rolagem() -> void:
	var topo := ALTURA_TOPO + 22
	var altura := size.y - ALTURA_BASE - 6 - topo
	if painel == PAINEL_BARCO:
		altura = altura * 0.5 - 14
	rolagem.position = Vector2(size.x - LARGURA_PAINEL, topo)
	rolagem.size = Vector2(LARGURA_PAINEL - 4, altura)


func _limpar_ficha(assinatura: String) -> bool:
	if assinatura == assinatura_ficha:
		return false
	assinatura_ficha = assinatura
	for filho in ficha.get_children():
		filho.queue_free()
	return true


func _rotulo(texto: String, cor: Color) -> Label:
	var r := Label.new()
	r.text = texto
	r.autowrap_mode = TextServer.AUTOWRAP_WORD
	r.custom_minimum_size.x = LARGURA_PAINEL - 16
	r.add_theme_color_override("font_color", cor)
	return r


func _reconstruir_equipe() -> void:
	if estado.is_empty() or not _limpar_ficha(JSON.stringify(["equipe", estado["pessoas"]])):
		return
	ficha.add_child(_botao("Contratar (chega em 10 dias)", _enviar_ordem.bind({"tipo": "Contratar", "funcao": "PRODUCAO"}, "")))
	for p in estado["pessoas"]:
		if p["funcao"] != "PRODUCAO" or p["situacao"] == "DESLIGADA":
			continue
		var situacao := ""
		if p["situacao"] != "ATIVA":
			situacao = " " + NOMES_SITUACAO[p["situacao"]] % int(p["diasRestantesSituacao"]) if "%d" in NOMES_SITUACAO[p["situacao"]] else NOMES_SITUACAO[p["situacao"]]
		var seguranca := "" if p["treinoSegurancaEmDia"] else " NR!"
		var cor := Paleta.CRITICO if int(p["moral"]) < 40 else (Paleta.TEXTO if p["situacao"] == "ATIVA" else Paleta.PISO_ESCURO)
		ficha.add_child(_rotulo("%s N%d moral %d%s%s" % [p["nome"], int(p["nivel"]), int(p["moral"]), seguranca, situacao], cor))
		if p["situacao"] == "A_CAMINHO":
			continue
		var acoes := HBoxContainer.new()
		acoes.add_theme_constant_override("separation", 2)
		var um := _botao("1:1", _enviar_ordem.bind({"tipo": "FazerUmAUm", "pessoaId": p["id"]}, ""))
		um.disabled = not p["podeFazerUmAUm"]
		acoes.add_child(um)
		var disponivel: bool = p["situacao"] == "ATIVA"
		var tecnico := _botao("Treino téc.", _enviar_ordem.bind({"tipo": "Treinar", "pessoaId": p["id"], "treinamento": "TECNICO"}, ""))
		tecnico.disabled = not disponivel or int(p["nivel"]) >= 5
		acoes.add_child(tecnico)
		var nr := _botao("NR", _enviar_ordem.bind({"tipo": "Treinar", "pessoaId": p["id"], "treinamento": "SEGURANCA"}, ""))
		nr.disabled = not disponivel
		nr.tooltip_text = "Treinamento de segurança: põe o treino em dia e reduz a chance de acidente."
		acoes.add_child(nr)
		ficha.add_child(acoes)


func _reconstruir_fornecedores() -> void:
	if estado.is_empty() or not _limpar_ficha(JSON.stringify(["fornec", estado["fornecedores"], estado["estoque"], estado["pedidosACaminho"]])):
		return
	for f in estado["fornecedores"]:
		var cor := Paleta.MONTAGEM_LIMPA if f["nota"] in ["A", "B"] else (Paleta.ALERTA if f["nota"] in ["C", "D", "-"] else Paleta.CRITICO)
		ficha.add_child(_rotulo("%s  nota %s" % [f["nome"], f["nota"]], cor))
		ficha.add_child(_rotulo("  PPM %d | prazo %d/%d | 8D %d/%d" % [int(f["ppm"]), int(f["pedidosNoPrazo"]),
				int(f["pedidosEntregues"]), int(f["oitoDsNoPrazo"]), int(f["oitoDsRespondidos"])], Paleta.TEXTO))
		if float(f["totalChargeback"]) > 0:
			ficha.add_child(_rotulo("  chargeback R$ %s | RNC abertas %d" % [str(f["totalChargeback"]), int(f["rncsAbertas"])], Paleta.ALERTA))
	var itens: Array = estado["estoque"].keys()
	itens.sort()
	var linha_estoque := "Estoque (kits):"
	for item in itens:
		linha_estoque += " %s %d" % [item, int(estado["estoque"][item])]
	ficha.add_child(_rotulo(linha_estoque, Paleta.TEXTO_CLARO))
	for pedido in estado["pedidosACaminho"]:
		var partes := str(pedido["dataPrevista"]).split("-")
		ficha.add_child(_rotulo("  a caminho: %d %s, prev. %s/%s" % [int(pedido["kits"]), pedido["item"], partes[2], partes[1]], Paleta.PISO_ESCURO))


## Refaz os Controls da ficha só quando algo nela mudou (evita piscar a cada tick).
func _reconstruir_ficha(barco: Dictionary) -> void:
	var assinatura := "barco" if barco.is_empty() else JSON.stringify([barco["id"], barco["status"], barco["ncs"]])
	if not _limpar_ficha(assinatura) or barco.is_empty():
		return

	for nc in barco["ncs"]:
		var aberta: bool = nc["status"] == "ABERTA"
		var linha := Label.new()
		linha.text = "%s %s: %s" % [nc["gate"], NOMES_GRAVIDADE.get(nc["gravidade"], nc["gravidade"]), nc["descricao"]]
		linha.autowrap_mode = TextServer.AUTOWRAP_WORD
		linha.custom_minimum_size.x = LARGURA_PAINEL - 8
		linha.add_theme_color_override("font_color", Paleta.CRITICO if aberta else Paleta.PISO_ESCURO)
		ficha.add_child(linha)
		if not aberta:
			var situacao := Label.new()
			situacao.text = "  %s (%s HH)" % [NOMES_STATUS_NC.get(nc["status"], nc["status"]), str(int(nc["hhRetrabalho"]))]
			situacao.add_theme_color_override("font_color", Paleta.PISO_ESCURO)
			ficha.add_child(situacao)
			continue
		var acoes := HBoxContainer.new()
		acoes.add_theme_constant_override("separation", 2)
		acoes.add_child(_botao("Retrabalho", _decidir_nc.bind(nc["id"], "RETRABALHO")))
		var concessao := _botao("Concessão", _decidir_nc.bind(nc["id"], "CONCESSAO"))
		concessao.disabled = nc["gravidade"] == "CRITICA"
		concessao.tooltip_text = "NC crítica não aceita concessão." if concessao.disabled else "Libera sem consertar: risco de garantia."
		acoes.add_child(concessao)
		if nc["origem"] == "FORNECEDOR":
			acoes.add_child(_botao("RNC", _decidir_nc.bind(nc["id"], "RNC_FORNECEDOR")))
		ficha.add_child(acoes)


func _avisar(texto: String) -> void:
	avisos.push_front(texto)
	if avisos.size() > MAX_AVISOS:
		avisos.resize(MAX_AVISOS)


# ---------------------------------------------------------------- desenho

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Paleta.FUNDO_ESCURO)
	if estado.is_empty():
		var texto := erro_api if not erro_api.is_empty() else "Conectando à API..."
		draw_string(fonte_titulo, Vector2(16, 40), "MARELLAR", HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TITULO, Paleta.TEXTO_CLARO)
		draw_multiline_string(fonte, Vector2(16, 64), texto, HORIZONTAL_ALIGNMENT_LEFT, size.x - 32,
				TAMANHO_TEXTO, -1, Paleta.ALERTA)
		return
	_desenhar_topo()
	_desenhar_baias()
	_desenhar_painel()
	draw_rect(Rect2(0, size.y - ALTURA_BASE, size.x, ALTURA_BASE), Paleta.FUNDO)


func _desenhar_topo() -> void:
	draw_rect(Rect2(0, 0, size.x, ALTURA_TOPO), Paleta.FUNDO)
	var partes := str(estado["data"]).split("-")
	var em_inspecao = estado["barcoEmInspecao"]
	var texto := "%s/%s %02dh | %s dia %d/21 | Inspeção: %s (fila %d) | NCs: %d | Acidentes: %d | Marta %d" % [
		partes[2], partes[1], int(estado["hora"]), estado["mesReferencia"], int(estado["diaUtilDoMes"]),
		"livre" if em_inspecao == null else em_inspecao, estado["filaInspecao"].size(),
		int(estado["qualidade"]["ncsAbertas"]), int(estado["seguranca"]["acidentes"]), int(estado["inspetor"]["moral"])]
	draw_string(fonte, Vector2(5, 12), texto, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Paleta.TEXTO)
	var vel := "PAUSA" if velocidade == 0 else "x%d" % velocidade
	draw_string(fonte_titulo, Vector2(size.x - 44, 12), vel, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TITULO,
			Paleta.ALERTA if velocidade == 0 else Paleta.MONTAGEM_LIMPA)


func _desenhar_baias() -> void:
	areas_barcos.clear()
	var area := Rect2(4, ALTURA_TOPO + 4, size.x - LARGURA_PAINEL - 10, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	var barcos: Array = estado["barcos"]
	var colunas := 2
	var linhas := maxi(1, int(ceil(barcos.size() / float(colunas))))
	var tam := Vector2(floor((area.size.x - 4 * (colunas - 1)) / colunas), floor((area.size.y - 4 * (linhas - 1)) / linhas))
	for i in barcos.size():
		var pos := area.position + Vector2((i % colunas) * (tam.x + 4), (i / colunas) * (tam.y + 4))
		_desenhar_baia(barcos[i], Rect2(pos, tam))


func _desenhar_baia(barco: Dictionary, baia: Rect2) -> void:
	var id: String = barco["id"]
	var gate: String = barco["gateAtual"]
	var status: String = barco["status"]
	var entregue := status == "ENTREGUE"
	var etapa := Sprites.etapa_do_gate(gate, entregue)
	var zona_suja := etapa == Sprites.ETAPA_MOLDE
	var cor_zona := Paleta.PRODUCAO_SUJA if zona_suja else Paleta.MONTAGEM_LIMPA
	var t := Time.get_ticks_msec() / 1000.0
	areas_barcos[id] = baia

	# piso, faixa da zona (suja = laminação, limpa = montagem) e grade de tiles 16 px
	draw_rect(baia, Paleta.ARMAZEM_ESCURO)
	for x in range(int(baia.position.x) + 16, int(baia.end.x), 16):
		draw_line(Vector2(x, baia.position.y + 3), Vector2(x, baia.end.y), Color(Paleta.ARMAZEM, 0.35))
	draw_rect(Rect2(baia.position, Vector2(baia.size.x, 3)), cor_zona)

	# barco
	var linha_dagua := baia.position + Vector2(baia.size.x / 2 - 44, baia.size.y * 0.62)
	Sprites.barco(self, linha_dagua, etapa, float(barco["progressoGate"]), t)

	# bonequinhos trabalhando (laminação de marrom, montagem de verde)
	var uniforme := Paleta.PRODUCAO_SUJA if zona_suja else Paleta.MONTAGEM_LIMPA_ESCURA
	var alocadas: Array = barco["pessoasAlocadas"]
	for k in alocadas.size():
		var frente := k % 2 == 0
		var x := linha_dagua.x + 8 + (k / 2) * 26
		var y := linha_dagua.y + (12.0 if frente else -16.0)
		var quadro := int(t * 4 + k) % 2 if velocidade > 0 else 0
		Sprites.pessoa(self, Vector2(x, y), uniforme, quadro)

	# inspetora ao lado do barco em inspeção
	if estado["barcoEmInspecao"] == id:
		Sprites.inspetor(self, linha_dagua + Vector2(96, 10), int(t * 2) % 2)

	# selo de NC aberta
	var abertas := 0
	for nc in barco["ncs"]:
		if nc["status"] == "ABERTA":
			abertas += 1
	if abertas > 0:
		Sprites.selo_nc(self, linha_dagua + Vector2(88, -26), fonte, abertas, int(t * 3) % 2 == 0)

	# textos e barra de progresso
	var esquerda := baia.position.x + 4
	draw_string(fonte_titulo, Vector2(esquerda, baia.position.y + 14), id, HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TITULO, Paleta.TEXTO_CLARO)
	draw_string(fonte, Vector2(baia.end.x - 22, baia.position.y + 15), "P%d" % int(barco["prioridade"]),
			HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Paleta.ALERTA)
	draw_string(fonte, Vector2(esquerda, baia.position.y + 27), "%s - %s" % [gate, barco["nomeGate"]],
			HORIZONTAL_ALIGNMENT_LEFT, baia.size.x - 8, TAMANHO_TEXTO, Paleta.TEXTO)

	var barra := Rect2(esquerda, baia.end.y - 17, baia.size.x - 8, 3)
	draw_rect(barra, Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * float(barco["progressoGate"]), barra.size.y)), cor_zona)
	var detalhe := ""
	if status == "EM_RETRABALHO":
		detalhe = "faltam %d HH de retrabalho" % int(ceil(float(barco["hhRetrabalhoPendente"])))
	elif status == "EM_PRODUCAO":
		detalhe = "faltam %d HH" % int(ceil(float(barco["hhRestantesNoGate"])))
	var texto_status: String = NOMES_STATUS.get(status, status)
	if barco["retido"] and status != "APROVADO":
		texto_status += " (segurar)"
	draw_string(fonte, Vector2(esquerda, baia.end.y - 3), "%s  %s" % [texto_status, detalhe],
			HORIZONTAL_ALIGNMENT_LEFT, baia.size.x - 8, TAMANHO_TEXTO, _cor_status(status))

	if id == selecionado:
		draw_rect(baia.grow(-0.5), Paleta.ALERTA, false, 1.0)


func _desenhar_painel() -> void:
	var painel: Rect2 = Rect2(size.x - LARGURA_PAINEL - 2, ALTURA_TOPO + 4, LARGURA_PAINEL, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	draw_rect(painel, Paleta.FUNDO)
	var x := painel.position.x + 6
	var barco := _barco(selecionado)

	# título do painel (o conteúdo são Controls dentro da rolagem)
	var titulo := "FICHA DO BARCO" if barco.is_empty() else "BARCO %s" % selecionado
	if self.painel == PAINEL_EQUIPE:
		titulo = "EQUIPE"
	elif self.painel == PAINEL_FORNECEDORES:
		titulo = "FORNECEDORES"
	draw_string(fonte_titulo, Vector2(x, painel.position.y + 12), titulo, HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TITULO, Paleta.ALERTA)
	if self.painel != PAINEL_BARCO:
		return
	if barco.is_empty():
		draw_string(fonte, Vector2(x, painel.position.y + 28), "Clique num barco.", HORIZONTAL_ALIGNMENT_LEFT, -1,
				TAMANHO_TEXTO, Paleta.PISO_ESCURO)
	elif (barco["ncs"] as Array).is_empty():
		draw_string(fonte, Vector2(x, painel.position.y + 28), "%s | %d gates aprovados | sem NC" % [
			barco["cliente"], (barco["gatesAprovados"] as Array).size()], HORIZONTAL_ALIGNMENT_LEFT,
			LARGURA_PAINEL - 10, TAMANHO_TEXTO, Paleta.TEXTO)

	# avisos na metade de baixo
	var topo_avisos := rolagem.position.y + rolagem.size.y + 8
	draw_line(Vector2(painel.position.x, topo_avisos - 4), Vector2(painel.end.x, topo_avisos - 4), Paleta.ARMAZEM_ESCURO)
	draw_string(fonte_titulo, Vector2(x, topo_avisos + 8), "AVISOS", HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TITULO, Paleta.ALERTA)
	var y := topo_avisos + 22
	for aviso in avisos:
		if y > painel.end.y - 8:
			break
		var linhas := fonte.get_multiline_string_size(aviso, HORIZONTAL_ALIGNMENT_LEFT, LARGURA_PAINEL - 12, TAMANHO_TEXTO)
		draw_multiline_string(fonte, Vector2(x, y), aviso, HORIZONTAL_ALIGNMENT_LEFT, LARGURA_PAINEL - 12,
				TAMANHO_TEXTO, -1, Paleta.TEXTO)
		y += linhas.y + 3
	if not erro_api.is_empty():
		draw_multiline_string(fonte, Vector2(x, painel.end.y - 24), erro_api, HORIZONTAL_ALIGNMENT_LEFT,
				LARGURA_PAINEL - 12, TAMANHO_TEXTO, 2, Paleta.CRITICO)


# ---------------------------------------------------------------- utilitários

func _barco(id: String) -> Dictionary:
	if estado.is_empty() or id.is_empty():
		return {}
	for barco in estado["barcos"]:
		if barco["id"] == id:
			return barco
	return {}


func _cor_status(status: String) -> Color:
	match status:
		"AGUARDANDO_INSPECAO", "EM_RETRABALHO":
			return Paleta.ALERTA
		"EM_INSPECAO":
			return Paleta.AGUA
		"AGUARDANDO_DECISAO", "APROVADO", "PARADO_MATERIAL":
			return Paleta.CRITICO
		"ENTREGUE":
			return Paleta.MONTAGEM_LIMPA
	return Paleta.TEXTO


func _motivo_pausa(motivo: String) -> String:
	match motivo:
		"FIM_DO_MES":
			return "fim do mês. A reunião do conselho entra no M5."
		"NC_CRITICA":
			return "NC crítica detectada. Clique no barco e decida."
		"ACIDENTE":
			return "acidente de trabalho. Veja a equipe (F1)."
	return motivo
