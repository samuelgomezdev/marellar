class_name Inspecao3D
extends Control
## Inspeção 3D dentro do jogo, em pixel art: o barco selecionado no estado do gate atual.
## A malha vem de assets/barcos/marellar-<linha>.glb (exportada da página three.js; veja docs/ferramentas/exportar_barco.mjs).
## O visual é do jogo: cena em baixa resolução, luz em degraus, contorno escuro e redução para a Endesga 32.
## Vistas: exterior (órbita), raio-X, corte longitudinal, plantas (de cima, ortográficas) e, por compartimento,
## "De cima" e "Entrar" (primeira pessoa). Clicar numa peça abre o formulário de NC; a NC vira a ordem
## RegistrarNcInspecao do motor e aparece como marcador colorido pela gravidade.
## Nenhuma regra de jogo aqui: o gate, o barco e as NCs vêm do estado do motor.

signal fechar
signal registrar_nc(ordem: Dictionary)

const REDUCAO := 1            # pixels da tela base (640x360) por pixel do 3D: 1 = resolução cheia do jogo
## Reduzir para a Endesga 32 troca as cores dos itens (teca vira laranja, cinzas viram azul); fica desligado
## e o pixel art vem do pixel nítido, da luz em degraus e do contorno.
const REDUZIR_PALETA := false
const ALTURA_TITULO := 16
const ALTURA_BOTOES := 22
const ALTURA_BERCO := 0.6
const SEM_CORTE := 1000.0
## Gate do motor -> etapa de montagem do modelo (gates da página 3D: DM, BA, BF convés, BF casaria, ST/CT, PI).
const ETAPA_DO_GATE := {"RE": -1, "DM": 0, "BA": 1, "BF": 3, "ST": 4, "CT": 4, "PI": 5, "TR": 5, "TM": 5, "ET": 5}
const VISTAS := [["ext", "Exterior"], ["raiox", "Raio-X"], ["corte", "Corte"], ["inf", "Planta inf."], ["pri", "Planta sup."]]
const VELOCIDADE_ANDAR := 2.2  # m/s em primeira pessoa
const COR_GRAVIDADE := {"LEVE": Color("#fee761"), "MEDIA": Color("#f77622"), "CRITICA": Color("#e43b44")}
const LARGURA_FORMULARIO := 236

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
var vista := "ext"                   # ext, raiox, corte, inf, pri, cima (compartimento) ou dentro
var compartimento: Dictionary = {}   # compartimento escolhido (de dados.compartimentos)
var _linha := 0
var _container: SubViewportContainer
var _viewport: SubViewport
var _camera: Camera3D
var _luz_cabine: OmniLight3D
var _sol: DirectionalLight3D
var _pivo: Node3D                    # centro do barco; a câmera orbita em volta dele
var _modelo: Node3D
var _berco: Node3D
var _agua: MeshInstance3D
var _itens: Array[Node3D] = []       # nós de item (com metadado "extras")
var _contornos: Array[ShaderMaterial] = []
var _materiais := {}                 # MeshInstance3D -> [material normal por superfície]
var _fantasma := ShaderMaterial.new()
var _medias := {}                    # textura -> cor média
var _guinada := deg_to_rad(-35.0)    # rotação em volta do eixo vertical
var _arfagem := deg_to_rad(-22.0)    # inclinação da câmera
var _distancia := 18.0
var _tamanho_orto := 8.0
var _centro_orto := Vector3.ZERO
var _olho := Vector3.ZERO            # primeira pessoa, em coordenadas do barco
var _rumo := 0.0                     # 0 = proa, +90° = boreste
var _inclinacao := 0.0
var _arrastando := false
var _botoes_vista := {}
var _lista_comps: OptionButton
var _dica: Label
var _corpos := {}                    # StaticBody3D -> MeshInstance3D (colisão de cada malha, para o clique)
var _corte_y := SEM_CORTE
var _corte_z := SEM_CORTE
var _destacado: Node3D = null        # item sob o mouse
var _toque := Vector2(-1, -1)        # onde o botão foi apertado (clique sem arrastar abre a NC)
var _ncs: Array = []                 # NcInspecao3d do motor, só deste barco
var _marcas: Control
var _formulario: FormularioNc3d
var _texto_item := ""

## Aberta é o estado (a lógica usa isto); o `visible` acompanha com a animação de abrir e fechar (Movimento).
var aberta := false:
	set(v):
		if v == aberta:
			return
		aberta = v
		if v:
			Movimento.abrir(self)
		else:
			Movimento.fechar(self)
