extends SceneTree
## Gera assets/tema/marellar_theme.tres a partir de scripts/tokens.gd.
## Os cantos são StyleBoxTexture 9-slice com chanfro de 2 px; as texturas (8×8 px) são desenhadas aqui,
## pixel a pixel, e ficam embutidas no .tres. Não edite o .tres à mão: mude o Tokens e rode de novo.
##
##   godot --headless --path client-godot --script res://ferramentas/gerar_tema.gd

const T := preload("res://scripts/tokens.gd")
const DESTINO := "res://assets/tema/marellar_theme.tres"
const LADO := 8   # lado da textura: chanfro (2) + borda (1) + miolo + borda + chanfro

var tema := Theme.new()
static var _texturas := {}   # mesma combinação de cores = mesma textura no .tres


func _init() -> void:
	_geral()
	_rotulos()
	_paineis()
	_botoes()
	_abas()
	_campos()
	_barras_de_progresso()
	_dica()
	_rolagem()
	_separadores()
	DirAccess.make_dir_recursive_absolute(ProjectSettings.globalize_path(DESTINO.get_base_dir()))
	var erro := ResourceSaver.save(tema, DESTINO)
	if erro != OK:
		push_error("Não consegui salvar %s (erro %d)" % [DESTINO, erro])
		quit(1)
		return
	print("Tema gerado em ", DESTINO)
	quit()


# ---------------------------------------------------------------- texturas

