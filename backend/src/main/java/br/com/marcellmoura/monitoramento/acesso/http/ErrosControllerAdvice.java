package br.com.marcellmoura.monitoramento.acesso.http;

import br.com.marcellmoura.monitoramento.acesso.application.SenhaInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MissingServletRequestParameterException;

@RestControllerAdvice
public class ErrosControllerAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(ErrosControllerAdvice.class);
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroApi> credencial(AuthenticationException ex) {
        return ResponseEntity.status(401).body(new ErroApi("CREDENCIAL_INVALIDA", "E-mail ou senha inválidos."));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApi> camposInvalidos(MethodArgumentNotValidException ex) {
        var campos = new java.util.TreeMap<String, String>();
        ex.getBindingResult().getFieldErrors().forEach(erro -> campos.put(erro.getField(), "Valor inválido."));
        return ResponseEntity.badRequest().body(new ErroApi("DADOS_INVALIDOS", "Confira os campos informados.",
                java.util.UUID.randomUUID().toString(), campos));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,
            HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    ResponseEntity<ErroApi> validacao(Exception ex) {
        return ResponseEntity.badRequest().body(new ErroApi("DADOS_INVALIDOS", "Confira os campos informados."));
    }
    @ExceptionHandler(SenhaInvalidaException.class)
    ResponseEntity<ErroApi> regra(SenhaInvalidaException ex) {
        return ResponseEntity.badRequest().body(new ErroApi("SENHA_INVALIDA", ex.getMessage()));
    }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroApi> ausente(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(new ErroApi("NAO_ENCONTRADO", "Recurso não encontrado."));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroApi> metodo(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(new ErroApi("METODO_NAO_PERMITIDO", "Método não permitido."));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErroApi> formato(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(415).body(new ErroApi("FORMATO_NAO_SUPORTADO", "Envie os dados em JSON."));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroApi> uploadMuitoGrande() {
        return ResponseEntity.status(413).body(new ErroApi("UPLOAD_MUITO_GRANDE", "O arquivo ultrapassa o limite de envio."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroApi> inesperado(Exception ex) {
        var erro = new ErroApi("ERRO_INTERNO", "Não foi possível concluir a operação.");
        LOG.error("Falha interna {}: {}", erro.identificador(), ex.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(erro);
    }
}
