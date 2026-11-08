class_name Sprites
extends RefCounted
## Sprites provisórios desenhados em código, pixel a pixel, na paleta Endesga 32.
## Cada função desenha num CanvasItem já posicionado; 1 unidade = 1 pixel da resolução base (640x360).

const ETAPA_MOLDE := 0      # RE, DM: casco sendo laminado dentro do molde (zona suja)
const ETAPA_BERCO := 1      # BA, BF: casco no berço, montagem
const ETAPA_PRONTO := 2     # ST, CT: barco montado, em seco
const ETAPA_PISCINA := 3    # PI: collaudo na piscina interna
const ETAPA_CARRETA := 4    # TR: carreta de Osasco até a marina
const ETAPA_AGUA := 5       # TM, ET e entregue: barco no mar


static func etapa_do_gate(gate: String, entregue: bool) -> int:
	if entregue:
		return ETAPA_AGUA
	match gate:
		"RE", "DM":
			return ETAPA_MOLDE
		"BA", "BF":
			return ETAPA_BERCO
		"ST", "CT":
			return ETAPA_PRONTO
		"PI":
			return ETAPA_PISCINA
		"TR":
			return ETAPA_CARRETA
	return ETAPA_AGUA


## Barco de 33 pés (~5 tiles = 80 px). `origem` é o canto esquerdo da linha d'água.
static func barco(c: CanvasItem, origem: Vector2, etapa: int, progresso: float, t: float) -> void:
	var o := origem.floor()
	var balanco := 0.0
	if etapa == ETAPA_AGUA:
		balanco = 1.0 if int(t * 1.5) % 2 == 0 else 0.0
		_agua(c, o + Vector2(-8, 4), 100, t)
	elif etapa == ETAPA_PISCINA:
		balanco = 1.0 if int(t * 0.8) % 2 == 0 else 0.0
		_piscina(c, o, t)
	elif etapa == ETAPA_CARRETA:
		_carreta(c, o, t)
		o.y -= 7
	o.y -= balanco

	if etapa == ETAPA_MOLDE:
		# molde marrom; o casco branco vai aparecendo conforme a laminação avança
		c.draw_rect(Rect2(o + Vector2(-4, -14), Vector2(90, 18)), Paleta.PRODUCAO_SUJA_ESCURA)
		c.draw_rect(Rect2(o + Vector2(-2, -12), Vector2(86, 14)), Paleta.PRODUCAO_SUJA)
		var largura := int(70 * clampf(progresso, 0.08, 1.0))
		_casco(c, o, largura, false)
		return

	if etapa == ETAPA_BERCO:
		# berço de madeira e metal
		for x in [8, 34, 60]:
			c.draw_rect(Rect2(o + Vector2(x, 0), Vector2(4, 6)), Paleta.MADEIRA)
		c.draw_rect(Rect2(o + Vector2(2, 6), Vector2(72, 2)), Paleta.METAL)

	_casco(c, o, 70, true)
	if etapa >= ETAPA_PRONTO:
		# cabine com vidros
		c.draw_rect(Rect2(o + Vector2(22, -20), Vector2(30, 7)), Paleta.CASCO)
		c.draw_rect(Rect2(o + Vector2(24, -19), Vector2(26, 3)), Paleta.AGUA_ESCURA)
		c.draw_rect(Rect2(o + Vector2(40, -24), Vector2(2, 4)), Paleta.METAL)
	if etapa == ETAPA_PISCINA:
		# parte de baixo do casco submersa: a água passa na frente
		var nivel := origem.floor() + Vector2(-9, -4)
		c.draw_rect(Rect2(nivel, Vector2(102, 5)), Color(Paleta.AGUA_ESCURA, 0.85))
		c.draw_rect(Rect2(nivel, Vector2(102, 1)), Paleta.AGUA)


static func _casco(c: CanvasItem, o: Vector2, comprimento: int, com_convés: bool) -> void:
	var fim := float(comprimento)
	var pontos := PackedVector2Array([
		o + Vector2(0, -13), o + Vector2(fim, -13), o + Vector2(fim + 14, -9),
		o + Vector2(fim + 4, 0), o + Vector2(3, 0)])
	c.draw_colored_polygon(pontos, Paleta.CASCO)
	var contorno := pontos.duplicate()
	contorno.append(pontos[0])
	c.draw_polyline(contorno, Paleta.CASCO_SOMBRA, 1.0)
	c.draw_line(o + Vector2(2, -4), o + Vector2(fim + 8, -4), Paleta.AGUA_ESCURA, 1.0)  # faixa do costado
	if com_convés:
		c.draw_rect(Rect2(o + Vector2(4, -13), Vector2(fim - 4, 2)), Paleta.CASCO_SOMBRA)


