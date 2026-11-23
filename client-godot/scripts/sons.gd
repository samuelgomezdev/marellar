extends Node
## Autoload Sons: um som curto por tipo de evento (token som.evento; nunca dois eventos com o mesmo som).
## Os arquivos são gerados por ferramentas/gerar_sons.gd e podem ser trocados por sons do jsfxr.
## Toca mesmo com a árvore pausada (hitstop). Sons de interface já ligados: foco (só quando o foco
## muda por teclado ou controle), clique (todo botão) e abrir/fechar painel (Movimento).
## Os sons dos eventos de jogo entram na sprint 4.

const ARQUIVOS := {
	"foco": "res://assets/sons/foco.wav",
	"clique": "res://assets/sons/clique.wav",
	"abrir_painel": "res://assets/sons/abrir_painel.wav",
	"fechar_painel": "res://assets/sons/fechar_painel.wav",
	"aprovar": "res://assets/sons/aprovar.wav",
	"reprovar": "res://assets/sons/reprovar.wav",
	"nc_encontrada": "res://assets/sons/nc_encontrada.wav",
	"entrega": "res://assets/sons/entrega.wav",
	"venda_fechada": "res://assets/sons/venda_fechada.wav",
	"multa": "res://assets/sons/multa.wav",
}
const VOZES := 6              # sons ao mesmo tempo
const VOLUME_DB := -8.0
const JANELA_FOCO_MS := 150   # o som de foco só toca se o foco mudou logo depois de uma tecla ou botão do controle

var _sons := {}
var _vozes: Array[AudioStreamPlayer] = []
var _proxima := 0
var _ultima_entrada_ms := -100000


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS
	for evento in ARQUIVOS:
		_sons[evento] = load(ARQUIVOS[evento])
	for i in VOZES:
		var voz := AudioStreamPlayer.new()
		voz.volume_db = VOLUME_DB
		add_child(voz)
		_vozes.append(voz)
	get_tree().node_added.connect(_no_adicionado)
	get_viewport().gui_focus_changed.connect(_foco_mudou)


func tocar(evento: String) -> void:
	var som: AudioStream = _sons.get(evento)
	if som == null:
		push_warning("Sons: evento sem som: %s" % evento)
		return
	var voz := _vozes[_proxima]
	_proxima = (_proxima + 1) % VOZES
	voz.stream = som
	voz.play()


func _input(event: InputEvent) -> void:
	if (event is InputEventKey or event is InputEventJoypadButton or event is InputEventJoypadMotion) and event.is_pressed():
		_ultima_entrada_ms = Time.get_ticks_msec()


func _no_adicionado(no: Node) -> void:
	if no is BaseButton:
		(no as BaseButton).pressed.connect(tocar.bind("clique"))


func _foco_mudou(_controle: Control) -> void:
	if Time.get_ticks_msec() - _ultima_entrada_ms <= JANELA_FOCO_MS:
		tocar("foco")
