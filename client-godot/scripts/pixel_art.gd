class_name PixelArt
extends RefCounted
## Sprites em pixel art gerados em código (pixel a pixel, paleta Endesga 32) e guardados como textura.
## Visão de cima com leve inclinação, como no mockup da fábrica. 1 tile = 16 px = 2 m.

const LARGURA_CASCO := 26   # boca de ~4 m
const COMPRIMENTO_CASCO := 84  # 33 pés ≈ 5 tiles

enum Etapa { MOLDE, ABERTO, FECHADO, PRONTO }

static var _cache := {}


# ---------------------------------------------------------------- casco visto de cima (proa para cima)

## Meia largura do casco na linha y: proa curva, popa reta.
static func _meia_boca(y: int, comprimento: int, largura: int) -> float:
	var t := float(y) / float(comprimento - 1)
	var meia := largura / 2.0
	if t < 0.4:
		meia *= pow(t / 0.4, 0.55)
	if y >= comprimento - 2:
		meia -= 1.0
	return meia


static func _dentro(x: int, y: int, comprimento: int, largura: int, recuo: float) -> bool:
	if y < 0 or y >= comprimento:
		return false
	return absf(x + 0.5 - largura / 2.0) <= _meia_boca(y, comprimento, largura) - recuo


## Casco na etapa pedida. `progresso` (0 a 1) só conta no molde: o gelcoat vai cobrindo o molde.
static func casco(etapa: Etapa, progresso: float = 1.0) -> ImageTexture:
	var passo := int(clampf(progresso, 0.0, 1.0) * 8) if etapa == Etapa.MOLDE else 0
	var chave := "casco_%d_%d" % [etapa, passo]
	if _cache.has(chave):
		return _cache[chave]
	var tex: ImageTexture
	if etapa == Etapa.MOLDE:
		tex = ImageTexture.create_from_image(_molde(passo / 8.0))
	else:
		tex = ImageTexture.create_from_image(_casco(etapa))
	_cache[chave] = tex
	return tex


static func _casco(etapa: Etapa) -> Image:
	var w := LARGURA_CASCO
	var h := COMPRIMENTO_CASCO
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	var cx := w / 2.0
	for y in h:
		for x in w:
			if not _dentro(x, y, h, w, 0):
				continue
			var borda := not (_dentro(x - 1, y, h, w, 0) and _dentro(x + 1, y, h, w, 0)
					and _dentro(x, y - 1, h, w, 0) and _dentro(x, y + 1, h, w, 0))
			var cor := Paleta.CASCO
			if borda:
				cor = Paleta.PISO_ESCURO
			elif not _dentro(x + 2, y, h, w, 0):
				cor = Paleta.CASCO_SOMBRA  # sombra do lado direito: volume
			img.set_pixel(x, y, cor)

	var interno := func(x: int, y: int) -> bool: return _dentro(x, y, h, w, 3) and y > 3 and y < h - 3
	if etapa == Etapa.ABERTO:
		# barco aberto: fundo, longarinas, anteparas e os dois motores na popa
		for y in h:
			for x in w:
				if not interno.call(x, y):
					continue
				var cor := Paleta.PISO_ESCURO
				if x == int(cx) - 5 or x == int(cx) + 4 or (y % 14 == 0):
					cor = Paleta.ARMAZEM
				img.set_pixel(x, y, cor)
		for lado in [-7, 2]:
			for y in range(h - 18, h - 8):
				for x in range(int(cx) + lado, int(cx) + lado + 5):
					var cor := Paleta.FUNDO if y > h - 17 else Paleta.METAL
					if x == int(cx) + lado or y == h - 9:
						cor = Paleta.FUNDO_ESCURO
					img.set_pixel(x, y, cor)
		return img

	# convés fechado: teca na proa e no cockpit
	var teca := func(y: int) -> Color: return Paleta.TECA if y % 2 == 0 else Paleta.TECA_ESCURA
	for y in h:
		for x in w:
			if not interno.call(x, y):
				continue
			var t := float(y) / h
			if (t > 0.14 and t < 0.34) or t > 0.70:
				img.set_pixel(x, y, teca.call(y))
	# cabine
	var topo := int(h * 0.38)
	var base := int(h * 0.66)
	for y in range(topo, base):
		for x in range(int(cx) - 8, int(cx) + 8):
			var cor := Paleta.CASCO
			if y == topo or y == base - 1 or x == int(cx) - 8 or x == int(cx) + 7:
				cor = Paleta.CASCO_SOMBRA
			img.set_pixel(x, y, cor)
	if etapa == Etapa.PRONTO:
		# para-brisa, teto e radar
		for y in range(topo + 1, topo + 4):
			for x in range(int(cx) - 7, int(cx) + 7):
				img.set_pixel(x, y, Paleta.AGUA_ESCURA if y > topo + 1 else Paleta.AGUA)
		img.set_pixel(int(cx) - 1, topo + 8, Paleta.METAL)
		img.set_pixel(int(cx), topo + 8, Paleta.FUNDO)
		# balaustrada na proa
		for y in range(6, int(h * 0.36)):
			for x in w:
				if _dentro(x, y, h, w, 1) and not _dentro(x, y, h, w, 2):
					img.set_pixel(x, y, Paleta.METAL)
	return img