## Piscina interna de concreto: borda clara, água parada com reflexo e escada.
static func _piscina(c: CanvasItem, o: Vector2, t: float) -> void:
	var p := o + Vector2(-12, -7)
	c.draw_rect(Rect2(p, Vector2(108, 18)), Paleta.PISO_ESCURO)                 # borda
	c.draw_rect(Rect2(p + Vector2(3, 3), Vector2(102, 13)), Paleta.AGUA_ESCURA)  # tanque
	c.draw_rect(Rect2(p + Vector2(3, 3), Vector2(102, 2)), Paleta.AGUA)          # linha d'água
	c.draw_rect(Rect2(p + Vector2(0, 0), Vector2(108, 1)), Paleta.PISO)
	var fase := int(t * 2) % 6
	for x in range(6 + fase, 100, 12):
		c.draw_rect(Rect2(p + Vector2(x, 9 + (x / 12) % 2 * 3), Vector2(4, 1)), Paleta.AGUA)
	for y in [2, 6, 10]:  # escada
		c.draw_rect(Rect2(p + Vector2(98, y), Vector2(5, 1)), Paleta.METAL)
	c.draw_rect(Rect2(p + Vector2(98, -4), Vector2(1, 16)), Paleta.METAL)
	c.draw_rect(Rect2(p + Vector2(102, -4), Vector2(1, 16)), Paleta.METAL)


## Carreta rodoviária com rodas que giram.
static func _carreta(c: CanvasItem, o: Vector2, t: float) -> void:
	c.draw_rect(Rect2(o + Vector2(-6, -1), Vector2(96, 3)), Paleta.FUNDO_ESCURO)    # chassi
	for x in [6, 40, 66]:
		c.draw_rect(Rect2(o + Vector2(x, -7), Vector2(3, 6)), Paleta.MADEIRA)       # berços
	c.draw_rect(Rect2(o + Vector2(90, -4), Vector2(10, 2)), Paleta.METAL)          # engate
	var giro := int(t * 4) % 2
	for x in [52, 62, 72]:
		c.draw_rect(Rect2(o + Vector2(x, 2), Vector2(7, 7)), Paleta.FUNDO_ESCURO)
		c.draw_rect(Rect2(o + Vector2(x + 2 + giro, 4 + giro), Vector2(2, 2)), Paleta.METAL)


static func _agua(c: CanvasItem, o: Vector2, largura: int, t: float) -> void:
	c.draw_rect(Rect2(o, Vector2(largura, 10)), Paleta.AGUA_ESCURA)
	var fase := int(t * 3) % 8
	for x in range(fase, largura, 8):
		c.draw_rect(Rect2(o + Vector2(x, 2 + (x / 8) % 2 * 3), Vector2(3, 1)), Paleta.AGUA)


## Bonequinho de 6x11 px. `quadro` alterna a pose de trabalho (0 ou 1).
static func pessoa(c: CanvasItem, pe: Vector2, uniforme: Color, quadro: int) -> void:
	var o := pe.floor() + Vector2(-3, -11)
	c.draw_rect(Rect2(o + Vector2(1, 0), Vector2(4, 1)), Paleta.CABELO)
	c.draw_rect(Rect2(o + Vector2(1, 1), Vector2(4, 3)), Paleta.PELE)
	c.draw_rect(Rect2(o + Vector2(0, 4), Vector2(6, 4)), uniforme)
	# braço que trabalha
	var braco := Vector2(6, 4) if quadro == 0 else Vector2(6, 2)
	c.draw_rect(Rect2(o + braco, Vector2(1, 2)), Paleta.PELE_ESCURA)
	c.draw_rect(Rect2(o + Vector2(1, 8), Vector2(1, 3)), Paleta.FUNDO_ESCURO)
	c.draw_rect(Rect2(o + Vector2(4, 8), Vector2(1, 3)), Paleta.FUNDO_ESCURO)


## Inspetora com prancheta.
static func inspetor(c: CanvasItem, pe: Vector2, quadro: int) -> void:
	pessoa(c, pe, Paleta.AGUA, 0)
	var o := pe.floor() + Vector2(3, -8 - quadro)
	c.draw_rect(Rect2(o, Vector2(3, 4)), Paleta.TEXTO)
	c.draw_rect(Rect2(o + Vector2(1, 1), Vector2(1, 1)), Paleta.FUNDO_ESCURO)


## Selo vermelho de NC com a quantidade (forma além da cor, para acessibilidade).
static func selo_nc(c: CanvasItem, centro: Vector2, fonte: Font, quantidade: int, aceso: bool) -> void:
	var cor := Paleta.CRITICO if aceso else Paleta.NAO_CONFORME_ESCURO
	var o := centro.floor()
	c.draw_colored_polygon(PackedVector2Array([o + Vector2(0, -7), o + Vector2(7, 5), o + Vector2(-7, 5)]), cor)
	c.draw_string(fonte, o + Vector2(-2, 4), "!", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.TEXTO_CLARO)
	if quantidade > 1:
		c.draw_string(fonte, o + Vector2(8, 4), "x%d" % quantidade, HORIZONTAL_ALIGNMENT_LEFT, -1, 16, cor)
