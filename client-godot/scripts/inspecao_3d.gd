class_name Inspecao3D
extends Control
## Inspeção 3D dentro do jogo, em pixel art: o barco selecionado no estado do gate atual.
## A malha vem de assets/barcos/marellar-<linha>.glb (exportada da página three.js; veja docs/ferramentas/exportar_barco.mjs).
## O visual é do jogo: cena em baixa resolução, luz em degraus, contorno escuro e redução para a Endesga 32.
## Nenhuma regra de jogo aqui: o gate e o barco vêm do estado do motor.

signal fechar

const REDUCAO := 2            # pixels da tela por pixel do 3D (a tela base é 640x360)
const ALTURA_TITULO := 16
const ALTURA_DICA := 14
const ALTURA_BERCO := 0.6
## Gate do motor -> etapa de montagem do modelo (gates da página 3D: DM, BA, BF convés, BF casaria, ST/CT, PI).
const ETAPA_DO_GATE := {"RE": -1, "DM": 0, "BA": 1, "BF": 3, "ST": 4, "CT": 4, "PI": 5, "TR": 5, "TM": 5, "ET": 5}

const TOON := preload("res://shaders/toon.gdshader")
const TOON_VIDRO := preload("res://shaders/toon_vidro.gdshader")
const CONTORNO := preload("res://shaders/contorno.gdshader")
const PALETA := preload("res://shaders/paleta.gdshader")

var fonte: Font
var fonte_titulo: Font
var tamanho_texto := 16
var tamanho_titulo := 8

var barco: Dictionary = {}
var dados: Dictionary = {}           # marellar-<linha>.json
var _linha := 0
var _container: SubViewportContainer
var _viewport: SubViewport
var _camera: Camera3D
var _pivo: Node3D                    # centro do barco; a câmera orbita em volta dele
var _modelo: Node3D
var _berco: Node3D
var _agua: MeshInstance3D
var _itens: Array[Node3D] = []       # nós de item (com metadado "extras")
var _contornos: Array[ShaderMaterial] = []
var _medias := {}                    # textura -> cor média
var _guinada := deg_to_rad(-35.0)    # rotação em volta do eixo vertical
var _arfagem := deg_to_rad(-22.0)    # inclinação da câmera
var _distancia := 18.0
var _arrastando := false


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	_container = SubViewportContainer.new()
	_container.stretch = true
	_container.stretch_shrink = REDUCAO
	_container.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	_container.mouse_filter = Control.MOUSE_FILTER_IGNORE
	var material := ShaderMaterial.new()
	material.shader = PALETA
	var cores := PackedVector3Array()
	for c in Paleta.ENDESGA_32:
		cores.append(Vector3(c.r, c.g, c.b))
	material.set_shader_parameter("paleta", cores)
	_container.material = material
	add_child(_container)

	_viewport = SubViewport.new()
	_viewport.own_world_3d = true
	_viewport.msaa_3d = Viewport.MSAA_DISABLED
	_viewport.positional_shadow_atlas_size = 0
	_container.add_child(_viewport)
	_montar_mundo()
	resized.connect(_posicionar)
	_posicionar()


func _posicionar() -> void:
	if _container == null:
		return
	_container.position = Vector2(0, ALTURA_TITULO)
	_container.size = Vector2(size.x, size.y - ALTURA_TITULO - ALTURA_DICA)


## Abre a inspeção de um barco do estado (BarcoVisao do motor).
func abrir(novo: Dictionary) -> void:
	barco = novo
	var linha := int(str(barco.get("linha", "L33")).trim_prefix("L"))
	if linha != _linha:
		_carregar_linha(linha)
	_aplicar_gate()
	visible = true
	queue_redraw()


# ---------------------------------------------------------------- mundo 3D

