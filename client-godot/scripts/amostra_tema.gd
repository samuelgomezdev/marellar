extends Control
## Cena de amostra do design system (scenes/amostra_tema.tscn), para conferir o visual e a sensação.
## Etapa 3: som e impacto. A etapa 4 acrescenta todos os componentes e estados lado a lado.
##   godot --path client-godot res://scenes/amostra_tema.tscn

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
	var coluna := VBoxContainer.new()
	coluna.position = Vector2(Tokens.GRADE, Tokens.GRADE)
	coluna.add_theme_constant_override("separation", Tokens.GRADE / 2)
	add_child(coluna)

	var titulo := Label.new()
	titulo.text = "SOM E IMPACTO"
	titulo.theme_type_variation = "Titulo"
	coluna.add_child(titulo)
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

	set_process(true)
	var relogio := Node.new()   # conta quadros parados mesmo com a árvore pausada
	relogio.process_mode = Node.PROCESS_MODE_ALWAYS
	relogio.set_script(_RELOGIO)
	relogio.set_meta("amostra", self)
	add_child(relogio)


func _process(delta: float) -> void:
	_x = fmod(_x + delta * 120.0, 312.0)
	_quadrado.position.x = floorf(_x)


func _quadro_sempre() -> void:
	if get_tree().paused:
		_parados += 1
	_contador.text = "quadros parados pelo hitstop: %d" % _parados


const _RELOGIO := preload("res://scripts/amostra_relogio.gd")
