class_name Planta
extends RefCounted
## Planta vista de cima (mockup telas/1_fabrica.png), um galpão por vez:
##   G2 · montagem: mezanino, baias com o barco a 90° e a popa para o mezanino, piscina interna e marina;
##   G1 · laminação: os moldes, pintura e móveis (galpão separado, produção suja).
## Os dois têm a rua interna e o almoxarifado com os paletes de kits.
## Só desenha o estado que veio do motor; devolve as áreas clicáveis de cada barco.

const BAIAS_G2 := 6
const ESTACOES_MOLDE := 2
const LARGURA_PISCINA := 116
const ALTURA_MEZANINO := 30
const ALTURA_RUA := 26
const ALTURA_PATIO := 50

const CORES_ITEM := {
	"resina": Paleta.PRODUCAO_SUJA,
	"gelcoat": Paleta.CASCO,
	"eletrica": Paleta.ALERTA,
	"vidro": Paleta.AGUA,
	"motor": Paleta.ARMAZEM,
}
const NOMES_ITEM := {
	"resina": "Resina",
	"gelcoat": "Gel",
	"eletrica": "Elétrica",
	"vidro": "Vidros",
	"motor": "Motores",
}


## Em que galpão o barco está agora: "G1" (laminação) ou "G2" (montagem, piscina, carreta, marina).
static func galpao_do_barco(b: Dictionary) -> String:
	return "G1" if str(b["gateAtual"]) in ["RE", "DM"] and b["status"] != "ENTREGUE" else "G2"


## Desenha o galpão pedido em `area` e devolve {id do barco: Rect2 clicável}.
static func desenhar(c: CanvasItem, area: Rect2, estado: Dictionary, selecionado: String, t: float,
		animar: bool, fonte: Font, galpao: String = "G2") -> Dictionary:
	var clicaveis := {}
	var lateral := Rect2(area.end.x - LARGURA_PISCINA - 4, area.position.y + 4, LARGURA_PISCINA,
			area.size.y - ALTURA_PATIO - 8)
	var chao := Rect2(area.position, Vector2(lateral.position.x - area.position.x - 4, area.size.y))
	var parede := Rect2(chao.position, Vector2(chao.size.x, ALTURA_MEZANINO))
	var patio := Rect2(chao.position.x, area.end.y - ALTURA_PATIO, chao.size.x, ALTURA_PATIO)
	var rua := Rect2(chao.position.x, patio.position.y - ALTURA_RUA, chao.size.x, ALTURA_RUA)
	var baias := Rect2(chao.position.x + 4, parede.end.y + 2, chao.size.x - 8, rua.position.y - parede.end.y - 4)

	var no_molde: Array = []
	var em_baia: Array = []
	var na_piscina: Array = []
	var na_carreta: Array = []
	var na_marina: Array = []
	var entregues := 0
	for b in estado["barcos"]:
		if b["status"] == "ENTREGUE":
			entregues += 1
			continue
		match str(b["gateAtual"]):
			"RE", "DM":
				no_molde.append(b)
			"PI":
				na_piscina.append(b)
			"TR":
				na_carreta.append(b)
			"TM", "ET":
				na_marina.append(b)
			_:
				em_baia.append(b)

	c.draw_rect(area, Paleta.PISO_ESCURO)
	_piso(c, chao)
	if galpao == "G1":
		_desenhar_g1(c, chao, parede, baias, lateral, no_molde, estado, selecionado, t, animar, fonte, clicaveis)
	else:
		_desenhar_mezanino(c, parede, estado, t, animar, fonte)
		var largura_baia := floorf(baias.size.x / BAIAS_G2)
		for i in BAIAS_G2:
			var baia := Rect2(baias.position.x + i * largura_baia, baias.position.y, largura_baia, baias.size.y)
			_faixa_seguranca(c, baia.position, baia.size.y)
			c.draw_string(fonte, Vector2(baia.position.x + 4, baia.end.y - 3), "B%d" % (i + 1), HORIZONTAL_ALIGNMENT_LEFT, -1, 16,
					Color(Paleta.PISO_ESCURO, 0.9))
			if i < em_baia.size():
				var b: Dictionary = em_baia[i]
				clicaveis[b["id"]] = _barco_na_baia(c, baia, b, estado, selecionado, t, animar, fonte)
		_faixa_seguranca(c, Vector2(baias.position.x + BAIAS_G2 * largura_baia, baias.position.y), baias.size.y)
		_desenhar_piscina(c, lateral, na_piscina, selecionado, t, fonte, clicaveis)
		_desenhar_marina(c, Rect2(lateral.position.x, lateral.end.y + 4, lateral.size.x, area.end.y - lateral.end.y - 6),
				na_marina, entregues, selecionado, t, fonte, clicaveis)

	_desenhar_rua(c, rua, t, animar)
	for b in na_carreta:
		if galpao == "G2":
			clicaveis[b["id"]] = _carreta(c, rua, b, selecionado, t)
	_desenhar_patio(c, patio, estado, fonte)
	return clicaveis


