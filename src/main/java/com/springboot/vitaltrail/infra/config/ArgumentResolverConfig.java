package com.springboot.vitaltrail.infra.config;

import net.kaczmarzyk.spring.data.jpa.web.SpecificationArgumentResolver;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


import java.util.List;

@SpringBootConfiguration
@EnableAutoConfiguration
// @Configuration
// @EnableJpaRepositories
public class ArgumentResolverConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new SpecificationArgumentResolver());
        // resolvers.add(new PageableHandlerMethodArgumentResolver());
    }
}
