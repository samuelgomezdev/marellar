extends Control
## Tela da Fábrica (MVP): baias com os barcos de 33 pés, bonequinhos trabalhando,
## barra de topo, painel à direita (ficha do barco, equipe ou fornecedores), avisos e barra de ordens.
## Arte provisória desenhada em código (scripts/sprites.gd) na paleta Endesga 32.
## Nenhuma regra de jogo aqui: tudo vem do motor Java pela API.

const SEMENTE := 42
const SEGUNDOS_POR_TICK_X1 := 2.5  # 1 dia útil (8 ticks) ≈ 20 s na velocidade 1 (GDD, Loops de jogo)
const MAX_TICKS_POR_AVANCO := 4    # alcança o relógio depois de uma resposta lenta, sem saltar meio dia
const ALTURA_TOPO := 22     # Pixelify Sans 16 (20 px de linha) e números em Jersey 20
const ALTURA_BASE := 28     # botões do tema com a Pixelify Sans 16: 22 px
const ALTURA_ABAS := 24     # faixa das abas G1/G2, logo abaixo da barra de cima
const SEGUNDOS_PISCAR := 1.2  # barcos clicáveis piscando depois de um clique em botão desativado
const LARGURA_PAINEL := 200
const TAMANHO_TEXTO := Tokens.TAMANHO_UI       # Pixelify Sans
const TAMANHO_TITULO := Tokens.TAMANHO_TITULO   # Jersey 10, só nos títulos de tela
const MAX_AVISOS := 6
const LARGURA_AVISO := 360   # aviso de pausa automática, no centro
const LARGURA_PROPOSTA := 420
const NOMES_OPCAO := {"CLIENTE": "Prazo do cliente", "SEM_DESISTENCIA": "Sem desistir", "FABRICA": "Prazo da fábrica"}


const NOMES_STATUS := {
	"EM_PRODUCAO": "Em produção",
	"AGUARDANDO_INSPECAO": "Na fila da inspeção",
	"EM_INSPECAO": "Em inspeção",
	"AGUARDANDO_DECISAO": "NC: decida!",
	"EM_RETRABALHO": "Em retrabalho",
	"APROVADO": "Aprovado, segurado",
	"PARADO_MATERIAL": "Parado: sem material",
	"ENTREGUE": "Entregue",
}
const NOMES_GRAVIDADE := {"LEVE": "leve", "MEDIA": "média", "CRITICA": "CRÍTICA"}
const NOMES_SITUACAO := {
	"ATIVA": "", "A_CAMINHO": "chega em %d d", "EM_TREINAMENTO": "treino %d d",
	"AFASTADA": "afastado %d d", "DESLIGADA": "saiu",
}
const NOMES_GALPAO := {"G1": "Laminação", "G2": "Montagem"}
const DICA_SEM_BARCO := "Clique num barco"
const DICA_ENTREGUE := "Barco já entregue"
const PAINEL_BARCO := "barco"
const PAINEL_EQUIPE := "equipe"
const PAINEL_FORNECEDORES := "fornecedores"
const NOMES_STATUS_NC := {
	"ABERTA": "aberta", "EM_RETRABALHO": "em retrabalho", "RESOLVIDA": "resolvida",
	"CONCEDIDA": "concessão",
}

var estado: Dictionary = {}
var velocidade := 0
var acumulado := 0.0
var ocupado := false
var ultimo_evento := 0
var avisos: Array[Dictionary] = []  # {texto, quando}
var area_cartao := Rect2()
var galpao := "G2"  # galpão na tela: G1 laminação ou G2 montagem
var botoes_abas := {}  # "G1"/"G2" -> Button na faixa das abas
var barra_abas: HBoxContainer
var piscar_ate := -1.0  # até quando os barcos clicáveis piscam (segundos de Time.get_ticks_msec)
var efeitos: Array[Dictionary] = []  # {tipo, texto, cor, barco, inicio}
var posicao_barcos := {}  # última posição conhecida de cada barco, para os efeitos
var gate_anterior := {}
var pos_inspetora := Vector2(-1, -1)
var inspetora_andando := false
var area_alerta_nc := Rect2()
var area_alerta_proposta := Rect2()
var proposta_alerta := ""          # id da proposta aberta mostrada no alerta (a mais antiga)
var tela_proposta: PanelContainer
var proposta_na_tela := ""         # id da proposta na janela
var assinatura_proposta := ""      # a janela só se refaz quando a proposta muda (as datas andam com o dia)
var barco_alerta_nc := ""
var erro_api := ""
var selecionado := ""
var areas_barcos := {}  # id do barco -> Rect2 clicável
var assinatura_ficha := ""
var painel := PAINEL_BARCO

var fonte: Font
var fonte_titulo: Font
var botoes_velocidade := {}
var botao_prioridade: Button
var botao_segurar: Button
var ficha: VBoxContainer
var rolagem: ScrollContainer
var botoes_painel := {}
var tela_conselho: PanelContainer
var tela_gestao: PanelContainer
var gestao_fechamento := false   # painel aberto no modo fechamento do mês
var tela_saves: PanelContainer
var tela_ficha: FichaBarco
var tela_3d: Inspecao3D
var tela_rodada: Rodada2D
var botao_ficha: Button
var botao_conselho: Button
var botao_carregar: Button
var camada_foco: Control   # botões sem fundo por cima do que é desenhado à mão (barcos, cartão, alerta de NC)
var botoes_barcos := {}    # id do barco -> Button sem fundo na área clicável do barco
var botao_cartao: Button
var botao_alerta: Button
var botao_proposta: Button
var botao_fechar_painel: Button   # X do painel lateral (Equipe/Fornecedores)
var painel_desenhado := ""   # painel lateral na tela (continua desenhado enquanto esmaece ao fechar)
var alfa_painel := 0.0:       # transparência do painel lateral na animação de abrir/fechar (Movimento)
	set(v):
		alfa_painel = v
		if rolagem != null:
			rolagem.modulate.a = v
			botao_fechar_painel.modulate.a = v
var tela_aviso: PanelContainer   # aviso de pausa automática (M12-1)
var pausa_auto := ""       # motivo da pausa automática em vigor ("" = jogo rodando ou pausado pelo jogador)
var velocidade_antes := 1  # velocidade de antes da pausa automática: Continuar volta para ela
var ultima_mensagem := {}  # tipo do evento -> última mensagem (o aviso de pausa repete a do evento)
var medir_ate := -1.0      # --medir N: até quando (segundos) mede quadros e chamadas antes de sair
var quadros_ms: Array[float] = []
var desenho_ms: Array[float] = []
var interface_ms: Array[float] = []   # tempo de _atualizar_interface depois de cada avanço
var foco_ao_fechar := {}   # painel -> Control que tinha o foco antes de abrir (o foco volta para ele ao fechar)


func _ready() -> void:
	# Fontes e estilos vêm do tema do projeto (assets/tema, gerado do Tokens).
	fonte = ThemeDB.get_project_theme().default_font
	fonte_titulo = load(Tokens.FONTE_TITULO)
	Api.erro.connect(_on_erro_api)
	_ajustar_entrada()
	_montar_camada_foco()
	_montar_barra_inferior()
	_montar_abas()
	_montar_ficha()
	_montar_ficha_completa()
	_montar_inspecao_3d()
	_atualizar_interface()
	PixelArt.gerar_tudo()   # antes da partida: nenhum sprite grande é gerado no meio do jogo (M12-3)
	if await Api.criar_partida(SEMENTE):
		await _atualizar_estado()
		_avisar("Partida criada. Espaço pausa; 1, 2 e 3 mudam a velocidade. Clique num barco.")
		_opcoes_de_desenvolvimento(OS.get_cmdline_user_args())


## Opções para testes e capturas de tela, passadas depois de "--":
##   godot --path client-godot -- --dias 12 --selecionar 33-002 --rodar
func _opcoes_de_desenvolvimento(args: PackedStringArray) -> void:
	var i := args.find("--dias")
	if i >= 0 and i + 1 < args.size():
		var faltam := int(args[i + 1]) * 8
		while faltam > 0:
			var resposta = await Api.avancar(mini(faltam, 168))
			if resposta == null:
				return
			estado = resposta["estado"]
			faltam -= int(resposta["resultado"]["ticksExecutados"])
		await _buscar_eventos()
	if args.has("--rodada"):   # avança até a primeira pausa da rodada e abre a tela (capturas)
		var k := args.find("--pular")   # --pular K: passa as K primeiras rodadas (ficam com o inspetor)
		var pular := int(args[k + 1]) if k >= 0 and k + 1 < args.size() else 0
		for _volta in 400:
			var resposta = await Api.avancar(8)
			if resposta == null:
				return
			estado = resposta["estado"]
			if resposta["resultado"]["motivoPausa"] == "RODADA_INSPECAO":
				if pular <= 0:
					break
				pular -= 1
		await _buscar_eventos()
		_atualizar_interface()
		_abrir_rodada(str(estado.get("barcoPausa", "")))
		var j := args.find("--marcar")   # --marcar M1,M2: marca e conclui depois de 1 s
		if j >= 0 and j + 1 < args.size():
			await get_tree().create_timer(1.0).timeout
			for id in args[j + 1].split(","):
				tela_rodada._alternar(id)
			await get_tree().create_timer(1.0).timeout
			tela_rodada._enviar()
	if args.has("--proposta"):   # avança até o vendedor trazer uma proposta e abre a janela dela (capturas)
		for _dia in 80:
			if estado.has("vendas") and not (estado["vendas"]["propostas"] as Array).is_empty():
				break
			var resposta = await Api.avancar(8)
			if resposta == null:
				return
			estado = resposta["estado"]
		await _buscar_eventos()
		_atualizar_interface()
		_abrir_proposta(str(estado["vendas"]["propostas"][0]["id"]) if not (estado["vendas"]["propostas"] as Array).is_empty() else "")
	if args.has("--ate-pausa"):   # avança até a próxima NC crítica ou acidente e mostra o aviso (capturas)
		for _volta in 200:
			var resposta = await Api.avancar(168)
			if resposta == null:
				return
			estado = resposta["estado"]
			var motivo = resposta["resultado"]["motivoPausa"]
			if motivo in ["NC_CRITICA", "ACIDENTE", "DERROTA"]:
				await _buscar_eventos()
				velocidade = 4
				_pausa_automatica(str(motivo))
				break
		if args.has("--apertar"):   # aperta o botão em foco no aviso (testa a ação principal)
			await get_tree().create_timer(1.0).timeout
			var dono := get_viewport().gui_get_focus_owner()
			if dono is BaseButton:
				(dono as BaseButton).pressed.emit()
	if args.has("--salvar"):
		await _salvar()
	if args.has("--carregar"):
		await _carregar_ultimo_save()
	i = args.find("--selecionar")
	if i >= 0 and i + 1 < args.size():
		selecionado = args[i + 1]
	i = args.find("--galpao")
	if i >= 0 and i + 1 < args.size():
		galpao = args[i + 1]
	if args.has("--ficha"):
		_abrir_ficha_completa()
	if args.has("--inspecao"):
		_abrir_inspecao_3d()
		i = args.find("--gate")   # só para capturas: mostra a inspeção como se o barco estivesse noutro gate
		if i >= 0 and i + 1 < args.size() and tela_3d.aberta:
			var copia := _barco(selecionado).duplicate()
			copia["gateAtual"] = args[i + 1]
			tela_3d.abrir(copia)
		i = args.find("--comp")
		if i >= 0 and i + 1 < args.size() and tela_3d.aberta:
			for c in tela_3d.dados.get("compartimentos", []):
				if c["id"] == args[i + 1]:
					tela_3d.compartimento = c
		i = args.find("--vista")
		if i >= 0 and i + 1 < args.size() and tela_3d.aberta:
			tela_3d.mudar_vista(args[i + 1])
		i = args.find("--clique")   # só para testes: clica num ponto da tela de inspeção (x,y em pixels da tela base)
		if i >= 0 and i + 1 < args.size() and tela_3d.aberta:
			var xy := args[i + 1].split(",")
			await get_tree().process_frame
			await get_tree().physics_frame
			tela_3d.clicar(Vector2(float(xy[0]), float(xy[1])) - tela_3d.position)
			if args.has("--registrar"):
				tela_3d._formulario._confirmar()
	if args.has("--saves"):
		await _abrir_saves()
	if args.has("--exportar-log"):   # baixa o log de playtest (teste do F8 no navegador)
		_baixar_log()
	if args.has("--gestao"):
		_abrir_gestao(false)
	i = args.find("--painel")
	if i >= 0 and i + 1 < args.size():
		_abrir_painel(args[i + 1])   # abre com a animação; só trocar `painel` deixava o painel transparente
	if estado.get("motivoPausa") in ["FIM_DO_MES", "DERROTA"] and estado.get("conselho") != null:
		_abrir_fim_do_mes(str(estado["motivoPausa"]))
	_atualizar_interface()
	if args.has("--rodar"):
		_mudar_velocidade(4)
	i = args.find("--medir")
	if i >= 0 and i + 1 < args.size():
		Api.medindo = true
		medir_ate = Time.get_ticks_msec() / 1000.0 + float(args[i + 1])
		_mudar_velocidade(4)


func _process(delta: float) -> void:
	if medir_ate > 0:
		_medir(delta)
	queue_redraw()  # bonequinhos animados
	_sincronizar_botoes_planta()
	if velocidade == 0 or Api.partida_id.is_empty():
		return
	# O relógio conta mesmo durante a chamada: se a resposta atrasar, o próximo avanço leva os ticks
	# que faltam (M12-3). Antes o tempo parava a cada chamada e o x4 rodava como x2,7, aos trancos.
	acumulado += delta
	if ocupado:
		return
	var periodo := SEGUNDOS_POR_TICK_X1 / velocidade
	if acumulado >= periodo:
		var ticks := mini(int(acumulado / periodo), MAX_TICKS_POR_AVANCO)
		acumulado = fmod(acumulado, periodo) if ticks < MAX_TICKS_POR_AVANCO else 0.0
		_avancar(ticks)


# ---------------------------------------------------------------- ordens e API

func _avancar(ticks: int) -> void:
	ocupado = true
	var resposta = await Api.avancar(ticks)
	ocupado = false
	if resposta == null:
		_mudar_velocidade(0)
		return
	estado = resposta["estado"]
	_detectar_mudancas_de_gate()
	if int(estado.get("ultimoEventoId", 0)) > ultimo_evento:   # sem evento novo, não chama a API
		await _buscar_eventos()
	if resposta["resultado"]["pausado"]:
		_pausa_automatica(str(resposta["resultado"]["motivoPausa"]))
	var inicio := Time.get_ticks_usec()
	_atualizar_interface()
	if medir_ate > 0:
		interface_ms.append((Time.get_ticks_usec() - inicio) / 1000.0)


