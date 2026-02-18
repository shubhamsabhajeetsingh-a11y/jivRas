package com.jivRas.groceries.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map "http://localhost:8080/images/..." to the local folder "./data/images/"
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:./data/images/");
    }
}