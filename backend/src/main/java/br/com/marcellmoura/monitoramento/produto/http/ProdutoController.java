package br.com.marcellmoura.monitoramento.produto.http;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
import br.com.marcellmoura.monitoramento.produto.application.ProdutoService;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import tools.jackson.databind.JsonNode;

@RestController
@RequestMapping("/api/produtos")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ProdutoController {
    private final ProdutoService service;
    public ProdutoController(ProdutoService service) { this.service = service; }
    public record Cadastro(@NotBlank @Size(max=255) String nome, @Size(max=100) String categoria,
            @NotNull Unidade unidade, @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal precoCompra,
            @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal precoVenda) {
        public Cadastro {
            if (nome != null) { nome = nome.strip(); }
            if (categoria != null) { categoria = categoria.strip(); }
        }
    }
    public record Pagina(List<ProdutoResposta> itens, int pagina, int tamanho, long total, int paginas) {}
    @GetMapping public Pagina listar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @RequestParam(defaultValue="0") @Min(0) int pagina,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int tamanho) {
        var resultado = service.listar(pagina, tamanho);
        return new Pagina(resultado.map(p -> ProdutoResposta.de(p, ator.getPerfil())).getContent(),
                pagina, tamanho, resultado.getTotalElements(), resultado.getTotalPages());
    }
    @GetMapping("/{id}") public ProdutoResposta consultar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable @Positive Long id) {
        return ProdutoResposta.de(service.consultar(id), ator.getPerfil());
    }
    @PostMapping public ResponseEntity<ProdutoResposta> cadastrar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @Valid @RequestBody Cadastro body) {
        var produto = service.cadastrar(body.nome(), body.categoria(), body.unidade(), body.precoCompra(), body.precoVenda());
        return ResponseEntity.created(URI.create("/api/produtos/" + produto.getId()))
                .body(ProdutoResposta.de(produto, ator.getPerfil()));
    }
    @PatchMapping("/{id}") public ProdutoResposta editar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable @Positive Long id, @RequestBody Map<String, JsonNode> body) {
        var alteracao = EdicaoProdutoRequest.interpretar(body, ator.getPerfil());
        return ProdutoResposta.de(service.editar(id, ator.getId(), alteracao), ator.getPerfil());
    }
}
