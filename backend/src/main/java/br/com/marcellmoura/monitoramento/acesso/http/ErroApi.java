package br.com.marcellmoura.monitoramento.acesso.http;

import java.io.IOException;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletResponse;

public record ErroApi(String codigo, String mensagem, String identificador) {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    public ErroApi(String codigo, String mensagem) { this(codigo, mensagem, UUID.randomUUID().toString()); }

    public static void escrever(HttpServletResponse response, int status, String codigo, String mensagem)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.writeValueAsString(new ErroApi(codigo, mensagem)));
    }
}
