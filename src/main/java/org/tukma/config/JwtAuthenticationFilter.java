package org.tukma.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.util.AntPathMatcher;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.RequestContextFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtCompilationUnit compilationUnit;
    private final UserDetailsService userDetailsService;
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/api/v1/auth/**",
            "/api/v1/applicant/**",
            "/api/v1/jobs/get-all-jobs",
            "/api/v1/jobs/get-job-details/**",
            "/api/v1/jobs/job-metadata",
            "/api/v1/jobs/search",
            "/api/v1/resume/cleanup-duplicates",
            "/api/v1/survey/**",
            "/debug/**",
            "/ws/**"
    );
    private final RequestContextFilter requestContextFilter;

    private boolean isExcludedPath(String requestPath) {
        if (requestPath.startsWith("/ws")) {
            return true;  // Completely bypass JWT filter for all WebSocket paths
        }
        if (requestPath.equals("/api/v1/auth/user-status")) return false;
        return EXCLUDED_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtCompilationUnit compilationUnit, UserDetailsService userDetailsService, RequestContextFilter requestContextFilter) {
        this.compilationUnit = compilationUnit;
        this.userDetailsService = userDetailsService;
        this.requestContextFilter = requestContextFilter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isExcludedPath(request.getRequestURI())) {
            System.out.println("Excluded path: " + request.getRequestURI());
            if (request.getRequestURI().equals("/ws/interview")) {
                System.out.println("Ignoring jwt for ws");
            }
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);
        Claims assumedToken = JwtCompilationUnit.resurrect(token);
        if (assumedToken == null) {
            blockRequest(response, "Invalid token.");
            return;
        }

        if (StringUtils.hasText(token)) {
            String username = assumedToken.get("subject").toString();
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("jwt")) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    private void blockRequest(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
        response.getWriter().flush();
    }
}
