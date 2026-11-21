package br.com.marcellmoura.monitoramento.support;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class ClienteHttpTeste {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final String base;
    public ClienteHttpTeste(int porta) { base = "http://127.0.0.1:" + porta; }
    public HttpResponse<String> enviar(String metodo, String rota, Object corpo, boolean csrf) throws Exception {
        var body = corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo));
        return enviar(metodo, rota, "application/json", body, csrf);
    }
    public HttpResponse<String> enviarMultipart(String rota, Map<String, String> campos, String nome, byte[] arquivo, boolean csrf)
            throws Exception {
        String separador = "limite-" + UUID.randomUUID();
        var bytes = new ByteArrayOutputStream();
        for (var campo : campos.entrySet()) {
            write(bytes, "--" + separador + "\r\nContent-Disposition: form-data; name=\"" + campo.getKey()
                    + "\"\r\n\r\n" + campo.getValue() + "\r\n");
        }
        if (arquivo != null) {
            write(bytes, "--" + separador + "\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"" + nome
                    + "\"\r\nContent-Type: application/pdf\r\n\r\n");
            bytes.writeBytes(arquivo);
            write(bytes, "\r\n");
        }
        write(bytes, "--" + separador + "--\r\n");
        return enviar("POST", rota, "multipart/form-data; boundary=" + separador,
                HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray()), csrf);
    }
    private HttpResponse<String> enviar(String metodo, String rota, String tipo, HttpRequest.BodyPublisher corpo, boolean csrf)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + rota)).header("Content-Type", tipo);
        if (csrf) {
            var token = json(enviar("GET", "/api/auth/csrf", null, false));
            builder.header(token.get("headerName").asText(), token.get("token").asText());
        }
        return client.send(builder.method(metodo, corpo).build(), HttpResponse.BodyHandlers.ofString());
    }
    private static void write(ByteArrayOutputStream saida, String valor) { saida.writeBytes(valor.getBytes(StandardCharsets.UTF_8)); }
    public static JsonNode json(HttpResponse<String> resposta) { return JSON.readTree(resposta.body()); }
}
