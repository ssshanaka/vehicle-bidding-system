package com.sliit.vehiclebiddingsystem.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web Configuration to register global interceptors
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private GlobalModelAttributeHandler globalModelAttributeHandler;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(globalModelAttributeHandler)
                .addPathPatterns("/**") // Apply to all paths
                .excludePathPatterns(
                    "/static/**", // Exclude static resources
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/error" // Exclude error pages
                );
    }
}
