package br.com.marcellmoura.monitoramento.importacao.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.acesso.security.UsuarioAutenticado;
import br.com.marcellmoura.monitoramento.importacao.application.ImportacaoException;
import br.com.marcellmoura.monitoramento.importacao.application.ConferirCotacao;
import br.com.marcellmoura.monitoramento.importacao.domain.ConferenciaCotacao;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.importacao.application.RecepcaoPdf;
import br.com.marcellmoura.monitoramento.importacao.application.RegistroImportacoes;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;

@RestController
@RequestMapping("/api/importacoes")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ImportacaoController {
    private final RecepcaoPdf recepcao;
    private final RegistroImportacoes registro;
    private final ConferirCotacao conferencia;

    public ImportacaoController(RecepcaoPdf recepcao, RegistroImportacoes registro, ConferirCotacao conferencia) {
        this.recepcao = recepcao;
        this.registro = registro;
        this.conferencia = conferencia;
    }

    public record Pagina(List<TentativaImportacao> itens, int pagina, int tamanho, long total, int paginas) {}

    @GetMapping
    public Pagina listar(@RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanho) {
        var dados = registro.listar(pagina, tamanho);
        return new Pagina(dados.getContent(), dados.getNumber(), dados.getSize(), dados.getTotalElements(), dados.getTotalPages());
    }

    @GetMapping("/{id}")
    public TentativaImportacao consultar(@PathVariable @Positive long id) { return registro.consultar(id); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TentativaImportacao> enviar(@AuthenticationPrincipal UsuarioAutenticado ator,
            @RequestParam @Positive long fornecedorId, @RequestParam(required = false) MultipartFile arquivo) throws IOException {
        String nome = arquivo == null ? null : arquivo.getOriginalFilename();
        long tamanho = arquivo == null ? 0 : arquivo.getSize();
        try (var conteudo = arquivo == null ? InputStream.nullInputStream() : arquivo.getInputStream()) {
            var tentativa = recepcao.receber(fornecedorId, ator.getId(), nome, tamanho, conteudo);
            return ResponseEntity.created(URI.create("/api/importacoes/" + tentativa.id())).body(tentativa);
        }
    }

    @PostMapping("/{id}/confirmacao")
    public TentativaImportacao confirmar(@AuthenticationPrincipal UsuarioAutenticado ator, @PathVariable @Positive long id) {
        return registro.confirmar(id, ator.getId());
    }

    @GetMapping("/{id}/previa")
    public PreviaImportacao previa(@PathVariable @Positive long id) { return registro.previa(id); }

    @PostMapping("/{id}/reprocessamento")
    public TentativaImportacao reprocessar(@PathVariable @Positive long id) { return registro.reprocessar(id); }

    @GetMapping("/{id}/conferencia")
    public ConferenciaCotacao consultarConferencia(@PathVariable @Positive long id) { return conferencia.consultar(id); }

    public record ConfirmacaoComercial(@NotNull @Min(0) Long revisao, @NotBlank @Size(max = 100) String coluna,
            @NotNull Unidade unidade, @NotBlank @Size(max = 120) String embalagem, @NotNull LocalDate dataComercial) {
        public ConfirmacaoComercial {
            if (embalagem != null) embalagem = embalagem.strip();
        }
    }

    @PutMapping("/{id}/conferencia")
    public ConferenciaCotacao confirmarConferencia(@AuthenticationPrincipal UsuarioAutenticado ator,
            @PathVariable @Positive long id, @Valid @RequestBody ConfirmacaoComercial dados) {
        var base = new ConferenciaCotacao.Base(dados.coluna(), dados.unidade(), dados.embalagem());
        return conferencia.confirmar(id, ator.getId(), new ConferirCotacao.Escolha(dados.revisao(), base, dados.dataComercial()));
    }

    @ExceptionHandler(ImportacaoException.class)
    ResponseEntity<ErroApi> falha(ImportacaoException ex) {
        return ResponseEntity.status(ex.status()).body(new ErroApi(ex.codigo(), ex.getMessage()));
    }
}
