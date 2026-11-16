package br.com.marcellmoura.monitoramento.usuario.http;

import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import br.com.marcellmoura.monitoramento.acesso.http.ErroApi;
import br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException;

@RestControllerAdvice
@Order(0)
public class FalhasUsuarioAdvice {
    @ExceptionHandler(FalhaUsuarioException.class)
    ResponseEntity<ErroApi> tratar(FalhaUsuarioException ex) {
        return switch (ex.getMotivo()) {
            case NAO_ENCONTRADO -> ResponseEntity.status(404)
                    .body(new ErroApi("USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
            case EMAIL_DUPLICADO -> ResponseEntity.status(409)
                    .body(new ErroApi("EMAIL_DUPLICADO", "Já existe um usuário com esse e-mail."));
            case ULTIMO_ADMINISTRADOR -> ResponseEntity.status(409)
                    .body(new ErroApi("ULTIMO_ADMINISTRADOR", "É necessário manter um Administrador ativo."));
            case ACESSO_REVOGADO -> ResponseEntity.status(401)
                    .body(new ErroApi("SESSAO_INVALIDA", "Seu acesso foi alterado. Entre novamente."));
        };
    }
}