## --medir: guarda o tempo de cada quadro; no fim imprime o resumo e sai.
func _medir(delta: float) -> void:
	quadros_ms.append(delta * 1000.0)
	if delta > 0.05:
		print("MEDICAO pico %.0f ms em %.1f s, tick %d, ocupado %s, ultimo evento %d" % [delta * 1000.0,
				Time.get_ticks_msec() / 1000.0, int(estado.get("tick", 0)), ocupado, ultimo_evento])
	if velocidade == 0 and not pausa_auto.is_empty() and pausa_auto != "DERROTA":
		_fechar_conselho()   # medição sem parar: fim do mês e rodada ficam com o conselho e o inspetor
		_fechar_gestao()
		_fechar_aviso_pausa(false)
		_mudar_velocidade(4)
	if Time.get_ticks_msec() / 1000.0 < medir_ate:
		return
	medir_ate = -1.0
	print("MEDICAO quadros: %s" % _resumo_ms(quadros_ms))
	var lentos := quadros_ms.filter(func(q): return q > 1000.0 / 30.0).size()
	print("MEDICAO quadros acima de 33 ms: %d de %d" % [lentos, quadros_ms.size()])
	print("MEDICAO _draw: %s" % _resumo_ms(desenho_ms))
	print("MEDICAO _atualizar_interface: %s" % _resumo_ms(interface_ms))
	var por_rota := {}
	for m in Api.medicoes:
		if not por_rota.has(m["rota"]):
			por_rota[m["rota"]] = {"total": [] as Array[float], "json": [] as Array[float], "bytes": 0}
		por_rota[m["rota"]]["total"].append(m["total_ms"])
		por_rota[m["rota"]]["json"].append(m["json_ms"])
		por_rota[m["rota"]]["bytes"] = maxi(por_rota[m["rota"]]["bytes"], m["bytes"])
	for rota in por_rota:
		var r: Dictionary = por_rota[rota]
		print("MEDICAO %s: total %s | JSON %s | maior resposta %d bytes" % [rota, _resumo_ms(r["total"]), _resumo_ms(r["json"]), r["bytes"]])
	print("MEDICAO dia final: %s d%d" % [estado.get("mesReferencia"), int(estado.get("diaUtilDoMes", 0))])
	get_tree().quit()


static func _resumo_ms(valores: Array[float]) -> String:
	if valores.is_empty():
		return "sem dados"
	var ordenados := valores.duplicate()
	ordenados.sort()
	var soma := 0.0
	for v in ordenados:
		soma += v
	return "n=%d media %.1f ms, p95 %.1f ms, max %.1f ms" % [ordenados.size(), soma / ordenados.size(),
			ordenados[int(ordenados.size() * 0.95)], ordenados[-1]]


func _atualizar_estado() -> void:
	var novo = await Api.estado()
	if novo != null:
		estado = novo
		_atualizar_interface()


func _buscar_eventos() -> void:
	var lista = await Api.eventos(ultimo_evento)
	if lista == null:
		return
	for evento in lista:
		ultimo_evento = maxi(ultimo_evento, int(evento["id"]))
		ultima_mensagem[str(evento["tipo"])] = str(evento["mensagem"])
		Playtest.evento(evento)
		_avisar(str(evento["mensagem"]))
		_efeito_do_evento(evento)


func _enviar_ordem(dados: Dictionary, aviso: String) -> void:
	var novo = await Api.ordem(dados)
	if novo != null:
		estado = novo
		erro_api = ""
		if not aviso.is_empty():
			_avisar(aviso)
		await _buscar_eventos()
		_atualizar_interface()


func _mudar_velocidade(nova: int) -> void:
	if nova > 0 and not pausa_auto.is_empty():
		pausa_auto = ""
		_fechar_aviso_pausa()
	velocidade = nova
	acumulado = 0.0
	for chave in botoes_velocidade:
		botoes_velocidade[chave].set_pressed_no_signal(chave == nova)
	if not Api.partida_id.is_empty():
		Api.ordem({"tipo": "MudarVelocidade", "velocidade": "PAUSA" if nova == 0 else "X%d" % nova})


func _priorizar() -> void:
	if not selecionado.is_empty():
		_enviar_ordem({"tipo": "DefinirPrioridade", "barcoId": selecionado, "posicao": 1},
				"%s agora é a prioridade 1." % selecionado)


func _alternar_segurar() -> void:
	var barco := _barco(selecionado)
	if barco.is_empty():
		return
	var liberar: bool = barco["retido"]
	_enviar_ordem({"tipo": "LiberarGate" if liberar else "SegurarGate", "barcoId": selecionado},
			"%s %s." % [selecionado, "liberado" if liberar else "segurado no gate"])


func _decidir_nc(nc_id: String, decisao: String) -> void:
	_enviar_ordem({"tipo": "DecidirNc", "ncId": nc_id, "decisao": decisao}, "")


func _salvar() -> void:
	var resumo = await Api.salvar()
	if resumo != null:
		_avisar("Partida salva (%s, %02dh). F9 carrega o último save." % [_data_curta(str(resumo["data"])), int(resumo["hora"])])


## Carrega o save mais recente. O motor joga de novo as mesmas ordens com a mesma semente.
func _carregar_ultimo_save() -> void:
	_mudar_velocidade(0)
	var lista = await Api.saves()
	if lista == null:
		return
	if lista.is_empty():
		_avisar("Nenhum save ainda. F5 salva a partida.")
		return
	await _carregar(str(lista[0]["id"]))


func _carregar(id: String) -> void:
	_mudar_velocidade(0)
	_avisar("Carregando o save...")
	if not await Api.carregar(id):
		return
	_fechar_conselho()
	tela_ficha.aberta = false
	selecionado = ""
	painel = PAINEL_BARCO
	await _atualizar_estado()
	ultimo_evento = int(estado["ultimoEventoId"])  # eventos antigos já foram vistos
	_avisar("Save carregado: %s, %02dh." % [_data_curta(str(estado["data"])), int(estado["hora"])])
	if estado.get("motivoPausa") in ["FIM_DO_MES", "DERROTA"] and estado.get("conselho") != null:
		_abrir_fim_do_mes(str(estado["motivoPausa"]))
	_atualizar_interface()


## "2027-04-12" -> "12/04/2027".
func _data_curta(iso: String) -> String:
	var p := iso.split("-")
	return iso if p.size() != 3 else "%s/%s/%s" % [p[2], p[1], p[0]]


func _on_erro_api(mensagem: String) -> void:
	erro_api = mensagem
	push_warning(mensagem)


# ---------------------------------------------------------------- entrada

func _unhandled_input(event: InputEvent) -> void:
	# Esc ou B (ui_cancel) fecha o que estiver aberto, do mais de cima para o de baixo.
	if event.is_action_pressed("ui_cancel"):
		_fechar_o_de_cima()
		get_viewport().set_input_as_handled()
		return
	# Sem nada em foco, a primeira seta, D-pad ou Tab põe o foco no barco selecionado (ou em Pausa).
	if get_viewport().gui_get_focus_owner() == null:
		for acao in ["ui_up", "ui_down", "ui_left", "ui_right", "ui_focus_next", "ui_focus_prev"]:
			if event.is_action_pressed(acao):
				_focar_padrao()
				get_viewport().set_input_as_handled()
				return
	if tela_conselho != null or tela_saves != null or tela_proposta != null or tela_rodada.aberta:
		return
	if tela_gestao != null and not (event is InputEventKey and event.keycode == KEY_P):
		return
	if event is InputEventKey and event.pressed and not event.echo:
		# Letras não valem no 3D: lá WASD/R/F movem a câmera e o formulário tem campo de texto.
		var letras_livres: bool = not tela_3d.aberta
		match event.keycode:
			KEY_SPACE:
				if velocidade == 0 and not pausa_auto.is_empty():
					_continuar_depois_da_pausa()
				else:
					_mudar_velocidade(0 if velocidade != 0 else 1)
			KEY_1:
				_mudar_velocidade(1)
			KEY_2:
				_mudar_velocidade(2)
			KEY_3:
				_mudar_velocidade(4)
			KEY_F1:
				_abrir_painel(PAINEL_EQUIPE)
			KEY_F2:
				_abrir_painel(PAINEL_FORNECEDORES)
			KEY_E:   # alternativa ao F1, que em alguns notebooks hiberna
				if letras_livres:
					_abrir_painel(PAINEL_EQUIPE)
			KEY_F:
				if letras_livres:
					_abrir_painel(PAINEL_FORNECEDORES)
			KEY_R:   # rodada de inspeção do barco da pausa ou do selecionado
				if letras_livres:
					var alvo := str(estado.get("barcoPausa", "")) if pausa_auto == "RODADA_INSPECAO" else selecionado
					if not _rodada_aberta(alvo).is_empty():
						_fechar_aviso_pausa(false)
						_abrir_rodada(alvo)
			KEY_P:   # painel de gestão
				if letras_livres:
					_alternar_gestao()
			KEY_V:   # vendas: a proposta mais antiga esperando resposta
				if letras_livres and not proposta_alerta.is_empty():
					_abrir_proposta(proposta_alerta)
			KEY_G:
				if letras_livres:
					_mudar_galpao("G1" if galpao == "G2" else "G2")
			KEY_F5:
				_salvar()
			KEY_F8:
				_baixar_log()
			KEY_F9:
				_carregar_ultimo_save()
			KEY_ENTER, KEY_KP_ENTER:
				# Com um botão em foco, o Enter é do botão (ele age ao soltar a tecla).
				if get_viewport().gui_get_focus_owner() == null:
					_abrir_ficha_completa()


## Fecha o painel de cima: saves, conselho, formulário/vista do 3D, 3D, ficha, painel lateral;
## sem nada aberto, tira a seleção.
func _fechar_o_de_cima() -> void:
	if tela_rodada.aberta:
		if tela_rodada.resultado.is_empty():
			_cancelar_rodada()
		else:
			_fechar_rodada_e_seguir()
		return
	if tela_aviso != null:
		_fechar_aviso_pausa()
		return
	if tela_saves != null:
		_fechar_saves()
		return
	if tela_proposta != null:
		_fechar_proposta()
		return
	if tela_gestao != null:
		_fechar_gestao()
		return
	if tela_conselho != null:
		_fechar_conselho()
		return
	if tela_3d.aberta:
		if not tela_3d.voltar():   # formulário e vistas voltam ao exterior; do exterior, fecha
			_fechar_inspecao_3d()
		return
	if tela_ficha.aberta:
		_fechar_ficha_completa()
		return
	if painel != PAINEL_BARCO:
		_fechar_painel()
		return
	selecionado = ""
	_atualizar_interface()


func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
		# Clique fora do painel lateral só fecha o painel (M12-8). Saves e conselho têm o véu.
		if painel != PAINEL_BARCO and not _retangulo_painel().has_point(event.position):
			_fechar_painel()
			accept_event()
			return
		if area_alerta_proposta.has_point(event.position) and not proposta_alerta.is_empty():
			_abrir_proposta(proposta_alerta)
			return
		if area_alerta_nc.has_point(event.position) and not barco_alerta_nc.is_empty():
			selecionado = barco_alerta_nc
			galpao = Planta.galpao_do_barco(_barco(selecionado))
			_abrir_ficha_completa()
			return
		if area_cartao.has_point(event.position):
			_abrir_ficha_completa()
			return
		for id in areas_barcos:
			if areas_barcos[id].has_point(event.position):
				selecionado = id
				painel = PAINEL_BARCO
				_atualizar_interface()
				if event.double_click:
					_abrir_ficha_completa()
				return


# ---------------------------------------------------------------- interface (Controls)

func _montar_barra_inferior() -> void:
	var barra := HBoxContainer.new()
	barra.set_anchors_and_offsets_preset(Control.PRESET_BOTTOM_WIDE)
	barra.offset_top = -ALTURA_BASE + 3
	barra.offset_bottom = -3
	barra.offset_left = 2
	barra.offset_right = -2
	barra.add_theme_constant_override("separation", 2)
	add_child(barra)

	for opcao in [[0, "II"], [1, "x1"], [2, "x2"], [4, "x4"]]:
		var botao := _botao(opcao[1], _mudar_velocidade.bind(opcao[0]))
		botao.toggle_mode = true
		botao.tooltip_text = "Pausa (Espaço)" if opcao[0] == 0 else "Velocidade %s (tecla %d)" % [opcao[1], 3 if opcao[0] == 4 else opcao[0]]
		barra.add_child(botao)
		botoes_velocidade[opcao[0]] = botao
	botoes_velocidade[0].set_pressed_no_signal(true)

	barra.add_child(VSeparator.new())
	botao_ficha = _botao("Ficha", _alternar_ficha_completa)
	botao_ficha.tooltip_text = "Ficha completa do barco selecionado (ou duplo clique no barco)."
	barra.add_child(botao_ficha)
	botao_prioridade = _botao("Prior. 1", _priorizar)
	botao_prioridade.tooltip_text = "Coloca o barco selecionado na prioridade 1."
	barra.add_child(botao_prioridade)
	botao_segurar = _botao("Segurar", _alternar_segurar)
	botao_segurar.tooltip_text = "Segura o barco no gate atual depois de aprovado."
	barra.add_child(botao_segurar)
	for b in [botao_ficha, botao_prioridade, botao_segurar]:
		_depende_de_barco(b)
	barra.add_child(VSeparator.new())
	for opcao in [[PAINEL_EQUIPE, "Equipe", "F1"], [PAINEL_FORNECEDORES, "Fornec.", "F2"]]:
		var b := _botao(opcao[1], _abrir_painel.bind(opcao[0]))
		b.toggle_mode = true
		b.tooltip_text = "Atalho: %s" % opcao[2]
		barra.add_child(b)
		botoes_painel[opcao[0]] = b
	botao_conselho = _botao("Gestão", _alternar_gestao)
	botao_conselho.tooltip_text = "Painel de gestão: metas do mês, números e vendas (P)."
	barra.add_child(botao_conselho)
	barra.add_child(VSeparator.new())
	var salvar := _botao("Salvar", _salvar)
	salvar.tooltip_text = "Atalho: F5"
	barra.add_child(salvar)
	botao_carregar = _botao("Carregar", _alternar_saves)
	botao_carregar.tooltip_text = "Atalho: F9 (último save)"
	barra.add_child(botao_carregar)


