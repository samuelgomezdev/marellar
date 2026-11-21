class_name FormularioNc3d
extends PanelContainer
## Formulário da NC marcada no ponto clicado do barco em 3D. Monta a ordem RegistrarNcInspecao;
## quem valida (defeito do catálogo, gravidade mínima) e grava é o motor.

signal confirmar(dados: Dictionary)
signal cancelar

const GRAVIDADES := ["LEVE", "MEDIA", "CRITICA"]
const NOMES_GRAVIDADE := {"LEVE": "Leve", "MEDIA": "Média", "CRITICA": "Crítica"}
const SUGERIDOS := "*"
const TODOS := "+"

var catalogo: Array = []          # defeitos (data/nc-catalogo-inspecao.json, via marellar-<linha>.json)
var categorias: Dictionary = {}
var _tipo_item := ""
var _ponto := Vector3.ZERO
var _item := ""
var _compartimento := ""
var _titulo: Label
var _local: Label
var _categoria: OptionButton
var _defeito: OptionButton
var _gravidade: OptionButton
var _observacao: LineEdit
var _erro: Label
var _ok: Button
var _codigos_categoria: Array[String] = []
var _codigos_defeito: Array[String] = []


func _ready() -> void:
	var fundo := StyleBoxFlat.new()
	fundo.bg_color = Paleta.FUNDO
	fundo.border_color = Paleta.ALERTA
	fundo.set_border_width_all(1)
	fundo.set_content_margin_all(5)
	add_theme_stylebox_override("panel", fundo)
	mouse_filter = Control.MOUSE_FILTER_STOP
	var caixa := VBoxContainer.new()
	caixa.add_theme_constant_override("separation", 2)
	add_child(caixa)
	var cabecalho := HBoxContainer.new()
	caixa.add_child(cabecalho)
	_titulo = _rotulo(cabecalho, Paleta.ALERTA)
	_titulo.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	var x := Button.new()
	x.text = "X"
	x.tooltip_text = "Fechar (Esc)"
	x.pressed.connect(func(): cancelar.emit())
	cabecalho.add_child(x)
	_local = _rotulo(caixa, Paleta.TEXTO)
	_local.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_local.custom_minimum_size.x = 220   # sem largura, o rótulo com quebra mede uma letra por linha
	_categoria = _opcoes(caixa)
	_categoria.item_selected.connect(func(_i): _preencher_defeitos(""))
	_defeito = _opcoes(caixa)
	_defeito.item_selected.connect(func(_i): _preencher_gravidade())
	_gravidade = _opcoes(caixa)
	_observacao = LineEdit.new()
	_observacao.placeholder_text = "Observação (opcional)"
	_observacao.max_length = 300
	caixa.add_child(_observacao)
	_erro = _rotulo(caixa, Paleta.CRITICO)
	_erro.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	_erro.custom_minimum_size.x = 220
	var botoes := HBoxContainer.new()
	botoes.add_theme_constant_override("separation", 4)
	caixa.add_child(botoes)
	_ok = Button.new()
	_ok.text = "Registrar NC"
	_ok.pressed.connect(_confirmar)
	botoes.add_child(_ok)
	var nao := Button.new()
	nao.text = "Cancelar"
	nao.pressed.connect(func(): cancelar.emit())
	botoes.add_child(nao)


## Abre o formulário para o ponto clicado (em coordenadas do barco) e o item da estrutura que foi clicado.
func abrir(ponto: Vector3, extras: Dictionary, compartimento: String, nome_compartimento: String, gate: String) -> void:
	_ponto = ponto
	_item = str(extras.get("codigo", ""))
	_tipo_item = str(extras.get("tipo", ""))
	_compartimento = compartimento
	_titulo.text = "Nova NC · gate %s" % gate
	var onde := nome_compartimento if not nome_compartimento.is_empty() else "fora dos compartimentos"
	_local.text = "%s %s\n%s" % [_item, str(extras.get("descricao", "")).capitalize(), onde]
	_categoria.clear()
	_codigos_categoria.clear()
	_categoria.add_item("Sugeridos para esta peça")
	_codigos_categoria.append(SUGERIDOS)
	for chave in categorias:
		_categoria.add_item(str(categorias[chave]))
		_codigos_categoria.append(str(chave))
	_categoria.add_item("Todos os defeitos")
	_codigos_categoria.append(TODOS)
	_categoria.select(0)
	_preencher_defeitos("")
	_observacao.text = ""
	_erro.text = ""
	_erro.visible = false
	visible = true
	_ok.call_deferred("grab_focus")   # ação principal do formulário


func mostrar_erro(texto: String) -> void:
	_erro.text = texto
	_erro.visible = not texto.is_empty()
	reset_size()


func _preencher_defeitos(manter: String) -> void:
	var filtro: String = _codigos_categoria[maxi(0, _categoria.selected)]
	var lista: Array = []
	for d in catalogo:
		var aplica: Array = d.get("aplica", [])
		if filtro == TODOS or (filtro == SUGERIDOS and (_tipo_item.is_empty() or aplica.has(_tipo_item))) or d["categoria"] == filtro:
			lista.append(d)
	if lista.is_empty():
		lista = catalogo
	_defeito.clear()
	_codigos_defeito.clear()
	for d in lista:
		var prefixo := ""
		if filtro == SUGERIDOS or filtro == TODOS:
			prefixo = str(categorias.get(d["categoria"], "")).split(" ")[0] + ": "
		_defeito.add_item(prefixo + str(d["descricao"]))
		_codigos_defeito.append(str(d["codigo"]))
	var i := _codigos_defeito.find(manter)
	_defeito.select(maxi(0, i))
	_preencher_gravidade()


## A gravidade começa na do catálogo e só pode subir.
func _preencher_gravidade() -> void:
	var minima := 0
	var codigo := _codigo_defeito()
	for d in catalogo:
		if d["codigo"] == codigo:
			minima = GRAVIDADES.find(str(d["gravidade"]))
	_gravidade.clear()
	for g in GRAVIDADES.slice(maxi(0, minima)):
		_gravidade.add_item(NOMES_GRAVIDADE[g])
		_gravidade.set_item_metadata(_gravidade.item_count - 1, g)
	_gravidade.select(0)


func _codigo_defeito() -> String:
	return _codigos_defeito[_defeito.selected] if _defeito.selected >= 0 and _defeito.selected < _codigos_defeito.size() else ""


func _confirmar() -> void:
	if _codigo_defeito().is_empty():
		mostrar_erro("Escolha um defeito.")
		return
	confirmar.emit({
		"codigoDefeito": _codigo_defeito(),
		"gravidade": str(_gravidade.get_item_metadata(_gravidade.selected)),
		"item": _item,
		"compartimento": _compartimento,
		"x": snappedf(_ponto.x, 0.001), "y": snappedf(_ponto.y, 0.001), "z": snappedf(_ponto.z, 0.001),
		"observacao": _observacao.text.strip_edges(),
	})


func _rotulo(pai: Control, cor: Color) -> Label:
	var l := Label.new()
	l.add_theme_color_override("font_color", cor)
	pai.add_child(l)
	return l


func _opcoes(pai: Control) -> OptionButton:
	var o := OptionButton.new()
	o.clip_text = true
	o.fit_to_longest_item = false
	pai.add_child(o)
	return o
