class_name Sprites
extends RefCounted
## Ícones desenhados direto no CanvasItem (os sprites com textura ficam em PixelArt).


## Selo vermelho de NC com a quantidade (forma além da cor, para acessibilidade).
static func selo_nc(c: CanvasItem, centro: Vector2, fonte: Font, quantidade: int, aceso: bool) -> void:
	var cor := Paleta.CRITICO if aceso else Paleta.NAO_CONFORME_ESCURO
	var o := centro.floor()
	c.draw_colored_polygon(PackedVector2Array([o + Vector2(0, -7), o + Vector2(7, 5), o + Vector2(-7, 5)]), cor)
	c.draw_string(fonte, o + Vector2(-2, 4), "!", HORIZONTAL_ALIGNMENT_LEFT, -1, 16, Paleta.TEXTO_CLARO)
	if quantidade > 1:
		c.draw_string(fonte, o + Vector2(8, 4), "x%d" % quantidade, HORIZONTAL_ALIGNMENT_LEFT, -1, 16, cor)