## Abas grandes G1/G2 com o nome do galpão e quantos barcos há em cada um (sempre visíveis na fábrica).
func _montar_abas() -> void:
	barra_abas = HBoxContainer.new()
	barra_abas.position = Vector2(4, ALTURA_TOPO + 1)
	barra_abas.size = Vector2(0, ALTURA_ABAS - 2)
	barra_abas.add_theme_constant_override("separation", 3)
	add_child(barra_abas)
	for aba in ["G1", "G2"]:
		var b := _botao(aba, _mudar_galpao.bind(aba))
		b.toggle_mode = true
		b.tooltip_text = "Atalho: G troca de galpão"
		b.custom_minimum_size.x = 150
		barra_abas.add_child(b)
		botoes_abas[aba] = b
	botoes_abas[galpao].set_pressed_no_signal(true)


## Botão que depende de um barco selecionado: desativado, mostra a dica e, se clicado, faz os barcos piscarem.
func _depende_de_barco(b: Button) -> void:
	b.set_meta("dica", b.tooltip_text)
	b.gui_input.connect(func(evento: InputEvent):
		if b.disabled and evento is InputEventMouseButton and evento.pressed and evento.button_index == MOUSE_BUTTON_LEFT:
			piscar_ate = Time.get_ticks_msec() / 1000.0 + SEGUNDOS_PISCAR)


func _atualizar_dica(b: Button, motivo: String) -> void:
	b.tooltip_text = motivo if b.disabled else str(b.get_meta("dica", ""))


func _montar_ficha() -> void:
	rolagem = ScrollContainer.new()
	rolagem.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	add_child(rolagem)
	ficha = VBoxContainer.new()
	ficha.add_theme_constant_override("separation", 1)
	ficha.custom_minimum_size.x = LARGURA_PAINEL - 14
	rolagem.add_child(ficha)
	botao_fechar_painel = _botao_fechar(_fechar_painel)
	add_child(botao_fechar_painel)


func _montar_ficha_completa() -> void:
	tela_ficha = FichaBarco.new()
	tela_ficha.fonte = fonte
	tela_ficha.fonte_titulo = fonte_titulo
	tela_ficha.tamanho_texto = TAMANHO_TEXTO
	tela_ficha.tamanho_titulo = TAMANHO_TITULO
	tela_ficha.visible = false
	tela_ficha.position = Vector2(0, ALTURA_TOPO)
	tela_ficha.size = Vector2(size.x, size.y - ALTURA_TOPO - ALTURA_BASE)
	add_child(tela_ficha)
	tela_ficha.fechar.connect(_fechar_ficha_completa)
	tela_ficha.priorizar.connect(_priorizar)
	tela_ficha.alternar_segurar.connect(_alternar_segurar)
	tela_ficha.decidir_nc.connect(_decidir_nc)
	tela_ficha.inspecionar.connect(_abrir_inspecao_3d)
	tela_ficha.rodada.connect(func(): _abrir_rodada(selecionado))
	tela_rodada = Rodada2D.new()
	tela_rodada.position = Vector2(0, ALTURA_TOPO)
	tela_rodada.size = Vector2(size.x, size.y - ALTURA_TOPO - ALTURA_BASE)
	add_child(tela_rodada)
	tela_rodada.concluir.connect(_concluir_rodada)
	tela_rodada.cancelar.connect(_cancelar_rodada)
	tela_rodada.continuar.connect(_fechar_rodada_e_seguir)


## Inspeção 3D (pixel art) por cima da fábrica e da ficha, entre as barras de cima e de baixo.
func _montar_inspecao_3d() -> void:
	tela_3d = Inspecao3D.new()
	tela_3d.fonte = fonte
	tela_3d.fonte_titulo = fonte_titulo
	tela_3d.tamanho_texto = TAMANHO_TEXTO
	tela_3d.tamanho_titulo = TAMANHO_TITULO
	tela_3d.visible = false
	tela_3d.position = Vector2(0, ALTURA_TOPO)
	tela_3d.size = Vector2(size.x, size.y - ALTURA_TOPO - ALTURA_BASE)
	add_child(tela_3d)
	tela_3d.fechar.connect(_fechar_inspecao_3d)
	tela_3d.registrar_nc.connect(_registrar_nc_3d)


func _abrir_inspecao_3d() -> void:
	var barco := _barco(selecionado)
	if barco.is_empty():
		_avisar("Selecione um barco para inspecionar em 3D.")
		return
	_guardar_foco("3d", tela_ficha.botao_3d)
	tela_3d.abrir(barco)
	tela_3d.atualizar_ncs(estado.get("ncsInspecao", []))
	_atualizar_interface()
	tela_3d.focar_acao_principal()


## NC marcada no 3D: vira a ordem RegistrarNcInspecao; o motor valida e devolve o estado com a NC.
func _registrar_nc_3d(ordem: Dictionary) -> void:
	erro_api = ""
	var novo = await Api.ordem(ordem)
	if novo == null:
		tela_3d.resultado_nc(erro_api if not erro_api.is_empty() else "O motor recusou a NC.")
		return
	estado = novo
	tela_3d.resultado_nc("")
	await _buscar_eventos()
	_atualizar_interface()


func _fechar_inspecao_3d() -> void:
	tela_3d.aberta = false
	_atualizar_interface()
	_devolver_foco("3d")


func _mudar_galpao(novo: String) -> void:
	galpao = novo
	for aba in botoes_abas:
		botoes_abas[aba].set_pressed_no_signal(aba == galpao)
	queue_redraw()


func _abrir_ficha_completa() -> void:
	if _barco(selecionado).is_empty():
		_avisar("Selecione um barco para abrir a ficha.")
		return
	if not tela_ficha.aberta:
		_guardar_foco("ficha", botao_ficha)
	tela_ficha.aberta = true
	_atualizar_interface()
	tela_ficha.focar_acao_principal()


func _fechar_ficha_completa() -> void:
	tela_ficha.aberta = false
	_atualizar_interface()
	_devolver_foco("ficha")


## O mesmo botão que abre fecha (M12-8).
func _alternar_ficha_completa() -> void:
	if tela_ficha.aberta:
		_fechar_ficha_completa()
	else:
		_abrir_ficha_completa()


func _abrir_painel(qual: String) -> void:
	if painel == qual:
		_fechar_painel()
		return
	if tela_ficha.aberta:
		_fechar_ficha_completa()
	var estava_fechado := painel == PAINEL_BARCO
	if estava_fechado:
		_guardar_foco("lateral", botoes_painel[qual])
	painel = qual
	painel_desenhado = qual
	_atualizar_interface()
	if estava_fechado:
		_animar_painel(1.0, "abrir_painel")
	# Equipe: a ação principal é Contratar; Fornecedores não tem ação, o foco vai para o X.
	var botoes := _botoes_em(ficha)
	_focar_depois(botoes[0] if qual == PAINEL_EQUIPE and not botoes.is_empty() else botao_fechar_painel)


func _fechar_painel() -> void:
	painel = PAINEL_BARCO
	_atualizar_interface()
	_devolver_foco("lateral")
	_animar_painel(0.0, "fechar_painel")


## O painel lateral é desenhado à mão: a animação muda alfa_painel (desenho, lista e X juntos).
func _animar_painel(alvo: float, som: String) -> void:
	Sons.tocar(som)
	var t := create_tween().set_trans(Tokens.MOV_UI_TRANSICAO).set_ease(Tokens.MOV_UI_SUAVIZACAO)
	t.tween_property(self, "alfa_painel", alvo, Tokens.MOV_UI)
	if alvo == 0.0:
		t.tween_callback(func():
			if painel == PAINEL_BARCO:
				painel_desenhado = ""
				_posicionar_rolagem())


func _botao(texto: String, acao: Callable) -> Button:
	var b := Button.new()
	b.text = texto
	b.pressed.connect(acao)
	return b


## X pequeno no canto de um painel.
func _botao_fechar(acao: Callable) -> Button:
	var b := _botao("X", acao)
	b.tooltip_text = "Fechar (Esc)"
	return b


# ---------------------------------------------------------------- foco (mouse, teclado e controle)

## Espaço só pausa: sai do ui_accept para não apertar o botão em foco. Botão A do controle confirma (ui_accept)
## e B fecha (ui_cancel); o Godot 4.7 não traz esses dois botões nas ações de interface.
func _ajustar_entrada() -> void:
	for e in InputMap.action_get_events("ui_accept"):
		if e is InputEventKey and (e as InputEventKey).keycode == KEY_SPACE:
			InputMap.action_erase_event("ui_accept", e)
	for par in [["ui_accept", JOY_BUTTON_A], ["ui_cancel", JOY_BUTTON_B]]:
		var tem := false
		for e in InputMap.action_get_events(par[0]):
			tem = tem or (e is InputEventJoypadButton and (e as InputEventJoypadButton).button_index == par[1])
		if not tem:
			var botao := InputEventJoypadButton.new()
			botao.button_index = par[1]
			InputMap.action_add_event(par[0], botao)


func _montar_camada_foco() -> void:
	camada_foco = Control.new()
	camada_foco.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	camada_foco.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(camada_foco)
	botao_cartao = _botao_invisivel(_abrir_ficha_completa)
	botao_alerta = _botao_invisivel(_abrir_alerta_nc)
	botao_proposta = _botao_invisivel(func(): _abrir_proposta(proposta_alerta))


## Botão sem fundo, só para o teclado e o controle: o mouse continua no _gui_input da fábrica
## (clique e duplo clique como antes). O contorno de foco vem do tema.
func _botao_invisivel(acao: Callable) -> Button:
	var b := Button.new()
	b.flat = true
	b.mouse_filter = Control.MOUSE_FILTER_IGNORE
	b.visible = false
	b.pressed.connect(acao)
	camada_foco.add_child(b)
	return b


## Põe os botões invisíveis sobre o que foi desenhado no último quadro. Com a ficha ou o 3D na frente,
## os botões dos barcos só se escondem (o foco volta para o barco ao fechar).
func _sincronizar_botoes_planta() -> void:
	var na_planta := not estado.is_empty() and not tela_ficha.aberta and not tela_3d.aberta
	for id in botoes_barcos.keys():
		if not areas_barcos.has(id):
			botoes_barcos[id].queue_free()
			botoes_barcos.erase(id)
	for id in areas_barcos:
		var b: Button = botoes_barcos.get(id)
		if b == null:
			b = _botao_invisivel(_acionar_barco.bind(id))
			b.focus_entered.connect(_focar_barco.bind(id))
			botoes_barcos[id] = b
		b.visible = na_planta
		_cobrir(b, areas_barcos[id])
	_mostrar_invisivel(botao_cartao, area_cartao, na_planta)
	_mostrar_invisivel(botao_alerta, area_alerta_nc, na_planta and not barco_alerta_nc.is_empty())
	_mostrar_invisivel(botao_proposta, area_alerta_proposta, na_planta and not proposta_alerta.is_empty())
	if na_planta:
		_vizinhos_dos_barcos()


## Ordem do foco nos barcos: esquerda e direita só entre barcos (da esquerda para a direita), para cima
## as abas, para baixo o cartão do barco (ou a barra de baixo). Sem isso, a busca automática pulava para o cartão.
func _vizinhos_dos_barcos() -> void:
	var lista: Array = botoes_barcos.values()
	lista.sort_custom(func(a: Control, b: Control): return a.position.x < b.position.x or (a.position.x == b.position.x and a.position.y < b.position.y))
	var abaixo: Control = botao_cartao if botao_cartao.visible else botoes_velocidade[0]
	for i in lista.size():
		var b: Control = lista[i]
		b.focus_neighbor_left = b.get_path_to(lista[maxi(i - 1, 0)])
		b.focus_neighbor_right = b.get_path_to(lista[mini(i + 1, lista.size() - 1)])
		b.focus_neighbor_top = b.get_path_to(botoes_abas[galpao])
		b.focus_neighbor_bottom = b.get_path_to(abaixo)
	var do_selecionado: Control = botoes_barcos.get(selecionado)
	if do_selecionado != null:
		botao_cartao.focus_neighbor_top = botao_cartao.get_path_to(do_selecionado)


func _cobrir(b: Control, r: Rect2) -> void:
	b.position = r.position.floor()
	b.size = r.size.ceil()


func _mostrar_invisivel(b: Button, r: Rect2, mostrar: bool) -> void:
	b.visible = mostrar and r.size.x > 0
	if b.visible:
		_cobrir(b, r)


## Foco num barco (teclado ou controle) seleciona o barco; Enter ou A num barco já selecionado abre a ficha.
func _focar_barco(id: String) -> void:
	if selecionado != id:
		selecionado = id
		_atualizar_interface()


func _acionar_barco(id: String) -> void:
	if selecionado == id:
		_abrir_ficha_completa()
	else:
		_focar_barco(id)


func _abrir_alerta_nc() -> void:
	if barco_alerta_nc.is_empty():
		return
	selecionado = barco_alerta_nc
	galpao = Planta.galpao_do_barco(_barco(selecionado))
	_abrir_ficha_completa()


func _focar_padrao() -> void:
	var b: Button = botoes_barcos.get(selecionado)
	_focar_depois(b if b != null else botoes_velocidade[0])


## O foco vai depois do quadro: controles recém-criados ainda não estão no lugar.
func _focar_depois(c: Control) -> void:
	if c != null:
		c.call_deferred("grab_focus")


func _guardar_foco(nome: String, quem_abriu: Control) -> void:
	var dono := get_viewport().gui_get_focus_owner()
	foco_ao_fechar[nome] = dono if dono != null else quem_abriu


func _devolver_foco(nome: String) -> void:
	var c = foco_ao_fechar.get(nome)
	foco_ao_fechar.erase(nome)
	await get_tree().process_frame   # os botões da planta reaparecem no próximo quadro
	if is_instance_valid(c) and (c as Control).is_visible_in_tree():
		(c as Control).grab_focus()
	else:
		_focar_padrao()


