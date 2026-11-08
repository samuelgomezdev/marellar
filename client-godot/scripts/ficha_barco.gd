class_name FichaBarco
extends Control
## Ficha completa do barco (mockup telas/2_ficha_do_barco.png): caminho pelos gates, horas-homem,
## material, não conformidades com as decisões e cotas do cliente. Só exibe o que o motor manda;
## as ações viram sinais que a tela da fábrica transforma em ordens.

signal fechar
signal priorizar
signal alternar_segurar
signal decidir_nc(nc_id: String, decisao: String)

const GATES := ["RE", "DM", "BA", "BF", "ST", "CT", "PI", "TR", "TM", "ET"]
const NOMES_STATUS_NC := {"EM_RETRABALHO": "em retrabalho", "RESOLVIDA": "fechada", "CONCEDIDA": "concessão",
	"PENDENTE_INSPECAO": "pendente"}
const NOMES_GRAVIDADE := {"LEVE": "leve", "MEDIA": "média", "CRITICA": "CRÍTICA"}
const MARGEM := 6.0
const LARGURA_ESQUERDA := 196.0
const ALTURA_BOTOES := 22.0

var barco: Dictionary = {}
var hoje := 0
var fonte: Font
var fonte_titulo: Font
var tamanho_texto := 16
var tamanho_titulo := 8
var animado := false

var _lista_ncs: VBoxContainer
var _rolagem_ncs: ScrollContainer
var _assinatura_ncs := ""
var _botao_prioridade: Button
var _botao_segurar: Button


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	_rolagem_ncs = ScrollContainer.new()
	_rolagem_ncs.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(_rolagem_ncs)
	_lista_ncs = VBoxContainer.new()
	_lista_ncs.add_theme_constant_override("separation", 1)
	_rolagem_ncs.add_child(_lista_ncs)

	var botoes := HBoxContainer.new()
	botoes.name = "Botoes"
	botoes.add_theme_constant_override("separation", 4)
	add_child(botoes)
	botoes.add_child(_botao("< Fábrica", func(): fechar.emit()))
	_botao_prioridade = _botao("Prioridade 1", func(): priorizar.emit())
	botoes.add_child(_botao_prioridade)
	_botao_segurar = _botao("Segurar gate", func(): alternar_segurar.emit())
	botoes.add_child(_botao_segurar)
	resized.connect(_posicionar)
	_posicionar()


func atualizar(novo: Dictionary, tick: int) -> void:
	barco = novo
	hoje = tick / 8
	var ativo: bool = not barco.is_empty() and barco["status"] != "ENTREGUE"
	_botao_prioridade.disabled = not ativo or int(barco["prioridade"]) == 1
	_botao_segurar.disabled = not ativo
	_botao_segurar.text = "Liberar gate" if ativo and barco["retido"] else "Segurar gate"
	_reconstruir_ncs()
	queue_redraw()


func _process(_delta: float) -> void:
	if animado:
		queue_redraw()


# ---------------------------------------------------------------- layout

func _area_direita() -> Rect2:
	var x := MARGEM + LARGURA_ESQUERDA + MARGEM
	return Rect2(x, 18, size.x - x - MARGEM, size.y - 18 - ALTURA_BOTOES - MARGEM)


## Caixa das NCs (esquerda) e das cotas (direita), abaixo do caminho pelos gates.
func _caixas_inferiores() -> Array[Rect2]:
	var d := _area_direita()
	var topo := d.position.y + 82
	var altura := d.end.y - topo
	var meia := floorf((d.size.x - MARGEM) / 2)
	return [Rect2(d.position.x, topo, meia, altura), Rect2(d.position.x + meia + MARGEM, topo, d.size.x - meia - MARGEM, altura)]


func _posicionar() -> void:
	if _rolagem_ncs == null:
		return
	var caixa: Rect2 = _caixas_inferiores()[0]
	_rolagem_ncs.position = caixa.position + Vector2(4, 16)
	_rolagem_ncs.size = caixa.size - Vector2(8, 20)
	_lista_ncs.custom_minimum_size.x = _rolagem_ncs.size.x - 10
	var botoes := get_node("Botoes") as HBoxContainer
	botoes.position = Vector2(MARGEM, size.y - ALTURA_BOTOES - 2)
	botoes.size = Vector2(size.x - 2 * MARGEM, ALTURA_BOTOES - 2)