var _botao_fechar: Button


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	_container = SubViewportContainer.new()
	_container.stretch = true
	_container.stretch_shrink = REDUCAO
	_container.texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	_container.mouse_filter = Control.MOUSE_FILTER_IGNORE
	if REDUZIR_PALETA:
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
	_fantasma.shader = TOON_VIDRO
	_fantasma.set_shader_parameter("cor", Color(Paleta.PISO, 0.13))
	_montar_mundo()
	_marcas = Control.new()
	_marcas.mouse_filter = Control.MOUSE_FILTER_IGNORE
	_marcas.draw.connect(_desenhar_marcas)
	add_child(_marcas)
	_montar_botoes()
	_dica = Label.new()
	_dica.add_theme_color_override("font_color", Paleta.TEXTO)
	var fundo_dica := StyleBoxFlat.new()
	fundo_dica.bg_color = Color(Paleta.FUNDO_ESCURO, 0.85)
	fundo_dica.content_margin_left = 3
	fundo_dica.content_margin_right = 3
	_dica.add_theme_stylebox_override("normal", fundo_dica)
	_dica.size_flags_horizontal = Control.SIZE_SHRINK_BEGIN
	_dica.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(_dica)
	_formulario = FormularioNc3d.new()
	_formulario.visible = false
	_formulario.confirmar.connect(_confirmar_nc)
	_formulario.cancelar.connect(_fechar_formulario)
	add_child(_formulario)
	resized.connect(_posicionar)
	_posicionar()


func _posicionar() -> void:
	if _container == null:
		return
	_container.position = Vector2(0, ALTURA_TITULO)
	_container.size = Vector2(size.x, size.y - ALTURA_TITULO - ALTURA_BOTOES)
	var barra := get_node("Botoes") as HBoxContainer
	barra.position = Vector2(4, size.y - ALTURA_BOTOES + 2)
	barra.size = Vector2(size.x - 8, ALTURA_BOTOES - 4)
	_dica.position = Vector2(4, size.y - ALTURA_BOTOES - 17)
	_dica.size = Vector2(0, 15)   # largura do texto
	_marcas.position = _container.position
	_marcas.size = _container.size
	_formulario.position = Vector2(size.x - LARGURA_FORMULARIO - 4, ALTURA_TITULO + 4)
	_formulario.size = Vector2(LARGURA_FORMULARIO, 0)
	_formulario.custom_minimum_size.x = LARGURA_FORMULARIO
	_botao_fechar.position = Vector2(size.x - 18, 0)
	_botao_fechar.size = Vector2(16, 16)


## Abre a inspeção de um barco do estado (BarcoVisao do motor).
func abrir(novo: Dictionary) -> void:
	barco = novo
	var linha := int(str(barco.get("linha", "L33")).trim_prefix("L"))
	if linha != _linha:
		_carregar_linha(linha)
	_aplicar_gate()
	_formulario.visible = false
	aberta = true
	mudar_vista("ext")


## Esc: de uma vista qualquer volta ao exterior; no exterior, devolve false (quem chamou fecha a inspeção).
func voltar() -> bool:
	if Movimento.aberto(_formulario):
		_fechar_formulario()
		return true
	if vista == "ext":
		return false
	mudar_vista("ext")
	return true


# ---------------------------------------------------------------- botões

func _montar_botoes() -> void:
	var barra := HBoxContainer.new()
	barra.name = "Botoes"
	barra.add_theme_constant_override("separation", 3)
	add_child(barra)
	for v in VISTAS:
		var b := _botao(v[1], mudar_vista.bind(v[0]))
		b.toggle_mode = true
		barra.add_child(b)
		_botoes_vista[v[0]] = b
	barra.add_child(VSeparator.new())
	_lista_comps = OptionButton.new()
	_lista_comps.fit_to_longest_item = false
	_lista_comps.custom_minimum_size.x = 150
	_lista_comps.clip_text = true
	_lista_comps.item_selected.connect(func(i): compartimento = (dados.get("compartimentos", []) as Array)[i])
	barra.add_child(_lista_comps)
	var cima := _botao("De cima", func(): mudar_vista("cima"))
	cima.toggle_mode = true
	barra.add_child(cima)
	_botoes_vista["cima"] = cima
	var dentro := _botao("Entrar", func(): mudar_vista("dentro"))
	dentro.toggle_mode = true
	barra.add_child(dentro)
	_botoes_vista["dentro"] = dentro
	_botao_fechar = _botao("X", func(): fechar.emit())
	_botao_fechar.tooltip_text = "Fechar (Esc)"
	add_child(_botao_fechar)


## Ao abrir, o foco vai para a vista em uso (Exterior ao abrir).
func focar_acao_principal() -> void:
	var b: Button = _botoes_vista.get(vista, _botoes_vista["ext"])
	b.call_deferred("grab_focus")