static func _molde(progresso: float) -> Image:
	var w := LARGURA_CASCO + 8
	var h := COMPRIMENTO_CASCO + 6
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	var hw := LARGURA_CASCO
	var hh := COMPRIMENTO_CASCO
	for y in h:
		for x in w:
			# flange do molde: o casco com 4 px a mais
			if not _dentro(x, y, h, w, 0):
				continue
			var cor := Paleta.PRODUCAO_SUJA
			var borda := not (_dentro(x - 1, y, h, w, 0) and _dentro(x + 1, y, h, w, 0)
					and _dentro(x, y - 1, h, w, 0) and _dentro(x, y + 1, h, w, 0))
			if borda or not _dentro(x, y, h, w, 2):
				cor = Paleta.PRODUCAO_SUJA_ESCURA
			img.set_pixel(x, y, cor)
	# cavidade do casco; o gelcoat branco sobe da popa para a proa conforme a laminação
	var limite := int(hh * (1.0 - progresso))
	for y in hh:
		for x in hw:
			if not _dentro(x, y, hh, hw, 1):
				continue
			var cor := Paleta.MADEIRA_ESCURA
			if y >= limite:
				cor = Paleta.CASCO if (x + y) % 5 != 0 else Paleta.CASCO_SOMBRA
			img.set_pixel(x + 4, y + 3, cor)
	return img


# ---------------------------------------------------------------- bonequinhos

const _PESSOA := [
	[
		"..HHHH..",
		".HHHHHH.",
		".SSSSSS.",
		"..SSSS..",
		".UUUUUU.",
		"SUUUUUUS",
		"SUUUUUUS",
		".UUUUUU.",
		".PP..PP.",
		".PP..PP.",
		".BB..BB.",
	],
	[
		"..HHHH..",
		".HHHHHH.",
		".SSSSSS.",
		"..SSSS..",
		"SUUUUUU.",
		".UUUUUUS",
		".UUUUUUS",
		".UUUUUU.",
		".PP..PP.",
		"..P..PP.",
		"..B..BB.",
	],
]


## Bonequinho 10 x 13 (com contorno). `quadro` 0 ou 1 alterna braços e pernas.
static func pessoa(uniforme: Color, quadro: int, capacete: bool = false) -> ImageTexture:
	var chave := "pessoa_%s_%d_%s" % [uniforme.to_html(), quadro, capacete]
	if _cache.has(chave):
		return _cache[chave]
	var cores := {
		"H": Paleta.ALERTA if capacete else Paleta.CABELO,
		"S": Paleta.PELE,
		"U": uniforme,
		"P": Paleta.ARMAZEM_ESCURO,
		"B": Paleta.FUNDO_ESCURO,
	}
	var tex := ImageTexture.create_from_image(_com_contorno(_de_texto(_PESSOA[quadro % 2], cores)))
	_cache[chave] = tex
	return tex


# ---------------------------------------------------------------- balões (estilo Kairosoft)

const _BALAO := [
	".WWWWWWW.",
	"WWWWWWWWW",
	"WWWWWWWWW",
	"WWWWWWWWW",
	"WWWWWWWWW",
	"WWWWWWWWW",
	".WWWWWWW.",
	"...WW....",
	"...W.....",
]
const _ICONES := {
	"alerta": ["....X....", "....X....", "....X....", ".........", "....X...."],
	"ok": [".......X.", "......X..", "X...XX...", ".X.XX....", "..XX....."],
	"espera": ["..XXXXX..", "..X...X..", "..XXXXX..", "..X...X..", "..XXXXX.."],
	"material": ["XXXXXXXXX", "X...X...X", "XXXXXXXXX", "X...X...X", "XXXXXXXXX"],
}


