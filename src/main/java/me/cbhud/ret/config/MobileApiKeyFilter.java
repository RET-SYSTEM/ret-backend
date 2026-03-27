package me.cbhud.ret.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class MobileApiKeyFilter extends OncePerRequestFilter {

    private final String mobileApiKey;

    public MobileApiKeyFilter(@Value("${mobile.api-key:dev-mobile-key}") String mobileApiKey) {
        this.mobileApiKey = mobileApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Allow CORS preflight requests
        if (HttpMethod.OPTIONS.name().equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only protect API endpoints
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestApiKey = request.getHeader("x-mobile-api-key");

        if (requestApiKey == null || !requestApiKey.equals(mobileApiKey)) {
            log.warn("Blocked request to {} due to missing or invalid x-mobile-api-key", request.getRequestURI());
            
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid Mobile Target API Key\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