## Formulário fechado (Cancelar, X, Esc ou clique fora): o foco volta para a vista se tiver ficado no formulário.
func _fechar_formulario() -> void:
	var dono := get_viewport().gui_get_focus_owner()
	Movimento.fechar(_formulario)
	if dono == null or _formulario.is_ancestor_of(dono):
		focar_acao_principal()


func _botao(texto: String, acao: Callable) -> Button:
	var b := Button.new()
	b.text = texto
	b.pressed.connect(acao)
	return b


# ---------------------------------------------------------------- mundo 3D

func _montar_mundo() -> void:
	var ambiente := Environment.new()
	ambiente.background_mode = Environment.BG_COLOR
	ambiente.background_color = Paleta.FUNDO_ESCURO
	ambiente.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	ambiente.ambient_light_color = Color.WHITE
	ambiente.ambient_light_energy = 0.2
	ambiente.tonemap_mode = Environment.TONE_MAPPER_LINEAR
	var mundo := WorldEnvironment.new()
	mundo.environment = ambiente
	_viewport.add_child(mundo)

	_sol = DirectionalLight3D.new()
	_sol.rotation = Vector3(deg_to_rad(-55), deg_to_rad(-30), 0)
	_sol.light_energy = 0.9   # lado iluminado ~ a cor do item
	_sol.shadow_enabled = true
	_sol.directional_shadow_max_distance = 40.0
	_viewport.add_child(_sol)

	var chao := MeshInstance3D.new()
	var plano := PlaneMesh.new()
	plano.size = Vector2(60, 60)
	chao.mesh = plano
	chao.material_override = _toon(Paleta.ARMAZEM_ESCURO, null, Color.BLACK, false)
	_viewport.add_child(chao)

	_agua = MeshInstance3D.new()
	var agua := PlaneMesh.new()
	agua.size = Vector2(60, 60)
	_agua.mesh = agua
	var mat_agua := ShaderMaterial.new()
	mat_agua.shader = TOON_VIDRO
	mat_agua.set_shader_parameter("cor", Color(Paleta.AGUA, 0.7))
	mat_agua.set_shader_parameter("cortavel", false)
	_agua.material_override = mat_agua
	_agua.visible = false
	_viewport.add_child(_agua)

	_pivo = Node3D.new()
	_viewport.add_child(_pivo)
	_camera = Camera3D.new()
	_camera.fov = 32.0
	_camera.near = 0.05
	_camera.far = 200.0
	_viewport.add_child(_camera)
	_luz_cabine = OmniLight3D.new()     # primeira pessoa: luz junto do olho, como a lanterna do inspetor
	_luz_cabine.omni_range = 6.0
	_luz_cabine.omni_attenuation = 1.5
	_luz_cabine.light_energy = 0.8
	_luz_cabine.visible = false
	_camera.add_child(_luz_cabine)


func _carregar_linha(linha: int) -> void:
	if _modelo != null:
		_modelo.queue_free()
		_berco.queue_free()
	_itens.clear()
	_contornos.clear()
	_materiais.clear()
	_linha = linha
	var arquivo := FileAccess.open("res://assets/barcos/marellar-%d.json" % linha, FileAccess.READ)
	dados = JSON.parse_string(arquivo.get_as_text()) if arquivo != null else {}
	var cena: PackedScene = load("res://assets/barcos/marellar-%d.glb" % linha)
	_modelo = cena.instantiate()
	_modelo.position = Vector3(-_dim("L", 10.0) / 2.0, ALTURA_BERCO, 0)   # x do modelo vai da popa (0) à proa (L)
	_viewport.add_child(_modelo)
	for no in _modelo.find_children("*", "Node3D", true, false):
		if no.has_meta("extras") and (no.get_meta("extras") as Dictionary).has("codigo"):
			_itens.append(no)
	_corpos.clear()
	for m in _modelo.find_children("*", "MeshInstance3D", true, false):
		var mi := m as MeshInstance3D
		_pixelizar(mi)
		mi.create_trimesh_collision()   # o clique acha a peça por raio (física do SubViewport)
		for filho in mi.get_children():
			if filho is StaticBody3D:
				_corpos[filho] = mi
	_formulario.catalogo = dados.get("defeitos", [])
	_formulario.categorias = dados.get("categorias", {})
	_berco = _montar_berco(_dim("L", 10.0), _dim("B", 3.4))
	_viewport.add_child(_berco)
	var caixa := _caixa_do_modelo()
	_distancia = caixa.size.x * 1.15
	_pivo.position = Vector3(caixa.get_center().x, ALTURA_BERCO + minf(caixa.size.y * 0.25, 1.0), 0)
	_lista_comps.clear()
	for c in dados.get("compartimentos", []):
		_lista_comps.add_item(str(c["nome"]))
	if _lista_comps.item_count > 0:
		_lista_comps.select(0)
		compartimento = dados["compartimentos"][0]


