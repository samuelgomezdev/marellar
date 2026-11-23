// Pós-processamento da inspeção 3D: cada pixel da imagem em baixa resolução vai para a cor mais próxima
// da Endesga 32 (comparação em Oklab), com dithering ordenado Bayer 4x4. A paleta vem de Paleta.ENDESGA_32.
shader_type canvas_item;

uniform vec3 paleta[32];
uniform float dithering : hint_range(0.0, 1.0) = 0.35;

const float BAYER[16] = float[16](0.0, 8.0, 2.0, 10.0, 12.0, 4.0, 14.0, 6.0, 3.0, 11.0, 1.0, 9.0, 15.0, 7.0, 13.0, 5.0);

vec3 linear(vec3 c) {
	return mix(c / 12.92, pow((c + 0.055) / 1.055, vec3(2.4)), step(0.04045, c));
}

vec3 oklab(vec3 c) {
	vec3 l = linear(c);
	vec3 lms = vec3(
		0.4122214708 * l.r + 0.5363325363 * l.g + 0.0514459929 * l.b,
		0.2119034982 * l.r + 0.6806995451 * l.g + 0.1073969566 * l.b,
		0.0883024619 * l.r + 0.2817188376 * l.g + 0.6299787005 * l.b);
	lms = pow(max(lms, vec3(0.0)), vec3(1.0 / 3.0));
	return vec3(
		0.2104542553 * lms.x + 0.7936177850 * lms.y - 0.0040720468 * lms.z,
		1.9779984951 * lms.x - 2.4285922050 * lms.y + 0.4505937099 * lms.z,
		0.0259040371 * lms.x + 0.7827717662 * lms.y - 0.8086757660 * lms.z);
}

void fragment() {
	vec2 tamanho = 1.0 / TEXTURE_PIXEL_SIZE;
	vec2 pixel = floor(UV * tamanho);
	vec3 c = texture(TEXTURE, (pixel + 0.5) * TEXTURE_PIXEL_SIZE).rgb;
	int i = int(mod(pixel.x, 4.0)) + int(mod(pixel.y, 4.0)) * 4;
	c = clamp(c + (BAYER[i] / 16.0 - 0.47) * dithering * 0.12, 0.0, 1.0);
	vec3 alvo = oklab(c);
	float melhor = 1e9;
	vec3 escolhida = paleta[0];
	for (int k = 0; k < 32; k++) {
		vec3 d = oklab(paleta[k]) - alvo;
		float dist = dot(d, d);
		if (dist < melhor) {
			melhor = dist;
			escolhida = paleta[k];
		}
	}
	COLOR = vec4(escolhida, 1.0);
}
