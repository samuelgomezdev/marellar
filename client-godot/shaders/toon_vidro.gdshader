// Vidros e peças transparentes da inspeção 3D: mesma luz em degraus, com transparência e sem contorno.
shader_type spatial;
render_mode blend_mix, cull_disabled, depth_draw_never, specular_disabled;

uniform vec4 cor : source_color = vec4(0.6, 0.8, 1.0, 0.4);
uniform float bandas = 3.0;
uniform vec3 destaque : source_color = vec3(0.0);

void fragment() {
	ALBEDO = cor.rgb;
	ALPHA = cor.a;
	EMISSION = destaque;
}

void light() {
	float n = clamp(dot(NORMAL, LIGHT), 0.0, 1.0) * ATTENUATION;
	float degrau = floor(n * bandas + 0.5) / bandas;
	DIFFUSE_LIGHT += ALBEDO * LIGHT_COLOR / PI * mix(0.5, 1.0, degrau);
}