func _dim(nome: String, padrao: float) -> float:
	return float(dados.get("dims", {}).get(nome, padrao))


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
	var normais: Array[Material] = []
	for i in m.mesh.get_surface_count():
		var original := m.get_active_material(i) as BaseMaterial3D
		var cor := Color.WHITE
		var textura: Texture2D = null
		var emissao := Color.BLACK
		var transparente := false
		if original != null:
			cor = original.albedo_color
			if original.albedo_texture != null:
				if _extras_do_item(m).get("peca", "") == "casco":   # pintura do casco: faixas largas, a textura fica legível
					textura = original.albedo_texture
				else:                                                # ripas da teca viram ruído em baixa resolução: cor média
					cor *= _cor_media(original.albedo_texture)
			transparente = original.transparency != BaseMaterial3D.TRANSPARENCY_DISABLED
			if original.emission_enabled:
				emissao = original.emission * original.emission_energy_multiplier
		var novo: ShaderMaterial
		if transparente:
			novo = ShaderMaterial.new()
			novo.shader = TOON_VIDRO
			novo.set_shader_parameter("cor", Color(cor, clampf(cor.a, 0.25, 0.6)))
		else:
			novo = _toon(cor, textura, emissao, true)
			var contorno := ShaderMaterial.new()
			contorno.shader = CONTORNO
			contorno.set_shader_parameter("cor", Paleta.FUNDO_ESCURO)
			novo.next_pass = contorno
			_contornos.append(contorno)
		m.set_surface_override_material(i, novo)
		normais.append(novo)
	m.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_ON
	_materiais[m] = normais


## Dados do item (extras do glTF) a que a malha pertence.
func _extras_do_item(no: Node) -> Dictionary:
	for a in range(12):
		if no == null:
			break
		if no.has_meta("extras"):
			var e: Dictionary = no.get_meta("extras")
			if e.has("codigo"):
				return e
		no = no.get_parent()
	return {}


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


func _toon(cor: Color, textura: Texture2D, emissao: Color, cortavel: bool) -> ShaderMaterial:
	var mat := ShaderMaterial.new()
	mat.shader = TOON
	mat.set_shader_parameter("cor", cor)
	mat.set_shader_parameter("emissao", Vector3(emissao.r, emissao.g, emissao.b))
	mat.set_shader_parameter("cortavel", cortavel)
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
		bloco.material_override = _toon(Paleta.AGUA_ESCURA, null, Color.BLACK, false)
		berco.add_child(bloco)
	return berco


## Mostra só as peças montadas até o gate atual do barco (opcionais ficam de fora: o MVP não tem configuração).
func _aplicar_gate() -> void:
	var etapa: int = ETAPA_DO_GATE.get(str(barco.get("gateAtual", "ET")), 5)
	for no in _itens:
		var e: Dictionary = no.get_meta("extras")
		no.visible = bool(e.get("padrao", false)) and int(e.get("etapa", 0)) <= etapa
	for corpo in _corpos:   # peça escondida não recebe clique
		(corpo as StaticBody3D).collision_layer = 1 if (_corpos[corpo] as Node3D).is_visible_in_tree() else 0
	var na_agua := _na_agua()
	_agua.visible = na_agua
	_berco.visible = not na_agua
	_modelo.position.y = 0.0 if na_agua else ALTURA_BERCO
	_agua.position.y = _dim("T", 0.5) if na_agua else -1.0


# ---------------------------------------------------------------- vistas

func _na_agua() -> bool:
	return int(ETAPA_DO_GATE.get(str(barco.get("gateAtual", "ET")), 5)) >= 5


