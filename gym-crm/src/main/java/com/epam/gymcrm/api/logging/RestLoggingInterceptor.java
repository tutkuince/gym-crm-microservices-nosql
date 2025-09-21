package com.epam.gymcrm.api.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RestLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RestLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tx = (String) request.getAttribute("transactionId");
        logger.info("[txId={}] Incoming request: method={}, uri={}, params={}",
                tx, request.getMethod(), request.getRequestURI(), request.getQueryString());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String tx = (String) request.getAttribute("transactionId");
        logger.info("[txId={}] Completed request: status={}, uri={}", tx, response.getStatus(), request.getRequestURI());
    }
}