func _atualizar_interface() -> void:
	if not estado.is_empty():
		Playtest.observar(estado)
		Playtest.tela(_tela_de_cima())
	var barco := _barco(selecionado)
	var ativo: bool = not barco.is_empty() and barco["status"] != "ENTREGUE"
	botao_ficha.disabled = barco.is_empty()
	botao_prioridade.disabled = not ativo
	botao_segurar.disabled = not ativo
	botao_segurar.text = "Liberar" if ativo and barco["retido"] else "Segurar"
	var motivo := DICA_SEM_BARCO if barco.is_empty() else DICA_ENTREGUE
	for b in [botao_ficha, botao_prioridade, botao_segurar]:
		_atualizar_dica(b, motivo)
	_atualizar_abas()
	botao_conselho.disabled = estado.is_empty()
	if tela_gestao != null:
		_atualizar_gestao()
	if tela_3d != null and tela_3d.aberta:
		tela_3d.atualizar_ncs(estado.get("ncsInspecao", []))
	if tela_proposta != null:
		_atualizar_proposta()
	if tela_ficha.aberta:
		if barco.is_empty():
			tela_ficha.aberta = false
		else:
			tela_ficha.animado = velocidade > 0
			tela_ficha.rodada_disponivel = not _rodada_aberta(str(barco["id"])).is_empty()
			tela_ficha.atualizar(barco, int(estado["tick"]))
	for chave in botoes_painel:
		botoes_painel[chave].set_pressed_no_signal(painel == chave)
	_posicionar_rolagem()
	match painel:
		PAINEL_EQUIPE:
			_reconstruir_equipe()
		PAINEL_FORNECEDORES:
			_reconstruir_fornecedores()
		_:
			assinatura_ficha = ""  # o próximo painel aberto se refaz


func _atualizar_abas() -> void:
	# Sempre visíveis na fábrica; a ficha completa e a inspeção 3D ocupam a tela toda e escondem a faixa.
	barra_abas.visible = not estado.is_empty() and not tela_ficha.aberta and not (tela_3d != null and tela_3d.aberta)
	if estado.is_empty():
		return
	for aba in botoes_abas:
		var quantos := 0
		for b in estado["barcos"]:
			if Planta.galpao_do_barco(b) == aba and b["status"] != "ENTREGUE":
				quantos += 1
		botoes_abas[aba].text = "%s %s · %d %s" % [aba, NOMES_GALPAO[aba], quantos, "barco" if quantos == 1 else "barcos"]
		botoes_abas[aba].set_pressed_no_signal(aba == galpao)


## Topo da planta: abaixo da barra de cima e da faixa das abas.
func _topo_planta() -> float:
	return ALTURA_TOPO + ALTURA_ABAS


## Equipe e fornecedores abrem por cima da fábrica, à direita.
func _posicionar_rolagem() -> void:
	var topo := _topo_planta() + 22
	rolagem.visible = painel_desenhado != ""
	var r := _retangulo_painel()
	botao_fechar_painel.visible = rolagem.visible
	botao_fechar_painel.position = Vector2(r.end.x - 18, r.position.y + 2)
	botao_fechar_painel.size = Vector2(16, 16)
	rolagem.position = Vector2(size.x - LARGURA_PAINEL, topo)
	rolagem.size = Vector2(LARGURA_PAINEL - 4, size.y - ALTURA_BASE - 6 - topo)


func _limpar_ficha(assinatura: String) -> bool:
	if assinatura == assinatura_ficha:
		return false
	assinatura_ficha = assinatura
	# O painel é refeito quando o estado muda: o foco volta para o botão na mesma posição.
	var indice := _botoes_em(ficha).find(get_viewport().gui_get_focus_owner())
	for filho in ficha.get_children():
		ficha.remove_child(filho)
		filho.queue_free()
	if indice >= 0:
		_focar_indice.call_deferred(ficha, indice)
	return true


func _focar_indice(pai: Node, indice: int) -> void:
	var botoes := _botoes_em(pai)
	if not botoes.is_empty():
		botoes[mini(indice, botoes.size() - 1)].grab_focus()


## Botões dentro de um container, na ordem da tela.
static func _botoes_em(pai: Node) -> Array:
	var lista := []
	for filho in pai.get_children():
		if filho is BaseButton:
			lista.append(filho)
		lista.append_array(_botoes_em(filho))
	return lista


func _rotulo(texto: String, cor: Color) -> RichTextLabel:
	return _rotulo_rico(texto, cor, LARGURA_PAINEL - 16)


## Rótulo com os números em Jersey (Texto.bbcode); quebra linha e cresce com o texto.
static func _rotulo_rico(texto: String, cor: Color, largura: float) -> RichTextLabel:
	var r := RichTextLabel.new()
	r.bbcode_enabled = true
	r.fit_content = true
	r.scroll_active = false
	r.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
	r.custom_minimum_size.x = largura
	r.mouse_filter = Control.MOUSE_FILTER_PASS
	r.add_theme_color_override("default_color", cor)
	r.text = Texto.bbcode(texto)
	return r


func _reconstruir_equipe() -> void:
	if estado.is_empty() or not _limpar_ficha(JSON.stringify(["equipe", estado["pessoas"]])):
		return
	var contratar := _botao("Contratar (10 dias)", _enviar_ordem.bind({"tipo": "Contratar", "funcao": "PRODUCAO"}, ""))
	contratar.theme_type_variation = "BotaoAcao"   # a única ação principal do painel Equipe
	ficha.add_child(contratar)
	var treinos: Dictionary = estado.get("treinamentos", {})
	ficha.add_child(_rotulo("Téc -NC +veloc · NR -acid.", Tokens.TEXTO_CALMO))
	for p in estado["pessoas"]:
		if p["funcao"] != "PRODUCAO" or p["situacao"] == "DESLIGADA":
			continue
		var situacao := ""
		if p["situacao"] != "ATIVA":
			situacao = " " + NOMES_SITUACAO[p["situacao"]] % int(p["diasRestantesSituacao"]) if "%d" in NOMES_SITUACAO[p["situacao"]] else NOMES_SITUACAO[p["situacao"]]
		var seguranca := "" if p["treinoSegurancaEmDia"] else " NR!"
		var cor := Tokens.ATENCAO if int(p["moral"]) < 40 else (Tokens.TEXTO if p["situacao"] == "ATIVA" else Tokens.TEXTO_CALMO)
		ficha.add_child(_rotulo("%s N%d moral %d%s%s" % [p["nome"], int(p["nivel"]), int(p["moral"]), seguranca, situacao], cor))
		if p["situacao"] == "A_CAMINHO":
			continue
		var acoes := HBoxContainer.new()
		acoes.add_theme_constant_override("separation", 2)
		var um := _botao("1:1", _enviar_ordem.bind({"tipo": "FazerUmAUm", "pessoaId": p["id"]}, ""))
		um.disabled = not p["podeFazerUmAUm"]
		acoes.add_child(um)
		var disponivel: bool = p["situacao"] == "ATIVA"
		var tecnico := _botao("Treino téc.", _enviar_ordem.bind({"tipo": "Treinar", "pessoaId": p["id"], "treinamento": "TECNICO"}, ""))
		tecnico.disabled = not disponivel or int(p["nivel"]) >= 5
		tecnico.tooltip_text = _dica_treino_tecnico(treinos, int(p["nivel"]))
		acoes.add_child(tecnico)
		var nr := _botao("NR", _enviar_ordem.bind({"tipo": "Treinar", "pessoaId": p["id"], "treinamento": "SEGURANCA"}, ""))
		nr.disabled = not disponivel
		nr.tooltip_text = _dica_treino_nr(treinos, bool(p["treinoSegurancaEmDia"]))
		acoes.add_child(nr)
		ficha.add_child(acoes)


## Variação de "antes" para "depois" em %, com sinal (+20%, -17%); mesma conta do motor (PessoasRegra.variacao).
static func _variacao(depois: float, antes: float) -> String:
	var pct := roundi((depois / antes - 1.0) * 100.0)
	return ("+" if pct > 0 else "") + str(pct) + "%"


## Dica de uma linha do treino técnico com os números do motor (estado.treinamentos).
static func _dica_treino_tecnico(treinos: Dictionary, nivel: int) -> String:
	if nivel >= 5:
		return "Nível máximo."
	if treinos.is_empty():
		return "Treino técnico: sobe 1 nível; menos NC e mais velocidade."
	var antes: Dictionary = treinos["niveis"][nivel - 1]
	var depois: Dictionary = treinos["niveis"][nivel]
	return "%d dias fora da linha. N%d→N%d: velocidade %s, NC %s, salário R$ %s→%s." % [
		int(treinos["diasTreinamento"]), nivel, nivel + 1,
		_variacao(float(depois["fatorVelocidade"]), float(antes["fatorVelocidade"])),
		_variacao(float(depois["fatorNc"]), float(antes["fatorNc"])),
		str(int(antes["salario"])), str(int(depois["salario"]))]


## Dica de uma linha do treino de NR com o multiplicador do motor.
static func _dica_treino_nr(treinos: Dictionary, em_dia: bool) -> String:
	if treinos.is_empty():
		return "Treino de NR: menos chance de acidente."
	var dias := int(treinos["diasTreinamento"])
	if em_dia:
		return "NR já em dia: treinar de novo só tira %d dias da linha." % dias
	return "%d dias fora da linha. NR em dia: chance de acidente %s." % [
		dias, _variacao(1.0, float(treinos["multiplicadorAcidenteSemNr"]))]


func _reconstruir_fornecedores() -> void:
	if estado.is_empty() or not _limpar_ficha(JSON.stringify(["fornec", estado["fornecedores"], estado["estoque"], estado["pedidosACaminho"]])):
		return
	for f in estado["fornecedores"]:
		var cor := Tokens.OK if f["nota"] in ["A", "B"] else (Tokens.ATENCAO if f["nota"] in ["C", "D", "-"] else Tokens.NC)
		ficha.add_child(_rotulo("%s  nota %s" % [f["nome"], f["nota"]], cor))
		ficha.add_child(_rotulo("  PPM %d | prazo %d/%d | 8D %d/%d" % [int(f["ppm"]), int(f["pedidosNoPrazo"]),
				int(f["pedidosEntregues"]), int(f["oitoDsNoPrazo"]), int(f["oitoDsRespondidos"])], Tokens.TEXTO))
		if float(f["totalChargeback"]) > 0:
			ficha.add_child(_rotulo("  chargeback R$ %s | RNC abertas %d" % [str(f["totalChargeback"]), int(f["rncsAbertas"])], Tokens.ATENCAO))
	var itens: Array = estado["estoque"].keys()
	itens.sort()
	var linha_estoque := "Estoque (kits):"
	for item in itens:
		linha_estoque += " %s %d" % [item, int(estado["estoque"][item])]
	ficha.add_child(_rotulo(linha_estoque, Tokens.TEXTO))
	for pedido in estado["pedidosACaminho"]:
		var partes := str(pedido["dataPrevista"]).split("-")
		ficha.add_child(_rotulo("  a caminho: %d %s, prev. %s/%s" % [int(pedido["kits"]), pedido["item"], partes[2], partes[1]], Tokens.TEXTO_CALMO))


func _avisar(texto: String) -> void:
	avisos.push_front({"texto": texto, "quando": Time.get_ticks_msec() / 1000.0})
	if avisos.size() > MAX_AVISOS:
		avisos.resize(MAX_AVISOS)


# ---------------------------------------------------------------- desenho

func _draw() -> void:
	var inicio_desenho := Time.get_ticks_usec()
	_desenhar_tudo()
	if medir_ate > 0:
		var ms := (Time.get_ticks_usec() - inicio_desenho) / 1000.0
		desenho_ms.append(ms)
		if ms > 20.0:
			print("MEDICAO _draw lento %.0f ms, tick %d" % [ms, int(estado.get("tick", 0))])


func _desenhar_tudo() -> void:
	draw_rect(Rect2(Vector2.ZERO, size), Tokens.FUNDO)
	if estado.is_empty():
		var texto := erro_api if not erro_api.is_empty() else "Conectando à API..."
		draw_string(fonte_titulo, Vector2(16, 44), "MARELLAR", HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TITULO, Tokens.TEXTO)
		draw_multiline_string(fonte, Vector2(16, 64), texto, HORIZONTAL_ALIGNMENT_LEFT, size.x - 32,
				TAMANHO_TEXTO, -1, Tokens.ATENCAO)
		return
	var t := Time.get_ticks_msec() / 1000.0
	if not tela_ficha.aberta:
		var area := Rect2(0, _topo_planta(), size.x, size.y - _topo_planta() - ALTURA_BASE)
		areas_barcos = Planta.desenhar(self, area, estado, selecionado, t, velocidade > 0, fonte, galpao)
		for id in areas_barcos:
			posicao_barcos[id] = areas_barcos[id]
		_desenhar_piscar(t)
		_desenhar_inspetora(area)
		_desenhar_efeitos()
		_desenhar_luz_do_turno(area)
		_desenhar_cartao()
		_desenhar_alertas(t)
		_desenhar_painel()
	else:
		area_cartao = Rect2()
		area_alerta_nc = Rect2()
	_desenhar_topo()
	draw_rect(Rect2(0, size.y - ALTURA_BASE, size.x, ALTURA_BASE), Tokens.PAINEL)


# ---------------------------------------------------------------- game feel (GDD, Áudio e sensação)

## Números e sinais subindo do barco, confete na entrega.
func _efeito_do_evento(evento: Dictionary) -> void:
	var barco = evento.get("barcoId")
	if barco == null:
		return
	var texto := ""
	var cor := Tokens.TEXTO
	var tipo := "texto"
	match str(evento["tipo"]):
		"GATE_APROVADO":
			texto = "gate OK!"
			cor = Tokens.OK
		"NC_DETECTADA", "NC_CRITICA":
			texto = "NC!"
			cor = Tokens.NC
		"RETRABALHO_CONCLUIDO":
			texto = "retrabalho OK"
			cor = Tokens.OK
		"QUEBRA_PECA":
			texto = "quebrou! -HH"
			cor = Tokens.NC
		"ACIDENTE":
			texto = "ACIDENTE"
			cor = Tokens.NC
		"PARADO_MATERIAL":
			texto = "sem material"
			cor = Tokens.ATENCAO
		"MULTA":
			texto = "multa!"
			cor = Tokens.NC
		"CASCO_NOVO":
			texto = "casco novo"
			cor = Tokens.OK
		"BARCO_ENTREGUE":
			texto = "ENTREGUE!"
			cor = Tokens.OK
			tipo = "confete"
		"FINANCEIRO":
			var achado := RegEx.create_from_string("R\\$ ?([0-9.]+)").search(str(evento["mensagem"]))
			if achado == null:
				return
			texto = "+R$ %s mil" % str(int(achado.get_string(1).replace(".", "")) / 1000)
			cor = Tokens.OK
		_:
			return
	efeitos.append({"tipo": tipo, "texto": texto, "cor": cor, "barco": str(barco), "inicio": Time.get_ticks_msec() / 1000.0})


