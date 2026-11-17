package br.marellar.api.inspecao;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** /inspecao e /inspecao/ abrem a pagina 3D (static/inspecao/index.html). */
@Configuration
public class InspecaoPagina implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/inspecao", "/inspecao/index.html");
        registry.addRedirectViewController("/inspecao/", "/inspecao/index.html");
    }
}
