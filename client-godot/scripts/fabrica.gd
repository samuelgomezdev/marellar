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
var avisos: Array[Dictionary] = []  # {texto, quando}
var area_cartao := Rect2()
var area_alerta_nc := Rect2()
var barco_alerta_nc := ""
var erro_api := ""
var selecionado := ""
var areas_barcos := {}  # id do barco -> Rect2 clicável
var assinatura_ficha := ""
var painel := PAINEL_BARCO

var fonte: Font
var fonte_titulo: Font
var botoes_velocidade := {}
var botao_prioridade: Button
var botao_segurar: Button
var ficha: VBoxContainer
var rolagem: ScrollContainer
var botoes_painel := {}
var tela_conselho: PanelContainer
var tela_saves: PanelContainer
var tela_ficha: FichaBarco
var botao_ficha: Button
var botao_conselho: Button


func _ready() -> void:
	fonte = _fonte_pixel(FONTE_TEXTO)
	fonte_titulo = _fonte_pixel(FONTE_TITULO)
	theme = Theme.new()
	theme.default_font = fonte
	theme.default_font_size = TAMANHO_TEXTO
	Api.erro.connect(_on_erro_api)
	_montar_barra_inferior()
	_montar_ficha()
	_montar_ficha_completa()
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
	if args.has("--salvar"):
		await _salvar()
	if args.has("--carregar"):
		await _carregar_ultimo_save()
	i = args.find("--selecionar")
	if i >= 0 and i + 1 < args.size():
		selecionado = args[i + 1]
	if args.has("--ficha"):
		_abrir_ficha_completa()
	if args.has("--saves"):
		await _abrir_saves()
	i = args.find("--painel")
	if i >= 0 and i + 1 < args.size():
		painel = args[i + 1]
	if estado.get("motivoPausa") in ["FIM_DO_MES", "DERROTA"] and estado.get("conselho") != null:
		_abrir_conselho()
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
		var motivo := str(resposta["resultado"]["motivoPausa"])
		_avisar("PAUSA: %s" % _motivo_pausa(motivo))
		if motivo == "FIM_DO_MES" or motivo == "DERROTA":
			_abrir_conselho()
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


func _salvar() -> void:
	var resumo = await Api.salvar()
	if resumo != null:
		_avisar("Partida salva (%s, %02dh). F9 carrega o último save." % [_data_curta(str(resumo["data"])), int(resumo["hora"])])


## Carrega o save mais recente. O motor joga de novo as mesmas ordens com a mesma semente.
func _carregar_ultimo_save() -> void:
	_mudar_velocidade(0)
	var lista = await Api.saves()
	if lista == null:
		return
	if lista.is_empty():
		_avisar("Nenhum save ainda. F5 salva a partida.")
		return
	await _carregar(str(lista[0]["id"]))


func _carregar(id: String) -> void:
	_mudar_velocidade(0)
	_avisar("Carregando o save...")
	if not await Api.carregar(id):
		return
	_fechar_conselho()
	tela_ficha.visible = false
	selecionado = ""
	painel = PAINEL_BARCO
	await _atualizar_estado()
	ultimo_evento = int(estado["ultimoEventoId"])  # eventos antigos já foram vistos
	_avisar("Save carregado: %s, %02dh." % [_data_curta(str(estado["data"])), int(estado["hora"])])
	if estado.get("motivoPausa") in ["FIM_DO_MES", "DERROTA"] and estado.get("conselho") != null:
		_abrir_conselho()
	_atualizar_interface()


## "2027-04-12" -> "12/04/2027".
func _data_curta(iso: String) -> String:
	var p := iso.split("-")
	return iso if p.size() != 3 else "%s/%s/%s" % [p[2], p[1], p[0]]


func _on_erro_api(mensagem: String) -> void:
	erro_api = mensagem
	push_warning(mensagem)


# ---------------------------------------------------------------- entrada

