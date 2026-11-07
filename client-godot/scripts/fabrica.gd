extends Control
## Tela da Fábrica (MVP): baias com os barcos de 33 pés, bonequinhos trabalhando,
## barra de topo com data e fila de inspeção, avisos à direita e barra de baixo com as ordens.
## Arte provisória: retângulos na paleta Endesga 32. A arte final vem depois.

const SEMENTE := 42
const SEGUNDOS_POR_TICK_X1 := 2.5  # 1 dia útil (8 ticks) ≈ 20 s na velocidade 1 (GDD, Loops de jogo)
const ALTURA_TOPO := 16
const ALTURA_BASE := 24
const LARGURA_AVISOS := 190
const FONTE_PEQUENA := 8
const FONTE_MEDIA := 10
const MAX_AVISOS := 6

var estado: Dictionary = {}
var velocidade := 0
var acumulado := 0.0
var ocupado := false
var ultimo_evento := 0
var avisos: Array[String] = []
var erro_api := ""
var selecionado := ""
var areas_barcos := {}  # id do barco -> Rect2 clicável

var fonte: Font
var botoes_velocidade := {}
var rotulo_selecao: Label
var botao_prioridade: Button
var botao_segurar: Button


func _ready() -> void:
	fonte = ThemeDB.fallback_font
	theme = Theme.new()
	theme.default_font_size = FONTE_PEQUENA
	Api.erro.connect(_on_erro_api)
	_montar_barra_inferior()
	_atualizar_botoes()
	if await Api.criar_partida(SEMENTE):
		await _atualizar_estado()
		_avisar("Partida criada (semente %d). Espaço pausa; 1, 2 e 3 mudam a velocidade." % SEMENTE)
		# "godot --path client-godot -- --rodar" abre o jogo já em x4 (testes e capturas de tela)
		if OS.get_cmdline_user_args().has("--rodar"):
			_mudar_velocidade(4)


func _process(delta: float) -> void:
	queue_redraw()  # bonequinhos animados
	if velocidade == 0 or ocupado or Api.partida_id.is_empty():
		return
	acumulado += delta
	if acumulado >= SEGUNDOS_POR_TICK_X1 / velocidade:
		acumulado = 0.0
		_avancar(1)


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
	_atualizar_botoes()


func _atualizar_estado() -> void:
	var novo = await Api.estado()
	if novo != null:
		estado = novo
		_atualizar_botoes()


func _buscar_eventos() -> void:
	var lista = await Api.eventos(ultimo_evento)
	if lista == null:
		return
	for evento in lista:
		ultimo_evento = maxi(ultimo_evento, int(evento["id"]))
		_avisar(str(evento["mensagem"]))


func _mudar_velocidade(nova: int) -> void:
	velocidade = nova
	acumulado = 0.0
	for chave in botoes_velocidade:
		botoes_velocidade[chave].set_pressed_no_signal(chave == nova)
	if not Api.partida_id.is_empty():
		Api.ordem({"tipo": "MudarVelocidade", "velocidade": "PAUSA" if nova == 0 else "X%d" % nova})


func _priorizar() -> void:
	if selecionado.is_empty():
		return
	var novo = await Api.ordem({"tipo": "DefinirPrioridade", "barcoId": selecionado, "posicao": 1})
	if novo != null:
		estado = novo
		_avisar("%s agora é a prioridade 1." % selecionado)
		_atualizar_botoes()


func _alternar_segurar() -> void:
	var barco := _barco(selecionado)
	if barco.is_empty():
		return
	var tipo := "LiberarGate" if barco["retido"] else "SegurarGate"
	var novo = await Api.ordem({"tipo": tipo, "barcoId": selecionado})
	if novo != null:
		estado = novo
		_avisar("%s %s." % [selecionado, "liberado" if tipo == "LiberarGate" else "segurado no gate"])
		_atualizar_botoes()


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


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		selecionado = ""
		for id in areas_barcos:
			if areas_barcos[id].has_point(event.position):
				selecionado = id
		_atualizar_botoes()


# ---------------------------------------------------------------- interface

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
		var botao := Button.new()
		botao.text = opcao[1]
		botao.toggle_mode = true
		botao.focus_mode = Control.FOCUS_NONE
		botao.pressed.connect(_mudar_velocidade.bind(opcao[0]))
		barra.add_child(botao)
		botoes_velocidade[opcao[0]] = botao
	botoes_velocidade[0].set_pressed_no_signal(true)

	barra.add_child(VSeparator.new())
	rotulo_selecao = Label.new()
	rotulo_selecao.add_theme_color_override("font_color", Paleta.TEXTO)
	barra.add_child(rotulo_selecao)

	botao_prioridade = Button.new()
	botao_prioridade.text = "Prioridade 1"
	botao_prioridade.focus_mode = Control.FOCUS_NONE
	botao_prioridade.pressed.connect(_priorizar)
	barra.add_child(botao_prioridade)

	botao_segurar = Button.new()
	botao_segurar.focus_mode = Control.FOCUS_NONE
	botao_segurar.pressed.connect(_alternar_segurar)
	barra.add_child(botao_segurar)