# ---------------------------------------------------------------- G1 · laminação

static func _desenhar_g1(c: CanvasItem, chao: Rect2, parede: Rect2, baias: Rect2, lateral: Rect2, moldes: Array,
		estado: Dictionary, selecionado: String, t: float, animar: bool, fonte: Font, clicaveis: Dictionary) -> void:
	# piso de produção suja, com manchas de resina
	c.draw_rect(chao, Color(Paleta.PRODUCAO_SUJA, 0.25))
	for k in 60:
		var pos := chao.position + Vector2(fposmod(k * 37.0, chao.size.x), parede.size.y + fposmod(k * 53.0, chao.size.y - parede.size.y))
		c.draw_rect(Rect2(pos.floor(), Vector2(2 + k % 3, 1 + k % 2)), Color(Paleta.PRODUCAO_SUJA_ESCURA, 0.3))
	# parede de concreto com exaustores girando
	c.draw_rect(parede, Paleta.ARMAZEM)
	for y in range(int(parede.position.y) + 6, int(parede.end.y), 7):
		c.draw_line(Vector2(parede.position.x, y), Vector2(parede.end.x, y), Paleta.ARMAZEM_ESCURO)
	var x := parede.position.x + 20
	var giro := t * 6.0 if animar else 0.0
	while x < parede.end.x - 110:
		var centro := Vector2(x, parede.position.y + 14)
		c.draw_circle(centro, 10, Paleta.FUNDO_ESCURO)
		c.draw_circle(centro, 9, Paleta.METAL)
		for pa in 3:
			var ang := giro + pa * TAU / 3
			c.draw_line(centro, centro + Vector2(cos(ang), sin(ang)) * 8, Paleta.FUNDO, 3.0)
		c.draw_circle(centro, 2, Paleta.FUNDO_ESCURO)
		x += 64
	var placa := Rect2(parede.end.x - 100, parede.position.y + 5, 94, 14)
	c.draw_rect(placa, Paleta.FUNDO_ESCURO)
	c.draw_string(fonte, placa.position + Vector2(4, 11), "G1 · LAMINAÇÃO", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.TECA)

	# estações de molde (1 molde por linha; a segunda é para a próxima linha)
	var largura := floorf(baias.size.x * 0.62 / ESTACOES_MOLDE)
	for i in ESTACOES_MOLDE:
		var estacao := Rect2(baias.position.x + i * largura, baias.position.y, largura, baias.size.y)
		_faixa_seguranca(c, estacao.position, estacao.size.y)
		c.draw_string(fonte, Vector2(estacao.position.x + 4, estacao.end.y - 3), "Molde %d" % (i + 1), HORIZONTAL_ALIGNMENT_LEFT,
				-1, 16, Paleta.PRODUCAO_SUJA_ESCURA)
		if i < moldes.size():
			var b: Dictionary = moldes[i]
			clicaveis[b["id"]] = _barco_na_baia(c, estacao, b, estado, selecionado, t, animar, fonte)
		else:
			var vazio := PixelArt.casco(PixelArt.Etapa.MOLDE, 0.0)
			c.draw_texture(vazio, Vector2(estacao.position.x + (estacao.size.x - vazio.get_width()) / 2, estacao.position.y + 40).floor(),
					Color(1, 1, 1, 0.5))
	_faixa_seguranca(c, Vector2(baias.position.x + ESTACOES_MOLDE * largura, baias.position.y), baias.size.y)

	# pintura e móveis: o resto do G1 (cenário)
	var resto := Rect2(baias.position.x + ESTACOES_MOLDE * largura + 6, baias.position.y, baias.end.x - baias.position.x - ESTACOES_MOLDE * largura - 6, baias.size.y)
	var cabine := Rect2(resto.position, Vector2(resto.size.x, resto.size.y * 0.5 - 3))
	c.draw_rect(cabine, Paleta.FUNDO)
	c.draw_rect(cabine.grow(-2), Paleta.CASCO_SOMBRA)
	for y in range(int(cabine.position.y) + 6, int(cabine.end.y) - 2, 6):
		c.draw_line(Vector2(cabine.position.x + 4, y), Vector2(cabine.end.x - 4, y), Paleta.PISO_ESCURO)
	c.draw_string(fonte, cabine.position + Vector2(5, 14), "CABINE DE PINTURA", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.FUNDO)
	var moveis := Rect2(resto.position.x, cabine.end.y + 6, resto.size.x, resto.size.y - cabine.size.y - 6)
	c.draw_rect(moveis, Color(Paleta.TECA, 0.35))
	for k in 5:
		var tabua := Rect2(moveis.position.x + 6, moveis.position.y + 20 + k * 7, moveis.size.x * (0.5 + 0.08 * k), 4)
		c.draw_rect(tabua, Paleta.TECA if k % 2 == 0 else Paleta.TECA_ESCURA)
	c.draw_string(fonte, moveis.position + Vector2(5, 14), "MÓVEIS", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.MADEIRA_ESCURA)

	# lateral: saída de resíduos e chuveiros do vestiário
	c.draw_rect(lateral, Paleta.FUNDO)
	c.draw_rect(lateral.grow(-0.5), Paleta.ARMAZEM_ESCURO, false, 1.0)
	c.draw_string(fonte, lateral.position + Vector2(6, 14), "VESTIÁRIO G1", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.TEXTO)
	for k in 4:
		var box := Rect2(lateral.position.x + 8 + k * 26, lateral.position.y + 24, 20, 20)
		c.draw_rect(box, Paleta.CASCO_SOMBRA)
		c.draw_circle(box.get_center(), 3, Paleta.AGUA)
	c.draw_string(fonte, lateral.position + Vector2(6, 70), "RESÍDUOS", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.TEXTO)
	for k in 3:
		var tambor := Vector2(lateral.position.x + 18 + k * 30, lateral.position.y + 88)
		c.draw_circle(tambor, 9, Paleta.FUNDO_ESCURO)
		c.draw_circle(tambor, 8, [Paleta.NAO_CONFORME, Paleta.ALERTA, Paleta.MONTAGEM_LIMPA_ESCURA][k])
		c.draw_circle(tambor, 3, Color(Paleta.FUNDO_ESCURO, 0.4))


