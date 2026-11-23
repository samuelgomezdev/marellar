// Vidros e peças transparentes da inspeção 3D: mesma luz em degraus, com transparência e sem contorno.
shader_type spatial;
render_mode blend_mix, cull_disabled, depth_draw_never, specular_disabled;

uniform vec4 cor : source_color = vec4(0.6, 0.8, 1.0, 0.4);
uniform float bandas = 3.0;
uniform vec3 destaque : source_color = vec3(0.0);
// Cortes da inspeção (parâmetros globais, em coordenadas do mundo): some o que fica acima de inspecao_corte_y
// e o que fica a boreste de inspecao_corte_z. Chão, berço e água desligam com cortavel = false.
global uniform float inspecao_corte_y;
global uniform float inspecao_corte_z;
uniform bool cortavel = true;
varying vec3 mundo;

void vertex() {
	mundo = (MODEL_MATRIX * vec4(VERTEX, 1.0)).xyz;
}

void fragment() {
	if (cortavel && (mundo.y > inspecao_corte_y || mundo.z > inspecao_corte_z)) {
		discard;
	}
	ALBEDO = cor.rgb;
	ALPHA = cor.a;
	EMISSION = destaque;
}

void light() {
	float n = clamp(dot(NORMAL, LIGHT), 0.0, 1.0) * ATTENUATION;
	float degrau = floor(n * bandas + 0.5) / bandas;
	// o Godot multiplica pelo ALBEDO depois: aqui entra só a luz
	DIFFUSE_LIGHT += LIGHT_COLOR / PI * mix(0.5, 1.0, degrau);
}