func _unhandled_input(event: InputEvent) -> void:
	if tela_conselho != null or tela_saves != null:
		if event is InputEventKey and event.pressed and event.keycode == KEY_ESCAPE:
			_fechar_saves()
		return
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
			KEY_F5:
				_salvar()
			KEY_F9:
				_carregar_ultimo_save()
			KEY_ENTER, KEY_KP_ENTER:
				_abrir_ficha_completa()
			KEY_ESCAPE:
				if tela_ficha.visible:
					_fechar_ficha_completa()
					return
				selecionado = ""
				painel = PAINEL_BARCO
				_atualizar_interface()


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		if area_alerta_nc.has_point(event.position) and not barco_alerta_nc.is_empty():
			selecionado = barco_alerta_nc
			_abrir_ficha_completa()
			return
		if area_cartao.has_point(event.position):
			_abrir_ficha_completa()
			return
		for id in areas_barcos:
			if areas_barcos[id].has_point(event.position):
				selecionado = id
				painel = PAINEL_BARCO
				_atualizar_interface()
				if event.double_click:
					_abrir_ficha_completa()
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
	botao_ficha = _botao("Ficha", _abrir_ficha_completa)
	botao_ficha.tooltip_text = "Ficha completa do barco selecionado (ou duplo clique no barco)."
	barra.add_child(botao_ficha)
	botao_prioridade = _botao("Prior. 1", _priorizar)
	botao_prioridade.tooltip_text = "Coloca o barco selecionado na prioridade 1."
	barra.add_child(botao_prioridade)
	botao_segurar = _botao("Segurar", _alternar_segurar)
	botao_segurar.tooltip_text = "Segura o barco no gate atual depois de aprovado."
	barra.add_child(botao_segurar)
	barra.add_child(VSeparator.new())
	for opcao in [[PAINEL_EQUIPE, "Equipe", "F1"], [PAINEL_FORNECEDORES, "Fornec.", "F2"]]:
		var b := _botao(opcao[1], _abrir_painel.bind(opcao[0]))
		b.toggle_mode = true
		b.tooltip_text = "Atalho: %s" % opcao[2]
		barra.add_child(b)
		botoes_painel[opcao[0]] = b
	botao_conselho = _botao("Conselho", _abrir_conselho)
	botao_conselho.tooltip_text = "Última reunião do conselho."
	barra.add_child(botao_conselho)
	barra.add_child(VSeparator.new())
	var salvar := _botao("Salvar", _salvar)
	salvar.tooltip_text = "Atalho: F5"
	barra.add_child(salvar)
	var carregar := _botao("Carregar", _abrir_saves)
	carregar.tooltip_text = "Atalho: F9 (último save)"
	barra.add_child(carregar)


func _montar_ficha() -> void:
	rolagem = ScrollContainer.new()
	rolagem.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(rolagem)
	ficha = VBoxContainer.new()
	ficha.add_theme_constant_override("separation", 1)
	ficha.custom_minimum_size.x = LARGURA_PAINEL - 14
	rolagem.add_child(ficha)


func _montar_ficha_completa() -> void:
	tela_ficha = FichaBarco.new()
	tela_ficha.fonte = fonte
	tela_ficha.fonte_titulo = fonte_titulo
	tela_ficha.tamanho_texto = TAMANHO_TEXTO
	tela_ficha.tamanho_titulo = TAMANHO_TITULO
	tela_ficha.visible = false
	tela_ficha.position = Vector2(0, ALTURA_TOPO)
	tela_ficha.size = Vector2(size.x, size.y - ALTURA_TOPO - ALTURA_BASE)
	add_child(tela_ficha)
	tela_ficha.fechar.connect(_fechar_ficha_completa)
	tela_ficha.priorizar.connect(_priorizar)
	tela_ficha.alternar_segurar.connect(_alternar_segurar)
	tela_ficha.decidir_nc.connect(_decidir_nc)


func _abrir_ficha_completa() -> void:
	if _barco(selecionado).is_empty():
		_avisar("Selecione um barco para abrir a ficha.")
		return
	tela_ficha.visible = true
	_atualizar_interface()