# ---------------------------------------------------------------- piso, mezanino, rua e pátio

static func _piso(c: CanvasItem, r: Rect2) -> void:
	c.draw_rect(r, Paleta.PISO)
	var linha := Color(Paleta.PISO_ESCURO, 0.35)
	for x in range(int(r.position.x) + 16, int(r.end.x), 16):
		c.draw_line(Vector2(x, r.position.y), Vector2(x, r.end.y), linha)
	for y in range(int(r.position.y) + 16, int(r.end.y), 16):
		c.draw_line(Vector2(r.position.x, y), Vector2(r.end.x, y), linha)


static func _faixa_seguranca(c: CanvasItem, topo: Vector2, altura: float) -> void:
	var y := 0
	while y < altura:
		var cor := Paleta.FAIXA if (y / 4) % 2 == 0 else Paleta.FUNDO_ESCURO
		c.draw_rect(Rect2(topo.x - 1, topo.y + y, 2, mini(4, int(altura) - y)), cor)
		y += 4


static func _desenhar_mezanino(c: CanvasItem, r: Rect2, estado: Dictionary, t: float, animar: bool, fonte: Font) -> void:
	# parede de tijolo
	c.draw_rect(r, Paleta.TIJOLO)
	for y in range(int(r.position.y) + 4, int(r.end.y), 5):
		c.draw_line(Vector2(r.position.x, y), Vector2(r.end.x, y), Paleta.TIJOLO_ESCURO)
		var desloca := 0 if ((y - int(r.position.y)) / 5) % 2 == 0 else 6
		for x in range(int(r.position.x) + desloca, int(r.end.x), 12):
			c.draw_line(Vector2(x, y - 4), Vector2(x, y), Paleta.TIJOLO_ESCURO)
	# janelas e prateleiras de consumíveis, alternando
	var x := r.position.x + 14
	var k := 0
	while x + 30 < r.end.x - 96:
		if k % 3 == 1:
			c.draw_rect(Rect2(x - 1, r.position.y + 5, 34, 18), Paleta.FUNDO_ESCURO)
			for linha in 2:
				for coluna in 6:
					var cor: Color = [Paleta.CRITICO, Paleta.ALERTA, Paleta.MONTAGEM_LIMPA, Paleta.AGUA][(coluna + linha + k) % 4]
					c.draw_rect(Rect2(x + 1 + coluna * 5, r.position.y + 7 + linha * 8, 4, 6), cor)
		else:
			c.draw_rect(Rect2(x, r.position.y + 6, 18, 12), Paleta.CASCO_SOMBRA)
			c.draw_rect(Rect2(x + 2, r.position.y + 8, 14, 8), Paleta.AGUA_ESCURA)
			c.draw_rect(Rect2(x + 2, r.position.y + 8, 5, 2), Paleta.AGUA)
		x += 52
		k += 1
	# guarda-corpo
	c.draw_rect(Rect2(r.position.x, r.end.y - 3, r.size.x, 1), Paleta.FAIXA)
	c.draw_rect(Rect2(r.position.x, r.end.y - 2, r.size.x, 2), Paleta.FUNDO_ESCURO)
	var placa := Rect2(r.end.x - 92, r.position.y + 5, 86, 14)
	c.draw_rect(placa, Paleta.FUNDO_ESCURO)
	c.draw_string(fonte, placa.position + Vector2(4, 11), "MEZANINO +3,5 m", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.ALERTA)

	# quem está sem barco fica no mezanino (e a inspetora, se não estiver inspecionando)
	var livres: Array = []
	for p in estado["pessoas"]:
		if p["funcao"] == "PRODUCAO" and p["situacao"] == "ATIVA" and p["alocadoEm"] == null:
			livres.append(p)
	var passo := 22.0
	for i in mini(livres.size(), int(r.size.x / passo) - 2):
		var px := r.position.x + 40 + i * passo + (sin(t * 0.7 + i) * 3.0 if animar else 0.0)
		_desenhar_pessoa(c, Vector2(px, r.end.y - 3), Paleta.MONTAGEM_LIMPA_ESCURA, int(t * 2 + i) % 2 if animar else 0)


