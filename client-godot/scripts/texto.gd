class_name Texto
extends RefCounted
## Texto da interface com os números em Jersey 10 (token fonte.numero). A Pixelify Sans sem suavização
## desenha o "5" parecido com "S" e o "3" com "8"; por isso os números de valor (caixa, moral, HH, R$,
## datas, contagens) saem em Jersey, na mesma linha de base do texto. Botões continuam só em Pixelify.
##   Texto.desenhar(self, Vector2(8, 16), "Caixa R$ 3,00 mi", Tokens.TEXTO)
##   rotulo_rico.text = Texto.bbcode("Moral 75%")

static var _fonte_texto: Font
static var _fonte_numero: Font
static var _numero: RegEx


static func _preparar() -> void:
	if _numero == null:
		_fonte_texto = ThemeDB.get_project_theme().default_font
		_fonte_numero = load(Tokens.FONTE_NUMERO)
		# números separados por um espaço ficam num trecho só ("d1 07h"): o espaço da Pixelify é estreito
		# e "1 07" pareceria "107".
		_numero = RegEx.create_from_string("[0-9][0-9.,/%:]*( [0-9][0-9.,/%:]*)*")


## Divide o texto em trechos: [[trecho, é_número], ...].
static func partes(texto: String) -> Array:
	_preparar()
	var lista := []
	var inicio := 0
	for m in _numero.search_all(texto):
		if m.get_start() > inicio:
			lista.append([texto.substr(inicio, m.get_start() - inicio), false])
		lista.append([m.get_string(), true])
		inicio = m.get_end()
	if inicio < texto.length():
		lista.append([texto.substr(inicio), false])
	return lista


static func largura(texto: String, tamanho := Tokens.TAMANHO_UI) -> float:
	var total := 0.0
	for p in partes(texto):
		total += _fonte(p[1]).get_string_size(p[0], HORIZONTAL_ALIGNMENT_LEFT, -1, _tamanho(p[1], tamanho)).x
	return total


## Desenha numa linha. `pos` é a linha de base (como draw_string). Com alinhamento à direita ou ao centro,
## `largura_max` é a largura da área (a partir de pos.x). Devolve a largura desenhada.
static func desenhar(c: CanvasItem, pos: Vector2, texto: String, cor: Color,
		alinhamento := HORIZONTAL_ALIGNMENT_LEFT, largura_max := -1.0, tamanho := Tokens.TAMANHO_UI) -> float:
	var total := largura(texto, tamanho)
	var x := pos.x
	if alinhamento == HORIZONTAL_ALIGNMENT_RIGHT and largura_max > 0:
		x = pos.x + largura_max - total
	elif alinhamento == HORIZONTAL_ALIGNMENT_CENTER and largura_max > 0:
		x = pos.x + floorf((largura_max - total) / 2.0)
	for p in partes(texto):
		var f := _fonte(p[1])
		var t := _tamanho(p[1], tamanho)
		c.draw_string(f, Vector2(floorf(x), pos.y), p[0], HORIZONTAL_ALIGNMENT_LEFT, -1, t, cor)
		x += f.get_string_size(p[0], HORIZONTAL_ALIGNMENT_LEFT, -1, t).x
	return total


## O mesmo para RichTextLabel (bbcode_enabled): números com [font] da Jersey.
static func bbcode(texto: String) -> String:
	var saida := ""
	for p in partes(texto):
		var trecho: String = p[0].replace("[", "[lb]")
		if p[1]:
			saida += "[font=%s][font_size=%d]%s[/font_size][/font]" % [Tokens.FONTE_NUMERO, Tokens.TAMANHO_NUMERO, trecho]
		else:
			saida += trecho
	return saida


static func _fonte(eh_numero: bool) -> Font:
	return _fonte_numero if eh_numero else _fonte_texto


static func _tamanho(eh_numero: bool, tamanho_texto: int) -> int:
	return Tokens.TAMANHO_NUMERO if eh_numero else tamanho_texto