## Muda a vista: ext, raiox, corte, inf, pri (barco inteiro) ou cima, dentro (compartimento escolhido).
func mudar_vista(nova: String) -> void:
	if (nova == "cima" or nova == "dentro") and compartimento.is_empty():
		return
	vista = nova
	for chave in _botoes_vista:
		_botoes_vista[chave].set_pressed_no_signal(chave == nova)
	_aplicar_raio_x(nova == "raiox")
	var corte_y := SEM_CORTE
	var corte_z := SEM_CORTE
	var base := _modelo.position.y if _modelo != null else 0.0
	match nova:
		"corte":
			corte_z = 0.02            # some boreste: o corte fica na linha de centro
			_guinada = 0.0            # olhando de boreste para o corte
			_arfagem = deg_to_rad(-12.0)
		"inf":
			corte_y = base + _altura_planta_inferior()
		"pri":
			corte_y = base + _altura_planta_superior()
		"cima":
			var c := compartimento
			var corte = c.get("corte")
			corte_y = base + (float(corte) if corte != null else float(c["piso"]) + 1.9)
		"ext", "raiox":
			_guinada = deg_to_rad(-35.0)
			_arfagem = deg_to_rad(-22.0)
	RenderingServer.global_shader_parameter_set("inspecao_corte_y", corte_y)
	RenderingServer.global_shader_parameter_set("inspecao_corte_z", corte_z)
	_corte_y = corte_y
	_corte_z = corte_z
	# a água da piscina passa por dentro do casco: nas vistas cortadas e dentro do barco ela sai de cena
	_agua.visible = _na_agua() and nova in ["ext", "raiox"]
	if nova == "dentro":
		_entrar(compartimento)
	else:
		_luz_cabine.visible = false
		_sol.light_energy = 0.9
		_camera.fov = 32.0
	if nova in ["inf", "pri"]:
		_enquadrar_planta()
	elif nova == "cima":
		_enquadrar_compartimento(compartimento)
	_atualizar_camera()
	_atualizar_dica()
	queue_redraw()


## Planta inferior: logo abaixo do piso de quem anda no barco (porão, cabines); superior: acima dos móveis.
func _altura_planta_inferior() -> float:
	var piso = dados.get("dims", {}).get("piso")
	return float(piso["salao"]) - 0.15 if piso is Dictionary else _dim("Daft", 1.2) - 0.12


func _altura_planta_superior() -> float:
	var piso = dados.get("dims", {}).get("piso")
	return float(piso["salao"]) + 2.0 if piso is Dictionary else _dim("Daft", 1.2) + 1.2


## Raio-X: a casca (casco, convés, casaria, vidros) fica quase transparente e sem sombra.
func _aplicar_raio_x(ligado: bool) -> void:
	for m in _materiais:
		var mi := m as MeshInstance3D
		var casca: bool = _extras_do_item(mi).get("casca", false)
		var normais: Array = _materiais[mi]
		for i in normais.size():
			mi.set_surface_override_material(i, _fantasma if ligado and casca else normais[i])
		mi.cast_shadow = GeometryInstance3D.SHADOW_CASTING_SETTING_OFF if ligado and casca else GeometryInstance3D.SHADOW_CASTING_SETTING_ON


func _enquadrar_planta() -> void:
	var comprimento := _dim("L", 10.0)
	_centro_orto = Vector3(_pivo.position.x, 0, 0)
	_tamanho_orto = maxf((comprimento + 3.0) / _aspecto(), _dim("B", 3.4) + 1.5)


func _enquadrar_compartimento(c: Dictionary) -> void:
	var comprimento := _dim("L", 10.0)
	var boca := _dim("B", 3.4)
	var x0 := maxf(float(c["x0"]), -1.5)
	var x1 := minf(float(c["x1"]), comprimento + 0.5)
	var z0 := maxf(float(c["z0"]), -boca / 2.0 - 0.2)
	var z1 := minf(float(c["z1"]), boca / 2.0 + 0.2)
	_centro_orto = Vector3((x0 + x1) / 2.0 - comprimento / 2.0, 0, (z0 + z1) / 2.0)
	_tamanho_orto = maxf((x1 - x0 + 1.0) / _aspecto(), z1 - z0 + 1.0)


func _aspecto() -> float:
	return float(_viewport.size.x) / maxf(1.0, float(_viewport.size.y))


func _entrar(c: Dictionary) -> void:
	var e: Dictionary = c["entrada"]
	_olho = Vector3(float(e["x"]), float(e["y"]), float(e["z"]))
	_rumo = float(e["rumo"])
	_inclinacao = float(e["inclinacao"])
	_camera.fov = 70.0
	_luz_cabine.visible = true
	_sol.light_energy = 0.5   # dentro, as paredes finas deixam o sol vazar; quem dá volume é a luz de cabine


# ---------------------------------------------------------------- câmera

