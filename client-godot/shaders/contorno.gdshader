// Contorno escuro das peças (casca invertida): desenha as faces de trás um pouco infladas, numa cor só.
// Fica como next_pass do material toon; a espessura é em metros e é ajustada pela distância da câmera.
shader_type spatial;
render_mode unshaded, cull_front, specular_disabled;

uniform vec4 cor : source_color = vec4(0.094, 0.078, 0.145, 1.0);  // #181425
uniform float espessura = 0.02;
// Cortes da inspeção (parâmetros globais, em coordenadas do mundo): some o que fica acima de inspecao_corte_y
// e o que fica a boreste de inspecao_corte_z. Chão, berço e água desligam com cortavel = false.
global uniform float inspecao_corte_y;
global uniform float inspecao_corte_z;
uniform bool cortavel = true;
varying vec3 mundo;

void vertex() {
	VERTEX += NORMAL * espessura;
	mundo = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
	if (cortavel && (mundo.y > inspecao_corte_y || mundo.z > inspecao_corte_z)) {
		discard;
	}
	ALBEDO = cor.rgb;
}