func _atualizar_botoes() -> void:
	var barco := _barco(selecionado)
	var ativo: bool = not barco.is_empty() and barco["status"] != "ENTREGUE"
	rotulo_selecao.text = "Clique num barco" if barco.is_empty() else "Barco %s" % selecionado
	botao_prioridade.disabled = not ativo
	botao_segurar.disabled = not ativo
	botao_segurar.text = "Liberar gate" if ativo and barco["retido"] else "Segurar gate"


func _avisar(texto: String) -> void:
	avisos.push_front(texto)
	if avisos.size() > MAX_AVISOS:
		avisos.resize(MAX_AVISOS)


# ---------------------------------------------------------------- desenho

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Paleta.FUNDO_ESCURO)
	if estado.is_empty():
		var texto := erro_api if not erro_api.is_empty() else "Conectando à API..."
		draw_multiline_string(fonte, Vector2(16, 40), texto, HORIZONTAL_ALIGNMENT_LEFT, size.x - 32,
				FONTE_MEDIA, -1, Paleta.ALERTA)
		return
	_desenhar_topo()
	_desenhar_baias()
	_desenhar_avisos()
	draw_rect(Rect2(0, size.y - ALTURA_BASE, size.x, ALTURA_BASE), Paleta.FUNDO)


func _desenhar_topo() -> void:
	draw_rect(Rect2(0, 0, size.x, ALTURA_TOPO), Paleta.FUNDO)
	var partes := str(estado["data"]).split("-")
	var data := "%s/%s/%s" % [partes[2], partes[1], partes[0]]
	var em_inspecao = estado["barcoEmInspecao"]
	var texto := "%s %02dh  |  %s, dia %d/21  |  Inspeção: %s, fila %d  |  Ociosas: %s h" % [
		data, int(estado["hora"]), estado["mesReferencia"], int(estado["diaUtilDoMes"]),
		"livre" if em_inspecao == null else em_inspecao, estado["filaInspecao"].size(),
		str(int(estado["horasOciosas"]))]
	draw_string(fonte, Vector2(6, 11), texto, HORIZONTAL_ALIGNMENT_LEFT, -1, FONTE_PEQUENA, Paleta.TEXTO)
	var vel := "PAUSA" if velocidade == 0 else "x%d" % velocidade
	draw_string(fonte, Vector2(size.x - 40, 11), vel, HORIZONTAL_ALIGNMENT_LEFT, -1, FONTE_PEQUENA,
			Paleta.ALERTA if velocidade == 0 else Paleta.MONTAGEM_LIMPA)


