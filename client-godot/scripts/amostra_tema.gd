extends Control
## Cena de amostra do design system (scenes/amostra_tema.tscn): todos os componentes e estados lado a lado,
## e som e impacto, para conferir o visual e a sensação. Tudo vem do tema e do Tokens; nada de cor escrita aqui.
##   Jogar Marellar.bat abre o jogo; a amostra abre pelo Godot:
##   Godot_v4.7.2-stable_win64.exe --path marellar/client-godot res://scenes/amostra_tema.tscn

const ESTADOS := [["normal", "normal"], ["hover", "sobre"], ["focus", "foco"], ["pressed", "apertado"], ["disabled", "desativado"]]

var _quadrado: ColorRect      # anda sozinho: para durante o hitstop
var _contador: Label          # roda sempre: conta os quadros parados
var _janela: PanelContainer   # painel para conferir o abrir/fechar (Movimento)
var _parados := 0
var _x := 0.0


func _ready() -> void:
	var fundo := ColorRect.new()
	fundo.color = Tokens.FUNDO
	fundo.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	add_child(fundo)
	var abas := TabContainer.new()
	abas.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	abas.offset_left = Tokens.GRADE / 2
	abas.offset_top = Tokens.GRADE / 2
	abas.offset_right = -Tokens.GRADE / 2
	abas.offset_bottom = -Tokens.GRADE / 2
	add_child(abas)
	abas.add_child(_componentes())
	abas.add_child(_som_e_impacto())
	var relogio := Node.new()   # conta quadros parados mesmo com a árvore pausada
	relogio.process_mode = Node.PROCESS_MODE_ALWAYS
	relogio.set_script(_RELOGIO)
	relogio.set_meta("amostra", self)
	add_child(relogio)


# ---------------------------------------------------------------- componentes

func _componentes() -> Control:
	var pagina := VBoxContainer.new()
	pagina.name = "Componentes"
	pagina.add_theme_constant_override("separation", 3)

	var titulo := Label.new()
	titulo.text = "AÇÃO E INSPEÇÃO · 1.900 HH · 5/5"
	titulo.theme_type_variation = "Titulo"
	pagina.add_child(titulo)

	# botões: um estado por coluna (os estados forçados são desenhados com o estilo do tema)
	var grade := GridContainer.new()
	grade.columns = ESTADOS.size() + 1
	grade.add_theme_constant_override("h_separation", 4)
	grade.add_theme_constant_override("v_separation", 3)
	pagina.add_child(grade)
	grade.add_child(_rotulo("", "RotuloCalmo"))
	for e in ESTADOS:
		grade.add_child(_rotulo(e[1], "RotuloCalmo"))
	for tipo in [["Button", "Botão"], ["BotaoAcao", "Ação"], ["OptionButton", "Lista"], ["LineEdit", "Campo"]]:
		grade.add_child(_rotulo(tipo[1], "RotuloCalmo"))
		for e in ESTADOS:
			grade.add_child(_EstadoFixo.criar(tipo[0], e[0], "Inspeção" if tipo[0] != "LineEdit" else "ç ã õ É"))

	# significado das cores
	var cores := HBoxContainer.new()
	cores.add_theme_constant_override("separation", 10)
	pagina.add_child(cores)
	for v in [["Label", "Texto"], ["RotuloCalmo", "Calmo"], ["RotuloOk", "OK"], ["RotuloAtencao", "Atenção"], ["RotuloNc", "NC"]]:
		cores.add_child(_rotulo(v[1], v[0]))
	var leve := _rotulo("NC leve", "Label")
	leve.add_theme_color_override("font_color", Tokens.NC_LEVE)
	cores.add_child(leve)
	var marca := _rotulo("Marca", "Label")
	marca.add_theme_color_override("font_color", Tokens.MARCA_CLARA)
	cores.add_child(marca)

	var barras := HBoxContainer.new()
	barras.add_theme_constant_override("separation", 6)
	pagina.add_child(barras)
	for v in [["ProgressBar", 0.7], ["BarraOk", 1.0], ["BarraAtencao", 0.5], ["BarraNc", 0.2]]:
		var b := ProgressBar.new()
		b.theme_type_variation = v[0]
		b.show_percentage = false
		b.max_value = 1.0
		b.value = v[1]
		b.custom_minimum_size = Vector2(Tokens.grade(9), Tokens.GRADE)
		barras.add_child(b)

	# painéis, dica e números
	var linha := HBoxContainer.new()
	linha.add_theme_constant_override("separation", 6)
	pagina.add_child(linha)
	var painel := PanelContainer.new()
	painel.add_child(_rotulo("Painel", "Label"))
	linha.add_child(painel)
	var janela := PanelContainer.new()
	janela.theme_type_variation = "Janela"
	janela.add_child(_rotulo("Janela", "Label"))
	linha.add_child(janela)
	linha.add_child(_rotulo("Dica sobre a cena", "RotuloDica"))
	var numeros := RichTextLabel.new()
	numeros.bbcode_enabled = true
	numeros.fit_content = true
	numeros.scroll_active = false
	numeros.autowrap_mode = TextServer.AUTOWRAP_OFF
	numeros.custom_minimum_size.x = Tokens.grade(22)
	numeros.text = Texto.bbcode("Caixa R$ 3,00 mi · moral 75% · 5/5")
	linha.add_child(numeros)
	return pagina


