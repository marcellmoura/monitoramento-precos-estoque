package br.com.marcellmoura.monitoramento.acesso.security;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;

public final class ValidacaoSessaoFilter extends OncePerRequestFilter {
    private static final Set<String> ROTAS_PRIMEIRO_ACESSO = Set.of(
            "/api/auth/csrf", "/api/auth/me", "/api/auth/senha", "/api/auth/logout");
    private final UsuarioRepository usuarios;
    public ValidacaoSessaoFilter(UsuarioRepository usuarios) { this.usuarios = usuarios; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado principal) {
            var usuario = usuarios.findById(principal.getId()).orElse(null);
            if (usuario == null || !usuario.isAtivo()
                    || usuario.getVersaoCredencial() != principal.getVersaoCredencial()) {
                var session = request.getSession(false);
                if (session != null) { session.invalidate(); }
                SecurityContextHolder.clearContext();
                ErroApi.escrever(response, 401, "SESSAO_INVALIDA", "Entre novamente para continuar.");
                return;
            }
            if (usuario.isTrocaSenhaObrigatoria() && !ROTAS_PRIMEIRO_ACESSO.contains(request.getRequestURI().substring(request.getContextPath().length()))) {
                ErroApi.escrever(response, 403, "TROCA_SENHA_OBRIGATORIA", "Troque a senha para continuar.");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
