package com.nexa.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite le nombre de tentatives de connexion/inscription par adresse IP,
 * pour se protéger contre le brute-force et le credential stuffing.
 * 10 tentatives / minute / IP sur /api/auth/** ; au-delà : HTTP 429.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/auth/")) {
            String ip = clientIp(request);
            Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Trop de tentatives. Réessayez dans une minute.\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}
