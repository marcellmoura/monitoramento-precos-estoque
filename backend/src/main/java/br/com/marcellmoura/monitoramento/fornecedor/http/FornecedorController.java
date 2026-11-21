package br.com.marcellmoura.monitoramento.fornecedor.http;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.fornecedor.application.FornecedorService;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;

@RestController
@RequestMapping("/api/fornecedores")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FornecedorController {
    private final FornecedorService service;

    public FornecedorController(FornecedorService service) {
        this.service = service;
    }

    public record Cadastro(@NotBlank @Size(max = 255) String nome, @Size(max = 32) String cnpj,
            @Size(max = 40) @Pattern(regexp = "(?:\\([0-9]{2}\\)|[0-9]{2}) *[0-9]{4,5}[- ]?[0-9]{4}") String telefone,
            @Email @Size(max = 254) String email) {
        public Cadastro {
            nome = normalizar(nome);
            cnpj = normalizar(cnpj);
            telefone = normalizar(telefone);
            email = normalizar(email);
        }

        private static String normalizar(String valor) {
            return valor == null || valor.isBlank() ? null : valor.strip();
        }
    }

    public record Resposta(Long id, String codigo, String nome, String cnpj, String telefone, String email) {
        static Resposta de(Fornecedor fornecedor) {
            return new Resposta(fornecedor.getId(), fornecedor.getCodigo(), fornecedor.getNome(),
                    fornecedor.getCnpj(), fornecedor.getTelefone(), fornecedor.getEmail());
        }
    }

    public record Pagina(List<Resposta> itens, int pagina, int tamanho, long total, int paginas) {}

    @GetMapping
    public Pagina listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        var resultado = service.listar(pagina, tamanho);
        return new Pagina(resultado.map(Resposta::de).getContent(), pagina, tamanho,
                resultado.getTotalElements(), resultado.getTotalPages());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> consultar(@PathVariable @Positive Long id) {
        var fornecedor = service.consultar(id);
        if (fornecedor.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(new ErroApi("FORNECEDOR_NAO_ENCONTRADO", "Fornecedor não encontrado."));
        }
        return ResponseEntity.ok(Resposta.de(fornecedor.get()));
    }

    @PostMapping
    public ResponseEntity<Resposta> cadastrar(@Valid @RequestBody Cadastro cadastro) {
        var fornecedor = service.cadastrar(cadastro.nome(), cadastro.cnpj(), cadastro.telefone(), cadastro.email());
        return ResponseEntity.created(URI.create("/api/fornecedores/" + fornecedor.getId()))
                .body(Resposta.de(fornecedor));
    }
}
