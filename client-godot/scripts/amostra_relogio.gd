extends Node
## Relógio da amostra do tema: roda sempre (mesmo no hitstop) e avisa a amostra a cada quadro.


func _process(_delta: float) -> void:
	get_meta("amostra")._quadro_sempre()
