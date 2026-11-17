extends SceneTree
## Confere o barco exportado: godot --headless --path client-godot --script res://ferramentas/conferir_barco.gd
## Imprime quantos itens chegaram com os dados (extras do glTF viram metadados) e o tamanho do casco.

func _initialize() -> void:
	var cena: PackedScene = load("res://assets/barcos/marellar-33.glb")
	var barco := cena.instantiate()
	var itens := 0
	var exemplo := {}
	for no in barco.find_children("*", "Node3D", true, false):
		var extras = no.get_meta("extras") if no.has_meta("extras") else null
		if extras is Dictionary and extras.has("codigo"):
			itens += 1
			if exemplo.is_empty():
				exemplo = extras
	var caixa := AABB()
	for m in barco.find_children("*", "MeshInstance3D", true, false):
		var mi := m as MeshInstance3D
		var caixa_mi := _transformacao(mi, barco) * mi.get_aabb()
		caixa = caixa.merge(caixa_mi) if caixa.size != Vector3.ZERO else caixa_mi
	print("itens com dados: %d" % itens)
	print("exemplo: %s" % JSON.stringify(exemplo))
	print("caixa: %s" % caixa)
	barco.free()
	quit()


## Transformação do nó em relação à raiz do barco (sem precisar estar na árvore).
func _transformacao(no: Node3D, raiz: Node) -> Transform3D:
	var t := no.transform
	var pai := no.get_parent()
	while pai != null and pai != raiz:
		t = (pai as Node3D).transform * t
		pai = pai.get_parent()
	return t
