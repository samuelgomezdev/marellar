extends Node
## Autoload Impacto: sensação de peso nos eventos importantes (GDD, princípio "sentir o impacto").
## Só a infraestrutura nesta sprint; ligar aos eventos (aprovar, reprovar, entregar, fechar venda) é a sprint 4.
##   Impacto.hitstop(4)   pausa a cena por 3 a 6 quadros (token mov.impacto)
##   Impacto.tremor(2.0)  treme a tela alguns pixels inteiros por ~0,25 s
## Roda sempre (PROCESS_MODE_ALWAYS): o áudio (Sons) e os nós marcados como sempre ativos não param.

const DURACAO_TREMOR := 0.25

var _quadros := 0             # quadros de hitstop que faltam
var _pausou_aqui := false     # o hitstop pausou a árvore (se já estava pausada, não despausa)
var _tremor := 0.0
var _tempo_tremor := 0.0
var _base := Transform2D.IDENTITY


func _ready() -> void:
	process_mode = Node.PROCESS_MODE_ALWAYS


func hitstop(quadros: int) -> void:
	quadros = clampi(quadros, Tokens.IMPACTO_MIN, Tokens.IMPACTO_MAX)
	if _quadros > 0:
		_quadros = maxi(_quadros, quadros)
		return
	_quadros = quadros
	if not get_tree().paused:
		get_tree().paused = true
		_pausou_aqui = true


## Intensidade em pixels da tela base (1 = leve, 4 = forte).
func tremor(intensidade: float) -> void:
	if _tempo_tremor <= 0.0:
		_base = get_viewport().canvas_transform
	_tremor = maxf(_tremor if _tempo_tremor > 0.0 else 0.0, intensidade)
	_tempo_tremor = DURACAO_TREMOR


func em_hitstop() -> bool:
	return _quadros > 0 or _pausou_aqui


func _process(delta: float) -> void:
	# O Impacto roda antes da cena em cada quadro: conta o quadro parado e só despausa no quadro seguinte
	# ao último, para a cena ficar parada exatamente o número de quadros pedido.
	if _quadros > 0:
		_quadros -= 1
	elif _pausou_aqui:
		get_tree().paused = false
		_pausou_aqui = false
	if _tempo_tremor > 0.0:
		_tempo_tremor -= delta
		var t := get_viewport().canvas_transform
		if _tempo_tremor <= 0.0:
			t.origin = _base.origin
		else:
			# deslocamento em pixels inteiros (pixel art nítida), diminuindo até parar
			var forca := roundi(_tremor * _tempo_tremor / DURACAO_TREMOR)
			t.origin = _base.origin + Vector2(randi_range(-forca, forca), randi_range(-forca, forca))
		get_viewport().canvas_transform = t