func _fechar_ficha_completa() -> void:
	tela_ficha.visible = false
	_atualizar_interface()


func _abrir_painel(qual: String) -> void:
	tela_ficha.visible = false
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
	botao_ficha.disabled = barco.is_empty()
	botao_prioridade.disabled = not ativo
	botao_segurar.disabled = not ativo
	botao_segurar.text = "Liberar" if ativo and barco["retido"] else "Segurar"
	botao_conselho.disabled = estado.get("conselho") == null
	if tela_ficha.visible:
		if barco.is_empty():
			tela_ficha.visible = false
		else:
			tela_ficha.animado = velocidade > 0
			tela_ficha.atualizar(barco, int(estado["tick"]))
	for chave in botoes_painel:
		botoes_painel[chave].set_pressed_no_signal(painel == chave)
	_posicionar_rolagem()
	match painel:
		PAINEL_EQUIPE:
			_reconstruir_equipe()
		PAINEL_FORNECEDORES:
			_reconstruir_fornecedores()
		_:
			assinatura_ficha = ""  # o próximo painel aberto se refaz


## Equipe e fornecedores abrem por cima da fábrica, à direita.
func _posicionar_rolagem() -> void:
	var topo := ALTURA_TOPO + 22
	rolagem.visible = painel != PAINEL_BARCO
	rolagem.position = Vector2(size.x - LARGURA_PAINEL, topo)
	rolagem.size = Vector2(LARGURA_PAINEL - 4, size.y - ALTURA_BASE - 6 - topo)


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


func _avisar(texto: String) -> void:
	avisos.push_front({"texto": texto, "quando": Time.get_ticks_msec() / 1000.0})
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
	var t := Time.get_ticks_msec() / 1000.0
	if not tela_ficha.visible:
		var area := Rect2(0, ALTURA_TOPO, size.x, size.y - ALTURA_TOPO - ALTURA_BASE)
		areas_barcos = Planta.desenhar(self, area, estado, selecionado, t, velocidade > 0, fonte)
		_desenhar_cartao()
		_desenhar_alertas(t)
		_desenhar_painel()
	else:
		areas_barcos = {}
		area_cartao = Rect2()
		area_alerta_nc = Rect2()
	_desenhar_topo()
	draw_rect(Rect2(0, size.y - ALTURA_BASE, size.x, ALTURA_BASE), Paleta.FUNDO)


## Barra de cima em blocos, como no mockup: data, caixa, valor, moral, NCs abertas e conselho.
func _desenhar_topo() -> void:
	var fin: Dictionary = estado["financeiro"]
	draw_rect(Rect2(0, 0, size.x, ALTURA_TOPO), Paleta.NAO_CONFORME_ESCURO if float(fin["caixa"]) < 0 else Paleta.FUNDO)
	var soma := 0
	var quantas := 0
	for p in estado["pessoas"]:
		if p["funcao"] == "PRODUCAO" and p["situacao"] in ["ATIVA", "TREINAMENTO", "AFASTADA"]:
			soma += int(p["moral"])
			quantas += 1
	var moral := soma / maxi(1, quantas)
	var ncs := int(estado["qualidade"]["ncsAbertas"])
	var paciencia := int(fin["pacienciaConselho"])
	var blocos := [
		["", "%s · dia %d · %02dh" % [estado["mesReferencia"], int(estado["diaUtilDoMes"]), int(estado["hora"])], Paleta.TEXTO_CLARO],
		["Caixa", _milhoes(fin["caixa"]), Paleta.MONTAGEM_LIMPA if float(fin["caixa"]) >= 0 else Paleta.CRITICO],
		["Valor", _milhoes(fin["valorEmpresa"]), Paleta.AGUA],
		["Moral", "%d%%" % moral, Paleta.MONTAGEM_LIMPA if moral >= 60 else (Paleta.ALERTA if moral >= 40 else Paleta.CRITICO)],
		["NCs", str(ncs), Paleta.CRITICO if ncs > 0 else Paleta.MONTAGEM_LIMPA],
		["Conselho", "%d/%d" % [paciencia, int(fin["pacienciaMaxima"])], Paleta.ALERTA if paciencia <= 2 else Paleta.TEXTO_CLARO],
	]
	var x := 2.0
	for b in blocos:
		var rotulo: String = b[0]
		var valor: String = b[1]
		var largura_rotulo := 0.0 if rotulo.is_empty() else fonte.get_string_size(rotulo + " ", HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO).x
		var largura := largura_rotulo + fonte.get_string_size(valor, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO).x + 6
		draw_rect(Rect2(x, 1, largura, ALTURA_TOPO - 2), Paleta.FUNDO_ESCURO)
		draw_rect(Rect2(x, 1, largura, ALTURA_TOPO - 2).grow(-0.5), Paleta.ARMAZEM_ESCURO, false, 1.0)
		draw_string(fonte, Vector2(x + 3, 12), rotulo, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Paleta.TEXTO)
		draw_string(fonte, Vector2(x + 3 + largura_rotulo, 12), valor, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, b[2])
		x += largura + 2
	var vel := "PAUSA" if velocidade == 0 else "x%d" % velocidade
	draw_string(fonte, Vector2(x + 2, 12), vel, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO,
			Paleta.ALERTA if velocidade == 0 else Paleta.MONTAGEM_LIMPA)


