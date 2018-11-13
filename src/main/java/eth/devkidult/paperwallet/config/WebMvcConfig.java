package eth.devkidult.paperwallet.config;

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
                .excludePathPatterns("/")
                .excludePathPatterns("/signIn")
                .excludePathPatterns("/getTokenImage/**")
                .excludePathPatterns("/findPassword")
                .excludePathPatterns("/verifyEmail")
                .excludePathPatterns("/signUp")
                .excludePathPatterns("/checkEmail");
    }

}
