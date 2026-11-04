package dev.lslm.library.support;

import dev.lslm.library.repository.BibliotecaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Le X-Biblioteca-Id, X-Usuario-Id e X-Request-Id, valida o tenant e monta o RequestContext.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final BibliotecaRepository bibliotecaRepository;

    public TenantContextFilter(BibliotecaRepository bibliotecaRepository) {
        this.bibliotecaRepository = bibliotecaRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        response.setHeader("X-Request-Id", requestId);

        UUID bibliotecaId;
        UUID usuarioId;
        try {
            bibliotecaId = parse(request.getHeader("X-Biblioteca-Id"));
            usuarioId = parse(request.getHeader("X-Usuario-Id"));
        } catch (IllegalArgumentException e) {
            erro(response, 400, "CABECALHO_INVALIDO", "X-Biblioteca-Id e X-Usuario-Id devem ser UUIDs válidos.");
            return;
        }

        if (bibliotecaId != null) {
            var biblioteca = bibliotecaRepository.findById(bibliotecaId).orElse(null);
            if (biblioteca == null) {
                erro(response, 404, "BIBLIOTECA_NAO_ENCONTRADA", "Biblioteca não encontrada.");
                return;
            }
            if (!biblioteca.isAtiva()) {
                erro(response, 403, "BIBLIOTECA_INATIVA", "Biblioteca desativada.");
                return;
            }
        }

        RequestContext.set(new RequestContext.Data(bibliotecaId, usuarioId, request.getRemoteAddr(), requestId));
        MDC.put("requestId", requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
            MDC.remove("requestId");
        }
    }

    private static UUID parse(String value) {
        return value == null || value.isBlank() ? null : UUID.fromString(value.trim());
    }

    private static void erro(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