func _montar_mundo() -> void:
	var ambiente := Environment.new()
	ambiente.background_mode = Environment.BG_COLOR
	ambiente.background_color = Paleta.FUNDO_ESCURO
	ambiente.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	ambiente.ambient_light_color = Paleta.PISO
	ambiente.ambient_light_energy = 0.35
	ambiente.tonemap_mode = Environment.TONE_MAPPER_LINEAR
	var mundo := WorldEnvironment.new()
	mundo.environment = ambiente
	_viewport.add_child(mundo)

	var sol := DirectionalLight3D.new()
	sol.rotation = Vector3(deg_to_rad(-55), deg_to_rad(-30), 0)
	sol.light_energy = 1.6
	sol.shadow_enabled = true
	sol.directional_shadow_max_distance = 40.0
	_viewport.add_child(sol)

	var chao := MeshInstance3D.new()
	var plano := PlaneMesh.new()
	plano.size = Vector2(60, 60)
	chao.mesh = plano
	chao.material_override = _toon(Paleta.ARMAZEM_ESCURO, null, Color.BLACK)
	_viewport.add_child(chao)

	_agua = MeshInstance3D.new()
	var agua := PlaneMesh.new()
	agua.size = Vector2(60, 60)
	_agua.mesh = agua
	var mat_agua := ShaderMaterial.new()
	mat_agua.shader = TOON_VIDRO
	mat_agua.set_shader_parameter("cor", Color(Paleta.AGUA, 0.7))
	_agua.material_override = mat_agua
	_agua.visible = false
	_viewport.add_child(_agua)

	_pivo = Node3D.new()
	_viewport.add_child(_pivo)
	_camera = Camera3D.new()
	_camera.fov = 32.0
	_camera.near = 0.1
	_camera.far = 200.0
	_viewport.add_child(_camera)


func _carregar_linha(linha: int) -> void:
	if _modelo != null:
		_modelo.queue_free()
		_berco.queue_free()
	_itens.clear()
	_contornos.clear()
	_linha = linha
	var arquivo := FileAccess.open("res://assets/barcos/marellar-%d.json" % linha, FileAccess.READ)
	dados = JSON.parse_string(arquivo.get_as_text()) if arquivo != null else {}
	var cena: PackedScene = load("res://assets/barcos/marellar-%d.glb" % linha)
	_modelo = cena.instantiate()
	var comprimento: float = dados.get("dims", {}).get("L", 10.0)
	_modelo.position = Vector3(-comprimento / 2.0, ALTURA_BERCO, 0)   # x do modelo vai da popa (0) à proa (L)
	_viewport.add_child(_modelo)
	for no in _modelo.find_children("*", "Node3D", true, false):
		if no.has_meta("extras") and (no.get_meta("extras") as Dictionary).has("codigo"):
			_itens.append(no)
	for m in _modelo.find_children("*", "MeshInstance3D", true, false):
		_pixelizar(m as MeshInstance3D)
	_berco = _montar_berco(comprimento, float(dados.get("dims", {}).get("B", 3.4)))
	_viewport.add_child(_berco)
	var caixa := _caixa_do_modelo()
	_distancia = caixa.size.x * 1.15
	_pivo.position = Vector3(caixa.get_center().x, ALTURA_BERCO + minf(caixa.size.y * 0.25, 1.0), 0)
	_atualizar_camera()


## Caixa do barco montado (todas as peças), no sistema do mundo 3D.
func _caixa_do_modelo() -> AABB:
	var caixa := AABB()
	for m in _modelo.find_children("*", "MeshInstance3D", true, false):
		var mi := m as MeshInstance3D
		var t := mi.transform
		var pai := mi.get_parent()
		while pai != null and pai != _viewport:
			t = (pai as Node3D).transform * t
			pai = pai.get_parent()
		var c := t * mi.get_aabb()
		caixa = c if caixa.size == Vector3.ZERO else caixa.merge(c)
	return caixa


