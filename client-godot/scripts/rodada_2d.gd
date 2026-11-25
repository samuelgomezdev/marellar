class_name Rodada2D
extends Control
## Rodada curta de inspeção 2D (sprint 2, etapa 3): o barco visto de cima com marcas; o jogador clica nas
## que são defeito antes de o tempo acabar. O motor decide tudo: esta tela só mostra as marcas que ele manda
## (sem saber quais são NC), junta os cliques e, depois da ordem InspecionarRodada, mostra o resultado que
## ele devolve (achou, passou, falso alarme). Formas simples nas cores do tema ("cinza"); a arte é da sprint 4.

signal concluir(barco_id: String, marcas: Array)   ## fim da rodada: manda a ordem
signal cancelar                                     ## fechou sem inspecionar: fica com o inspetor
signal continuar                                    ## fechou o resultado

const ESCALA := 3                 # casco de 26 x 84 px vira 78 x 252
const PIXEL := 3
const TAMANHO_MARCA := 7 * PIXEL  # ícone de 7 x 7 px desenhado com pixel de 3
const ALTURA_BOTOES := 26.0
const NOMES_FALSAS := {"SUJEIRA": "sujeira", "REFLEXO": "reflexo de luz", "FITA": "fita crepe"}
const NOMES_GRAVIDADE := {"LEVE": "leve", "MEDIA": "média", "CRITICA": "CRÍTICA"}

## Ícones 7 x 7: "X" cor principal, "o" cor de apoio. Defeitos de verdade e marcas falsas parecidas de propósito.
const ICONES := {
	"BOLHA":    [".......", "..XXX..", ".XooX X", ".XoXX..", ".XXXX..", "..XX...", "......."],
	"SECO":     ["o.o.o..", ".o.o.o.", "o.o.o.o", ".o.o.o.", "o.o.o.o", ".o.o.o.", "..o.o.."],
	"TRINCA":   ["X......", ".X.....", ".X.X...", "..X.X..", "....X..", ".....X.", "......X"],
	"COR":      ["XXXXX..", "XXXXX..", "XXXXX..", "XXXXX..", ".......", ".......", "......."],
	"FIXACAO":  [".......", "..XXX..", ".X.o.X.", ".XoooX.", ".X.o.X.", "..XXX..", "....X.."],
	"FIO":      ["XX.....", "..X....", "..X....", "...XX..", ".....X.", ".....X.", "......X"],
	"PAINEL":   ["XXXXXXX", "X.....X", "X.o.X.X", "X.....X", "X.X.o.X", "X.....X", "XXXXXXX"],
	"AGUA":     ["...X...", "...X...", "..XoX..", ".XoooX.", ".XoooX.", "..XXX..", "......."],
	"VEDACAO":  ["XXXXXXX", "X.....X", "X.....X", "X..X..X", "X..XX.X", "X...X.X", "XXXX.XX"],
	"RISCO":    ["......X", ".....X.", "....X..", "...X...", "..X....", ".X.....", "X......"],
	"VIBRACAO": ["X.X.X.X", ".......", "X.X.X.X", ".......", "X.X.X.X", ".......", "X.X.X.X"],
	"AMASSADO": [".......", ".XXXXX.", "XoooooX", "Xo...oX", "XoooooX", ".XXXXX.", "......."],
	"DEFEITO":  ["X.....X", ".X...X.", "..X.X..", "...X...", "..X.X..", ".X...X.", "X.....X"],
	"SUJEIRA":  [".......", "..ooo..", ".oooXo.", ".ooooo.", "..oo...", ".......", "......."],
	"REFLEXO":  ["......o", ".....o.", "....o..", "...o...", "..o....", ".o.....", "o......"],
	"FITA":     [".......", ".......", "ooooooo", "ooooooo", ".......", ".......", "......."],
}

var barco: Dictionary = {}
var rodada: Dictionary = {}        # rodada aberta: barcoId, gate, marcas
var pontos := {}                    # id -> {nome, x, y}
var segundos := 45
var resultado: Dictionary = {}     # depois da ordem: o que era cada marca
var aberta := false
var fonte: Font
var fonte_titulo: Font

