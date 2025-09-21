package com.epam.gymcrm.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Configuration
public class FeignPropagationConfig {

    @Bean
    RequestInterceptor forwardingInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes sra) {
                var req = sra.getRequest();

                String auth = req.getHeader("Authorization");
                if (Objects.nonNull(auth) && !auth.isBlank()) template.header("Authorization", auth);

                String tx = req.getHeader("X-Transaction-Id");
                if (Objects.nonNull(tx) && !tx.isBlank()) template.header("X-Transaction-Id", tx);
            }
        };
    }
}
