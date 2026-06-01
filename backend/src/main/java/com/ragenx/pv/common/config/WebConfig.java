package com.ragenx.pv.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Permissive CORS for local development so the React dev server (a different origin, e.g.
 * http://localhost:5173) can call this API directly from the browser. No auth/credentials are
 * involved, so allowing any origin is fine for this exercise. {@code X-Trace-Id} is exposed so the
 * frontend can read the trace id from responses.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id");
    }
}
