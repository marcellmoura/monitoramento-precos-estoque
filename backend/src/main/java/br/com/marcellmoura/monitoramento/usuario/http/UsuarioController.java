package br.com.marcellmoura.monitoramento.usuario.http;

import java.net.URI;
import java.util.Locale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.marcellmoura.monitoramento.acesso.security.UsuarioAutenticado;
import br.com.marcellmoura.monitoramento.usuario.application.AdministracaoUsuarioService;
import br.com.marcellmoura.monitoramento.usuario.application.UsuarioResumo;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;

@RestController
@RequestMapping("/api/usuarios")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class UsuarioController {
    private final AdministracaoUsuarioService service;
    public UsuarioController(AdministracaoUsuarioService service) { this.service = service; }

    public record Cadastro(@NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=254) String email,
            @NotNull Perfil perfil, @NotBlank @Size(min=12) String senhaTemporaria) {
        public Cadastro {
            if (nome != null) { nome = nome.strip(); }
            if (email != null) { email = email.strip().toLowerCase(Locale.ROOT); }
        }
    }
    public record Acesso(@NotNull Perfil perfil, @NotNull Boolean ativo) {}
    public record Redefinicao(@NotBlank @Size(min=12) String senhaTemporaria) {}

    @GetMapping public AdministracaoUsuarioService.Pagina listar(
            @RequestParam(defaultValue="0") @Min(0) int pagina,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int tamanho) {
        return service.listar(pagina, tamanho);
    }
    @GetMapping("/{id}") public UsuarioResumo consultar(@PathVariable @Positive Long id) {
        return service.consultar(id);
    }
    @PostMapping public ResponseEntity<UsuarioResumo> criar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @Valid @RequestBody Cadastro body) {
        var usuario = service.criar(ator.getId(), ator.getVersaoCredencial(), body.nome(), body.email(),
                body.perfil(), body.senhaTemporaria());
        return ResponseEntity.created(URI.create("/api/usuarios/" + usuario.id())).body(usuario);
    }
    @PatchMapping("/{id}/acesso") public UsuarioResumo alterar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable @Positive Long id, @Valid @RequestBody Acesso body) {
        return service.alterarAcesso(ator.getId(), ator.getVersaoCredencial(), id, body.perfil(), body.ativo());
    }
    @PostMapping("/{id}/redefinir-senha") public ResponseEntity<Void> redefinir(
            @AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable @Positive Long id,
            @Valid @RequestBody Redefinicao body) {
        service.redefinirSenha(ator.getId(), ator.getVersaoCredencial(), id, body.senhaTemporaria());
        return ResponseEntity.noContent().build();
    }
}
