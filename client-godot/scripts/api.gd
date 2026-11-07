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


func _requisitar(metodo: int, caminho: String, corpo = null) -> Variant:
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
	var json = JSON.parse_string(texto) if not texto.is_empty() else {}
	if codigo >= 400:
		var detalhe = json.get("detail", texto) if json is Dictionary else texto
		erro.emit("Erro %d: %s" % [codigo, detalhe])
		return null
	return json