## Troca o material de cada superfície pela versão em pixel art (toon + contorno), mantendo a cor do .glb.
func _pixelizar(m: MeshInstance3D) -> void:
	for i in m.mesh.get_surface_count():
		var original := m.get_active_material(i) as BaseMaterial3D
		var cor := Color.WHITE
		var textura: Texture2D = null
		var emissao := Color.BLACK
		var transparente := false
		if original != null:
			cor = original.albedo_color
			if original.albedo_texture != null:
				if _peca_do_item(m) == "casco":   # pintura do casco: faixas largas, a textura fica legível
					textura = original.albedo_texture
				else:                              # ripas da teca viram ruído em baixa resolução: cor média
					cor *= _cor_media(original.albedo_texture)
			transparente = original.transparency != BaseMaterial3D.TRANSPARENCY_DISABLED
			if original.emission_enabled:
				emissao = original.emission * original.emission_energy_multiplier
		if transparente:
			var vidro := ShaderMaterial.new()
			vidro.shader = TOON_VIDRO
			vidro.set_shader_parameter("cor", Color(cor, clampf(cor.a, 0.25, 0.6)))
			m.set_surface_override_material(i, vidro)
		else:
			var toon := _toon(cor, textura, emissao)
			var contorno := ShaderMaterial.new()
			contorno.shader = CONTORNO
			contorno.set_shader_parameter("cor", Paleta.FUNDO_ESCURO)
			toon.next_pass = contorno
			_contornos.append(contorno)
			m.set_surface_override_material(i, toon)
		m.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF if transparente else GeometryInstance3D.SHADOW_CASTING_SETTING_ON


## "casco", "conves", "casaria", "internos" ou "acabamento": a peça do item a que a malha pertence.
func _peca_do_item(no: Node) -> String:
	for a in range(12):
		if no == null:
			break
		if no.has_meta("extras"):
			var e: Dictionary = no.get_meta("extras")
			if e.has("peca"):
				return str(e["peca"])
		no = no.get_parent()
	return ""


func _cor_media(textura: Texture2D) -> Color:
	if _medias.has(textura):
		return _medias[textura]
	var imagem := textura.get_image()
	var media := Color.WHITE
	if imagem != null:
		if imagem.is_compressed():
			imagem.decompress()
		var soma := Color(0, 0, 0, 0)
		var passo := maxi(1, imagem.get_width() / 32)
		var n := 0
		for y in range(0, imagem.get_height(), passo):
			for x in range(0, imagem.get_width(), passo):
				soma += imagem.get_pixel(x, y)
				n += 1
		media = soma / float(maxi(1, n))
	_medias[textura] = media
	return media


func _toon(cor: Color, textura: Texture2D, emissao: Color) -> ShaderMaterial:
	var mat := ShaderMaterial.new()
	mat.shader = TOON
	mat.set_shader_parameter("cor", cor)
	mat.set_shader_parameter("emissao", Vector3(emissao.r, emissao.g, emissao.b))
	if textura != null:
		mat.set_shader_parameter("textura", textura)
		mat.set_shader_parameter("usar_textura", true)
	return mat


func _montar_berco(comprimento: float, boca: float) -> Node3D:
	var berco := Node3D.new()
	for u in [0.2, 0.5, 0.75]:
		var bloco := MeshInstance3D.new()
		var caixa := BoxMesh.new()
		caixa.size = Vector3(0.3, ALTURA_BERCO, boca * 0.7)
		bloco.mesh = caixa
		bloco.position = Vector3(u * comprimento - comprimento / 2.0, ALTURA_BERCO / 2.0, 0)
		bloco.material_override = _toon(Paleta.AGUA_ESCURA, null, Color.BLACK)
		berco.add_child(bloco)
	return berco