# ---------------------------------------------------------------- NCs (Controls, com botões)

func _reconstruir_ncs() -> void:
	var assinatura := "" if barco.is_empty() else JSON.stringify([barco["id"], barco["ncs"]])
	if assinatura == _assinatura_ncs:
		return
	_assinatura_ncs = assinatura
	for filho in _lista_ncs.get_children():
		filho.queue_free()
	if barco.is_empty():
		return
	var ncs: Array = barco["ncs"].duplicate()
	# abertas primeiro, depois as mais recentes
	ncs.sort_custom(_antes_na_lista)
	if ncs.is_empty():
		_lista_ncs.add_child(_rotulo("Nenhuma NC neste barco.", Paleta.PISO_ESCURO))
	for nc in ncs:
		var aberta: bool = nc["status"] == "ABERTA"
		var cor := Paleta.CRITICO if aberta else (Paleta.MONTAGEM_LIMPA if nc["status"] == "RESOLVIDA" else Paleta.ALERTA)
		_lista_ncs.add_child(_rotulo("%s %s · %s %s" % [nc["id"], nc["descricao"], nc["gate"],
				NOMES_GRAVIDADE.get(nc["gravidade"], nc["gravidade"])], cor))
		if not aberta:
			var situacao: String = NOMES_STATUS_NC.get(nc["status"], str(nc["status"]).to_lower())
			if nc["status"] == "EM_RETRABALHO":
				situacao += " %d HH" % int(nc["hhRetrabalho"])
			_lista_ncs.add_child(_rotulo("   " + situacao, Paleta.PISO_ESCURO))
			continue
		var acoes := HBoxContainer.new()
		acoes.add_theme_constant_override("separation", 2)
		var retrabalho := _botao("Retrabalho", func(): decidir_nc.emit(nc["id"], "RETRABALHO"))
		retrabalho.tooltip_text = "Conserta agora: %d HH." % int(nc["hhRetrabalho"])
		acoes.add_child(retrabalho)
		var concessao := _botao("Concessão", func(): decidir_nc.emit(nc["id"], "CONCESSAO"))
		concessao.disabled = nc["gravidade"] == "CRITICA"
		concessao.tooltip_text = "NC crítica não aceita concessão." if concessao.disabled else "Libera sem consertar: risco de garantia."
		acoes.add_child(concessao)
		if nc["origem"] == "FORNECEDOR":
			var rnc := _botao("RNC", func(): decidir_nc.emit(nc["id"], "RNC_FORNECEDOR"))
			rnc.tooltip_text = "Abre RNC contra o fornecedor (chargeback)."
			acoes.add_child(rnc)
		_lista_ncs.add_child(acoes)


## NCs abertas primeiro; dentro do mesmo grupo, as mais recentes.
static func _antes_na_lista(a: Dictionary, b: Dictionary) -> bool:
	var a_aberta: bool = a["status"] == "ABERTA"
	var b_aberta: bool = b["status"] == "ABERTA"
	if a_aberta != b_aberta:
		return a_aberta
	return str(a["id"]) > str(b["id"])


# ---------------------------------------------------------------- desenho

func _draw() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(0, size.y - ALTURA_BOTOES - 3, size.x, ALTURA_BOTOES + 3), Paleta.FUNDO)
	if barco.is_empty():
		return
	var t := Time.get_ticks_msec() / 1000.0

	# cabeçalho
	draw_string(fonte_titulo, Vector2(MARGEM, 12), "BARCO %s" % barco["id"], HORIZONTAL_ALIGNMENT_LEFT, -1,
			tamanho_titulo, Paleta.ALERTA)
	var status := "entregue" if barco["status"] == "ENTREGUE" else "P%d" % int(barco["prioridade"])
	draw_string(fonte, Vector2(MARGEM + 112, 12), "Linha 33 pés · %s · %s%s" % [barco["cliente"], status,
			" · segurado" if barco["retido"] else ""], HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto, Paleta.TEXTO)

	_desenhar_baia(Rect2(MARGEM, 18, LARGURA_ESQUERDA, size.y - 18 - ALTURA_BOTOES - MARGEM), t)
	_desenhar_gates(_area_direita())
	var caixas := _caixas_inferiores()
	_caixa(caixas[0], "NÃO CONFORMIDADES")
	_desenhar_cotas(caixas[1])


