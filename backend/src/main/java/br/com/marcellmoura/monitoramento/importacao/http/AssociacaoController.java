package br.com.marcellmoura.monitoramento.importacao.http;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.acesso.security.UsuarioAutenticado;
import br.com.marcellmoura.monitoramento.importacao.application.AssociarItemCotacao;
import br.com.marcellmoura.monitoramento.importacao.application.ImportacaoException;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.ProdutoCatalogo;
import br.com.marcellmoura.monitoramento.importacao.domain.VinculoItem.Situacao;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;

@RestController
@RequestMapping("/api/importacoes")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AssociacaoController {
    private final AssociarItemCotacao associacao;

    public AssociacaoController(AssociarItemCotacao associacao) { this.associacao = associacao; }

    public record PaginaCatalogo(List<ProdutoCatalogo> itens, int pagina, int paginas) {}

    @GetMapping("/catalogo")
    public PaginaCatalogo pesquisar(@RequestParam Unidade unidade, @RequestParam(defaultValue = "") @Size(max = 255) String nome,
            @RequestParam(defaultValue = "0") @Min(0) int pagina) {
        var dados = associacao.pesquisar(unidade, nome, pagina);
        return new PaginaCatalogo(dados.getContent(), dados.getNumber(), dados.getTotalPages());
    }

    @GetMapping("/{id}/vinculos")
    public List<Situacao> listar(@PathVariable @Positive long id) { return associacao.listar(id); }

    public record ConfirmarVinculo(@NotNull @Positive Long produto, @NotNull @Positive Long conferencia,
            @NotNull @Min(0) Long revisaoItem, @NotNull @Min(0) Long vinculo) {}

    @PutMapping("/{id}/itens/{linha}/vinculo")
    public VinculoItem associar(@PathVariable @Positive long id, @PathVariable @Positive int linha,
            @Valid @RequestBody ConfirmarVinculo dados, @AuthenticationPrincipal UsuarioAutenticado ator) {
        return associacao.associar(id, linha, ator.getId(),
                new AssociarItemCotacao.Escolha(dados.produto(), dados.conferencia(), dados.revisaoItem(), dados.vinculo()));
    }

    @ExceptionHandler(ImportacaoException.class)
    ResponseEntity<ErroApi> falha(ImportacaoException erro) {
        return ResponseEntity.status(erro.status()).body(new ErroApi(erro.codigo(), erro.getMessage()));
    }
}
