package com.antigravity.billing.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter for the /auth/login endpoint.
 * Limits each IP to 10 login attempts per minute to prevent brute-force attacks.
 */
@Slf4j
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    // Per-IP bucket cache — auto-expires after 5 minutes of inactivity
    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build();

    private Bucket getBucketForIp(String ip) {
        return bucketCache.get(ip, key -> {
            Bandwidth limit = Bandwidth.classic(
                    MAX_REQUESTS_PER_MINUTE,
                    Refill.intervally(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
            );
            return Bucket.builder().addLimit(limit).build();
        });
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Only rate-limit login endpoint when enabled
        if (!enabled || !LOGIN_PATH.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Bucket bucket = getBucketForIp(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {} on login endpoint", clientIp);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                    "\"status\":429," +
                    "\"error\":\"TOO_MANY_REQUESTS\"," +
                    "\"message\":\"Too many login attempts. Please wait 1 minute before trying again.\"," +
                    "\"path\":\"" + LOGIN_PATH + "\"}"
            );
        }
    }
}