## Mostra só as peças montadas até o gate atual do barco (opcionais ficam de fora: o MVP não tem configuração).
func _aplicar_gate() -> void:
	var etapa: int = ETAPA_DO_GATE.get(str(barco.get("gateAtual", "ET")), 5)
	for no in _itens:
		var e: Dictionary = no.get_meta("extras")
		no.visible = bool(e.get("padrao", false)) and int(e.get("etapa", 0)) <= etapa
	var na_agua := etapa >= 5
	_agua.visible = na_agua
	_berco.visible = not na_agua
	var calado: float = dados.get("dims", {}).get("T", 0.5)
	_modelo.position.y = 0.0 if na_agua else ALTURA_BERCO
	_agua.position.y = calado if na_agua else -1.0


# ---------------------------------------------------------------- câmera

func _atualizar_camera() -> void:
	var direcao := Vector3(cos(_arfagem) * sin(_guinada), -sin(_arfagem), cos(_arfagem) * cos(_guinada))
	_camera.position = _pivo.position + direcao * _distancia
	_camera.look_at(_pivo.position, Vector3.UP)
	# contorno de ~1 pixel do 3D, qualquer que seja a distância
	var metros_por_pixel := 2.0 * _distancia * tan(deg_to_rad(_camera.fov / 2.0)) / maxf(1.0, _viewport.size.y)
	for c in _contornos:
		c.set_shader_parameter("espessura", metros_por_pixel * 0.9)


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton:
		var b := event as InputEventMouseButton
		if b.button_index == MOUSE_BUTTON_LEFT:
			_arrastando = b.pressed
		elif b.pressed and b.button_index == MOUSE_BUTTON_WHEEL_UP:
			_aproximar(0.88)
		elif b.pressed and b.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			_aproximar(1.0 / 0.88)
		accept_event()
	elif event is InputEventMouseMotion and _arrastando:
		var mov := (event as InputEventMouseMotion).relative
		_guinada -= mov.x * 0.01
		_arfagem = clampf(_arfagem + mov.y * 0.008, deg_to_rad(-85), deg_to_rad(-3))
		_atualizar_camera()
		accept_event()


func _aproximar(fator: float) -> void:
	var comprimento: float = dados.get("dims", {}).get("L", 10.0)
	_distancia = clampf(_distancia * fator, comprimento * 0.5, comprimento * 3.0)
	_atualizar_camera()


func _unhandled_input(event: InputEvent) -> void:
	if not visible or not event is InputEventKey or not event.pressed:
		return
	match (event as InputEventKey).keycode:
		KEY_LEFT:
			_guinada += 0.1
		KEY_RIGHT:
			_guinada -= 0.1
		KEY_UP:
			_aproximar(0.9)
		KEY_DOWN:
			_aproximar(1.0 / 0.9)
		_:
			return
	_atualizar_camera()
	get_viewport().set_input_as_handled()


# ---------------------------------------------------------------- moldura

func _draw() -> void:
	draw_rect(Rect2(0, 0, size.x, ALTURA_TITULO), Paleta.FUNDO)
	draw_rect(Rect2(0, size.y - ALTURA_DICA, size.x, ALTURA_DICA), Paleta.FUNDO)
	if barco.is_empty():
		return
	draw_string(fonte_titulo, Vector2(6, 12), "INSPEÇÃO 3D · CASCO %s" % barco["id"], HORIZONTAL_ALIGNMENT_LEFT, -1,
			tamanho_titulo, Paleta.ALERTA)
	var gate := "%s · %s" % [barco.get("gateAtual", ""), barco.get("nomeGate", "")]
	draw_string(fonte, Vector2(0, 12), gate, HORIZONTAL_ALIGNMENT_RIGHT, size.x - 6, tamanho_texto, Paleta.TEXTO_CLARO)
	var dica := "Arraste para girar · roda ou setas aproximam · Esc volta"
	if str(barco.get("gateAtual", "")) == "RE":
		dica = "O casco ainda não saiu do molde (RE: recebimento de materiais) · Esc volta"
	draw_string(fonte, Vector2(6, size.y - 3), dica, HORIZONTAL_ALIGNMENT_LEFT, size.x - 12, tamanho_texto, Paleta.TEXTO)
