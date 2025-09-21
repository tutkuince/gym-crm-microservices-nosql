package com.epam.gymcrm.api.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionIdFilterTest {

    @Test
    void testDoFilter_addsTransactionIdToMDC_whenHeaderMissing() throws Exception {
        TransactionIdFilter filter = new TransactionIdFilter();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (r, s) -> {
            assertThat(MDC.get("transactionId")).isNotBlank();
        };

        filter.doFilter(req, res, chain);

        String tx = res.getHeader("X-Transaction-Id");
        assertThat(tx).isNotBlank();

        assertThat(MDC.get("transactionId")).isNull();
    }

    @Test
    void testDoFilter_keepsIncomingHeader() throws Exception {
        TransactionIdFilter filter = new TransactionIdFilter();

        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        req.addHeader("X-Transaction-Id", "abc-123");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (r, s) -> assertThat(MDC.get("transactionId")).isEqualTo("abc-123");

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader("X-Transaction-Id")).isEqualTo("abc-123");
        assertThat(MDC.get("transactionId")).isNull();
    }
}