func _atualizar_camera() -> void:
	if _modelo == null:
		return
	var metros_por_pixel := 0.0
	if vista == "dentro":
		_camera.projection = Camera3D.PROJECTION_PERSPECTIVE
		_camera.position = _olho + _modelo.position
		_camera.rotation = Vector3(_inclinacao, -PI / 2.0 - _rumo, 0)
		metros_por_pixel = 0.004
	elif vista in ["inf", "pri", "cima"]:
		_camera.projection = Camera3D.PROJECTION_ORTHOGONAL
		_camera.size = _tamanho_orto
		_camera.position = _centro_orto + Vector3(0, 40, 0)
		_camera.look_at(_centro_orto, Vector3(0, 0, -1))   # proa para a direita, bombordo para cima
		metros_por_pixel = _tamanho_orto / maxf(1.0, _viewport.size.y)
	else:
		_camera.projection = Camera3D.PROJECTION_PERSPECTIVE
		var direcao := Vector3(cos(_arfagem) * sin(_guinada), -sin(_arfagem), cos(_arfagem) * cos(_guinada))
		_camera.position = _pivo.position + direcao * _distancia
		_camera.look_at(_pivo.position, Vector3.UP)
		metros_por_pixel = 2.0 * _distancia * tan(deg_to_rad(_camera.fov / 2.0)) / maxf(1.0, _viewport.size.y)
	for c in _contornos:   # contorno de ~1 pixel do 3D
		c.set_shader_parameter("espessura", metros_por_pixel * 0.9)


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton:
		var b := event as InputEventMouseButton
		if b.button_index == MOUSE_BUTTON_LEFT and b.pressed and Movimento.aberto(_formulario):
			_fechar_formulario()   # clique fora do formulário só fecha (M12-8)
			_toque = Vector2(-1000, -1000)
			accept_event()
			return
		if b.button_index == MOUSE_BUTTON_LEFT:
			_arrastando = b.pressed
			if b.pressed:
				_toque = b.position
			elif _toque.distance_to(b.position) < 4.0:
				clicar(b.position)
		elif b.pressed and b.button_index == MOUSE_BUTTON_WHEEL_UP:
			_aproximar(0.88)
		elif b.pressed and b.button_index == MOUSE_BUTTON_WHEEL_DOWN:
			_aproximar(1.0 / 0.88)
		accept_event()
	elif event is InputEventMouseMotion and not _arrastando:
		_passar((event as InputEventMouseMotion).position)
	elif event is InputEventMouseMotion and _arrastando:
		var mov := (event as InputEventMouseMotion).relative
		if vista == "dentro":
			_rumo += mov.x * 0.006
			_inclinacao = clampf(_inclinacao - mov.y * 0.006, -1.3, 1.3)
		elif vista in ["inf", "pri", "cima"]:
			var escala := _tamanho_orto / maxf(1.0, size.y)
			_centro_orto -= Vector3(mov.x, 0, mov.y) * escala
		else:
			_guinada -= mov.x * 0.01
			_arfagem = clampf(_arfagem + mov.y * 0.008, deg_to_rad(-85), deg_to_rad(-3))
		_atualizar_camera()
		accept_event()


func _aproximar(fator: float) -> void:
	var comprimento := _dim("L", 10.0)
	if vista == "dentro":
		_andar(Vector3(cos(_rumo), 0, sin(_rumo)) * (0.35 if fator < 1.0 else -0.35))
		return
	if vista in ["inf", "pri", "cima"]:
		_tamanho_orto = clampf(_tamanho_orto * fator, 1.5, comprimento * 1.5)
	else:
		_distancia = clampf(_distancia * fator, comprimento * 0.5, comprimento * 3.0)
	_atualizar_camera()


## Primeira pessoa: anda no plano, preso perto do barco (como na página three.js).
func _andar(passo: Vector3) -> void:
	var comprimento := _dim("L", 10.0)
	var boca := _dim("B", 3.4)
	_olho.x = clampf(_olho.x + passo.x, -3.0, comprimento + 2.0)
	_olho.z = clampf(_olho.z + passo.z, -boca / 2.0 - 1.0, boca / 2.0 + 1.0)
	_olho.y = clampf(_olho.y + passo.y, 0.5, 9.0)
	_atualizar_camera()


func _process(delta: float) -> void:
	if visible:
		_marcas.queue_redraw()   # os marcadores acompanham a câmera
	if not aberta or vista != "dentro":
		return
	var frente := Vector3(cos(_rumo), 0, sin(_rumo))
	var lado := Vector3(-sin(_rumo), 0, cos(_rumo))   # boreste
	var mov := Vector3.ZERO
	if Input.is_key_pressed(KEY_W) or Input.is_key_pressed(KEY_UP):
		mov += frente
	if Input.is_key_pressed(KEY_S) or Input.is_key_pressed(KEY_DOWN):
		mov -= frente
	if Input.is_key_pressed(KEY_D) or Input.is_key_pressed(KEY_RIGHT):
		mov += lado
	if Input.is_key_pressed(KEY_A) or Input.is_key_pressed(KEY_LEFT):
		mov -= lado
	if Input.is_key_pressed(KEY_R) or Input.is_key_pressed(KEY_PAGEUP):
		mov.y += 0.7
	if Input.is_key_pressed(KEY_F) or Input.is_key_pressed(KEY_PAGEDOWN):
		mov.y -= 0.7
	if mov != Vector3.ZERO:
		_andar(mov * VELOCIDADE_ANDAR * delta)


