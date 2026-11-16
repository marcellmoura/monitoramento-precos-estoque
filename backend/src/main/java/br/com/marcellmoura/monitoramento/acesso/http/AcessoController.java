package br.com.marcellmoura.monitoramento.acesso.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import br.com.marcellmoura.monitoramento.acesso.application.SenhaService;
import br.com.marcellmoura.monitoramento.acesso.security.UsuarioAutenticado;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequestMapping("/api/auth")
public class AcessoController {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final HttpSessionSecurityContextRepository contexts;
    private final UsuarioRepository usuarios;
    private final SenhaService senhas;

    public AcessoController(AuthenticationManager authenticationManager, SessionAuthenticationStrategy sessionStrategy,
            HttpSessionSecurityContextRepository contexts, UsuarioRepository usuarios, SenhaService senhas) {
        this.authenticationManager = authenticationManager; this.sessionStrategy = sessionStrategy;
        this.contexts = contexts; this.usuarios = usuarios; this.senhas = senhas;
    }
    public record Login(@NotBlank @Email @Size(max=254) String email, @NotBlank String senha) {
        public Login { if (email != null) { email = email.strip(); } }
    }
    public record TrocaSenha(@NotBlank String senhaAtual, @NotBlank @Size(min=12) String novaSenha) {}
    public record Identidade(Long id, String nome, String email, String perfil, boolean trocaSenhaObrigatoria) {
        static Identidade de(Usuario u) {
            return new Identidade(u.getId(), u.getNome(), u.getEmail(), u.getPerfil().name(), u.isTrocaSenhaObrigatoria());
        }
    }
    public record TokenCsrf(String token, String headerName) {}

    @GetMapping("/csrf") public TokenCsrf csrf(CsrfToken token) { return new TokenCsrf(token.getToken(), token.getHeaderName()); }

    @PostMapping("/login")
    public Identidade login(@Valid @RequestBody Login body, HttpServletRequest request, HttpServletResponse response) {
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.email(), body.senha()));
        sessionStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return me((UsuarioAutenticado) authentication.getPrincipal());
    }
    @GetMapping("/me") public Identidade me(@AuthenticationPrincipal UsuarioAutenticado principal) {
        return Identidade.de(usuarios.findById(principal.getId()).orElseThrow());
    }
    @PutMapping("/senha") public ResponseEntity<Void> trocar(@AuthenticationPrincipal UsuarioAutenticado principal,
            @Valid @RequestBody TrocaSenha body, HttpServletRequest request, HttpServletResponse response) {
        senhas.trocar(principal.getId(), body.senhaAtual(), body.novaSenha());
        encerrar(request, response);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        encerrar(request, response);
        return ResponseEntity.noContent().build();
    }
    private void encerrar(HttpServletRequest request, HttpServletResponse response) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        new CookieClearingLogoutHandler("SESSION").logout(request, response, authentication);
    }
}