func _rotulo(texto: String, variacao: String) -> Label:
	var l := Label.new()
	l.text = texto
	l.theme_type_variation = variacao
	return l


## Botão (ou campo) parado num estado, desenhado com os estilos do tema: dá para ver hover, foco e
## apertado lado a lado, o que um botão de verdade não mostra ao mesmo tempo.
class _EstadoFixo extends Control:
	var tipo := ""
	var estado := ""
	var texto := ""

	static func criar(tipo_: String, estado_: String, texto_: String) -> Control:
		var c := _EstadoFixo.new()
		c.tipo = tipo_
		c.estado = estado_
		c.texto = texto_
		c.custom_minimum_size = Vector2(Tokens.grade(11), 22)
		return c

	func _draw() -> void:
		var base := "LineEdit" if tipo == "LineEdit" else tipo
		var r := Rect2(Vector2.ZERO, size)
		var nome := estado
		if base == "LineEdit":
			nome = {"hover": "normal", "pressed": "normal", "disabled": "read_only"}.get(estado, estado)
		var fundo := "normal" if nome == "focus" else nome
		draw_style_box(get_theme_stylebox(fundo, base), r)
		if nome == "focus":
			draw_style_box(get_theme_stylebox("focus", base), r)
		var cor_nome := {"normal": "font_color", "hover": "font_hover_color", "focus": "font_focus_color",
			"pressed": "font_pressed_color", "disabled": "font_disabled_color", "read_only": "font_uneditable_color"}
		var cor := get_theme_color(cor_nome.get(nome, "font_color"), base)
		var f := get_theme_default_font()
		var deslocamento := 1.0 if nome == "pressed" else 0.0
		draw_string(f, Vector2(4, 16 + deslocamento), texto, HORIZONTAL_ALIGNMENT_CENTER if base != "LineEdit" else HORIZONTAL_ALIGNMENT_LEFT,
				size.x - 8, Tokens.TAMANHO_UI, cor)


# ---------------------------------------------------------------- som e impacto

func _som_e_impacto() -> Control:
	var coluna := VBoxContainer.new()
	coluna.name = "Som e impacto"
	coluna.add_theme_constant_override("separation", Tokens.GRADE / 2)
	var dica := Label.new()
	dica.text = "Um som por evento. O hitstop para o quadrado; som e contador seguem."
	dica.theme_type_variation = "RotuloCalmo"
	coluna.add_child(dica)

	var grade_sons := GridContainer.new()
	grade_sons.columns = 5
	coluna.add_child(grade_sons)
	for evento in Sons.ARQUIVOS:
		var b := Button.new()
		b.text = evento
		b.pressed.connect(Sons.tocar.bind(evento))
		grade_sons.add_child(b)

	var impacto := HBoxContainer.new()
	coluna.add_child(impacto)
	for q in [3, 6]:
		var b := Button.new()
		b.text = "Hitstop %d" % q
		b.pressed.connect(Impacto.hitstop.bind(q))
		impacto.add_child(b)
	for i in [1.0, 3.0]:
		var b := Button.new()
		b.text = "Tremor %d" % int(i)
		b.pressed.connect(Impacto.tremor.bind(i))
		impacto.add_child(b)
	var junto := Button.new()
	junto.text = "Aprovar (som + hitstop + tremor)"
	junto.theme_type_variation = "BotaoAcao"
	junto.pressed.connect(func():
		Sons.tocar("aprovar")
		Impacto.hitstop(4)
		Impacto.tremor(2.0))
	impacto.add_child(junto)

	var linha := HBoxContainer.new()
	coluna.add_child(linha)
	var abre := Button.new()
	abre.text = "Abrir / fechar painel"
	abre.pressed.connect(func():
		if Movimento.aberto(_janela):
			Movimento.fechar(_janela)
		else:
			Movimento.abrir(_janela))
	linha.add_child(abre)
	_contador = Label.new()
	_contador.process_mode = Node.PROCESS_MODE_ALWAYS
	linha.add_child(_contador)

	var pista := ColorRect.new()
	pista.color = Tokens.PAINEL
	pista.custom_minimum_size = Vector2(320, 12)
	coluna.add_child(pista)
	_quadrado = ColorRect.new()
	_quadrado.color = Tokens.ACAO
	_quadrado.size = Vector2(8, 8)
	_quadrado.position = Vector2(0, 2)
	pista.add_child(_quadrado)

	_janela = PanelContainer.new()
	_janela.theme_type_variation = "Janela"
	_janela.visible = false
	var texto := Label.new()
	texto.text = "Painel: abre e fecha em %d ms, saída suave." % int(Tokens.MOV_UI * 1000)
	_janela.add_child(texto)
	coluna.add_child(_janela)
	return coluna


func _process(delta: float) -> void:
	if _quadrado == null:
		return
	_x = fmod(_x + delta * 120.0, 312.0)
	_quadrado.position.x = floorf(_x)


func _quadro_sempre() -> void:
	if get_tree().paused:
		_parados += 1
	if _contador != null:
		_contador.text = "quadros parados pelo hitstop: %d" % _parados


const _RELOGIO := preload("res://scripts/amostra_relogio.gd")
