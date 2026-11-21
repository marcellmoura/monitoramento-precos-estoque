package br.com.marcellmoura.monitoramento.produto.http;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.produto.domain.ProdutoEdicaoInvalidaException;
import br.com.marcellmoura.monitoramento.produto.application.ProdutoNaoEncontradoException;

@RestControllerAdvice(assignableTypes = ProdutoController.class)
@Order(0)
public class ProdutoFalhasAdvice {
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    ResponseEntity<ErroApi> ausente(ProdutoNaoEncontradoException ex) {
        return ResponseEntity.status(404).body(new ErroApi("PRODUTO_NAO_ENCONTRADO", ex.getMessage()));
    }

    @ExceptionHandler(ProdutoEdicaoInvalidaException.class)
    ResponseEntity<ErroApi> invalido(ProdutoEdicaoInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErroApi("PRODUTO_EDICAO_INVALIDA", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroApi> negado(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(new ErroApi("ACESSO_NEGADO", ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ErroApi> conflito() {
        return ResponseEntity.status(409).body(new ErroApi("PRODUTO_DESATUALIZADO", "O produto mudou. Reabra a edição antes de salvar."));
    }
}
