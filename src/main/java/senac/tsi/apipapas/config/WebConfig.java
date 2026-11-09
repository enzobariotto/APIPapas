package senac.tsi.apipapas.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.accept.ApiVersionHolder;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import senac.tsi.apipapas.idempotencia.IdempotenciaFilter;
import senac.tsi.apipapas.seguranca.ApiKeyFilter;
import senac.tsi.apipapas.seguranca.RateLimitFilter;

import java.time.Duration;
import java.util.List;

@Configuration
class WebConfig implements WebMvcConfigurer {

    public static final String HEADER_VERSAO = "X-API-Version";
    public static final String VERSAO_PADRAO = "1";

    // ---------------------------------------------------------------- CORS

    /**
     * Política de CORS. Fica num filtro com a primeira posição na cadeia para que também as respostas dos
     * outros filtros (401, 429...) tragam os headers de CORS e possam ser lidas pelo navegador.
     */
    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(@Value("${app.cors.origens-permitidas}") List<String> origens) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origens.stream().map(String::trim).filter(o -> !o.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT,
                ApiKeyFilter.HEADER, IdempotenciaFilter.HEADER, HEADER_VERSAO));
        config.setExposedHeaders(List.of(HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER, HEADER_VERSAO,
                IdempotenciaFilter.HEADER_REPETIDA,
                RateLimitFilter.HEADER_LIMITE, RateLimitFilter.HEADER_RESTANTE, RateLimitFilter.HEADER_RESET));
        // A autenticação é pelo header X-API-Key, sem cookies: não há necessidade de enviar credenciais.
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(new CorsFilter(source));
        registro.setOrder(0);
        return registro;
    }

    // ---------------------------------------------------------- Versionamento

    /**
     * Versionamento pelo header X-API-Version (suporte nativo do Spring Framework 7).
     * Sem o header, vale a versão 1. Versão não suportada retorna 400.
     * Endpoints sem versão declarada atendem a todas as versões.
     */
    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(HEADER_VERSAO)
                .addSupportedVersions("1", "2")
                .setDefaultVersion(VERSAO_PADRAO);
    }

    /** Devolve no header X-API-Version a versão que atendeu a requisição. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                Object versao = request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE);
                if (versao instanceof ApiVersionHolder holder) {
                    versao = holder.getVersionIfPresent();
                }
                if (versao instanceof SemanticApiVersionParser.Version semantica) {
                    response.setHeader(HEADER_VERSAO, String.valueOf(semantica.getMajor()));
                } else if (versao != null) {
                    response.setHeader(HEADER_VERSAO, String.valueOf(versao));
                }
                return true;
            }
        });
    }
}