var _marcadas := {}                 # id da marca -> true
var _inicio := 0.0
var _enviada := false
var _area_casco := Rect2()
var _botoes_marcas := {}            # id -> Button sem fundo (teclado e controle)
var _barra: HBoxContainer
var _botao_concluir: Button
var _lista: VBoxContainer


func _ready() -> void:
	fonte = ThemeDB.get_project_theme().default_font
	fonte_titulo = load(Tokens.FONTE_TITULO)
	visible = false
	mouse_filter = Control.MOUSE_FILTER_STOP
	_barra = HBoxContainer.new()
	_barra.add_theme_constant_override("separation", 4)
	add_child(_barra)
	_lista = VBoxContainer.new()
	_lista.add_theme_constant_override("separation", 0)
	add_child(_lista)
	resized.connect(_posicionar)


## Começa a rodada do barco: marcas sem resultado, cronômetro zerado.
func abrir(barco_novo: Dictionary, rodada_nova: Dictionary, pontos_lista: Array, segundos_rodada: int) -> void:
	barco = barco_novo
	rodada = rodada_nova
	segundos = segundos_rodada
	pontos.clear()
	for p in pontos_lista:
		pontos[p["id"]] = p
	resultado = {}
	_marcadas.clear()
	_enviada = false
	_inicio = Time.get_ticks_msec() / 1000.0
	aberta = true
	Movimento.abrir(self)
	_montar_botoes()
	_montar_lista()
	queue_redraw()


## Mostra o que o motor devolveu: cada marca com o que ela era.
func mostrar_resultado(r: Dictionary) -> void:
	resultado = r
	_montar_botoes()
	_montar_lista()
	for m in r["marcas"]:
		if m["resultado"] == "ACHOU":
			Sons.tocar("nc_encontrada")
			Impacto.hitstop(4)
			break
	for m in r["marcas"]:
		if m["resultado"] == "ESCAPOU":
			Sons.tocar("reprovar")
			Impacto.tremor(2.0)
			break
	queue_redraw()


func fechar() -> void:
	if aberta:
		aberta = false
		Movimento.fechar(self)
		for b in _botoes_marcas.values():
			b.queue_free()
		_botoes_marcas.clear()


func restante() -> float:
	return maxf(0.0, segundos - (Time.get_ticks_msec() / 1000.0 - _inicio))


func _process(_delta: float) -> void:
	if not aberta:
		return
	queue_redraw()
	if resultado.is_empty() and not _enviada and restante() <= 0.0:
		_enviar()   # acabou o tempo: vale o que foi marcado


func _enviar() -> void:
	if _enviada:
		return
	_enviada = true
	concluir.emit(str(rodada["barcoId"]), _marcadas.keys())


func _alternar(id: String) -> void:
	if not resultado.is_empty() or _enviada:
		return
	if _marcadas.has(id):
		_marcadas.erase(id)
	else:
		_marcadas[id] = true
		Sons.tocar("clique")
	_montar_lista()
	queue_redraw()


# ---------------------------------------------------------------- layout e controles

func _posicionar() -> void:
	var largura_casco := PixelArt.LARGURA_CASCO * ESCALA
	var altura_casco := PixelArt.COMPRIMENTO_CASCO * ESCALA
	var topo := 30.0
	_area_casco = Rect2(Vector2(24 + 40, topo + floorf((size.y - topo - ALTURA_BOTOES - altura_casco) / 2)),
			Vector2(largura_casco, altura_casco))
	_barra.position = Vector2(6, size.y - ALTURA_BOTOES + 2)
	_barra.size = Vector2(size.x - 12, ALTURA_BOTOES - 4)
	_lista.position = Vector2(_area_casco.end.x + 56, topo + 4)
	_lista.size = Vector2(size.x - _lista.position.x - 8, 0)
	for id in _botoes_marcas:
		_botoes_marcas[id].position = _centro_marca(_marca(id)) - Vector2(TAMANHO_MARCA, TAMANHO_MARCA) / 2 - Vector2(2, 2)


