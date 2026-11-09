extends Node
## Cliente HTTP da marellar-api (autoload "Api").
## Nenhuma regra de jogo aqui: só pede o estado ao motor e envia as ordens do jogador.

signal erro(mensagem: String)

const BASE_URL := "http://127.0.0.1:8080/api/v1"

var partida_id: String = ""


func criar_partida(semente: int) -> bool:
	var resposta = await _requisitar(HTTPClient.METHOD_POST, "/partidas", {"semente": semente})
	if resposta == null:
		return false
	partida_id = resposta["id"]
	return true


func estado() -> Variant:
	return await _requisitar(HTTPClient.METHOD_GET, "/partidas/%s" % partida_id)


func avancar(ticks: int) -> Variant:
	return await _requisitar(HTTPClient.METHOD_POST, "/partidas/%s/avancar" % partida_id, {"ticks": ticks})


func ordem(dados: Dictionary) -> Variant:
	return await _requisitar(HTTPClient.METHOD_POST, "/partidas/%s/ordens" % partida_id, dados)


func eventos(desde: int) -> Variant:
	return await _requisitar(HTTPClient.METHOD_GET, "/partidas/%s/eventos?desde=%d" % [partida_id, desde])


func salvar() -> Variant:
	return await _requisitar(HTTPClient.METHOD_POST, "/partidas/%s/salvar" % partida_id)


## Saves gravados, do mais recente para o mais antigo.
func saves() -> Variant:
	return await _requisitar(HTTPClient.METHOD_GET, "/partidas/saves")


func carregar(id: String) -> bool:
	var resposta = await _requisitar(HTTPClient.METHOD_POST, "/partidas/carregar", {"id": id})
	if resposta == null:
		return false
	partida_id = resposta["id"]
	return true


func _requisitar(metodo: int, caminho: String, corpo = null) -> Variant:
	if OS.has_feature("web"):
		return await _requisitar_no_navegador(metodo, caminho, corpo)
	var http := HTTPRequest.new()
	add_child(http)
	var cabecalhos := PackedStringArray(["Content-Type: application/json", "Accept: application/json"])
	var dados := "" if corpo == null else JSON.stringify(corpo)
	var falha := http.request(BASE_URL + caminho, cabecalhos, metodo, dados)
	if falha != OK:
		http.queue_free()
		erro.emit("Não foi possível chamar a API (%s)." % error_string(falha))
		return null

	var resposta: Array = await http.request_completed
	http.queue_free()
	var resultado: int = resposta[0]
	var codigo: int = resposta[1]
	var texto: String = (resposta[3] as PackedByteArray).get_string_from_utf8()

	if resultado != HTTPRequest.RESULT_SUCCESS:
		erro.emit("API fora do ar. Rode na raiz do repositório: mvnw -pl marellar-api spring-boot:run")
		return null
	return _tratar_resposta(codigo, texto)


## No navegador (GitHub Pages) não há servidor: o motor Java roda compilado para JavaScript
## (marellar-web, TeaVM) na mesma página, com as mesmas rotas e respostas da API.
func _requisitar_no_navegador(metodo: int, caminho: String, corpo) -> Variant:
	var janela = JavaScriptBridge.get_interface("window")
	# O motor carrega os dados de data/ em paralelo com o Godot; espera ficar pronto.
	while janela == null or str(janela.marellarStatus) == "carregando":
		await get_tree().process_frame
		janela = JavaScriptBridge.get_interface("window")
	var situacao := str(janela.marellarStatus)
	if situacao != "pronto":
		erro.emit("O motor do jogo não carregou no navegador (%s)." % situacao)
		return null
	var nome_metodo := "POST" if metodo == HTTPClient.METHOD_POST else "GET"
	var dados := "" if corpo == null else JSON.stringify(corpo)
	var resposta = JSON.parse_string(str(janela.marellarRequisitar(nome_metodo, "/api/v1" + caminho, dados)))
	if not resposta is Dictionary:
		erro.emit("Resposta inválida do motor no navegador.")
		return null
	return _tratar_resposta(int(resposta["status"]), str(resposta["corpo"]))


func _tratar_resposta(codigo: int, texto: String) -> Variant:
	var json = JSON.parse_string(texto) if not texto.is_empty() else {}
	if codigo >= 400:
		var detalhe = json.get("detail", texto) if json is Dictionary else texto
		erro.emit("Erro %d: %s" % [codigo, detalhe])
		return null
	return json