## As setas do teclado são da câmera (antes da navegação de foco da interface). Tab e o D-pad do controle
## continuam trocando de botão. Num campo de texto, as setas ficam com o campo.
func _input(event: InputEvent) -> void:
	if not aberta or not event is InputEventKey:
		return
	var tecla := (event as InputEventKey).keycode
	if not tecla in [KEY_LEFT, KEY_RIGHT, KEY_UP, KEY_DOWN]:
		return
	if get_viewport().gui_get_focus_owner() is LineEdit:
		return
	get_viewport().set_input_as_handled()
	if vista == "dentro" or not event.pressed:
		return   # em primeira pessoa o _process lê as setas direto
	match tecla:
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

func _atualizar_dica() -> void:
	var textos := {
		"ext": "Arraste para girar · roda ou setas aproximam · Esc volta à fábrica",
		"raiox": "Raio-X: a casca fica transparente · arraste para girar · Esc volta ao exterior",
		"corte": "Corte na linha de centro, visto de boreste · arraste para girar · Esc volta",
		"inf": "Planta inferior, vista de cima · arraste move · roda aproxima · Esc volta",
		"pri": "Planta superior, vista de cima · arraste move · roda aproxima · Esc volta",
		"cima": "De cima: %s · arraste move · roda aproxima · Esc volta" % compartimento.get("nome", ""),
		"dentro": "Dentro: %s · arraste olha · WASD anda · R/F sobe e desce · Esc sai" % compartimento.get("nome", ""),
	}
	var texto: String = textos.get(vista, "")
	if str(barco.get("gateAtual", "")) == "RE":
		texto = "O casco ainda não saiu do molde (RE: recebimento de materiais) · Esc volta"
	_dica.text = texto


# ---------------------------------------------------------------- NC no ponto clicado

## Peça sob um ponto da tela (coordenadas desta tela): {ponto (mundo), malha, extras} ou {} se não há peça.
## Ignora o que está cortado e, no raio-X, a casca (o clique passa por ela, como na página three.js).
func _acertar(pos: Vector2) -> Dictionary:
	if _modelo == null:
		return {}
	var no_3d := (pos - _container.position) / float(REDUCAO)
	var origem := _camera.project_ray_origin(no_3d)
	var destino := origem + _camera.project_ray_normal(no_3d) * 300.0
	var espaco := _camera.get_world_3d().direct_space_state   # own_world_3d: o mundo vem da câmera
	var excluir: Array[RID] = []
	for tentativa in 24:
		var consulta := PhysicsRayQueryParameters3D.create(origem, destino)
		consulta.exclude = excluir
		var r := espaco.intersect_ray(consulta)
		if r.is_empty():
			return {}
		var corpo: Object = r["collider"]
		var malha: MeshInstance3D = _corpos.get(corpo)
		var ponto: Vector3 = r["position"]
		var extras := _extras_do_item(malha) if malha != null else {}
		var cortado := ponto.y > _corte_y + 0.001 or ponto.z > _corte_z + 0.001
		if malha == null or cortado or (vista == "raiox" and extras.get("casca", false)):
			excluir.append(r["rid"])
			continue
		return {"ponto": ponto, "malha": malha, "extras": extras}
	return {}


func _passar(pos: Vector2) -> void:
	var r := _acertar(pos)
	var item: Node3D = null
	if not r.is_empty():
		item = _no_do_item(r["malha"])
	if item == _destacado:
		return
	_acender(_destacado, false)
	_destacado = item
	_acender(item, true)
	_texto_item = "" if r.is_empty() else "%s · %s" % [r["extras"].get("codigo", ""), str(r["extras"].get("descricao", "")).capitalize()]


## Clique sem arrastar: abre o formulário de NC no ponto da peça.
func clicar(pos: Vector2) -> void:
	if str(barco.get("gateAtual", "")) == "RE":
		return
	var r := _acertar(pos)
	if r.is_empty():
		return
	var local: Vector3 = _modelo.to_local(r["ponto"])
	var extras: Dictionary = r["extras"]
	# como na página three.js: vale o compartimento do item; sem ele, a caixa do ambiente que contém o ponto
	var comp := str(extras.get("compartimento", "")) if extras.get("compartimento") != null else ""
	if comp.is_empty():
		comp = str(_compartimento_no_ponto(local).get("id", ""))
	var nome := ""
	for c in dados.get("compartimentos", []):
		if c["id"] == comp:
			nome = str(c["nome"])
	if nome.is_empty() and comp == "CASCO":
		nome = "Casco, obras vivas" if local.y <= _dim("T", 0.5) else "Casco, obras mortas"
	_formulario.abrir(local, extras, comp, nome, str(barco.get("gateAtual", "")))
	_posicionar()
	_formulario.reset_size()