func _montar_botoes() -> void:
	for filho in _barra.get_children():
		filho.queue_free()
	for b in _botoes_marcas.values():
		b.queue_free()
	_botoes_marcas.clear()
	if resultado.is_empty():
		var sair := Button.new()
		sair.text = "Deixar com o inspetor"
		sair.pressed.connect(func(): cancelar.emit())
		_barra.add_child(sair)
		_botao_concluir = Button.new()
		_botao_concluir.text = "Concluir rodada"
		_botao_concluir.theme_type_variation = "BotaoAcao"
		_botao_concluir.pressed.connect(_enviar)
		_barra.add_child(_botao_concluir)
		for m in rodada["marcas"]:
			var b := Button.new()
			b.flat = true
			b.custom_minimum_size = Vector2(TAMANHO_MARCA + 4, TAMANHO_MARCA + 4)
			b.size = b.custom_minimum_size
			b.tooltip_text = str(pontos.get(m["pontoId"], {}).get("nome", ""))
			b.pressed.connect(_alternar.bind(str(m["id"])))
			add_child(b)
			_botoes_marcas[str(m["id"])] = b
		_posicionar()
		_focar(_botoes_marcas.values()[0] if not _botoes_marcas.is_empty() else _botao_concluir)
	else:
		var seguir := Button.new()
		seguir.text = "Continuar"
		seguir.theme_type_variation = "BotaoAcao"
		seguir.pressed.connect(func(): continuar.emit())
		_barra.add_child(seguir)
		_focar(seguir)


func _focar(c: Control) -> void:
	await get_tree().process_frame
	if is_instance_valid(c) and c.is_visible_in_tree():
		c.grab_focus()


## Lista à direita: durante a rodada, uma linha de instrução; no resultado, uma linha por marca.
func _montar_lista() -> void:
	for filho in _lista.get_children():
		filho.queue_free()
	var largura := _lista.size.x
	if resultado.is_empty():
		_linha("Clique nas marcas que são defeito.", Tokens.TEXTO, largura)
		_linha("Nem toda marca é defeito.", Tokens.TEXTO_CALMO, largura)
		_linha("Marcadas: %d de %d" % [_marcadas.size(), (rodada["marcas"] as Array).size()], Tokens.TEXTO_CALMO, largura)
		return
	var achou := 0
	var passou := 0
	for m in resultado["marcas"]:
		var nome := str(pontos.get(m["pontoId"], {}).get("nome", m["pontoId"])).to_lower()
		match str(m["resultado"]):
			"ACHOU":
				achou += 1
				_linha("Achou: %s (%s), %s" % [str(m["descricao"]).to_lower(), NOMES_GRAVIDADE.get(str(m["gravidade"]), ""), nome],
						Tokens.OK, largura)
			"ESCAPOU":
				passou += 1
				_linha("Passou: %s (garantia ~R$ %s mil)" % [str(m["descricao"]).to_lower(), _mil(m["custoGarantia"])],
						Tokens.NC, largura)
			"FALSO_ALARME":
				_linha("Falso alarme: %s (+1 h de espera)" % NOMES_FALSAS.get(str(m["aparencia"]), "não era defeito"),
						Tokens.ATENCAO, largura)
			"IGNOROU":
				_linha("Certo: %s não é defeito" % NOMES_FALSAS.get(str(m["aparencia"]), "a marca"), Tokens.TEXTO_CALMO, largura)
	if achou + passou == 0:
		_linha("Nenhum defeito neste gate.", Tokens.OK, largura)


func _linha(texto: String, cor: Color, largura: float) -> void:
	var r := RichTextLabel.new()
	r.bbcode_enabled = true
	r.fit_content = true
	r.scroll_active = false
	r.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	r.custom_minimum_size.x = largura
	r.mouse_filter = Control.MOUSE_FILTER_IGNORE
	r.add_theme_color_override("default_color", cor)
	r.text = Texto.bbcode(texto)
	_lista.add_child(r)


