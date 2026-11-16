package br.com.marcellmoura.monitoramento.support;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public class ClienteHttpTeste {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    private final String base;
    public ClienteHttpTeste(int porta) { base = "http://127.0.0.1:" + porta; }
    public HttpResponse<String> enviar(String metodo, String rota, Object corpo, boolean csrf) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + rota)).header("Content-Type", "application/json");
        if (csrf) {
            var token = json(enviar("GET", "/api/auth/csrf", null, false));
            builder.header(token.get("headerName").asText(), token.get("token").asText());
        }
        return client.send(builder.method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(corpo))).build(), HttpResponse.BodyHandlers.ofString());
    }
    public static JsonNode json(HttpResponse<String> resposta) { return JSON.readTree(resposta.body()); }
}
