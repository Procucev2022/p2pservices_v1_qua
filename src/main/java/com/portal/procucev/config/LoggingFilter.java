package com.portal.procucev.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Traces the method and path of inbound requests.
 *
 * <p>Previously this wrote to {@code System.out} on every request. That is a synchronized,
 * unbuffered sink, so concurrent request threads contended on the console lock before any
 * application code ran. It now logs through SLF4J at DEBUG, which is a no-op in deployed
 * environments unless explicitly enabled.
 *
 * <p>Extends {@link OncePerRequestFilter} so FORWARD and ERROR dispatches are not logged twice.
 */
@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Parameterised call: SLF4J skips the formatting entirely when DEBUG is off, so no
        // isDebugEnabled() guard is needed.
        log.debug("Incoming request: {} {}", request.getMethod(), request.getRequestURI());
        chain.doFilter(request, response);
    }
}
