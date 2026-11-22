class_name Movimento
extends RefCounted
## Abrir e fechar painel com o token mov.ui: Tokens.MOV_UI (160 ms), saída suave, esmaecendo.
## Fechar deixa o painel visível até o fim da animação: quem decide se ele está aberto é a lógica
## (propriedade `aberta` da ficha e do 3D, ou Movimento.aberto()), não o `visible`.


static func abrir(c: CanvasItem, com_som := true) -> void:
	if com_som:
		_som("abrir_painel")
	c.set_meta("fechando", false)
	c.visible = true
	c.modulate.a = 0.0
	_animar(c, 1.0, Callable())


## `ao_fim` roda quando o painel some (ex.: queue_free de uma janela).
static func fechar(c: CanvasItem, ao_fim := Callable(), com_som := true) -> void:
	if com_som:
		_som("fechar_painel")
	c.set_meta("fechando", true)
	_animar(c, 0.0, func():
		if c.get_meta("fechando", false):
			c.visible = false
			c.modulate.a = 1.0
		if ao_fim.is_valid():
			ao_fim.call())


## Visível e não está fechando.
static func aberto(c: CanvasItem) -> bool:
	return c.visible and not c.get_meta("fechando", false)


static func _animar(c: CanvasItem, alvo: float, depois: Callable) -> void:
	var anterior = c.get_meta("tween_mov") if c.has_meta("tween_mov") else null
	if anterior is Tween and (anterior as Tween).is_valid():
		(anterior as Tween).kill()
	if not c.is_inside_tree():
		c.modulate.a = alvo
		if depois.is_valid():
			depois.call()
		return
	var t := c.create_tween().set_trans(Tokens.MOV_UI_TRANSICAO).set_ease(Tokens.MOV_UI_SUAVIZACAO)
	t.tween_property(c, "modulate:a", alvo, Tokens.MOV_UI)
	if depois.is_valid():
		t.tween_callback(depois)
	c.set_meta("tween_mov", t)


static func _som(evento: String) -> void:
	var arvore := Engine.get_main_loop() as SceneTree
	var sons := arvore.root.get_node_or_null("Sons") if arvore != null else null
	if sons != null:
		sons.tocar(evento)