## Cartão do barco selecionado no canto de baixo, à esquerda (clique abre a ficha).
func _desenhar_cartao() -> void:
	var barco := _barco(selecionado)
	area_cartao = Rect2()
	if barco.is_empty():
		return
	var r := Rect2(6, size.y - ALTURA_BASE - 70, 178, 66)
	area_cartao = r
	draw_rect(r, Paleta.FUNDO)
	draw_rect(r.grow(-0.5), Paleta.ARMAZEM, false, 1.0)
	var x := r.position.x + 6
	draw_string(fonte_titulo, Vector2(x, r.position.y + 12), "CASCO %s" % barco["id"], HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TITULO, Paleta.ALERTA)
	draw_string(fonte, Vector2(x, r.position.y + 26), "Gate", HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Paleta.PISO_ESCURO)
	draw_string(fonte, Vector2(x, r.position.y + 26), "%s · %s" % [barco["gateAtual"], barco["nomeGate"]],
			HORIZONTAL_ALIGNMENT_RIGHT, r.size.x - 12, TAMANHO_TEXTO, Paleta.TEXTO_CLARO)
	var progresso := float(barco["progressoGate"])
	draw_rect(Rect2(x, r.position.y + 31, r.size.x - 12, 3), Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(x, r.position.y + 31, (r.size.x - 12) * progresso, 3), Paleta.MONTAGEM_LIMPA)
	var abertas := 0
	for nc in barco["ncs"]:
		if nc["status"] == "ABERTA":
			abertas += 1
	var status: String = NOMES_STATUS.get(str(barco["status"]), str(barco["status"]))
	draw_string(fonte, Vector2(x, r.position.y + 46), "%s · %d%%" % [status, int(progresso * 100)], HORIZONTAL_ALIGNMENT_LEFT,
			r.size.x - 12, TAMANHO_TEXTO, _cor_status(str(barco["status"])))
	draw_string(fonte, Vector2(x, r.position.y + 60), "Equipe %d" % (barco["pessoasAlocadas"] as Array).size(),
			HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Paleta.TEXTO)
	draw_string(fonte, Vector2(x, r.position.y + 60), "%d NC  ·  ficha >" % abertas, HORIZONTAL_ALIGNMENT_RIGHT, r.size.x - 12,
			TAMANHO_TEXTO, Paleta.CRITICO if abertas > 0 else Paleta.PISO_ESCURO)