## Balão 11 x 11 com ícone: "alerta" (NC), "ok" (aprovado), "espera" (inspeção), "material" (parado).
static func balao(icone: String) -> ImageTexture:
	var chave := "balao_" + icone
	if _cache.has(chave):
		return _cache[chave]
	var img := _de_texto(_BALAO, {"W": Paleta.CASCO})
	var cor_icone: Color = {"alerta": Paleta.CRITICO, "ok": Paleta.MONTAGEM_LIMPA_ESCURA, "espera": Paleta.AGUA_ESCURA,
		"material": Paleta.PRODUCAO_SUJA_ESCURA}[icone]
	var linhas: Array = _ICONES[icone]
	for y in linhas.size():
		for x in str(linhas[y]).length():
			if str(linhas[y])[x] == "X":
				img.set_pixel(x, y + 1, cor_icone)
	var tex := ImageTexture.create_from_image(_com_contorno(img))
	_cache[chave] = tex
	return tex


# ---------------------------------------------------------------- veículos

const _EMPILHADEIRA := [
	"MM..........",
	"MM.YYYYYY...",
	"MM.YYYYYYY..",
	"MMMYGGGYYY..",
	"MM.YGGGYYYY.",
	"MM.YYYYYYYY.",
	"MM.YYYYYYYY.",
	"...KK...KK..",
]


static func empilhadeira() -> ImageTexture:
	if not _cache.has("empilhadeira"):
		_cache["empilhadeira"] = ImageTexture.create_from_image(_com_contorno(_de_texto(_EMPILHADEIRA,
				{"M": Paleta.METAL, "Y": Paleta.ALERTA, "G": Paleta.AGUA_ESCURA, "K": Paleta.FUNDO_ESCURO})))
	return _cache["empilhadeira"]


const _CAVALO := [
	"..CCCCCCCC....",
	".CCCCCCCCCC...",
	".CGGGCCCCCCC..",
	".CGGGCCCCCCCC.",
	".CCCCCCCCCCCC.",
	".CCCCCCCCCCCC.",
	".CGGGCCCCCCCC.",
	".CGGGCCCCCCC..",
	".CCCCCCCCCC...",
	"..CCCCCCCC....",
]


## Cavalo mecânico da carreta, visto de cima, de frente para a direita.
static func caminhao() -> ImageTexture:
	if not _cache.has("caminhao"):
		_cache["caminhao"] = ImageTexture.create_from_image(_com_contorno(_de_texto(_CAVALO,
				{"C": Paleta.AGUA_ESCURA, "G": Paleta.AGUA})))
	return _cache["caminhao"]


const _CAIXA := [
	"TTTTTTT",
	"TDDDDDT",
	"TTTTTTT",
	"TDDDDDT",
	"TTTTTTT",
]


## Caixa de kit no pátio; a cor indica o item.
static func caixa(cor: Color) -> ImageTexture:
	var chave := "caixa_" + cor.to_html()
	if not _cache.has(chave):
		_cache[chave] = ImageTexture.create_from_image(_com_contorno(_de_texto(_CAIXA, {"T": cor, "D": cor.darkened(0.3)})))
	return _cache[chave]


# ---------------------------------------------------------------- utilitários

static func _de_texto(linhas: Array, cores: Dictionary) -> Image:
	var h := linhas.size()
	var w := 0
	for l in linhas:
		w = maxi(w, str(l).length())
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	for y in h:
		var l := str(linhas[y])
		for x in l.length():
			if cores.has(l[x]):
				img.set_pixel(x, y, cores[l[x]])
	return img


## Contorno escuro de 1 px em volta do sprite: legível em piso claro e escuro.
static func _com_contorno(origem: Image) -> Image:
	var w := origem.get_width() + 2
	var h := origem.get_height() + 2
	var img := Image.create(w, h, false, Image.FORMAT_RGBA8)
	for y in origem.get_height():
		for x in origem.get_width():
			if origem.get_pixel(x, y).a == 0:
				continue
			for d in [Vector2i(-1, 0), Vector2i(1, 0), Vector2i(0, -1), Vector2i(0, 1)]:
				var px: int = x + 1 + d.x
				var py: int = y + 1 + d.y
				if img.get_pixel(px, py).a == 0:
					img.set_pixel(px, py, Paleta.FUNDO_ESCURO)
	for y in origem.get_height():
		for x in origem.get_width():
			var c := origem.get_pixel(x, y)
			if c.a > 0:
				img.set_pixel(x + 1, y + 1, c)
	return img
