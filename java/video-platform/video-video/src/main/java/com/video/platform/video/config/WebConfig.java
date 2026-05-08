package com.video.platform.video.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 🔥 Windows系统必须用 file:/// 开头，否则映射失效
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:///C:/test/covers/");
        registry.addResourceHandler("/videos/**")
                .addResourceLocations("file:///C:/test/videos/");
    }
}