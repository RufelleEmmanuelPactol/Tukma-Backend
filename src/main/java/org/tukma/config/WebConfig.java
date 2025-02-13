package org.tukma.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Apply to all endpoints
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:5500")// Allow specific origin (change to your frontend's URL)
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Allowed methods
                .allowedHeaders("*")  // Allowed headers
                .allowCredentials(true);
    }


}