## Alertas no canto de cima, à direita: NC esperando decisão (clicável) e os avisos recentes.
func _desenhar_alertas(t: float) -> void:
	var largura := 176.0
	var x := size.x - largura - 4
	var y := ALTURA_TOPO + 4.0
	area_alerta_nc = Rect2()
	barco_alerta_nc = ""
	for barco in estado["barcos"]:
		for nc in barco["ncs"]:
			if nc["status"] != "ABERTA" or not barco_alerta_nc.is_empty():
				continue
			barco_alerta_nc = barco["id"]
			var texto := "%s · %s. Decidir?" % [barco["id"], str(nc["descricao"]).to_lower()]
			var altura := fonte.get_multiline_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10, TAMANHO_TEXTO).y + 22
			area_alerta_nc = Rect2(x, y, largura, altura)
			draw_rect(area_alerta_nc, Paleta.NAO_CONFORME_ESCURO)
			draw_rect(area_alerta_nc.grow(-0.5), Paleta.CRITICO if int(t * 2) % 2 == 0 else Paleta.NAO_CONFORME, false, 1.0)
			Sprites.selo_nc(self, Vector2(x + 10, y + 9), fonte, 1, true)
			draw_string(fonte, Vector2(x + 20, y + 14), "NC no gate %s" % nc["gate"], HORIZONTAL_ALIGNMENT_LEFT, -1,
					TAMANHO_TEXTO, Paleta.TEXTO_CLARO)
			draw_multiline_string(fonte, Vector2(x + 5, y + 28), texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10,
					TAMANHO_TEXTO, -1, Paleta.TEXTO)
			y += altura + 3
	var agora := Time.get_ticks_msec() / 1000.0
	var mostrados := 0
	for aviso in avisos:
		var idade: float = agora - float(aviso["quando"])
		if idade > 12.0 or mostrados >= 3:
			break
		var texto: String = aviso["texto"]
		var altura := fonte.get_multiline_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10, TAMANHO_TEXTO).y + 6
		var opaco := clampf((12.0 - idade) / 2.0, 0.0, 1.0)
		draw_rect(Rect2(x, y, largura, altura), Color(Paleta.FUNDO, 0.92 * opaco))
		draw_rect(Rect2(x, y, 2, altura), Color(Paleta.ALERTA, opaco))
		draw_multiline_string(fonte, Vector2(x + 6, y + 13), texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10,
				TAMANHO_TEXTO, -1, Color(Paleta.TEXTO, opaco))
		y += altura + 3
		mostrados += 1
	if not erro_api.is_empty():
		draw_multiline_string(fonte, Vector2(x, y + 12), erro_api, HORIZONTAL_ALIGNMENT_LEFT, largura, TAMANHO_TEXTO, 3,
				Paleta.CRITICO)


