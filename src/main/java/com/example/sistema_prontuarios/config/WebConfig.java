package com.example.sistema_prontuarios.config;

import com.example.sistema_prontuarios.audit.AuditoriaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuditoriaInterceptor auditoria;

    public WebConfig(AuditoriaInterceptor auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(auditoria).addPathPatterns("/api/**");
    }
}