## Respingo quando o barco desce na piscina (o gate muda para PI).
func _detectar_mudancas_de_gate() -> void:
	for b in estado["barcos"]:
		var id: String = b["id"]
		if gate_anterior.has(id) and gate_anterior[id] != "PI" and b["gateAtual"] == "PI":
			efeitos.append({"tipo": "respingo", "texto": "", "cor": Paleta.AGUA, "barco": id,
					"inicio": Time.get_ticks_msec() / 1000.0})
		gate_anterior[id] = b["gateAtual"]


func _desenhar_efeitos() -> void:
	var agora := Time.get_ticks_msec() / 1000.0
	var vivos: Array[Dictionary] = []
	var por_barco := {}
	for e in efeitos:
		var idade: float = agora - float(e["inicio"])
		var duracao := 3.0 if e["tipo"] == "confete" else 2.4
		if idade > duracao or not posicao_barcos.has(e["barco"]):
			continue
		vivos.append(e)
		var r: Rect2 = posicao_barcos[e["barco"]]
		var fila: int = por_barco.get(e["barco"], 0)
		por_barco[e["barco"]] = fila + 1
		var alfa := clampf((duracao - idade) / 0.6, 0.0, 1.0)
		match str(e["tipo"]):
			"respingo":
				var centro := r.get_center()
				for anel in 3:
					var raio := (idade * 26.0) - anel * 8.0
					if raio > 0:
						draw_arc(centro, raio, 0, TAU, 24, Color(Paleta.AGUA, alfa * 0.8), 1.0)
				for gota in 10:
					var ang := gota * TAU / 10.0
					var d := Vector2(cos(ang), sin(ang)) * (8 + idade * 30) + Vector2(0, idade * idade * 30)
					draw_rect(Rect2((centro + d).floor(), Vector2(2, 2)), Color(Paleta.CASCO, alfa))
			"confete":
				for k in 36:
					var semente := float(k * 73 % 97) / 97.0
					var x0 := r.get_center().x + (semente - 0.5) * 80
					var vel := Vector2((semente - 0.5) * 40, -60 - semente * 40)
					var p := Vector2(x0, r.position.y) + vel * idade + Vector2(0, 70 * idade * idade)
					var cores := [Paleta.NAO_CONFORME, Paleta.FAIXA, Paleta.MONTAGEM_LIMPA, Paleta.AGUA, Paleta.ALERTA]
					draw_rect(Rect2(p.floor(), Vector2(2, 1 + k % 2)), Color(cores[k % cores.size()], alfa))
				_texto_flutuante(e, r, idade, alfa, fila)
			_:
				_texto_flutuante(e, r, idade, alfa, fila)
	efeitos = vivos


func _texto_flutuante(e: Dictionary, r: Rect2, idade: float, alfa: float, fila: int) -> void:
	var texto: String = e["texto"]
	var largura := fonte.get_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO).x
	var pos := Vector2(r.get_center().x - largura / 2, r.position.y + 6 - idade * 14 - fila * 12).floor()
	draw_string(fonte, pos + Vector2(1, 1), texto, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Color(Tokens.FUNDO, alfa))
	draw_string(fonte, pos, texto, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Color(e["cor"], alfa))


## A inspetora anda até o barco que está inspecionando e volta ao mezanino quando está livre.
func _desenhar_inspetora(area: Rect2) -> void:
	var alvo := Vector2(area.end.x - Planta.LARGURA_PISCINA - 30, area.position.y + Planta.ALTURA_MEZANINO - 3)
	var em_inspecao = estado["barcoEmInspecao"]
	if em_inspecao != null and areas_barcos.has(em_inspecao):
		var r: Rect2 = areas_barcos[em_inspecao]
		alvo = Vector2(r.end.x - 4, r.position.y + 26)
	if pos_inspetora.x < 0:
		pos_inspetora = alvo
	var passo := 70.0 * get_process_delta_time()
	inspetora_andando = pos_inspetora.distance_to(alvo) > 1.0
	pos_inspetora = pos_inspetora.move_toward(alvo, passo)
	var quadro := int(Time.get_ticks_msec() / 150) % 2 if inspetora_andando else 0
	var tex := PixelArt.pessoa(Paleta.AGUA, quadro)
	var canto := (pos_inspetora - Vector2(tex.get_width() / 2.0, tex.get_height())).floor()
	draw_texture(tex, canto)
	draw_rect(Rect2(canto + Vector2(tex.get_width() - 3, 6), Vector2(3, 4)), Paleta.PAPEL)  # prancheta


## Luz do turno: manhã levemente fria, fim da tarde alaranjado (GDD, Luz).
func _desenhar_luz_do_turno(area: Rect2) -> void:
	var hora := int(estado["hora"])
	var cor := Color(0, 0, 0, 0)
	if hora <= 7:
		cor = Color(Paleta.AGUA_ESCURA, 0.10)
	elif hora >= 14:
		cor = Color(Paleta.ALERTA_ESCURO, 0.14)
	elif hora >= 13:
		cor = Color(Paleta.ALERTA, 0.07)
	if cor.a > 0:
		draw_rect(area, cor)


## Barra de cima em blocos, como no mockup: data, caixa, valor, moral, NCs abertas e conselho.
func _desenhar_topo() -> void:
	var fin: Dictionary = estado["financeiro"]
	draw_rect(Rect2(0, 0, size.x, ALTURA_TOPO), Tokens.NC if float(fin["caixa"]) < 0 else Tokens.MARCA)
	var soma := 0
	var quantas := 0
	for p in estado["pessoas"]:
		if p["funcao"] == "PRODUCAO" and p["situacao"] in ["ATIVA", "TREINAMENTO", "AFASTADA"]:
			soma += int(p["moral"])
			quantas += 1
	var moral := soma / maxi(1, quantas)
	var ncs := int(estado["qualidade"]["ncsAbertas"])
	var paciencia := int(fin["pacienciaConselho"])
	var blocos := [
		["", "%s d%d %02dh" % [_mes_curto(str(estado["mesReferencia"])), int(estado["diaUtilDoMes"]), int(estado["hora"])], Tokens.TEXTO],
		["Caixa", _milhoes(fin["caixa"]), Tokens.OK if float(fin["caixa"]) >= 0 else Tokens.NC],
		["Valor", _milhoes(fin["valorEmpresa"]), Tokens.MARCA_CLARA],
		["Moral", "%d%%" % moral, Tokens.OK if moral >= 60 else (Tokens.ATENCAO if moral >= 40 else Tokens.NC)],
		["NCs", str(ncs), Tokens.NC if ncs > 0 else Tokens.OK],
		["Conselho", "%d/%d" % [paciencia, int(fin["pacienciaMaxima"])], _cor_conselho(int(fin["mesesRuinsSeguidos"]), int(fin["mesesRuinsParaDemissao"]))],
	]
	var x := 2.0
	for b in blocos:
		var rotulo: String = b[0]
		var valor: String = b[1]
		var largura_rotulo := 0.0 if rotulo.is_empty() else fonte.get_string_size(rotulo + " ", HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO).x
		var largura := largura_rotulo + Texto.largura(valor) + 6
		draw_rect(Rect2(x, 1, largura, ALTURA_TOPO - 2), Tokens.FUNDO)
		draw_rect(Rect2(x, 1, largura, ALTURA_TOPO - 2).grow(-0.5), Tokens.PAINEL, false, 1.0)
		draw_string(fonte, Vector2(x + 3, 16), rotulo, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Tokens.TEXTO_CALMO)
		Texto.desenhar(self, Vector2(x + 3 + largura_rotulo, 16), valor, b[2])
		x += largura + 1
	if velocidade == 0 and not pausa_auto.is_empty():
		_desenhar_faixa_pausa()
	elif not tela_ficha.aberta:
		_desenhar_chips_metas()
	var vel := "PAUSA" if velocidade == 0 else "x%d" % velocidade
	Texto.desenhar(self, Vector2(x + 1, 16), vel, Tokens.ATENCAO if velocidade == 0 else Tokens.OK)


## Metas do mês sempre à vista: um chip por meta à direita das abas G1/G2 (verde dentro, laranja a caminho,
## vermelho perdida). O motor diz o valor e se a meta está cumprida; aqui só se escolhe a cor e o texto curto.
func _desenhar_chips_metas() -> void:
	var g = estado.get("gestao")
	if g == null:
		return
	var x := size.x - 4
	var metas: Array = g["metasDoMes"]
	for i in range(metas.size() - 1, -1, -1):
		var m: Dictionary = metas[i]
		var texto := _texto_chip(m)
		var cor := _cor_meta(m, false)
		var largura := Texto.largura(texto) + 8
		x -= largura
		var r := Rect2(x, ALTURA_TOPO + 3, largura, ALTURA_ABAS - 6)
		draw_rect(r, Tokens.FUNDO)
		draw_rect(r.grow(-0.5), cor, false, 1.0)
		Texto.desenhar(self, Vector2(x + 4, ALTURA_TOPO + 17), texto, cor)
		x -= 2


func _texto_chip(m: Dictionary) -> String:
	var atual := float(m["atual"])
	match str(m["id"]):
		"M-DM2":
			return "DM %d/%d" % [int(atual), int(float(m["alvo"]))]
		"M-CAIXA":
			return ("%.1fmi" % (atual / 1000000.0)).replace(".", ",")
		"M-ACID":
			return "Ac %d" % int(atual)
		"M-RNC":
			return ("NC %.1f" % atual).replace(".", ",")
	return str(m["id"])


## Verde: cumprida. Meta de "chegar a" ainda não batida: laranja durante o mês. Limite estourado: vermelho.
## No fechamento tudo que não foi cumprido é vermelho.
func _cor_meta(m: Dictionary, final: bool) -> Color:
	if m["cumprida"]:
		return Tokens.OK
	return Tokens.NC if final or m["limite"] else Tokens.ATENCAO


## Pausa automática: "PAUSADO: motivo" à direita das abas G1/G2 até o jogo voltar a rodar (M12-1).
## A barra de cima não tem espaço sobrando; a faixa das abas fica logo abaixo dela.
func _desenhar_faixa_pausa() -> void:
	var texto := "PAUSADO: %s" % _motivo_curto(pausa_auto)
	var largura := fonte.get_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO).x + 12
	var r := Rect2(size.x - largura - 4, ALTURA_TOPO + 1, largura, ALTURA_ABAS - 2)
	draw_rect(r, Tokens.FUNDO)
	draw_rect(r.grow(-0.5), Tokens.ATENCAO, false, 1.0)
	draw_string(fonte, Vector2(r.position.x + 6, r.position.y + 16), texto, HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TEXTO, Tokens.ATENCAO)


## Depois de um clique em botão que precisa de barco: os barcos clicáveis piscam com o contorno de foco.
## Se o galpão na tela não tem barco, pisca a aba do outro galpão.
func _desenhar_piscar(t: float) -> void:
	if t > piscar_ate or int(t * 6) % 2 == 1:
		return
	var alvos: Array = areas_barcos.values()
	if alvos.is_empty():
		for aba in botoes_abas:
			if aba != galpao:
				alvos.append(Rect2(barra_abas.position + botoes_abas[aba].position, botoes_abas[aba].size))
	for r in alvos:
		draw_rect((r as Rect2).grow(2), Tokens.FOCO, false, 1.0)


## "mar/2027" -> "mar/27" (a barra de cima não tem espaço para o ano inteiro).
static func _mes_curto(mes: String) -> String:
	var p := mes.split("/")
	return mes if p.size() != 2 or p[1].length() != 4 else "%s/%s" % [p[0], p[1].substr(2)]


## Cartão do barco selecionado no canto de baixo, à esquerda (clique abre a ficha).
func _desenhar_cartao() -> void:
	var barco := _barco(selecionado)
	area_cartao = Rect2()
	if barco.is_empty():
		return
	var r := Rect2(6, size.y - ALTURA_BASE - 86, 182, 82)
	area_cartao = r
	draw_rect(r, Tokens.PAINEL)
	draw_rect(r.grow(-0.5), Tokens.FUNDO, false, 1.0)
	var x := r.position.x + 6
	Texto.desenhar(self, Vector2(x, r.position.y + 17), "CASCO %s" % barco["id"], Tokens.TEXTO)
	draw_string(fonte, Vector2(x, r.position.y + 34), "%s · %s" % [barco["gateAtual"], barco["nomeGate"]],
			HORIZONTAL_ALIGNMENT_LEFT, r.size.x - 12, TAMANHO_TEXTO, Tokens.TEXTO)
	var progresso := float(barco["progressoGate"])
	draw_rect(Rect2(x, r.position.y + 39, r.size.x - 12, 3), Tokens.FUNDO)
	draw_rect(Rect2(x, r.position.y + 39, (r.size.x - 12) * progresso, 3), Tokens.MARCA_CLARA)
	var abertas := 0
	for nc in barco["ncs"]:
		if nc["status"] == "ABERTA":
			abertas += 1
	var status: String = NOMES_STATUS.get(str(barco["status"]), str(barco["status"]))
	Texto.desenhar(self, Vector2(x, r.position.y + 58), "%s · %d%%" % [status, int(progresso * 100)], _cor_status(str(barco["status"])))
	Texto.desenhar(self, Vector2(x, r.position.y + 76), "Equipe %d" % (barco["pessoasAlocadas"] as Array).size(), Tokens.TEXTO)
	Texto.desenhar(self, Vector2(x, r.position.y + 76), "%d NC · ficha >" % abertas, Tokens.NC if abertas > 0 else Tokens.TEXTO_CALMO,
			HORIZONTAL_ALIGNMENT_RIGHT, r.size.x - 12)