func _compartimento_no_ponto(p: Vector3) -> Dictionary:
	for c in dados.get("compartimentos", []):
		if p.x >= float(c["x0"]) and p.x <= float(c["x1"]) and p.y >= float(c["y0"]) and p.y <= float(c["y1"]) \
				and p.z >= float(c["z0"]) and p.z <= float(c["z1"]):
			return c
	return {}


func _confirmar_nc(dados_nc: Dictionary) -> void:
	var ordem := dados_nc.duplicate()
	ordem["tipo"] = "RegistrarNcInspecao"
	ordem["barcoId"] = barco.get("id", "")
	registrar_nc.emit(ordem)


## Resposta do motor: erro aparece no formulário; sucesso fecha o formulário.
func resultado_nc(erro: String) -> void:
	if erro.is_empty():
		_fechar_formulario()   # NC registrada
	else:
		_formulario.mostrar_erro(erro)


## NCs da inspeção vindas do estado do motor (todas as da partida; aqui ficam só as deste barco).
func atualizar_ncs(todas: Array) -> void:
	_ncs = todas.filter(func(n): return n["barcoId"] == barco.get("id", ""))
	queue_redraw()


func _no_do_item(no: Node) -> Node3D:
	for a in range(12):
		if no == null:
			break
		if no.has_meta("extras") and (no.get_meta("extras") as Dictionary).has("codigo"):
			return no as Node3D
		no = no.get_parent()
	return null


func _acender(item: Node3D, ligado: bool) -> void:
	if item == null:
		return
	for m in item.find_children("*", "MeshInstance3D", true, false):
		for mat in _materiais.get(m, []):
			if mat is ShaderMaterial and (mat as ShaderMaterial).shader == TOON:
				(mat as ShaderMaterial).set_shader_parameter("destaque", Color(0.10, 0.22, 0.45) if ligado else Color.BLACK)


## Marcadores das NCs: quadrado de 7x7 pixels na cor da gravidade, com o número, sempre por cima.
func _desenhar_marcas() -> void:
	if _modelo == null:
		return
	for nc in _ncs:
		var local := Vector3(float(nc["x"]), float(nc["y"]), float(nc["z"]))
		var mundo := _modelo.to_global(local)
		if _camera.is_position_behind(mundo) or mundo.y > _corte_y + 0.01 or mundo.z > _corte_z + 0.01:
			continue
		var p := (_camera.unproject_position(mundo) * float(REDUCAO)).floor()
		var cor: Color = COR_GRAVIDADE.get(str(nc["gravidade"]), Paleta.ALERTA)
		_marcas.draw_rect(Rect2(p - Vector2(4, 4), Vector2(9, 9)), Paleta.FUNDO_ESCURO)
		_marcas.draw_rect(Rect2(p - Vector2(3, 3), Vector2(7, 7)), cor)
		_marcas.draw_string(fonte, p + Vector2(6, -2), str(int(nc["numero"])), HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto, cor)
	if not _texto_item.is_empty() and not Movimento.aberto(_formulario):   # item sob o mouse
		var largura := fonte.get_string_size(_texto_item, HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto).x + 8
		_marcas.draw_rect(Rect2(4, 4, largura, 15), Color(Paleta.FUNDO_ESCURO, 0.85))
		_marcas.draw_string(fonte, Vector2(8, 15), _texto_item, HORIZONTAL_ALIGNMENT_LEFT, -1, tamanho_texto, Paleta.TEXTO_CLARO)


# ---------------------------------------------------------------- moldura

func _draw() -> void:
	draw_rect(Rect2(0, 0, size.x, ALTURA_TITULO), Paleta.FUNDO)
	draw_rect(Rect2(0, size.y - ALTURA_BOTOES, size.x, ALTURA_BOTOES), Paleta.FUNDO)
	if barco.is_empty():
		return
	draw_string(fonte_titulo, Vector2(6, 12), "INSPEÇÃO 3D · CASCO %s" % barco["id"], HORIZONTAL_ALIGNMENT_LEFT, -1,
			tamanho_titulo, Paleta.ALERTA)
	var gate := "%s · %s" % [barco.get("gateAtual", ""), barco.get("nomeGate", "")]
	if not _ncs.is_empty():
		gate = "%d NC 3D · %s" % [_ncs.size(), gate]
	draw_string(fonte, Vector2(0, 12), gate, HORIZONTAL_ALIGNMENT_RIGHT, size.x - 24, tamanho_texto, Paleta.TEXTO_CLARO)