static func _desenhar_rua(c: CanvasItem, r: Rect2, t: float, animar: bool) -> void:
	c.draw_rect(r, Paleta.ARMAZEM)
	c.draw_rect(Rect2(r.position.x, r.position.y, r.size.x, 1), Paleta.ARMAZEM_ESCURO)
	c.draw_rect(Rect2(r.position.x, r.end.y - 1, r.size.x, 1), Paleta.ARMAZEM_ESCURO)
	var meio := r.position.y + r.size.y / 2
	for x in range(int(r.position.x) + 6, int(r.end.x) - 8, 20):
		c.draw_rect(Rect2(x, meio - 1, 10, 2), Paleta.FAIXA)
	# empilhadeira indo e voltando com kits
	var percurso := r.size.x - 40
	var vai := fposmod(t * 18.0, percurso * 2) if animar else percurso * 0.3
	var indo := vai < percurso
	var px := vai if indo else percurso * 2 - vai
	var tex := PixelArt.empilhadeira(indo)
	var origem := Vector2(r.position.x + 6 + px, r.position.y + (r.size.y - tex.get_height()) / 2).floor()
	if indo:
		c.draw_texture(tex, origem)
	else:
		c.draw_texture_rect(tex, Rect2(origem + Vector2(tex.get_width(), 0), Vector2(-tex.get_width(), tex.get_height())), false)


