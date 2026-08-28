package com.vilt.talentos.security;

import com.vilt.talentos.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        log.debug("JWT Filter — {} {} | Authorization header: {}", request.getMethod(), request.getRequestURI(),
                header != null ? header.substring(0, Math.min(30, header.length())) + "..." : "null");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                Claims claims = jwtService.parse(token);
                if (isTokenVersionCurrent(claims)) {
                    String role = claims.get("role", String.class);
                    log.debug("Claims — sub={} role={}", claims.getSubject(), role);
                    var auth = new UsernamePasswordAuthenticationToken(
                            claims.getSubject(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    log.debug("Authentication set: {}", auth.getAuthorities());
                }
            }
        }
        chain.doFilter(request, response);
    }

    private boolean isTokenVersionCurrent(Claims claims) {
        try {
            UUID userId = UUID.fromString(claims.getSubject());
            Number claimVersion = claims.get("tokenVersion", Number.class);
            int tokenVersion = claimVersion != null ? claimVersion.intValue() : 0;

            return userRepository.findById(userId)
                    .map(user -> {
                        int currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
                        if (tokenVersion != currentVersion) {
                            log.info("JWT rejeitado: tokenVersion desatualizado para usuário {} (token={}, atual={}).",
                                    userId, tokenVersion, currentVersion);
                            return false;
                        }
                        return true;
                    })
                    .orElse(false);
        } catch (IllegalArgumentException e) {
            log.warn("JWT rejeitado: subject inválido.");
            return false;
        }
    }
}
