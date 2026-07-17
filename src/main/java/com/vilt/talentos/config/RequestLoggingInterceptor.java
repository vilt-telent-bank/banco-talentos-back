package com.vilt.talentos.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class RequestLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Evita flood no log com rotas do Swagger ou requisições OPTIONS do navegador (CORS)
        if (uri.contains("/v3/api-docs") || uri.contains("/swagger-ui") || method.equals("OPTIONS")) {
            return true;
        }

        String user = "ANÔNIMO";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Verifica se o usuário está autenticado e não é o "anonymousUser" padrão do Spring
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            user = auth.getName();
        }

        log.info("[API ACCESS] Usuário: {} | Método: {} | Rota: {}", user, method, uri);

        // O return true permite que a requisição siga normalmente para o Controller
        return true;
    }
}