## Alertas no canto de cima, à direita: NC esperando decisão (clicável) e os avisos recentes.
func _desenhar_alertas(t: float) -> void:
	var largura := 176.0
	var x := size.x - largura - 4
	var y := _topo_planta() + 4.0
	area_alerta_nc = Rect2()
	barco_alerta_nc = ""
	y = _desenhar_alerta_proposta(x, y, largura, t)
	for barco in estado["barcos"]:
		for nc in barco["ncs"]:
			if nc["status"] != "ABERTA" or not barco_alerta_nc.is_empty():
				continue
			barco_alerta_nc = barco["id"]
			var texto := "%s · %s. Decidir?" % [barco["id"], str(nc["descricao"]).to_lower()]
			var altura := fonte.get_multiline_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10, TAMANHO_TEXTO).y + 22
			area_alerta_nc = Rect2(x, y, largura, altura)
			draw_rect(area_alerta_nc, Tokens.FUNDO)
			draw_rect(area_alerta_nc.grow(-0.5), Tokens.NC if int(t * 2) % 2 == 0 else Color(Tokens.NC, 0.45), false, 1.0)
			Sprites.selo_nc(self, Vector2(x + 10, y + 9), fonte, 1, true)
			draw_string(fonte, Vector2(x + 20, y + 15), "NC no gate %s" % nc["gate"], HORIZONTAL_ALIGNMENT_LEFT, -1,
					TAMANHO_TEXTO, Tokens.TEXTO)
			draw_multiline_string(fonte, Vector2(x + 5, y + 32), texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10,
					TAMANHO_TEXTO, -1, Tokens.TEXTO)
			y += altura + 3
	var agora := Time.get_ticks_msec() / 1000.0
	var mostrados := 0
	for aviso in avisos:
		var idade: float = agora - float(aviso["quando"])
		if idade > 8.0 or mostrados >= 2:
			break
		var texto: String = aviso["texto"]
		var altura := fonte.get_multiline_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10, TAMANHO_TEXTO).y + 6
		var opaco := clampf((8.0 - idade) / 1.5, 0.0, 1.0)
		draw_rect(Rect2(x, y, largura, altura), Color(Tokens.PAINEL, 0.92 * opaco))
		draw_rect(Rect2(x, y, 2, altura), Color(Tokens.MARCA_CLARA, opaco))
		draw_multiline_string(fonte, Vector2(x + 6, y + 13), texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10,
				TAMANHO_TEXTO, -1, Color(Tokens.TEXTO, opaco))
		y += altura + 3
		mostrados += 1
	if not erro_api.is_empty():
		draw_multiline_string(fonte, Vector2(x, y + 12), erro_api, HORIZONTAL_ALIGNMENT_LEFT, largura, TAMANHO_TEXTO, 3,
				Tokens.NC)


## Proposta do vendedor esperando resposta: cartão clicável (V) no topo dos alertas. Devolve o y seguinte.
func _desenhar_alerta_proposta(x: float, y: float, largura: float, t: float) -> float:
	area_alerta_proposta = Rect2()
	proposta_alerta = ""
	var vendas = estado.get("vendas")
	if vendas == null or (vendas["propostas"] as Array).is_empty():
		return y
	var propostas: Array = vendas["propostas"]
	var p: Dictionary = propostas[0]
	proposta_alerta = str(p["id"])
	var titulo := "PROPOSTA" if propostas.size() == 1 else "PROPOSTAS (%d)" % propostas.size()
	var texto := "%s · 33 pés em %d sem. Responder até %s" % [p["cliente"], _semanas(int(p["prazoPedidoDias"])),
			_data_dia_mes(str(p["validaAte"]))]
	var altura := fonte.get_multiline_string_size(texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10, TAMANHO_TEXTO).y + 22
	area_alerta_proposta = Rect2(x, y, largura, altura)
	draw_rect(area_alerta_proposta, Tokens.FUNDO)
	draw_rect(area_alerta_proposta.grow(-0.5), Tokens.MARCA_CLARA if int(t * 2) % 2 == 0 else Color(Tokens.MARCA_CLARA, 0.45),
			false, 1.0)
	draw_string(fonte, Vector2(x + 5, y + 15), "%s · %s" % [titulo, vendas["vendedor"]], HORIZONTAL_ALIGNMENT_LEFT, -1,
			TAMANHO_TEXTO, Tokens.MARCA_CLARA)
	draw_multiline_string(fonte, Vector2(x + 5, y + 32), texto, HORIZONTAL_ALIGNMENT_LEFT, largura - 10,
			TAMANHO_TEXTO, -1, Tokens.TEXTO)
	return y + altura + 3


## Equipe e fornecedores: painel por cima da fábrica, à direita (o conteúdo são Controls na rolagem).
func _retangulo_painel() -> Rect2:
	return Rect2(size.x - LARGURA_PAINEL - 2, _topo_planta() + 4, LARGURA_PAINEL, size.y - _topo_planta() - ALTURA_BASE - 8)


func _desenhar_painel() -> void:
	if painel_desenhado.is_empty():
		return
	var r := _retangulo_painel()
	draw_rect(r, Color(Tokens.PAINEL, alfa_painel))
	draw_rect(r.grow(-0.5), Color(Tokens.FUNDO, alfa_painel), false, 1.0)
	draw_string(fonte, Vector2(r.position.x + 6, r.position.y + 15), "EQUIPE" if painel_desenhado == PAINEL_EQUIPE else "FORNECEDORES",
			HORIZONTAL_ALIGNMENT_LEFT, -1, TAMANHO_TEXTO, Color(Tokens.TEXTO, alfa_painel))


# ---------------------------------------------------------------- aviso de pausa automática (M12-1)

## O motor pausou sozinho: o jogo para, a barra de cima mostra o motivo e um aviso no centro
## leva o jogador à ação. Fim do mês e derrota abrem direto o conselho, que já é o aviso grande.
func _pausa_automatica(motivo: String) -> void:
	if velocidade > 0:
		velocidade_antes = velocidade
	_mudar_velocidade(0)
	pausa_auto = motivo
	Playtest.pausa(motivo, estado)
	_avisar("PAUSADO: %s." % _motivo_curto(motivo))
	if motivo == "FIM_DO_MES" or motivo == "DERROTA":
		_abrir_fim_do_mes(motivo)
	else:
		var barco = estado.get("barcoPausa")
		_abrir_aviso_pausa(motivo, "" if barco == null else str(barco))


func _abrir_aviso_pausa(motivo: String, barco_id: String) -> void:
	_fechar_aviso_pausa(false)
	_guardar_foco("aviso", null)
	tela_aviso = PanelContainer.new()
	tela_aviso.theme_type_variation = "Janela"
	tela_aviso.custom_minimum_size = Vector2(LARGURA_AVISO, 0)
	_veu(tela_aviso, _fechar_aviso_pausa)
	add_child(tela_aviso)
	var itens := VBoxContainer.new()
	itens.add_theme_constant_override("separation", Tokens.GRADE / 2)
	tela_aviso.add_child(itens)

	var titulo := Label.new()
	titulo.text = "PAUSADO: %s" % _motivo_curto(motivo).to_upper()
	titulo.theme_type_variation = "Titulo"
	# vermelho só para perda (NC crítica, acidente); a inspeção é uma escolha, não uma perda
	titulo.add_theme_color_override("font_color", Tokens.MARCA_CLARA if motivo == "RODADA_INSPECAO" else Tokens.NC)
	itens.add_child(_cabecalho(titulo, _fechar_aviso_pausa))
	var texto: String = ultima_mensagem.get(motivo, "")
	if motivo == "RODADA_INSPECAO":
		texto = "%s terminou o gate %s. Inspecionar agora (%d s) ou deixar com o inspetor?" % [barco_id,
				_barco(barco_id).get("gateAtual", ""), int(estado["rodada"]["segundos"])]
	if texto.is_empty():
		texto = "%s parou." % barco_id if not barco_id.is_empty() else "O jogo parou."
	itens.add_child(_rotulo_rico(texto, Tokens.TEXTO, LARGURA_AVISO - 16))

	var botoes := HBoxContainer.new()
	botoes.add_theme_constant_override("separation", Tokens.GRADE)
	botoes.alignment = BoxContainer.ALIGNMENT_END
	itens.add_child(botoes)
	var acao: Button = null
	match motivo:
		"NC_CRITICA":
			if not _barco(barco_id).is_empty():
				acao = _botao("Abrir ficha do %s" % barco_id, _ir_para_barco.bind(barco_id))
		"ACIDENTE":
			acao = _botao("Ver equipe", _ir_para_equipe)
		"RODADA_INSPECAO":
			if not _rodada_aberta(barco_id).is_empty():
				acao = _botao("Inspecionar (R)", func():
					_fechar_aviso_pausa(false)
					foco_ao_fechar.erase("aviso")
					_abrir_rodada(barco_id))
	if acao != null:
		acao.theme_type_variation = "BotaoAcao"
		botoes.add_child(acao)
	var continuar := _botao("Deixar com o inspetor" if motivo == "RODADA_INSPECAO" else "Continuar (Espaço)",
			_continuar_depois_da_pausa)
	botoes.add_child(continuar)

	# no centro da tela, já com o tamanho final
	tela_aviso.reset_size()
	tela_aviso.position = ((size - tela_aviso.get_combined_minimum_size()) / 2).floor()
	Movimento.abrir(tela_aviso)
	_focar_depois(acao if acao != null else continuar)


func _fechar_aviso_pausa(devolver_foco := true) -> void:
	if tela_aviso == null:
		return
	var janela := tela_aviso
	tela_aviso = null
	Movimento.fechar(janela, janela.queue_free)
	if devolver_foco:
		_devolver_foco("aviso")


## Continuar: volta à velocidade de antes da pausa automática.
func _continuar_depois_da_pausa() -> void:
	if pausa_auto == "DERROTA":
		return
	Playtest.continuou(pausa_auto)
	_fechar_aviso_pausa()
	if tela_conselho != null:
		_fechar_conselho()
	if tela_gestao != null:
		_fechar_gestao()
	_mudar_velocidade(velocidade_antes)


func _ir_para_barco(barco_id: String) -> void:
	_fechar_aviso_pausa(false)
	foco_ao_fechar.erase("aviso")
	selecionado = barco_id
	_mudar_galpao(Planta.galpao_do_barco(_barco(barco_id)))
	painel = PAINEL_BARCO
	_abrir_ficha_completa()


func _ir_para_equipe() -> void:
	_fechar_aviso_pausa(false)
	foco_ao_fechar.erase("aviso")
	if painel != PAINEL_EQUIPE:
		_abrir_painel(PAINEL_EQUIPE)


# ---------------------------------------------------------------- rodada de inspeção 2D (sprint 2, etapa 3)

func _rodada_aberta(barco_id: String) -> Dictionary:
	var r = estado.get("rodada")
	if r == null or barco_id.is_empty():
		return {}
	for x in r["abertas"]:
		if x["barcoId"] == barco_id:
			return x
	return {}


## Abre a rodada (o jogo fica parado enquanto ela dura).
func _abrir_rodada(barco_id: String) -> void:
	var rodada := _rodada_aberta(barco_id)
	if rodada.is_empty():
		return
	if velocidade > 0:
		velocidade_antes = velocidade
	_mudar_velocidade(0)
	if pausa_auto.is_empty():
		pausa_auto = "RODADA_INSPECAO"   # Continuar no fim volta à velocidade de antes
	tela_rodada.abrir(_barco(barco_id), rodada, estado["rodada"]["pontos"], int(estado["rodada"]["segundos"]))


func _concluir_rodada(barco_id: String, marcas: Array) -> void:
	await _enviar_ordem({"tipo": "InspecionarRodada", "barcoId": barco_id, "marcas": marcas}, "")
	var r = estado.get("rodada", {}).get("ultimoResultado")
	if r != null and r["barcoId"] == barco_id:
		Playtest.rodada(r)
		tela_rodada.mostrar_resultado(r)
	else:
		_fechar_rodada_e_seguir()


## Fechou sem inspecionar: o inspetor faz a inspeção automática.
func _cancelar_rodada() -> void:
	tela_rodada.fechar()
	_continuar_depois_da_pausa()


func _fechar_rodada_e_seguir() -> void:
	tela_rodada.fechar()
	_continuar_depois_da_pausa()


# ---------------------------------------------------------------- vendas (M14)

## Janela da proposta: os dois lados de cada prazo (chance de o cliente desistir x multa estimada).
## O jogo não pausa: a proposta tem validade e a janela acompanha o dia.
func _abrir_proposta(id: String) -> void:
	if _proposta(id).is_empty():
		return
	if tela_proposta == null:
		_guardar_foco("proposta", botao_proposta)
	proposta_na_tela = id
	assinatura_proposta = ""
	_atualizar_proposta()


func _atualizar_proposta() -> void:
	var p := _proposta(proposta_na_tela)
	if p.is_empty():   # respondida, expirada ou recusada
		_fechar_proposta()
		return
	var assinatura := JSON.stringify(p)
	if assinatura == assinatura_proposta:
		return
	assinatura_proposta = assinatura
	var primeira := tela_proposta == null
	var foco := 0
	if not primeira:
		var dono := get_viewport().gui_get_focus_owner()
		foco = maxi(0, _botoes_em(tela_proposta).find(dono))
		tela_proposta.queue_free()
	tela_proposta = PanelContainer.new()
	tela_proposta.theme_type_variation = "Janela"
	tela_proposta.custom_minimum_size = Vector2(LARGURA_PROPOSTA, 0)
	_veu(tela_proposta, _fechar_proposta)
	add_child(tela_proposta)
	var itens := VBoxContainer.new()
	itens.add_theme_constant_override("separation", Tokens.GRADE / 2)
	tela_proposta.add_child(itens)
	var largura := LARGURA_PROPOSTA - 16

	var titulo := Label.new()
	titulo.text = "PROPOSTA · %s" % p["cliente"]
	titulo.theme_type_variation = "Titulo"
	itens.add_child(_cabecalho(titulo, _fechar_proposta))
	var vendas: Dictionary = estado["vendas"]
	itens.add_child(_rotulo_rico("%s: 33 pés por %s. Responda até %s." % [vendas["vendedor"], _reais(p["preco"]),
			_data_dia_mes(str(p["validaAte"]))], Tokens.TEXTO_CALMO, largura))
	itens.add_child(_rotulo_rico("Cliente pede %d sem (%s) · a fábrica consegue %d sem (%s)" % [
			_semanas(int(p["prazoPedidoDias"])), _data_dia_mes(str(p["dataPedida"])),
			_semanas(int(p["prazoFabricaDias"])), _data_dia_mes(str(p["dataFabrica"]))], Tokens.TEXTO, largura))

	for o in p["opcoes"]:
		var linha := HBoxContainer.new()
		linha.add_theme_constant_override("separation", Tokens.GRADE)
		var b := _botao("%s: %d sem (%s)" % [NOMES_OPCAO.get(str(o["tipo"]), str(o["tipo"])), _semanas(int(o["prazoDias"])),
				_data_dia_mes(str(o["data"]))], _aceitar_proposta.bind(str(p["id"]), int(o["prazoDias"])))
		b.custom_minimum_size.x = 196
		if str(o["tipo"]) == "FABRICA":
			b.theme_type_variation = "BotaoAcao"   # a palavra final é da fábrica: prazo que ela cumpre
		linha.add_child(b)
		var riscos := VBoxContainer.new()
		riscos.add_theme_constant_override("separation", 0)
		for r in _riscos(o):
			riscos.add_child(_rotulo_rico(r[0], r[1], largura - 204))
		linha.add_child(riscos)
		itens.add_child(linha)
	var recusar := _botao("Recusar", _recusar_proposta.bind(str(p["id"])))
	itens.add_child(recusar)

	tela_proposta.reset_size()
	tela_proposta.position = ((size - tela_proposta.get_combined_minimum_size()) / 2).floor()
	if primeira:
		Movimento.abrir(tela_proposta)
		var principal: Control = _botoes_em(itens)[1]   # o primeiro botão é o X
		for b in _botoes_em(itens):
			if (b as Control).theme_type_variation == "BotaoAcao":
				principal = b
		_focar_depois(principal)
	else:
		_focar_indice(tela_proposta, foco)


