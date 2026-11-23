// Luz em degraus para a inspeção 3D em pixel art: a cor da peça (vinda do .glb) com 3 faixas de luz.
// A redução final para a Endesga 32 acontece depois, no shader da tela (paleta.gdshader).
shader_type spatial;
render_mode cull_disabled, specular_disabled;

uniform vec4 cor : source_color = vec4(1.0);
uniform sampler2D textura : source_color, filter_nearest, repeat_enable;
uniform bool usar_textura = false;
uniform vec3 emissao : source_color = vec3(0.0);
uniform float bandas = 3.0;
uniform float sombra_minima = 0.45;   // quanto de cor sobra no lado sem luz
uniform vec3 destaque : source_color = vec3(0.0);  // brilho de seleção (passo 4)
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
	vec4 c = cor;
	if (usar_textura) {
		c *= texture(textura, UV);
	}
	ALBEDO = c.rgb;
	EMISSION = emissao + destaque;
}

void light() {
	float n = clamp(dot(NORMAL, LIGHT), 0.0, 1.0) * ATTENUATION;
	float degrau = floor(n * bandas + 0.5) / bandas;
	// o Godot multiplica pelo ALBEDO depois: aqui entra só a luz
	DIFFUSE_LIGHT += LIGHT_COLOR / PI * mix(sombra_minima, 1.0, degrau);
}