func _desenhar_baia(area: Rect2, t: float) -> void:
	draw_rect(area, Paleta.ARMAZEM_ESCURO)
	for x in range(int(area.position.x) + 16, int(area.end.x), 16):
		draw_line(Vector2(x, area.position.y), Vector2(x, area.end.y), Color(Paleta.ARMAZEM, 0.35))
	var gate: String = barco["gateAtual"]
	var etapa := Sprites.etapa_do_gate(gate, barco["status"] == "ENTREGUE")
	# barco em escala 2 (o sprite tem ~100 px de largura)
	var origem := Vector2(area.position.x + 4, area.position.y + area.size.y * 0.55)
	draw_set_transform(origem, 0.0, Vector2(2, 2))
	Sprites.barco(self, Vector2(4, 0), etapa, float(barco["progressoGate"]), t)
	var alocadas: Array = barco["pessoasAlocadas"]
	var uniforme := Paleta.PRODUCAO_SUJA if etapa == Sprites.ETAPA_MOLDE else Paleta.MONTAGEM_LIMPA_ESCURA
	for k in alocadas.size():
		var frente := k % 2 == 0
		var y := 12.0 if frente else -16.0
		if frente and etapa == Sprites.ETAPA_PISCINA:
			y += 4.0
		Sprites.pessoa(self, Vector2(12 + (k / 2) * 26, y), uniforme, int(t * 4 + k) % 2 if animado else 0)
	draw_set_transform(Vector2.ZERO)

	var x := area.position.x + 4
	draw_string(fonte, Vector2(x, area.position.y + 14), "%s · %s" % [gate, barco["nomeGate"]],
			HORIZONTAL_ALIGNMENT_LEFT, area.size.x - 8, tamanho_texto, Paleta.TEXTO_CLARO)
	var barra := Rect2(x, area.end.y - 22, area.size.x - 8, 3)
	draw_rect(barra, Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * float(barco["progressoGate"]), 3)), Paleta.MONTAGEM_LIMPA)
	draw_string(fonte, Vector2(x, area.end.y - 5), "Gate %d%% · %d pessoa(s)" % [int(float(barco["progressoGate"]) * 100),
			alocadas.size()], HORIZONTAL_ALIGNMENT_LEFT, area.size.x - 8, tamanho_texto, Paleta.TEXTO)


func _desenhar_gates(area: Rect2) -> void:
	var caixa := Rect2(area.position, Vector2(area.size.x, 76))
	_caixa(caixa, "CAMINHO PELOS GATES")
	var aprovados: Array = barco["gatesAprovados"]
	var atual: String = barco["gateAtual"]
	var ncs_abertas := false
	for nc in barco["ncs"]:
		ncs_abertas = ncs_abertas or nc["status"] == "ABERTA"
	var largura := floorf((caixa.size.x - 8 - 2 * (GATES.size() - 1)) / GATES.size())
	for i in GATES.size():
		var g: String = GATES[i]
		var r := Rect2(caixa.position.x + 4 + i * (largura + 2), caixa.position.y + 16, largura, 14)
		var fundo := Paleta.ARMAZEM_ESCURO
		var texto := Paleta.PISO_ESCURO
		if g in aprovados:
			fundo = Paleta.MONTAGEM_LIMPA_ESCURA
			texto = Paleta.TEXTO_CLARO
		if g == atual and barco["status"] != "ENTREGUE":
			fundo = Paleta.NAO_CONFORME_ESCURO if ncs_abertas else Paleta.ALERTA_ESCURO
			texto = Paleta.TEXTO_CLARO
		draw_rect(r, fundo)
		if g == atual and barco["status"] != "ENTREGUE":
			draw_rect(r.grow(-0.5), Paleta.ALERTA, false, 1.0)
		draw_string(fonte, Vector2(r.position.x, r.end.y - 2), g, HORIZONTAL_ALIGNMENT_CENTER, r.size.x, tamanho_texto, texto)

	var y := caixa.position.y + 46
	_linha_valor(caixa, y, "Horas-homem", "%s de %s" % [_milhar(barco["hhConcluidas"]), _milhar(barco["hhPrevistas"])],
			float(barco["hhConcluidas"]) / maxf(1.0, float(barco["hhPrevistas"])), Paleta.MONTAGEM_LIMPA)
	_linha_valor(caixa, y + 16, "Material", "R$ %s mil de %s mil" % [_mil(barco["materialAplicado"]), _mil(barco["materialPrevisto"])],
			float(barco["materialAplicado"]) / maxf(1.0, float(barco["materialPrevisto"])), Paleta.AGUA)