## Os dois lados do prazo: chance de o cliente desistir (laranja) e multa estimada (vermelho), uma linha cada.
func _riscos(o: Dictionary) -> Array:
	var chance := int(round(float(o["chanceDesistencia"]) * 100))
	var multa := float(o["multaEstimada"])
	var lista := []
	if chance > 0:
		lista.append(["Desiste: %d%%" % chance, Tokens.ATENCAO])
	if multa > 0:
		lista.append(["Multa ~%s" % _mil_reais(multa), Tokens.NC])
	if lista.is_empty():
		lista.append(["Sem risco", Tokens.OK])
	return lista


func _aceitar_proposta(id: String, prazo_dias: int) -> void:
	_fechar_proposta()
	await _enviar_ordem({"tipo": "AceitarProposta", "propostaId": id, "prazoDiasUteis": prazo_dias}, "")


func _recusar_proposta(id: String) -> void:
	_fechar_proposta()
	await _enviar_ordem({"tipo": "RecusarProposta", "propostaId": id}, "")


func _fechar_proposta() -> void:
	if tela_proposta != null:
		var janela := tela_proposta
		tela_proposta = null
		proposta_na_tela = ""
		Movimento.fechar(janela, janela.queue_free)
		_devolver_foco("proposta")


func _proposta(id: String) -> Dictionary:
	var vendas = estado.get("vendas")
	if vendas == null:
		return {}
	for p in vendas["propostas"]:
		if p["id"] == id:
			return p
	return {}


## Capacidade declarada para o próximo mês (vale no mês seguinte e define o prazo das propostas).
func _linha_capacidade(largura: float) -> HBoxContainer:
	var vendas: Dictionary = estado["vendas"]
	var vigente := int(vendas["capacidadeVigente"])
	var linha := HBoxContainer.new()
	linha.add_theme_constant_override("separation", Tokens.GRADE / 2)
	var declarada := int(vendas["capacidadeDeclarada"])
	var menos := _botao("-", _declarar_capacidade.bind(declarada - 1))
	menos.disabled = declarada <= 1
	menos.tooltip_text = "Menos barcos: prazo maior, menos vendas"
	var mais := _botao("+", _declarar_capacidade.bind(declarada + 1))
	mais.disabled = declarada >= int(vendas["capacidadeMaxima"])
	mais.tooltip_text = "Mais barcos: prazo menor, mais vendas e mais risco de multa"
	linha.add_child(_rotulo_rico("Capacidade %d/mês · próx. %d" % [vigente, declarada], Tokens.TEXTO, largura - 60))
	linha.add_child(menos)
	linha.add_child(mais)
	return linha


func _declarar_capacidade(barcos: int) -> void:
	await _enviar_ordem({"tipo": "DeclararCapacidade", "barcosPorMes": barcos}, "")
	_reabrir_tela_do_mes()


## Dias úteis -> semanas (semana iniciada conta, como no motor).
func _semanas(dias: int) -> int:
	var por_semana := 5
	var vendas = estado.get("vendas")
	if vendas != null:
		por_semana = int(vendas["diasUteisPorSemana"])
	return ceili(float(dias) / por_semana)


## "2027-08-12" -> "12/08".
static func _data_dia_mes(iso: String) -> String:
	var p := iso.split("-")
	return iso if p.size() != 3 else "%s/%s" % [p[2], p[1]]


## "R$ 12 mil".
func _mil_reais(valor: float) -> String:
	return "R$ %d mil" % int(round(valor / 1000.0))


# ---------------------------------------------------------------- painel de gestão (M15, sprint 2 etapa 4)

func _abrir_fim_do_mes(motivo: String) -> void:
	if motivo == "DERROTA":
		_abrir_conselho()
	else:
		_abrir_gestao(true)


func _alternar_gestao() -> void:
	if tela_gestao != null:
		_fechar_gestao()
	else:
		# parado no fechamento do mês, o painel volta no modo fechamento
		_abrir_gestao(pausa_auto == "FIM_DO_MES" and estado.get("conselho") != null)


func _reabrir_tela_do_mes() -> void:
	if tela_gestao != null:
		_abrir_gestao(gestao_fechamento)
	elif tela_conselho != null:
		_abrir_conselho()


var _assinatura_gestao := ""


## Refaz o painel quando o estado muda (o jogo pode estar rodando com ele aberto).
func _atualizar_gestao() -> void:
	var assinatura := JSON.stringify([estado.get("gestao"), estado.get("financeiro", {}).get("caixa"),
			estado.get("vendas"), estado.get("conselho") != null])
	if assinatura != _assinatura_gestao:
		_abrir_gestao(gestao_fechamento)


func _abrir_gestao(fechamento: bool) -> void:
	if estado.is_empty():
		return
	var r = estado.get("conselho")
	if fechamento and r == null:
		fechamento = false
	gestao_fechamento = fechamento
	_assinatura_gestao = JSON.stringify([estado.get("gestao"), estado.get("financeiro", {}).get("caixa"),
			estado.get("vendas"), estado.get("conselho") != null])
	var primeira := tela_gestao == null
	var foco := 0
	if primeira:
		_guardar_foco("gestao", botao_conselho)
	else:
		foco = maxi(0, _botoes_em(tela_gestao).find(get_viewport().gui_get_focus_owner()))
		tela_gestao.queue_free()
	tela_gestao = PanelContainer.new()
	tela_gestao.theme_type_variation = "Janela"
	tela_gestao.position = Vector2(16, ALTURA_TOPO + 4)
	tela_gestao.size = Vector2(size.x - 32, size.y - ALTURA_TOPO - ALTURA_BASE - 8)
	_veu(tela_gestao, _fechar_gestao)
	add_child(tela_gestao)
	if primeira:
		Movimento.abrir(tela_gestao)

	var rolagem_g := ScrollContainer.new()
	rolagem_g.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	rolagem_g.follow_focus = true
	tela_gestao.add_child(rolagem_g)
	var lista := VBoxContainer.new()
	lista.custom_minimum_size.x = tela_gestao.size.x - 24
	lista.add_theme_constant_override("separation", 2)
	rolagem_g.add_child(lista)
	var largura := lista.custom_minimum_size.x
	var g: Dictionary = estado["gestao"]
	var fin: Dictionary = estado["financeiro"]

	var titulo := Label.new()
	titulo.theme_type_variation = "Titulo"
	if fechamento:
		titulo.text = "FECHAMENTO DE %s" % str(r["mesReferencia"])
	else:
		titulo.text = "PAINEL DE GESTÃO · %s · dia %d de %d" % [_mes_curto(str(estado["mesReferencia"])),
				int(g["diaUtilDoMes"]), int(g["diasUteisPorMes"])]
	lista.add_child(_cabecalho(titulo, _fechar_gestao))

	var colunas := HBoxContainer.new()
	colunas.add_theme_constant_override("separation", Tokens.GRADE * 2)
	lista.add_child(colunas)
	var largura_col := floorf((largura - Tokens.GRADE * 2) / 2)
	var esquerda := VBoxContainer.new()
	esquerda.add_theme_constant_override("separation", 1)
	esquerda.custom_minimum_size.x = largura_col
	colunas.add_child(esquerda)
	var direita := VBoxContainer.new()
	direita.add_theme_constant_override("separation", 1)
	direita.custom_minimum_size.x = largura_col
	colunas.add_child(direita)

	# ----- metas (no fechamento, as do mês que fechou)
	var metas: Array = r["metas"] if fechamento else g["metasDoMes"]
	for m in metas:
		esquerda.add_child(_rotulo_rico(str(m["descricao"]), Tokens.TEXTO, largura_col))
		var linha := HBoxContainer.new()
		linha.add_theme_constant_override("separation", 6)
		var barra := _barra_meta(m, largura_col - 126, fechamento)
		barra.size_flags_vertical = Control.SIZE_SHRINK_CENTER
		linha.add_child(barra)
		linha.add_child(_rotulo_rico(str(m["resultado"]).replace(",00", ""), _cor_meta(m, fechamento), 120))
		esquerda.add_child(linha)
	var limite_ruim := int(g["metasPerdidasParaMesRuim"])
	var limite_meses := int(fin["mesesRuinsParaDemissao"])
	var ruins := int(fin["mesesRuinsSeguidos"])
	if fechamento:
		esquerda.add_child(_rotulo_rico(("MÊS RUIM: %d de %d seguidos" % [ruins, limite_meses]) if r["mesRuim"]
				else "Mês aceitável: contagem zerada", Tokens.NC if r["mesRuim"] else Tokens.OK, largura_col))
	else:
		var perdidas := int(g["metasPerdidas"])
		esquerda.add_child(_rotulo_rico("Hoje: %d perdida(s) · ruim = %d+" % [perdidas, limite_ruim],
				Tokens.NC if perdidas >= limite_ruim else (Tokens.ATENCAO if perdidas > 0 else Tokens.OK), largura_col))
	esquerda.add_child(_rotulo_rico("Meses ruins: %d de %d · paciência %d/%d" % [ruins, limite_meses,
			int(fin["pacienciaConselho"]), int(fin["pacienciaMaxima"])],
			_cor_conselho(ruins, limite_meses) if ruins > 0 else Tokens.TEXTO_CALMO, largura_col))

	# ----- números
	direita.add_child(_rotulo_rico("Caixa %s · a receber %s" % [_milhoes(fin["caixa"]), _milhoes(fin["cotasAReceber"])],
			Tokens.OK if float(fin["caixa"]) >= 0 else Tokens.NC, largura_col))
	if fechamento:
		var dre: Dictionary = r["dre"]
		direita.add_child(_rotulo_rico("EBITDA do mês %s" % _reais(dre["ebitda"]),
				Tokens.OK if float(dre["ebitda"]) >= 0 else Tokens.NC, largura_col))
		direita.add_child(_rotulo_rico("Receita %s" % _reais(dre["receita"]), Tokens.TEXTO, largura_col))
	else:
		var ebitda = g["ebitdaMesPassado"]
		direita.add_child(_rotulo_rico("EBITDA do mês passado %s" % ("-" if ebitda == null else _reais(ebitda)),
				Tokens.TEXTO if ebitda == null else (Tokens.OK if float(ebitda) >= 0 else Tokens.NC), largura_col))
		direita.add_child(_rotulo_rico("Receita do mês até agora %s" % _reais(fin["receitaDoMes"]), Tokens.TEXTO, largura_col))
	if not fechamento:
		direita.add_child(_rotulo_rico("Valor da empresa %s" % _reais(fin["valorEmpresa"]), Tokens.MARCA_CLARA, largura_col))
	var perdas := "Multas %s · garantia %s" % [_mil_reais(float(g["multasDoMes"])), _mil_reais(float(g["garantiaDoMes"]))]
	direita.add_child(_rotulo_rico(perdas, Tokens.NC if float(g["multasDoMes"]) + float(g["garantiaDoMes"]) > 0
			else Tokens.TEXTO_CALMO, largura_col))
	if not fechamento:
		direita.add_child(_rotulo_rico("NCs no mês %d · acidentes %d" % [int(g["ncsDetectadasDoMes"]), int(g["acidentesDoMes"])],
				Tokens.NC if int(g["acidentesDoMes"]) > 0 else Tokens.TEXTO, largura_col))
		direita.add_child(_rotulo_rico(_ncs_por_barco(), Tokens.TEXTO_CALMO, largura_col))

	# ----- vendas
	var vendas: Dictionary = estado["vendas"]
	if not fechamento:
		direita.add_child(_rotulo_rico("VENDAS · carteira %d · propostas %d" % [(vendas["carteira"] as Array).size(),
				(vendas["propostas"] as Array).size()], Tokens.TEXTO_CALMO, largura_col))
	direita.add_child(_linha_capacidade(largura_col))

	# ----- fechamento: decisão do conselho (uma por linha, na coluna da direita) e Continuar
	var botoes := HBoxContainer.new()
	botoes.add_theme_constant_override("separation", Tokens.GRADE / 2)
	var primeira_livre: Button = null
	if fechamento and r["derrota"] == null:
		direita.add_child(_rotulo_rico("Decisão do conselho (uma):", Tokens.TEXTO_CALMO, largura_col))
		var opcoes := VBoxContainer.new()
		opcoes.add_theme_constant_override("separation", 1)
		var escolhida = r["decisaoEscolhida"]
		for o in r["opcoes"]:
			var custo := "" if float(o["custo"]) == 0 else " (%s)" % _mil_reais(float(o["custo"]))
			var b := _botao("%s%s" % [o["titulo"], custo], _escolher_decisao.bind(o["id"]))
			b.tooltip_text = o["descricao"]
			b.disabled = escolhida != null
			if escolhida == o["id"]:
				b.text = "> " + b.text
			if primeira_livre == null and not b.disabled:
				primeira_livre = b
			opcoes.add_child(b)
		direita.add_child(opcoes)
	botoes.add_child(_botao("Baixar log (F8)", _baixar_log))
	botoes.add_child(_botao("Última reunião", func():
		_fechar_gestao()
		_abrir_conselho()))
	var fechar_ou_seguir := _botao("Continuar" if fechamento else "Fechar",
			_continuar_depois_da_pausa if fechamento else _fechar_gestao)
	if fechamento:
		fechar_ou_seguir.theme_type_variation = "BotaoAcao"
	botoes.add_child(fechar_ou_seguir)
	botoes.get_child(1).disabled = r == null
	lista.add_child(botoes)

	if primeira:
		# fechamento: a ação principal é a primeira decisão livre; decidido, Continuar (como na reunião)
		_focar_depois(primeira_livre if primeira_livre != null else fechar_ou_seguir)
	else:
		_focar_indice(tela_gestao, foco)