# ---------------------------------------------------------------- desenho

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Tokens.FUNDO)
	draw_rect(Rect2(0, size.y - ALTURA_BOTOES, size.x, ALTURA_BOTOES), Tokens.PAINEL)
	if rodada.is_empty():
		return
	var titulo := "RODADA DE INSPEÇÃO · %s · gate %s" % [rodada["barcoId"], rodada["gate"]]
	draw_string(fonte_titulo, Vector2(6, 19), titulo, HORIZONTAL_ALIGNMENT_LEFT, -1, Tokens.TAMANHO_TITULO, Tokens.TEXTO)
	if resultado.is_empty():
		_desenhar_tempo()

	# o barco visto de cima, proa para cima, com a água em volta
	draw_rect(_area_casco.grow(10), Paleta.AGUA if barco.get("gateAtual", "") in ["PI", "TM", "ET"] else Tokens.PAINEL)
	var etapa := PixelArt.Etapa.ABERTO if barco.get("gateAtual", "") == "DM" else Planta._etapa(str(barco.get("gateAtual", "")))
	draw_texture_rect(PixelArt.casco(etapa), _area_casco, false)
	var por_marca := {}
	for m in resultado.get("marcas", []):
		por_marca[m["id"]] = m
	for m in rodada["marcas"]:
		var centro := _centro_marca(m)
		_desenhar_icone(centro, str(m["aparencia"]))
		var r := Rect2(centro - Vector2(TAMANHO_MARCA, TAMANHO_MARCA) / 2, Vector2(TAMANHO_MARCA, TAMANHO_MARCA)).grow(2)
		if por_marca.has(m["id"]):
			_desenhar_carimbo(r, str(por_marca[m["id"]]["resultado"]))
		elif _marcadas.has(m["id"]):
			draw_rect(r, Tokens.FOCO, false, 1.0)
			draw_rect(r.grow(1), Tokens.FOCO, false, 1.0)


func _desenhar_tempo() -> void:
	var fracao := restante() / maxf(1.0, segundos)
	var barra := Rect2(size.x - 186, 8, 140, 6)
	draw_rect(barra, Tokens.PAINEL)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * fracao, barra.size.y)), Tokens.ATENCAO if fracao < 0.3 else Tokens.MARCA_CLARA)
	Texto.desenhar(self, Vector2(size.x - 40, 19), "%d s" % ceili(restante()), Tokens.ATENCAO if fracao < 0.3 else Tokens.TEXTO)


## Resultado na própria marca: verde achou, vermelho passou, laranja falso alarme; a falsa ignorada fica sem nada.
func _desenhar_carimbo(r: Rect2, resultado_marca: String) -> void:
	match resultado_marca:
		"ACHOU":
			draw_rect(r, Tokens.OK, false, 2.0)
		"ESCAPOU":
			draw_rect(r, Tokens.NC, false, 2.0)
			draw_line(r.position, r.end, Tokens.NC, 1.0)
		"FALSO_ALARME":
			draw_rect(r, Tokens.ATENCAO, false, 2.0)


func _desenhar_icone(centro: Vector2, aparencia: String) -> void:
	var linhas: Array = ICONES.get(aparencia, ICONES["DEFEITO"])
	var principal := Tokens.FUNDO
	var apoio := Color("#5a6988")
	var origem := (centro - Vector2(TAMANHO_MARCA, TAMANHO_MARCA) / 2).floor()
	for y in linhas.size():
		var linha := str(linhas[y])
		for x in linha.length():
			var c := linha[x]
			if c == "X":
				draw_rect(Rect2(origem + Vector2(x * PIXEL, y * PIXEL), Vector2(PIXEL, PIXEL)), principal)
			elif c == "o":
				draw_rect(Rect2(origem + Vector2(x * PIXEL, y * PIXEL), Vector2(PIXEL, PIXEL)), apoio)


func _centro_marca(m: Dictionary) -> Vector2:
	var p: Dictionary = pontos.get(m["pontoId"], {"x": 0.5, "y": 0.5})
	return (_area_casco.position + Vector2(float(p["x"]) * _area_casco.size.x, float(p["y"]) * _area_casco.size.y)).floor()


func _marca(id: String) -> Dictionary:
	for m in rodada.get("marcas", []):
		if m["id"] == id:
			return m
	return {}


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		for m in rodada.get("marcas", []):
			var r := Rect2(_centro_marca(m) - Vector2(TAMANHO_MARCA, TAMANHO_MARCA) / 2, Vector2(TAMANHO_MARCA, TAMANHO_MARCA)).grow(3)
			if r.has_point(event.position):
				_alternar(str(m["id"]))
				accept_event()
				return


static func _mil(valor) -> String:
	return str(int(round(float(valor) / 1000.0)))