func _linha_valor(caixa: Rect2, y: float, rotulo: String, valor: String, fracao: float, cor: Color) -> void:
	var x := caixa.position.x + 4
	draw_string(fonte, Vector2(x, y), rotulo, HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto, Paleta.PISO_ESCURO)
	var barra := Rect2(x + 76, y - 6, caixa.size.x * 0.32, 3)
	draw_rect(barra, Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * clampf(fracao, 0.0, 1.0), 3)), cor)
	draw_string(fonte, Vector2(barra.end.x + 6, y), valor, HORIZONTAL_ALIGNMENT_RIGHT, caixa.end.x - barra.end.x - 10,
			tamanho_texto, Paleta.TEXTO_CLARO)


func _desenhar_cotas(caixa: Rect2) -> void:
	_caixa(caixa, "COTAS DO CLIENTE")
	var x := caixa.position.x + 4
	var y := caixa.position.y + 28
	var direita := caixa.size.x - 8
	for c in barco["cotas"]:
		var nome := "Contrato" if c["gate"] == null else "Gate %s" % c["gate"]
		var situacao := "-"
		var cor := Paleta.PISO_ESCURO
		match c["situacao"]:
			"PAGA":
				situacao = "pago"
				cor = Paleta.MONTAGEM_LIMPA
			"A_RECEBER":
				var dias := maxi(0, int(c["diaRecebimento"]) - hoje)
				situacao = "a receber (%d d)" % dias if dias > 0 else "a receber"
				cor = Paleta.ALERTA
			_:
				if c["gate"] == barco["gateAtual"]:
					situacao = "no gate"
					cor = Paleta.ALERTA
		draw_string(fonte, Vector2(x, y), "%s %d%%" % [nome, int(float(c["percentual"]))], HORIZONTAL_ALIGNMENT_LEFT, -1,
				tamanho_texto, Paleta.TEXTO)
		draw_string(fonte, Vector2(x, y), situacao, HORIZONTAL_ALIGNMENT_RIGHT, direita, tamanho_texto, cor)
		y += 15
	var fracao := float(barco["valorRecebido"]) / maxf(1.0, float(barco["precoVenda"]))
	var barra := Rect2(x, caixa.end.y - 22, direita, 3)
	draw_rect(barra, Paleta.FUNDO_ESCURO)
	draw_rect(Rect2(barra.position, Vector2(barra.size.x * fracao, 3)), Paleta.MONTAGEM_LIMPA)
	draw_string(fonte, Vector2(x, caixa.end.y - 5), "Recebido", HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto, Paleta.PISO_ESCURO)
	draw_string(fonte, Vector2(x, caixa.end.y - 5), "R$ %s mil de %s mil" % [_mil(barco["valorRecebido"]), _mil(barco["precoVenda"])],
			HORIZONTAL_ALIGNMENT_RIGHT, direita, tamanho_texto, Paleta.MONTAGEM_LIMPA)


func _caixa(r: Rect2, titulo: String) -> void:
	draw_rect(r, Paleta.FUNDO)
	draw_rect(r.grow(-0.5), Paleta.ARMAZEM_ESCURO, false, 1.0)
	draw_string(fonte_titulo, r.position + Vector2(4, 12), titulo, HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_titulo, Paleta.ALERTA)


# ---------------------------------------------------------------- utilitários

func _botao(texto: String, acao: Callable) -> Button:
	var b := Button.new()
	b.text = texto
	b.focus_mode = Control.FOCUS_NONE
	b.pressed.connect(acao)
	return b


func _rotulo(texto: String, cor: Color) -> Label:
	var r := Label.new()
	r.text = texto
	r.autowrap_mode = TextServer.AUTOWRAP_WORD
	r.custom_minimum_size.x = _lista_ncs.custom_minimum_size.x
	r.add_theme_color_override("font_color", cor)
	return r


## Só para exibir: 1900 -> "1.900".
static func _milhar(valor) -> String:
	var texto := str(int(round(float(valor))))
	var saida := ""
	while texto.length() > 3:
		saida = "." + texto.substr(texto.length() - 3) + saida
		texto = texto.substr(0, texto.length() - 3)
	return texto + saida


## Só para exibir: 260000 -> "260".
static func _mil(valor) -> String:
	return _milhar(float(valor) / 1000.0)