## Barra da meta: "chegar a" enche até o alvo; "não passar de" enche até o limite e fica vermelha se passar.
func _barra_meta(m: Dictionary, largura: float, final: bool) -> Control:
	var c := Control.new()
	c.custom_minimum_size = Vector2(largura, 6)
	var atual := float(m["atual"])
	var alvo := float(m["alvo"])
	var fracao := 0.0
	if m["limite"]:
		fracao = (1.0 if atual > 0 else 0.0) if alvo == 0 else atual / alvo
	else:
		fracao = 1.0 if alvo <= 0 else atual / alvo
	var cor := _cor_meta(m, final)
	c.draw.connect(func():
		c.draw_rect(Rect2(0, 0, largura, 4), Tokens.FUNDO)
		c.draw_rect(Rect2(0, 0, largura * clampf(fracao, 0.0, 1.0), 4), cor))
	return c


## "NCs por barco: 33-001 1 · 33-002 0 ..." (só os barcos em produção).
func _ncs_por_barco() -> String:
	var partes := []
	for b in estado["barcos"]:
		if b["status"] == "ENTREGUE":
			continue
		partes.append("%s %d" % [str(b["id"]).substr(3), (b["ncs"] as Array).size()])
	return "NCs por barco: " + (" · ".join(partes) if not partes.is_empty() else "-")


## Log de playtest: baixa o arquivo (navegador) ou abre a pasta (desktop).
func _baixar_log() -> void:
	var onde := Playtest.exportar()
	_avisar("Log de playtest: %s" % onde if not onde.is_empty() else "Não foi possível salvar o log.")


## Nome da tela de cima, para o log saber onde o jogador estava quando parou.
func _tela_de_cima() -> String:
	if tela_rodada != null and tela_rodada.aberta:
		return "rodada"
	if tela_proposta != null:
		return "proposta"
	if tela_gestao != null:
		return "fechamento" if gestao_fechamento else "gestao"
	if tela_conselho != null:
		return "conselho"
	if tela_aviso != null:
		return "aviso:" + pausa_auto
	if tela_3d != null and tela_3d.aberta:
		return "inspecao_3d"
	if tela_ficha != null and tela_ficha.aberta:
		return "ficha"
	if painel != PAINEL_BARCO:
		return painel
	return "fabrica"


func _fechar_gestao() -> void:
	if tela_gestao != null:
		var janela := tela_gestao
		tela_gestao = null
		Movimento.fechar(janela, janela.queue_free)
		_devolver_foco("gestao")


# ---------------------------------------------------------------- lista de saves

func _alternar_saves() -> void:
	if tela_saves != null:
		_fechar_saves()
	else:
		_abrir_saves()


func _abrir_saves() -> void:
	if tela_saves == null:
		_guardar_foco("saves", botao_carregar)
	_mudar_velocidade(0)
	var lista = await Api.saves()
	if lista == null:
		return
	var primeira := tela_saves == null
	if tela_saves != null:
		tela_saves.queue_free()
	tela_saves = _nova_sobreposicao()
	if primeira:
		Movimento.abrir(tela_saves)
	var itens := VBoxContainer.new()
	itens.add_theme_constant_override("separation", 2)
	tela_saves.add_child(itens)
	var titulo := Label.new()
	titulo.text = "CARREGAR PARTIDA"
	titulo.theme_type_variation = "Titulo"
	titulo.add_theme_color_override("font_color", Tokens.MARCA_CLARA)
	itens.add_child(_cabecalho(titulo, _fechar_saves))
	if lista.is_empty():
		itens.add_child(_linha_conselho("Nenhum save ainda. Use Salvar (F5).", Tokens.TEXTO_CALMO, 320))
	for save in lista.slice(0, 8):
		var atual: bool = save["id"] == Api.partida_id
		var texto := "%s %02dh · semente %d%s" % [_data_curta(str(save["data"])), int(save["hora"]), int(save["semente"]),
				" · esta partida" if atual else ""]
		itens.add_child(_botao(texto, _carregar_save.bind(str(save["id"]))))
	var voltar := _botao("Voltar", _fechar_saves)
	itens.add_child(voltar)
	# ação principal: o save mais recente; sem save, Voltar (o primeiro botão da lista é o X)
	var botoes := _botoes_em(itens)
	_focar_depois(botoes[1] if botoes.size() > 2 else voltar)


## Camada transparente atrás de uma janela: clicar fora dela fecha a janela (M12-8). Some junto com a janela.
func _veu(janela: Control, fechar_janela: Callable) -> void:
	var v := Control.new()
	v.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	v.mouse_filter = Control.MOUSE_FILTER_STOP
	v.gui_input.connect(func(e: InputEvent):
		if e is InputEventMouseButton and e.pressed and e.button_index in [MOUSE_BUTTON_LEFT, MOUSE_BUTTON_RIGHT]:
			fechar_janela.call())
	add_child(v)
	janela.tree_exiting.connect(v.queue_free)


## Título com o X de fechar à direita.
func _cabecalho(titulo: Label, fechar_painel: Callable) -> HBoxContainer:
	var linha := HBoxContainer.new()
	titulo.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	linha.add_child(titulo)
	linha.add_child(_botao_fechar(fechar_painel))
	return linha


func _nova_sobreposicao() -> PanelContainer:
	var painel_novo := PanelContainer.new()
	painel_novo.theme_type_variation = "Janela"
	painel_novo.position = Vector2(size.x / 2 - 170, ALTURA_TOPO + 30)
	painel_novo.custom_minimum_size = Vector2(340, 0)
	_veu(painel_novo, _fechar_saves)
	add_child(painel_novo)
	return painel_novo


func _carregar_save(id: String) -> void:
	_fechar_saves()
	await _carregar(id)


func _fechar_saves() -> void:
	if tela_saves != null:
		var janela := tela_saves
		tela_saves = null
		Movimento.fechar(janela, janela.queue_free)
		_devolver_foco("saves")


# ---------------------------------------------------------------- tela do conselho

func _alternar_conselho() -> void:
	if tela_conselho != null:
		_fechar_conselho()
	else:
		_abrir_conselho()


func _abrir_conselho() -> void:
	var r = estado.get("conselho")
	if r == null:
		return
	var primeira := tela_conselho == null
	if tela_conselho != null:
		tela_conselho.queue_free()
	else:
		_guardar_foco("conselho", botao_conselho)
	tela_conselho = PanelContainer.new()
	tela_conselho.theme_type_variation = "Janela"
	tela_conselho.position = Vector2(40, ALTURA_TOPO + 6)
	tela_conselho.size = Vector2(size.x - 80, size.y - ALTURA_TOPO - ALTURA_BASE - 12)
	_veu(tela_conselho, _fechar_conselho)
	add_child(tela_conselho)
	if primeira:
		Movimento.abrir(tela_conselho)

	var rolagem_conselho := ScrollContainer.new()
	rolagem_conselho.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	tela_conselho.add_child(rolagem_conselho)
	var lista := VBoxContainer.new()
	lista.custom_minimum_size.x = tela_conselho.size.x - 24
	lista.add_theme_constant_override("separation", 0)
	rolagem_conselho.add_child(lista)
	var largura := lista.custom_minimum_size.x

	var titulo := Label.new()
	titulo.text = "REUNIÃO DO CONSELHO - %s" % r["mesReferencia"]
	titulo.theme_type_variation = "Titulo"
	lista.add_child(_cabecalho(titulo, _fechar_conselho))

	for m in r["metas"]:
		var cor := Tokens.OK if m["cumprida"] else Tokens.NC
		lista.add_child(_linha_conselho("%s %s  (%s)" % ["[OK]" if m["cumprida"] else "[X] ", m["descricao"], m["resultado"]], cor, largura))
	lista.add_child(_linha_conselho("Paciência do conselho: %d -> %d de %d" % [int(r["pacienciaAntes"]),
			int(r["pacienciaDepois"]), int(estado["financeiro"]["pacienciaMaxima"])],
			Tokens.ATENCAO if int(r["pacienciaDepois"]) < int(r["pacienciaAntes"]) else Tokens.TEXTO, largura))
	var limite := int(estado["financeiro"]["mesesRuinsParaDemissao"])
	if r["mesRuim"]:
		lista.add_child(_linha_conselho("MÊS RUIM: %d de %d meses ruins seguidos. No %dº o conselho demite o CEO." % [int(r["mesesRuinsSeguidos"]), limite, limite],
				Tokens.NC, largura))
	else:
		lista.add_child(_linha_conselho("Mês aceitável: a contagem de meses ruins voltou a zero.", Tokens.OK, largura))

	var dre: Dictionary = r["dre"]
	lista.add_child(_linha_conselho("DRE: receita %s | material %s | pessoal %s | fixas %s" % [_reais(dre["receita"]),
			_reais(float(dre["custoMaterial"]) + float(dre["consumiveis"]) + float(dre["materialRetrabalho"])),
			_reais(dre["pessoal"]), _reais(dre["despesasFixas"])], Tokens.TEXTO, largura))
	if float(dre["multas"]) > 0:
		lista.add_child(_linha_conselho("Multas por atraso: %s" % _reais(dre["multas"]), Tokens.NC, largura))
	var cor_ebitda := Tokens.OK if float(dre["ebitda"]) >= 0 else Tokens.NC
	lista.add_child(_linha_conselho("EBITDA %s | Caixa %s | Valor da empresa %s" % [_reais(dre["ebitda"]),
			_reais(r["caixa"]), _reais(r["valorEmpresa"])], cor_ebitda, largura))
	lista.add_child(_linha_conselho("Entregas %d | NCs %d | RNCs %d | concessões %d | acidentes %d" % [int(r["barcosEntregues"]),
			int(r["ncsDetectadas"]), int(r["rncs"]), int(r["concessoes"]), int(r["acidentes"])], Tokens.TEXTO_CALMO, largura))
	if r["alertaCaixaNegativo"]:
		lista.add_child(_linha_conselho("ALERTA: caixa negativo. 3 meses seguidos encerram a empresa.", Tokens.NC, largura))

	for f in r["falas"]:
		lista.add_child(_linha_conselho("%s: \"%s\"" % [f["conselheiro"], f["texto"]],
				Tokens.TEXTO if f["seria"] else Tokens.MARCA_CLARA, largura))

	if r["derrota"] != null:
		lista.add_child(_linha_conselho("FIM DE JOGO. %s" % ("Três meses de caixa negativo." if r["derrota"] == "CAIXA"
				else "Três meses ruins seguidos com o conselho."), Tokens.NC, largura))
		var nova := _botao("Nova partida", func(): get_tree().reload_current_scene())
		lista.add_child(nova)
		_focar_depois(nova)
		return

	lista.add_child(_linha_conselho("Decisão para o próximo mês (uma):", Tokens.TEXTO, largura))
	var escolhida = r["decisaoEscolhida"]
	for o in r["opcoes"]:
		var custo := "" if float(o["custo"]) == 0 else " (%s)" % _reais(o["custo"])
		var b := _botao("%s%s" % [o["titulo"], custo], _escolher_decisao.bind(o["id"]))
		b.tooltip_text = o["descricao"]
		b.disabled = escolhida != null
		if escolhida == o["id"]:
			b.text = "> " + b.text
		lista.add_child(b)
	var continuar := _botao("Continuar", _continuar_depois_da_pausa if pausa_auto == "FIM_DO_MES" else _fechar_conselho)
	if escolhida != null:
		continuar.theme_type_variation = "BotaoAcao"   # decidido: a ação principal passa a ser seguir o jogo
	lista.add_child(continuar)
	# ação principal: a primeira decisão ainda livre; já decidido, Continuar (o primeiro botão é o X)
	var alvo: Control = continuar
	for b in _botoes_em(lista).slice(1):
		if b != continuar and not b.disabled:
			alvo = b
			break
	_focar_depois(alvo)


func _linha_conselho(texto: String, cor: Color, largura: float) -> RichTextLabel:
	return _rotulo_rico(texto, cor, largura)


func _escolher_decisao(opcao_id: String) -> void:
	await _enviar_ordem({"tipo": "EscolherDecisaoConselho", "opcaoId": opcao_id}, "")
	_reabrir_tela_do_mes()


func _fechar_conselho() -> void:
	if tela_conselho != null:
		var janela := tela_conselho
		tela_conselho = null
		Movimento.fechar(janela, janela.queue_free)
		_devolver_foco("conselho")


## Só para exibir (as contas ficam no motor, em BigDecimal): "R$ 1.234.567".
func _reais(valor) -> String:
	var v := float(valor)
	var texto := str(int(round(absf(v))))
	var saida := ""
	while texto.length() > 3:
		saida = "." + texto.substr(texto.length() - 3) + saida
		texto = texto.substr(0, texto.length() - 3)
	return ("-" if v < 0 else "") + "R$ " + texto + saida


## "R$ 2,62 mi" para a barra de topo.
func _milhoes(valor) -> String:
	var v := float(valor) / 1000000.0
	return ("R$ %.2f mi" % v).replace(".", ",")


# ---------------------------------------------------------------- utilitários

func _barco(id: String) -> Dictionary:
	if estado.is_empty() or id.is_empty():
		return {}
	for barco in estado["barcos"]:
		if barco["id"] == id:
			return barco
	return {}


func _cor_status(status: String) -> Color:
	match status:
		"AGUARDANDO_INSPECAO", "EM_RETRABALHO", "APROVADO", "PARADO_MATERIAL":
			return Tokens.ATENCAO
		"EM_INSPECAO":
			return Tokens.MARCA_CLARA
		"AGUARDANDO_DECISAO":
			return Tokens.NC
		"ENTREGUE":
			return Tokens.OK
	return Tokens.TEXTO


## Motivo da pausa automática em poucas palavras (barra de cima, título do aviso e lista de avisos).
func _motivo_curto(motivo: String) -> String:
	match motivo:
		"FIM_DO_MES":
			return "fim do mês"
		"DERROTA":
			return "fim de jogo"
		"NC_CRITICA":
			return "NC crítica"
		"ACIDENTE":
			return "acidente"
		"RODADA_INSPECAO":
			return "inspeção"
	return motivo.to_lower()


## Cor do bloco "Conselho" na barra de cima: segue os meses ruins seguidos (a paciência é só termômetro).
func _cor_conselho(ruins: int, limite: int) -> Color:
	if ruins <= 0:
		return Tokens.TEXTO
	return Tokens.NC if ruins >= limite - 1 else Tokens.ATENCAO