static func _desenhar_patio(c: CanvasItem, r: Rect2, estado: Dictionary, fonte: Font) -> void:
	# almoxarifado: um palete por item, com uma caixa por kit (até 4) e o nome inteiro
	var itens: Array = estado["estoque"].keys()
	itens.sort()
	var x := r.position.x + 192  # o canto esquerdo fica para o cartão do barco selecionado
	c.draw_string(fonte, Vector2(x, r.position.y + 11), "ALMOXARIFADO", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.ARMAZEM_ESCURO)
	for item in itens:
		var kits := int(estado["estoque"][item])
		var cor: Color = CORES_ITEM.get(item, Paleta.MADEIRA)
		var palete := Rect2(x + 4, r.position.y + 16, 22, 16)
		# palete de madeira
		c.draw_rect(palete, Paleta.MADEIRA_ESCURA)
		for k in 4:
			c.draw_rect(Rect2(palete.position.x + 1, palete.position.y + 1 + k * 4, palete.size.x - 2, 3), Paleta.TECA_ESCURA)
		for k in mini(kits, 4):
			var caixa := Rect2(palete.position.x + 2 + (k % 2) * 10, palete.position.y + 2 + (k / 2) * 7, 9, 6)
			c.draw_rect(caixa, Paleta.FUNDO_ESCURO)
			c.draw_rect(caixa.grow(-1), cor)
			c.draw_rect(Rect2(caixa.position.x + 1, caixa.position.y + 1, caixa.size.x - 2, 1), cor.lightened(0.3))
		var nome: String = NOMES_ITEM.get(item, item)
		c.draw_string(fonte, Vector2(palete.end.x + 3, palete.end.y - 2), "x%d" % kits, HORIZONTAL_ALIGNMENT_LEFT, -1, 16,
				Paleta.CRITICO if kits == 0 else Paleta.FUNDO)
		c.draw_string(fonte, Vector2(x, r.end.y - 3), nome, HORIZONTAL_ALIGNMENT_LEFT, 60, 16, Paleta.ARMAZEM_ESCURO)
		x += 64
		if x > r.end.x - 50:
			break


# ---------------------------------------------------------------- barcos

static func _etapa(gate: String) -> PixelArt.Etapa:
	match gate:
		"RE", "DM":
			return PixelArt.Etapa.MOLDE
		"BA":
			return PixelArt.Etapa.ABERTO
		"BF":
			return PixelArt.Etapa.FECHADO
	return PixelArt.Etapa.PRONTO


static func _barco_na_baia(c: CanvasItem, baia: Rect2, b: Dictionary, estado: Dictionary, selecionado: String,
		t: float, animar: bool, fonte: Font) -> Rect2:
	var gate: String = b["gateAtual"]
	var etapa := _etapa(gate)
	var progresso := 0.0 if gate == "RE" else float(b["progressoGate"])
	if b["status"] == "PARADO_MATERIAL":
		# barco parado: a baia pulsa em vermelho (GDD, Luz)
		var pulso := 0.12 + 0.10 * sin(t * 4.0)
		c.draw_rect(baia.grow(-2), Color(Paleta.NAO_CONFORME, pulso))
	var tex := PixelArt.casco(etapa, progresso, etapa != PixelArt.Etapa.MOLDE)
	var pos := Vector2(baia.position.x + (baia.size.x - tex.get_width()) / 2,
			baia.position.y + 30 + (baia.size.y - 46 - tex.get_height()) / 2).floor()
	var casco := Rect2(pos, tex.get_size())

	# andaimes dos dois lados no barco aberto e fechado
	if etapa == PixelArt.Etapa.ABERTO or etapa == PixelArt.Etapa.FECHADO:
		for lado in [casco.position.x - 9, casco.end.x + 5]:
			c.draw_rect(Rect2(lado, casco.position.y + 6, 4, casco.size.y - 4), Paleta.ARMAZEM_ESCURO)
			for y in range(int(casco.position.y) + 8, int(casco.end.y), 5):
				c.draw_rect(Rect2(lado, y, 4, 1), Paleta.ALERTA_ESCURO)
	# sombra no formato do casco e o casco; na montagem a popa fica para o mezanino (README da Visão)
	c.draw_texture(tex, pos + Vector2(3, 3), Color(0.05, 0.05, 0.1, 0.28))
	c.draw_texture(tex, pos)

	# equipe em volta do casco
	var alocadas: Array = b["pessoasAlocadas"]
	var uniforme := Paleta.PRODUCAO_SUJA if etapa == PixelArt.Etapa.MOLDE else Paleta.MONTAGEM_LIMPA_ESCURA
	for k in alocadas.size():
		var esquerda := k % 2 == 0
		var px := casco.position.x - 12 if esquerda else casco.end.x + 2
		var py := casco.position.y + 22 + (k / 2) * 24
		var quadro := int(t * 4 + k) % 2 if animar else 0
		var balanca := (1.0 if quadro == 1 else 0.0) if animar else 0.0
		_desenhar_pessoa(c, Vector2(px + balanca, py), uniforme, quadro, etapa == PixelArt.Etapa.MOLDE)
	if animar and b["status"] in ["EM_PRODUCAO", "EM_RETRABALHO"]:
		_efeito_de_trabalho(c, casco, alocadas.size(), etapa == PixelArt.Etapa.MOLDE, t)

	# identificação na baia
	c.draw_string(fonte, Vector2(baia.position.x + 4, baia.position.y + 12), "%s P%d" % [b["id"], int(b["prioridade"])],
			HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.FUNDO)
	c.draw_string(fonte, Vector2(baia.position.x + 4, baia.position.y + 23), gate, HORIZONTAL_ALIGNMENT_LEFT, -1, 16,
			Paleta.ARMAZEM_ESCURO)
	_barra(c, Rect2(baia.position.x + 22, baia.position.y + 18, baia.size.x - 28, 2), float(b["progressoGate"]),
			Paleta.PRODUCAO_SUJA if etapa == PixelArt.Etapa.MOLDE else Paleta.MONTAGEM_LIMPA_ESCURA)
	_balao_do_barco(c, b, Vector2(casco.end.x - 4, casco.position.y - 4), t)
	if b["id"] == selecionado:
		c.draw_rect(casco.grow(4), Paleta.ALERTA, false, 1.0)
	return casco.grow(8)