func _desenhar_baias() -> void:
	areas_barcos.clear()
	var area := Rect2(4, ALTURA_TOPO + 4, size.x - LARGURA_AVISOS - 12, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	var barcos: Array = estado["barcos"]
	var colunas := 2
	var linhas := int(ceil(barcos.size() / float(colunas)))
	var tam := Vector2((area.size.x - 4 * (colunas - 1)) / colunas, (area.size.y - 4 * (linhas - 1)) / maxi(linhas, 1))
	for i in barcos.size():
		var pos := area.position + Vector2((i % colunas) * (tam.x + 4), (i / colunas) * (tam.y + 4))
		_desenhar_baia(barcos[i], Rect2(pos, tam))


func _desenhar_baia(barco: Dictionary, baia: Rect2) -> void:
	var id: String = barco["id"]
	var gate: String = barco["gateAtual"]
	var status: String = barco["status"]
	var zona_suja := gate == "DM"
	var cor_zona := Paleta.PRODUCAO_SUJA if zona_suja else Paleta.MONTAGEM_LIMPA
	areas_barcos[id] = baia

	# piso e faixa da zona (suja = laminação, limpa = montagem)
	draw_rect(baia, Paleta.ARMAZEM_ESCURO)
	draw_rect(Rect2(baia.position, Vector2(baia.size.x, 3)), cor_zona)
	if status == "ENTREGUE":
		draw_rect(Rect2(baia.position + Vector2(0, baia.size.y * 0.55), Vector2(baia.size.x, baia.size.y * 0.45)), Paleta.AGUA_ESCURA)

	# casco do barco de 33 pés (~5 tiles = 80 px)
	var centro := baia.position + Vector2(baia.size.x / 2, baia.size.y * 0.55)
	var casco := Rect2(centro - Vector2(40, 10), Vector2(68, 20))
	draw_rect(casco, Paleta.CASCO)
	draw_rect(Rect2(casco.position + Vector2(6, 4), Vector2(36, 12)), Paleta.CASCO_SOMBRA)
	draw_colored_polygon(PackedVector2Array([
		casco.position + Vector2(casco.size.x, 0),
		casco.position + Vector2(casco.size.x + 14, casco.size.y / 2),
		casco.position + Vector2(casco.size.x, casco.size.y)]), Paleta.CASCO)

	# bonequinhos alocados neste barco
	var t := Time.get_ticks_msec() / 1000.0
	var cor_uniforme := Paleta.PRODUCAO_SUJA if zona_suja else Paleta.MONTAGEM_LIMPA_ESCURA
	var alocadas: Array = barco["pessoasAlocadas"]
	for k in alocadas.size():
		var lado := -1 if k % 2 == 0 else 1
		var x := casco.position.x + 6 + (k / 2) * 22
		var y := centro.y + lado * 17 - 4
		var pulo := 1.0 if velocidade > 0 and int(t * 4 + k) % 2 == 0 else 0.0
		draw_rect(Rect2(x, y - pulo, 4, 7), cor_uniforme)
		draw_rect(Rect2(x, y - 3 - pulo, 4, 3), Paleta.TEXTO)

	# inspetor
	if estado["barcoEmInspecao"] == id:
		var xi := casco.end.x + 18
		draw_rect(Rect2(xi, centro.y - 4, 5, 8), Paleta.AGUA)
		draw_rect(Rect2(xi, centro.y - 8, 5, 4), Paleta.TEXTO)
		draw_string(fonte, Vector2(xi - 2, centro.y - 10), "Q", HORIZONTAL_ALIGNMENT_LEFT, -1, FONTE_PEQUENA, Paleta.AGUA)

	# textos e barra de progresso do gate
	var y0 := baia.position.y + 13
	draw_string(fonte, Vector2(baia.position.x + 4, y0), "%s  P%d" % [id, int(barco["prioridade"])],
			HORIZONTAL_ALIGNMENT_LEFT, -1, FONTE_MEDIA, Paleta.TEXTO_CLARO)
	draw_string(fonte, Vector2(baia.position.x + 4, y0 + 10), "%s - %s" % [gate, barco["nomeGate"]],
			HORIZONTAL_ALIGNMENT_LEFT, baia.size.x - 8, FONTE_PEQUENA, Paleta.TEXTO)
	var barra := Rect2(baia.position.x + 4, baia.end.y - 16, baia.size.x - 8, 4)
	draw_rect(barra, Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * float(barco["progressoGate"]), barra.size.y)), cor_zona)
	var restantes := int(ceil(float(barco["hhRestantesNoGate"])))
	draw_string(fonte, Vector2(barra.position.x, baia.end.y - 4), "%s  |  faltam %d HH" % [_texto_status(barco), restantes],
			HORIZONTAL_ALIGNMENT_LEFT, baia.size.x - 8, FONTE_PEQUENA, _cor_status(status))

	if id == selecionado:
		draw_rect(baia, Paleta.ALERTA, false, 1.0)


func _desenhar_avisos() -> void:
	var painel := Rect2(size.x - LARGURA_AVISOS - 4, ALTURA_TOPO + 4, LARGURA_AVISOS, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	draw_rect(painel, Paleta.FUNDO)
	draw_string(fonte, painel.position + Vector2(6, 12), "Avisos", HORIZONTAL_ALIGNMENT_LEFT, -1, FONTE_MEDIA, Paleta.ALERTA)
	var y := painel.position.y + 26
	for aviso in avisos:
		draw_multiline_string(fonte, Vector2(painel.position.x + 6, y), aviso, HORIZONTAL_ALIGNMENT_LEFT,
				painel.size.x - 12, FONTE_PEQUENA, 3, Paleta.TEXTO)
		y += 30
	if not erro_api.is_empty():
		draw_multiline_string(fonte, Vector2(painel.position.x + 6, painel.end.y - 30), erro_api,
				HORIZONTAL_ALIGNMENT_LEFT, painel.size.x - 12, FONTE_PEQUENA, 3, Paleta.CRITICO)


# ---------------------------------------------------------------- utilitários

func _barco(id: String) -> Dictionary:
	if estado.is_empty() or id.is_empty():
		return {}
	for barco in estado["barcos"]:
		if barco["id"] == id:
			return barco
	return {}


func _texto_status(barco: Dictionary) -> String:
	var texto: String = {
		"EM_PRODUCAO": "Em produção",
		"AGUARDANDO_INSPECAO": "Aguardando inspeção",
		"EM_INSPECAO": "Em inspeção",
		"APROVADO": "Aprovado, segurado",
		"PARADO_MATERIAL": "Parado: sem material",
		"ENTREGUE": "Entregue",
	}.get(barco["status"], barco["status"])
	if barco["retido"] and barco["status"] != "APROVADO":
		texto += " (segurar)"
	return texto


func _cor_status(status: String) -> Color:
	match status:
		"AGUARDANDO_INSPECAO":
			return Paleta.ALERTA
		"EM_INSPECAO":
			return Paleta.AGUA
		"APROVADO", "PARADO_MATERIAL":
			return Paleta.CRITICO
		"ENTREGUE":
			return Paleta.MONTAGEM_LIMPA
	return Paleta.TEXTO


func _motivo_pausa(motivo: String) -> String:
	match motivo:
		"FIM_DO_MES":
			return "fim do mês. A reunião do conselho entra no M5."
		"ACIDENTE":
			return "acidente de trabalho."
		"NC_CRITICA":
			return "NC crítica no gate."
	return motivo
