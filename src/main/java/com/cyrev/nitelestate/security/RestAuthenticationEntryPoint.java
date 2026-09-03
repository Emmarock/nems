package com.cyrev.nitelestate.security;

import com.cyrev.nitelestate.common.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Without this, Spring Security falls back to {@code Http403ForbiddenEntryPoint} for any request
 * with no (or an expired/invalid) JWT, since this app never configures httpBasic/formLogin —
 * meaning "you're not logged in" and "you're logged in but lack permission" were
 * indistinguishable to callers (both 403). This restores correct REST semantics: missing/invalid
 * credentials -> 401, authenticated-but-insufficient-role -> 403 (still handled by
 * GlobalExceptionHandler's AccessDeniedException mapping, untouched by this class).
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiError body = ApiError.of(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Authentication required - please log in", request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
