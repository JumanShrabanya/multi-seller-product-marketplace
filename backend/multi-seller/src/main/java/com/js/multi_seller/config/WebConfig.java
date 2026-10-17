package com.js.multi_seller.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SellerContextResolver sellerContextResolver;

    public WebConfig(SellerContextResolver sellerContextResolver) {
        this.sellerContextResolver = sellerContextResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(sellerContextResolver);
    }
}
