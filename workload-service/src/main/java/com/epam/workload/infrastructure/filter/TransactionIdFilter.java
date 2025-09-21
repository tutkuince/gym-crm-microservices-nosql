package com.epam.workload.infrastructure.filter;

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

    public static final String MDC_KEY = "transactionId";
    public static final String HDR_KEY = "X-Transaction-Id";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request  = (req instanceof HttpServletRequest r) ? r : null;
        HttpServletResponse response = (res instanceof HttpServletResponse r) ? r : null;

        String txId = Optional.ofNullable(request != null ? request.getHeader(HDR_KEY) : null)
                .filter(s -> !s.isBlank())
                .orElse(UUID.randomUUID().toString());

        try {
            MDC.put(MDC_KEY, txId);
            if (request != null)  { request.setAttribute(MDC_KEY, txId); }
            if (response != null) { response.setHeader(HDR_KEY, txId); }
            chain.doFilter(req, res);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
