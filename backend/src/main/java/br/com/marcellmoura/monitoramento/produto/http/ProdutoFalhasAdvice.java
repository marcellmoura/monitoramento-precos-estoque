package br.com.marcellmoura.monitoramento.produto.http;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.produto.application.ProdutoNaoEncontradoException;

@RestControllerAdvice
@Order(0)
public class ProdutoFalhasAdvice {
    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    ResponseEntity<ErroApi> ausente(ProdutoNaoEncontradoException ex) {
        return ResponseEntity.status(404).body(new ErroApi("PRODUTO_NAO_ENCONTRADO", ex.getMessage()));
    }
}