## Equipe e fornecedores: painel por cima da fábrica, à direita (o conteúdo são Controls na rolagem).
func _desenhar_painel() -> void:
	if painel == PAINEL_BARCO:
		return
	var r := Rect2(size.x - LARGURA_PAINEL - 2, ALTURA_TOPO + 4, LARGURA_PAINEL, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	draw_rect(r, Paleta.FUNDO)
	draw_rect(r.grow(-0.5), Paleta.ALERTA_ESCURO, false, 1.0)
	draw_string(fonte_titulo, Vector2(r.position.x + 6, r.position.y + 12), "EQUIPE" if painel == PAINEL_EQUIPE else "FORNECEDORES",
			HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TITULO, Paleta.ALERTA)


# ---------------------------------------------------------------- lista de saves

func _abrir_saves() -> void:
	_mudar_velocidade(0)
	var lista = await Api.saves()
	if lista == null:
		return
	_fechar_saves()
	tela_saves = _nova_sobreposicao(Paleta.AGUA)
	var itens := VBoxContainer.new()
	itens.add_theme_constant_override("separation", 2)
	tela_saves.add_child(itens)
	var titulo := Label.new()
	titulo.text = "CARREGAR PARTIDA"
	titulo.add_theme_font_override("font", fonte_titulo)
	titulo.add_theme_font_size_override("font_size", TAMANHO_TITULO)
	titulo.add_theme_color_override("font_color", Paleta.AGUA)
	itens.add_child(titulo)
	if lista.is_empty():
		itens.add_child(_linha_conselho("Nenhum save ainda. Use Salvar (F5).", Paleta.PISO_ESCURO, 320))
	for save in lista.slice(0, 8):
		var atual: bool = save["id"] == Api.partida_id
		var texto := "%s %02dh · semente %d%s" % [_data_curta(str(save["data"])), int(save["hora"]), int(save["semente"]),
				" · esta partida" if atual else ""]
		itens.add_child(_botao(texto, _carregar_save.bind(str(save["id"]))))
	itens.add_child(_botao("Voltar", _fechar_saves))


func _nova_sobreposicao(borda: Color) -> PanelContainer:
	var painel_novo := PanelContainer.new()
	var fundo := StyleBoxFlat.new()
	fundo.bg_color = Paleta.FUNDO_ESCURO
	fundo.border_color = borda
	fundo.set_border_width_all(1)
	fundo.set_content_margin_all(8)
	painel_novo.add_theme_stylebox_override("panel", fundo)
	painel_novo.position = Vector2(size.x / 2 - 170, ALTURA_TOPO + 30)
	painel_novo.custom_minimum_size = Vector2(340, 0)
	add_child(painel_novo)
	return painel_novo


func _carregar_save(id: String) -> void:
	_fechar_saves()
	await _carregar(id)


func _fechar_saves() -> void:
	if tela_saves != null:
		tela_saves.queue_free()
		tela_saves = null


# ---------------------------------------------------------------- tela do conselho

func _abrir_conselho() -> void:
	var r = estado.get("conselho")
	if r == null:
		return
	if tela_conselho != null:
		tela_conselho.queue_free()
	tela_conselho = PanelContainer.new()
	var fundo := StyleBoxFlat.new()
	fundo.bg_color = Paleta.FUNDO_ESCURO
	fundo.border_color = Paleta.ALERTA
	fundo.set_border_width_all(1)
	fundo.set_content_margin_all(6)
	tela_conselho.add_theme_stylebox_override("panel", fundo)
	tela_conselho.position = Vector2(40, ALTURA_TOPO + 6)
	tela_conselho.size = Vector2(size.x - 80, size.y - ALTURA_TOPO - ALTURA_BASE - 12)
	add_child(tela_conselho)

	var rolagem_conselho := ScrollContainer.new()
	rolagem_conselho.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	tela_conselho.add_child(rolagem_conselho)
	var lista := VBoxContainer.new()
	lista.custom_minimum_size.x = tela_conselho.size.x - 24
	lista.add_theme_constant_override("separation", 0)
	rolagem_conselho.add_child(lista)
	var largura := lista.custom_minimum_size.x

	var titulo := Label.new()
	titulo.text = "REUNIÃO DO CONSELHO - %s" % r["mesReferencia"]
	titulo.add_theme_color_override("font_color", Paleta.ALERTA)  # VT323: a Press Start 2P não tem "Ã"
	lista.add_child(titulo)

	for m in r["metas"]:
		var cor := Paleta.MONTAGEM_LIMPA if m["cumprida"] else Paleta.CRITICO
		lista.add_child(_linha_conselho("%s %s  (%s)" % ["[OK]" if m["cumprida"] else "[X] ", m["descricao"], m["resultado"]], cor, largura))
	lista.add_child(_linha_conselho("Paciência do conselho: %d -> %d de %d" % [int(r["pacienciaAntes"]),
			int(r["pacienciaDepois"]), int(estado["financeiro"]["pacienciaMaxima"])], Paleta.ALERTA, largura))

	var dre: Dictionary = r["dre"]
	lista.add_child(_linha_conselho("DRE: receita %s | material %s | pessoal %s | fixas %s" % [_reais(dre["receita"]),
			_reais(float(dre["custoMaterial"]) + float(dre["consumiveis"]) + float(dre["materialRetrabalho"])),
			_reais(dre["pessoal"]), _reais(dre["despesasFixas"])], Paleta.TEXTO, largura))
	var cor_ebitda := Paleta.MONTAGEM_LIMPA if float(dre["ebitda"]) >= 0 else Paleta.CRITICO
	lista.add_child(_linha_conselho("EBITDA %s | Caixa %s | Valor da empresa %s" % [_reais(dre["ebitda"]),
			_reais(r["caixa"]), _reais(r["valorEmpresa"])], cor_ebitda, largura))
	lista.add_child(_linha_conselho("Entregas %d | NCs %d | RNCs %d | concessões %d | acidentes %d" % [int(r["barcosEntregues"]),
			int(r["ncsDetectadas"]), int(r["rncs"]), int(r["concessoes"]), int(r["acidentes"])], Paleta.PISO_ESCURO, largura))
	if r["alertaCaixaNegativo"]:
		lista.add_child(_linha_conselho("ALERTA: caixa negativo. 3 meses seguidos encerram a empresa.", Paleta.CRITICO, largura))

	for f in r["falas"]:
		lista.add_child(_linha_conselho("%s: \"%s\"" % [f["conselheiro"], f["texto"]],
				Paleta.TEXTO_CLARO if f["seria"] else Paleta.AGUA, largura))

	if r["derrota"] != null:
		lista.add_child(_linha_conselho("FIM DE JOGO. %s" % ("Três meses de caixa negativo." if r["derrota"] == "CAIXA"
				else "O conselho perdeu a paciência."), Paleta.CRITICO, largura))
		lista.add_child(_botao("Nova partida", func(): get_tree().reload_current_scene()))
		return

	lista.add_child(_linha_conselho("Decisão para o próximo mês (uma):", Paleta.ALERTA, largura))
	var escolhida = r["decisaoEscolhida"]
	for o in r["opcoes"]:
		var custo := "" if float(o["custo"]) == 0 else " (%s)" % _reais(o["custo"])
		var b := _botao("%s%s" % [o["titulo"], custo], _escolher_decisao.bind(o["id"]))
		b.tooltip_text = o["descricao"]
		b.disabled = escolhida != null
		if escolhida == o["id"]:
			b.text = "> " + b.text
		lista.add_child(b)
	lista.add_child(_botao("Continuar", _fechar_conselho))


func _linha_conselho(texto: String, cor: Color, largura: float) -> Label:
	var r := Label.new()
	r.text = texto
	r.autowrap_mode = TextServer.AUTOWRAP_WORD
	r.custom_minimum_size.x = largura
	r.add_theme_color_override("font_color", cor)
	return r


func _escolher_decisao(opcao_id: String) -> void:
	await _enviar_ordem({"tipo": "EscolherDecisaoConselho", "opcaoId": opcao_id}, "")
	_abrir_conselho()


func _fechar_conselho() -> void:
	if tela_conselho != null:
		tela_conselho.queue_free()
		tela_conselho = null


## Só para exibir (as contas ficam no motor, em BigDecimal): "R$ 1.234.567".
func _reais(valor) -> String:
	var v := float(valor)
	var texto := str(int(round(absf(v))))
	var saida := ""
	while texto.length() > 3:
		saida = "." + texto.substr(texto.length() - 3) + saida
		texto = texto.substr(0, texto.length() - 3)
	return ("-" if v < 0 else "") + "R$ " + texto + saida


## "R$ 2,62 mi" para a barra de topo.
func _milhoes(valor) -> String:
	var v := float(valor) / 1000000.0
	return ("R$ %.2f mi" % v).replace(".", ",")


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
			return "fim do mês. Reunião do conselho."
		"DERROTA":
			return "fim de jogo."
		"NC_CRITICA":
			return "NC crítica detectada. Clique no barco e decida."
		"ACIDENTE":
			return "acidente de trabalho. Veja a equipe (F1)."
	return motivo
