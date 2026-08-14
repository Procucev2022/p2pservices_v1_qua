package com.portal.procucev.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.ContentCachingRequestWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestBodyCacheFilterTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    @Test
    void testDoFilterInternal() throws Exception {
        RequestBodyCacheFilter filter = new RequestBodyCacheFilter();

        filter.doFilterInternal(request, response, chain);
        verify(chain, times(1)).doFilter(any(ContentCachingRequestWrapper.class), eq(response));
    }
}
