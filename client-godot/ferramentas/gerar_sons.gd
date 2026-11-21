extends SceneTree
## Gera os sons provisórios da interface e do jogo em assets/sons/*.wav, no estilo do jsfxr
## (onda quadrada, dente de serra, triângulo, seno ou ruído, com variação de tom e envelope curto).
## Cada evento tem um som diferente (token som.evento). Para trocar por sons do jsfxr, basta
## substituir o .wav com o mesmo nome. Ruído com semente fixa: rodar de novo gera os mesmos arquivos.
##
##   godot --headless --path client-godot --script res://ferramentas/gerar_sons.gd

const PASTA := "res://assets/sons/"
const TAXA := 22050

## Cada som é uma lista de trechos: [forma, freq. inicial, freq. final, duração em s, volume, ciclo da quadrada].
const SONS := {
	"foco": [["quadrada", 1320, 1320, 0.035, 0.30, 0.5]],
	"clique": [["quadrada", 660, 440, 0.06, 0.40, 0.5]],
	"abrir_painel": [["triangulo", 330, 660, 0.12, 0.70, 0.5]],
	"fechar_painel": [["triangulo", 660, 330, 0.12, 0.70, 0.5]],
	"aprovar": [["quadrada", 523, 523, 0.08, 0.35, 0.25], ["quadrada", 659, 659, 0.08, 0.35, 0.25],
		["quadrada", 784, 784, 0.14, 0.35, 0.25]],
	"reprovar": [["serra", 220, 196, 0.12, 0.35, 0.5], ["serra", 165, 147, 0.20, 0.35, 0.5]],
	"nc_encontrada": [["quadrada", 880, 880, 0.07, 0.35, 0.5], ["silencio", 0, 0, 0.03, 0.0, 0.5],
		["quadrada", 880, 880, 0.07, 0.35, 0.5], ["silencio", 0, 0, 0.03, 0.0, 0.5],
		["quadrada", 988, 988, 0.10, 0.35, 0.5]],
	"entrega": [["quadrada", 523, 523, 0.07, 0.30, 0.125], ["quadrada", 659, 659, 0.07, 0.30, 0.125],
		["quadrada", 784, 784, 0.07, 0.30, 0.125], ["quadrada", 1047, 1047, 0.25, 0.30, 0.125]],
	"venda_fechada": [["ruido", 0, 0, 0.02, 0.25, 0.5], ["seno", 1568, 1568, 0.08, 0.55, 0.5],
		["seno", 2093, 2093, 0.20, 0.55, 0.5]],
	"multa": [["ruido", 0, 0, 0.05, 0.35, 0.5], ["serra", 196, 98, 0.35, 0.40, 0.5]],
}

var _semente := 1


func _init() -> void:
	DirAccess.make_dir_recursive_absolute(ProjectSettings.globalize_path(PASTA))
	for nome in SONS:
		_semente = nome.hash()
		var wav := _sintetizar(SONS[nome])
		var erro := wav.save_to_wav(PASTA + nome + ".wav")
		if erro != OK:
			push_error("Não consegui salvar %s (erro %d)" % [nome, erro])
			quit(1)
			return
		print("%-14s %.2f s" % [nome, wav.get_length()])
	quit()


func _sintetizar(trechos: Array) -> AudioStreamWAV:
	var dados := PackedByteArray()
	var fase := 0.0
	for t in trechos:
		var n := int(float(t[3]) * TAXA)
		for i in n:
			var p := float(i) / n
			var freq := lerpf(float(t[1]), float(t[2]), p)
			fase = fmod(fase + freq / TAXA, 1.0)
			var v := _onda(t[0], fase, float(t[5]))
			# envelope: ataque de 4 ms e queda nos últimos 40% do trecho
			var env := minf(1.0, i / (0.004 * TAXA)) * (1.0 if p < 0.6 else (1.0 - p) / 0.4)
			var amostra := clampi(int(v * env * float(t[4]) * 32767.0), -32768, 32767)
			dados.append(amostra & 0xff)
			dados.append((amostra >> 8) & 0xff)
	var wav := AudioStreamWAV.new()
	wav.format = AudioStreamWAV.FORMAT_16_BITS
	wav.mix_rate = TAXA
	wav.stereo = false
	wav.data = dados
	return wav


func _onda(forma: String, fase: float, ciclo: float) -> float:
	match forma:
		"quadrada":
			return 1.0 if fase < ciclo else -1.0
		"serra":
			return fase * 2.0 - 1.0
		"triangulo":
			return 1.0 - 4.0 * absf(fase - 0.5)
		"seno":
			return sin(fase * TAU)
		"ruido":
			_semente = (_semente * 1103515245 + 12345) & 0x7fffffff   # ruído com semente fixa
			return float(_semente) / 0x3fffffff - 1.0
	return 0.0
