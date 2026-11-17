// Contorno escuro das peças (casca invertida): desenha as faces de trás um pouco infladas, numa cor só.
// Fica como next_pass do material toon; a espessura é em metros e é ajustada pela distância da câmera.
shader_type spatial;
render_mode unshaded, cull_front, specular_disabled;

uniform vec4 cor : source_color = vec4(0.094, 0.078, 0.145, 1.0);  // #181425
uniform float espessura = 0.02;

void vertex() {
	VERTEX += NORMAL * espessura;
}

void fragment() {
	ALBEDO = cor.rgb;
}
