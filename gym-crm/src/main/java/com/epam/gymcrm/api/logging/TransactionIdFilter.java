package com.epam.gymcrm.api.logging;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransactionIdFilter implements Filter {

    private static final String MDC_KEY = "transactionId";
    private static final String HDR_KEY = "X-Transaction-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String txId = Optional.ofNullable(request.getHeader(HDR_KEY))
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        try {
            MDC.put(MDC_KEY, txId);
            request.setAttribute(MDC_KEY, txId);

            response.setHeader(HDR_KEY, txId);

            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