static func _balao_do_barco(c: CanvasItem, b: Dictionary, onde: Vector2, t: float) -> void:
	var icone := ""
	for nc in b["ncs"]:
		if nc["status"] == "ABERTA":
			icone = "alerta"
	if icone.is_empty():
		match str(b["status"]):
			"AGUARDANDO_INSPECAO", "EM_INSPECAO":
				icone = "espera"
			"APROVADO":
				icone = "ok"
			"PARADO_MATERIAL":
				icone = "material"
	if icone.is_empty():
		return
	var sobe := 1.0 if int(t * 2) % 2 == 0 else 0.0
	c.draw_texture(PixelArt.balao(icone), (onde - Vector2(0, sobe)).floor())


static func _carreta(c: CanvasItem, rua: Rect2, b: Dictionary, selecionado: String, t: float) -> Rect2:
	var tex := PixelArt.casco(PixelArt.Etapa.PRONTO)
	var comprimento := tex.get_height()
	var x := rua.position.x + 6 + float(b["progressoGate"]) * (rua.size.x - comprimento - 40)
	var y := rua.position.y + (rua.size.y - tex.get_width()) / 2
	var area := Rect2(Vector2(x, y).floor(), Vector2(comprimento, tex.get_width()))
	c.draw_rect(Rect2(area.position + Vector2(-2, 8), Vector2(comprimento + 6, 10)), Paleta.FUNDO_ESCURO)  # prancha
	# casco deitado na prancha, proa para a direita
	c.draw_set_transform(Vector2(area.end.x, area.position.y), PI / 2)
	c.draw_texture(tex, Vector2.ZERO)
	c.draw_set_transform(Vector2.ZERO)
	c.draw_texture(PixelArt.caminhao(), Vector2(area.end.x + 4, area.position.y + 6))
	if b["id"] == selecionado:
		c.draw_rect(area.grow(3), Paleta.ALERTA, false, 1.0)
	return area.grow(4)


