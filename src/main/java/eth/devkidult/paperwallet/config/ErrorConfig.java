package com.sweden.webwallet.config;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class ErrorConfig extends ServerProperties {

    @Override
    public void customize(ConfigurableEmbeddedServletContainer configurableEmbeddedServletContainer){
        super.customize(configurableEmbeddedServletContainer);
        configurableEmbeddedServletContainer.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/404error"));

    }
}
