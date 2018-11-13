package com.sweden.webwallet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        super.addInterceptors(registry);
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/admin/**")
                .excludePathPatterns("/admin/chart/**")
                .excludePathPatterns("/")
                .excludePathPatterns("/signIn")
                .excludePathPatterns("/getTokenImage/**")
                .excludePathPatterns("/findPassword")
                .excludePathPatterns("/verifyEmail")
                .excludePathPatterns("/signUp")
                .excludePathPatterns("/checkEmail")
                .excludePathPatterns("/checkAdminIdx");
        registry.addInterceptor(new AdminLoginCheckInterceptor())
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin")
                .excludePathPatterns("/admin/signIn");
    }

}