static func _desenhar_piscina(c: CanvasItem, r: Rect2, barcos: Array, selecionado: String, t: float, fonte: Font,
		clicaveis: Dictionary) -> void:
	c.draw_rect(r, Paleta.METAL)
	c.draw_rect(r.grow(-2), Paleta.ARMAZEM_ESCURO)
	var agua := r.grow(-4)
	c.draw_rect(agua, Paleta.AGUA_ESCURA)
	var fase := int(t * 4) % 12
	for y in range(int(agua.position.y) + 4, int(agua.end.y) - 2, 7):
		for x in range(int(agua.position.x) + ((y / 7) % 2) * 6 + fase / 2, int(agua.end.x) - 6, 12):
			c.draw_rect(Rect2(x, y, 5, 1), Color(Paleta.AGUA, 0.8))
	c.draw_string(fonte, Vector2(agua.position.x + 3, agua.position.y + 12), "PISCINA · PI", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.CASCO)
	var tex := PixelArt.casco(PixelArt.Etapa.PRONTO, 1.0, true)
	for i in mini(barcos.size(), 2):
		var b: Dictionary = barcos[i]
		var balanco := 1.0 if int(t * 1.2 + i) % 2 == 0 else 0.0
		var pos := Vector2(agua.position.x + agua.size.x / 2 - tex.get_width() / 2 + (i * 2 - 1) * (22 if barcos.size() > 1 else 0),
				agua.position.y + 30 + balanco).floor()
		var casco := Rect2(pos, tex.get_size())
		c.draw_rect(casco.grow(2), Color(Paleta.AGUA, 0.55), false, 1.0)  # marola
		c.draw_texture(tex, pos)
		c.draw_string(fonte, Vector2(casco.position.x - 4, casco.end.y + 14), b["id"], HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.CASCO)
		_balao_do_barco(c, b, Vector2(casco.end.x - 4, casco.position.y - 4), t)
		if b["id"] == selecionado:
			c.draw_rect(casco.grow(4), Paleta.ALERTA, false, 1.0)
		clicaveis[b["id"]] = casco.grow(6)


static func _desenhar_marina(c: CanvasItem, r: Rect2, barcos: Array, entregues: int, selecionado: String, t: float,
		fonte: Font, clicaveis: Dictionary) -> void:
	c.draw_rect(r, Paleta.FUNDO)
	c.draw_rect(r.grow(-0.5), Paleta.ARMAZEM_ESCURO, false, 1.0)
	c.draw_string(fonte, Vector2(r.position.x + 4, r.position.y + 12), "MARINA", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.AGUA)
	c.draw_string(fonte, Vector2(r.position.x + 4, r.end.y - 4), "entregues: %d" % entregues, HORIZONTAL_ALIGNMENT_LEFT, -1, 16,
			Paleta.MONTAGEM_LIMPA)
	var y := r.position.y + 26
	for b in barcos:
		var linha := Rect2(r.position.x + 2, y - 11, r.size.x - 4, 13)
		if b["id"] == selecionado:
			c.draw_rect(linha, Color(Paleta.ALERTA, 0.25))
		c.draw_string(fonte, Vector2(r.position.x + 4, y), "%s · %s" % [b["id"], b["gateAtual"]], HORIZONTAL_ALIGNMENT_LEFT, -1, 16,
				Paleta.TEXTO)
		clicaveis[b["id"]] = linha
		y += 13
		if y > r.end.y:
			break


## Pó subindo na laminação; faíscas de ferramenta na montagem.
static func _efeito_de_trabalho(c: CanvasItem, casco: Rect2, pessoas: int, laminacao: bool, t: float) -> void:
	for k in pessoas:
		var esquerda := k % 2 == 0
		var base := Vector2(casco.position.x - 3 if esquerda else casco.end.x + 3, casco.position.y + 16 + (k / 2) * 24)
		if laminacao:
			for p in 3:
				var fase := fposmod(t * 0.8 + p * 0.33 + k * 0.17, 1.0)
				var pos := base + Vector2((p - 1) * 3 + sin(t * 3 + p) * 1.5, -fase * 12)
				c.draw_rect(Rect2(pos.floor(), Vector2(1, 1)), Color(Paleta.TECA, 1.0 - fase))
		elif int(t * 8 + k * 3) % 9 == 0:
			for p in 4:
				var d := Vector2(cos(p * 1.7 + k), sin(p * 1.7 + k)) * (2 + p)
				c.draw_rect(Rect2((base + d).floor(), Vector2(1, 1)), Paleta.FAIXA if p % 2 == 0 else Paleta.CASCO)


# ---------------------------------------------------------------- utilitários

static func _desenhar_pessoa(c: CanvasItem, pe: Vector2, uniforme: Color, quadro: int, capacete: bool = false) -> void:
	var tex := PixelArt.pessoa(uniforme, quadro, capacete)
	c.draw_texture(tex, (pe - Vector2(tex.get_width() / 2.0, tex.get_height())).floor())


static func _barra(c: CanvasItem, r: Rect2, fracao: float, cor: Color) -> void:
	c.draw_rect(r, Paleta.PISO_ESCURO)
	c.draw_rect(Rect2(r.position, Vector2(r.size.x * clampf(fracao, 0.0, 1.0), r.size.y)), cor)
