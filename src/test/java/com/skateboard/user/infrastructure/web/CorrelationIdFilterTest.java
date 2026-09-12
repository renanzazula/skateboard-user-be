package com.skateboard.user.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new CorrelationIdFilter();
        MDC.remove(CorrelationIdFilter.MDC_KEY);
    }

    @Test
    void reusesIncomingCorrelationHeaderAndClearsMdcAfterward() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("incoming-correlation-id");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(CorrelationIdFilter.HEADER, "incoming-correlation-id");
        verify(chain).doFilter(request, response);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesNewCorrelationIdWhenHeaderIsMissing() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(CorrelationIdFilter.HEADER), headerValue.capture());
        assertThat(headerValue.getValue()).isNotBlank();
        assertThat(headerValue.getValue()).hasSize(36); // UUID string length
        verify(chain).doFilter(request, response);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void generatesNewCorrelationIdWhenHeaderIsBlank() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("   ");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<String> headerValue = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq(CorrelationIdFilter.HEADER), headerValue.capture());
        assertThat(headerValue.getValue()).isNotBlank();
        assertThat(headerValue.getValue()).isNotEqualTo("   ");
    }

    @Test
    void propagatesDownstreamExceptionButStillClearsMdc() throws ServletException, IOException {
        when(request.getHeader(CorrelationIdFilter.HEADER)).thenReturn("incoming-correlation-id");
        FilterChain chain = mock(FilterChain.class);
        doThrow(new ServletException("boom")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("boom");

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