## Caixa 8×8 com os quatro cantos chanfrados em CHANFRO px. Sem `borda`, o contorno tem a cor do miolo.
## `vazada`: só o contorno (o miolo fica transparente), usada no foco.
static func _imagem_caixa(miolo: Color, borda = null, vazada := false, borda_interna = null) -> Image:
	var img := Image.create_empty(LADO, LADO, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	var c := T.CHANFRO
	for y in LADO:
		for x in LADO:
			# distância até a borda mais próxima, contando o chanfro (diagonal)
			var dx := mini(x, LADO - 1 - x)
			var dy := mini(y, LADO - 1 - y)
			var d := mini(mini(dx, dy), dx + dy - c)
			if d < 0:
				continue                                     # canto cortado
			if d == 0:
				img.set_pixel(x, y, borda if borda != null else miolo)
			elif d == 1 and borda_interna != null:
				img.set_pixel(x, y, borda_interna)
			elif not vazada:
				img.set_pixel(x, y, miolo)
	return img


static func _textura(miolo: Color, borda = null, vazada := false, borda_interna = null) -> ImageTexture:
	var chave := [miolo, borda, vazada, borda_interna].hash()
	if not _texturas.has(chave):
		_texturas[chave] = ImageTexture.create_from_image(_imagem_caixa(miolo, borda, vazada, borda_interna))
	return _texturas[chave]


static func _caixa(miolo: Color, borda = null, margem_h := 4, margem_v := 1) -> StyleBoxTexture:
	var s := StyleBoxTexture.new()
	s.texture = _textura(miolo, borda)
	_margens_textura(s, T.CHANFRO + 1)
	s.content_margin_left = margem_h
	s.content_margin_right = margem_h
	s.content_margin_top = margem_v
	s.content_margin_bottom = margem_v
	return s


## Foco: contorno de 1 px em cor.foco e, por dentro, 1 px em cor.fundo, para o anel aparecer
## tanto em fundo escuro quanto em fundo claro (piso da fábrica, botão de ação).
static func _anel_foco() -> StyleBoxTexture:
	var s := StyleBoxTexture.new()
	s.texture = _textura(Color(0, 0, 0, 0), T.FOCO, true, T.FUNDO)
	_margens_textura(s, T.CHANFRO + 2)
	s.draw_center = false
	return s


static func _margens_textura(s: StyleBoxTexture, m: int) -> void:
	s.texture_margin_left = m
	s.texture_margin_right = m
	s.texture_margin_top = m
	s.texture_margin_bottom = m


static func _vazio() -> StyleBoxEmpty:
	return StyleBoxEmpty.new()


static func _icone_vazio() -> ImageTexture:
	var img := Image.create_empty(1, 1, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	return ImageTexture.create_from_image(img)


## Seta para baixo de 7×4 px, desenhada pixel a pixel (a do tema padrão do Godot é suavizada).
static func _seta(cor: Color) -> ImageTexture:
	var img := Image.create_empty(7, 4, false, Image.FORMAT_RGBA8)
	img.fill(Color(0, 0, 0, 0))
	for y in 4:
		for x in range(y, 7 - y):
			img.set_pixel(x, y, cor)
	return ImageTexture.create_from_image(img)


static func _apagado(c: Color) -> Color:
	return Color(c, 0.45)


# ---------------------------------------------------------------- tipos

func _geral() -> void:
	tema.default_font = _fonte(T.FONTE_UI)
	tema.default_font_size = T.TAMANHO_UI


func _fonte(caminho: String) -> FontFile:
	var f := load(caminho) as FontFile
	if f == null:
		push_error("Fonte não encontrada: %s (rode o import do projeto antes)" % caminho)
	return f


func _rotulos() -> void:
	tema.set_color("font_color", "Label", T.TEXTO)
	tema.set_color("font_shadow_color", "Label", Color(0, 0, 0, 0))
	tema.set_constant("line_spacing", "Label", 0)
	tema.set_stylebox("normal", "Label", _vazio())
	# Variações: o significado vem da cor, nunca o contrário.
	for v in [["RotuloCalmo", T.TEXTO_CALMO], ["RotuloOk", T.OK], ["RotuloAtencao", T.ATENCAO], ["RotuloNc", T.NC]]:
		tema.set_type_variation(v[0], "Label")
		tema.set_color("font_color", v[0], v[1])
	tema.set_type_variation("Titulo", "Label")
	tema.set_font("font", "Titulo", _fonte(T.FONTE_TITULO))
	tema.set_font_size("font_size", "Titulo", T.TAMANHO_TITULO)
	tema.set_color("font_color", "Titulo", T.TEXTO)


func _paineis() -> void:
	var m := T.GRADE
	tema.set_stylebox("panel", "Panel", _caixa(T.PAINEL, null, m, m))
	tema.set_stylebox("panel", "PanelContainer", _caixa(T.PAINEL, null, m, m))
	# Janela sobre a cena (conselho, saves, formulário): fundo escuro com contorno de painel.
	tema.set_type_variation("Janela", "PanelContainer")
	tema.set_stylebox("panel", "Janela", _caixa(T.FUNDO, T.PAINEL, m, m))
	tema.set_stylebox("panel", "ScrollContainer", _vazio())


func _estilo_botao(tipo: String, normal: StyleBox, hover: StyleBox, pressionado: StyleBox, desativado: StyleBox,
		texto: Color, texto_desativado: Color) -> void:
	tema.set_stylebox("normal", tipo, normal)
	tema.set_stylebox("hover", tipo, hover)
	tema.set_stylebox("pressed", tipo, pressionado)
	tema.set_stylebox("hover_pressed", tipo, pressionado)
	tema.set_stylebox("disabled", tipo, desativado)
	tema.set_stylebox("focus", tipo, _anel_foco())
	for cor in ["font_color", "font_hover_color", "font_pressed_color", "font_hover_pressed_color", "font_focus_color"]:
		tema.set_color(cor, tipo, texto)
	tema.set_color("font_disabled_color", tipo, texto_desativado)
	tema.set_color("icon_normal_color", tipo, texto)
	tema.set_color("icon_hover_color", tipo, texto)
	tema.set_color("icon_pressed_color", tipo, texto)
	tema.set_color("icon_disabled_color", tipo, texto_desativado)
	tema.set_constant("h_separation", tipo, 4)
	tema.set_constant("outline_size", tipo, 0)


func _botoes() -> void:
	# Pressionado (e botão ligado, como a velocidade escolhida) desce 1 px: a forma diz "apertado", não uma cor nova.
	var pressionado := _caixa(T.PAINEL, T.TEXTO)
	pressionado.content_margin_top += 1
	pressionado.content_margin_bottom -= 1
	for tipo in ["Button", "OptionButton"]:
		_estilo_botao(tipo, _caixa(T.FUNDO, T.PAINEL), _caixa(T.PAINEL, T.TEXTO_CALMO), pressionado,
				_caixa(T.FUNDO, _apagado(T.PAINEL)), T.TEXTO, _apagado(T.TEXTO_CALMO))
	tema.set_constant("arrow_margin", "OptionButton", 2)
	tema.set_icon("arrow", "OptionButton", _seta(T.TEXTO))

	# A única ação principal do painel (no máximo um BotaoAcao por painel).
	tema.set_type_variation("BotaoAcao", "Button")
	var acao_pressionado := _caixa(T.ACAO, T.FUNDO)
	acao_pressionado.content_margin_top += 1
	acao_pressionado.content_margin_bottom -= 1
	_estilo_botao("BotaoAcao", _caixa(T.ACAO), _caixa(T.ACAO, T.TEXTO), acao_pressionado,
			_caixa(T.FUNDO, _apagado(T.ACAO)), T.FUNDO, _apagado(T.ACAO))

	# Lista que abre do OptionButton.
	tema.set_stylebox("panel", "PopupMenu", _caixa(T.FUNDO, T.TEXTO_CALMO, 2, 2))
	tema.set_stylebox("hover", "PopupMenu", _caixa(T.PAINEL, null, 2, 0))
	tema.set_color("font_color", "PopupMenu", T.TEXTO_CALMO)
	tema.set_color("font_hover_color", "PopupMenu", T.TEXTO)
	tema.set_color("font_disabled_color", "PopupMenu", _apagado(T.TEXTO_CALMO))
	tema.set_color("font_separator_color", "PopupMenu", T.TEXTO_CALMO)
	tema.set_constant("v_separation", "PopupMenu", 0)
	tema.set_constant("outline_size", "PopupMenu", 0)


func _abas() -> void:
	var selecionada := _caixa(T.PAINEL, null, 6, 1)
	var outra := _caixa(T.FUNDO, T.PAINEL, 6, 1)
	var sobre := _caixa(T.PAINEL, T.TEXTO_CALMO, 6, 1)
	var desativada := _caixa(T.FUNDO, _apagado(T.PAINEL), 6, 1)
	for tipo in ["TabContainer", "TabBar"]:
		tema.set_stylebox("tab_selected", tipo, selecionada)
		tema.set_stylebox("tab_unselected", tipo, outra)
		tema.set_stylebox("tab_hovered", tipo, sobre)
		tema.set_stylebox("tab_disabled", tipo, desativada)
		tema.set_stylebox("tab_focus", tipo, _anel_foco())
		tema.set_color("font_selected_color", tipo, T.TEXTO)
		tema.set_color("font_unselected_color", tipo, T.TEXTO_CALMO)
		tema.set_color("font_hovered_color", tipo, T.TEXTO)
		tema.set_color("font_disabled_color", tipo, _apagado(T.TEXTO_CALMO))
		tema.set_constant("outline_size", tipo, 0)
	tema.set_stylebox("panel", "TabContainer", _caixa(T.PAINEL, null, T.GRADE, T.GRADE))
	tema.set_stylebox("tabbar_background", "TabContainer", _vazio())
	tema.set_constant("side_margin", "TabContainer", 0)


func _campos() -> void:
	tema.set_stylebox("normal", "LineEdit", _caixa(T.FUNDO, T.PAINEL))
	tema.set_stylebox("read_only", "LineEdit", _caixa(T.FUNDO, _apagado(T.PAINEL)))
	tema.set_stylebox("focus", "LineEdit", _anel_foco())
	tema.set_color("font_color", "LineEdit", T.TEXTO)
	tema.set_color("font_uneditable_color", "LineEdit", T.TEXTO_CALMO)
	tema.set_color("font_placeholder_color", "LineEdit", _apagado(T.TEXTO_CALMO))
	tema.set_color("font_selected_color", "LineEdit", T.TEXTO)
	tema.set_color("selection_color", "LineEdit", Color(T.MARCA_CLARA, 0.6))
	tema.set_color("caret_color", "LineEdit", T.TEXTO)
	tema.set_constant("caret_width", "LineEdit", 1)
	tema.set_constant("outline_size", "LineEdit", 0)


func _barras_de_progresso() -> void:
	tema.set_stylebox("background", "ProgressBar", _caixa(T.FUNDO, T.PAINEL, 0, 0))
	tema.set_stylebox("fill", "ProgressBar", _caixa(T.MARCA_CLARA, null, 0, 0))
	tema.set_color("font_color", "ProgressBar", T.TEXTO)
	tema.set_color("font_outline_color", "ProgressBar", T.FUNDO)
	tema.set_constant("outline_size", "ProgressBar", 0)
	# Barra com significado: progresso aprovado, em risco ou com NC.
	for v in [["BarraOk", T.OK], ["BarraAtencao", T.ATENCAO], ["BarraNc", T.NC]]:
		tema.set_type_variation(v[0], "ProgressBar")
		tema.set_stylebox("fill", v[0], _caixa(v[1], null, 0, 0))


func _dica() -> void:
	tema.set_stylebox("panel", "TooltipPanel", _caixa(T.FUNDO, T.TEXTO_CALMO, 4, 2))
	tema.set_color("font_color", "TooltipLabel", T.TEXTO)
	tema.set_color("font_shadow_color", "TooltipLabel", Color(0, 0, 0, 0))
	tema.set_constant("outline_size", "TooltipLabel", 0)


func _rolagem() -> void:
	var vazio := _icone_vazio()
	for tipo in ["VScrollBar", "HScrollBar"]:
		# 6 px de largura (3 + 3 de margem): fina, mas dá para pegar com o mouse.
		tema.set_stylebox("scroll", tipo, _caixa(T.FUNDO, T.PAINEL, 3, 3))
		tema.set_stylebox("scroll_focus", tipo, _anel_foco())
		tema.set_stylebox("grabber", tipo, _caixa(T.PAINEL, null, 3, 3))
		tema.set_stylebox("grabber_highlight", tipo, _caixa(T.TEXTO_CALMO, null, 3, 3))
		tema.set_stylebox("grabber_pressed", tipo, _caixa(T.TEXTO, null, 3, 3))
		for icone in ["increment", "increment_highlight", "increment_pressed", "decrement", "decrement_highlight", "decrement_pressed"]:
			tema.set_icon(icone, tipo, vazio)


func _separadores() -> void:
	for v in [["VSeparator", true], ["HSeparator", false]]:
		var linha := StyleBoxLine.new()
		linha.color = T.PAINEL
		linha.thickness = 1
		linha.vertical = v[1]
		tema.set_stylebox("separator", v[0], linha)
		tema.set_constant("separation", v[0], 4)
