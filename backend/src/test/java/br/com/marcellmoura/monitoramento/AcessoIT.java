package br.com.marcellmoura.monitoramento;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import br.com.marcellmoura.monitoramento.acesso.application.AdministradorInicialService;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(PostgreSqlTestConfiguration.class)
class AcessoIT {
    private static final String EMAIL = "admin@example.com";
    private static final String SENHA = "Temporaria-teste-123";
    private static final String NOVA = "Definitiva-teste-456";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    @Value("${local.server.port}") private int port;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdministradorInicialService administrador;
    private HttpClient client;
    private Long id;
    private String hash;

    @BeforeEach void preparar() {
        jdbc.update("DELETE FROM spring_session");
        usuarios.deleteAll();
        hash = encoder.encode(SENHA);
        id = usuarios.saveAndFlush(new Usuario("Administrador teste", EMAIL, hash, Perfil.ADMINISTRADOR)).getId();
        client = novoCliente();
    }
    private HttpClient novoCliente() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    private HttpResponse<String> enviar(HttpClient browser, String method, String path, String body, boolean csrf)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (csrf) {
            var token = JSON.readTree(enviar(browser, "GET", "/api/auth/csrf", null, false).body());
            builder.header(token.get("headerName").asText(), token.get("token").asText());
        }
        return browser.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> login(HttpClient browser) throws Exception {
        return enviar(browser, "POST", "/api/auth/login", "{\"email\":\" ADMIN@example.com \" ,\"senha\":\"" + SENHA + "\"}", true);
    }
    private HttpResponse<String> trocar(HttpClient browser, String atual, String nova) throws Exception {
        return enviar(browser, "PUT", "/api/auth/senha", JSON.writeValueAsString(
                java.util.Map.of("senhaAtual", atual, "novaSenha", nova)), true);
    }
    private JsonNode identidade(HttpClient browser) throws Exception {
        var result = enviar(browser, "GET", "/api/auth/me", null, false);
        assertThat(result.statusCode()).isEqualTo(200);
        return JSON.readTree(result.body());
    }

    @Test void autenticaPersisteSessaoSemSenhaEConfiguraCookie() throws Exception {
        var result = login(client);
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.body()).doesNotContain(SENHA, hash, "senhaHash", "password");
        assertThat(identidade(client).get("perfil").asText()).isEqualTo("ADMINISTRADOR");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session WHERE principal_name=?", Long.class, id.toString())).isEqualTo(1L);
        var cookie = result.headers().allValues("Set-Cookie").toString();
        assertThat(cookie).contains("HttpOnly", "SameSite=Lax", "Path=/");
        var bytes = jdbc.queryForObject("SELECT attribute_bytes FROM spring_session_attributes WHERE attribute_name='SPRING_SECURITY_CONTEXT'", byte[].class);
        assertThat(new String(bytes, StandardCharsets.ISO_8859_1)).doesNotContain(SENHA, hash);
    }
    @Test void rejeitaCredenciaisIncorretasEInativoComMesmoErro() throws Exception {
        var incorreta = enviar(client, "POST", "/api/auth/login", "{\"email\":\"admin@example.com\",\"senha\":\"incorreta\"}", true);
        jdbc.update("UPDATE usuarios SET ativo=false WHERE id=?", id);
        var inativo = login(novoCliente());
        assertThat(incorreta.statusCode()).isEqualTo(401);
        assertThat(inativo.statusCode()).isEqualTo(401);
        assertThat(JSON.readTree(incorreta.body()).get("codigo").asText()).isEqualTo("CREDENCIAL_INVALIDA");
        assertThat(JSON.readTree(inativo.body()).get("codigo").asText()).isEqualTo("CREDENCIAL_INVALIDA");
    }
    @Test void exigeSessaoEProtegeLoginContraCsrf() throws Exception {
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        assertThat(enviar(client,"POST","/api/auth/login","{}",false).statusCode()).isEqualTo(403);
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/login"))
                .header("X-CSRF-TOKEN", "invalido").POST(HttpRequest.BodyPublishers.ofString("{}")).build();
        assertThat(client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
    }
    @Test void bloqueiaAcessoDiretoAntesDaTroca() throws Exception {
        assertThat(login(client).statusCode()).isEqualTo(200);
        var result=enviar(client,"GET","/api/catalogo",null,false);
        assertThat(result.statusCode()).isEqualTo(403);
        assertThat(result.body()).contains("TROCA_SENHA_OBRIGATORIA");
        assertThat(identidade(client).get("trocaSenhaObrigatoria").asBoolean()).isTrue();
    }
    @Test void validaSenhaAtualMinimoEDiferencaSemAlterarCredencial() throws Exception {
        login(client);
        assertThat(trocar(client,"errada",NOVA).statusCode()).isEqualTo(400);
        assertThat(trocar(client,SENHA,"curta").statusCode()).isEqualTo(400);
        assertThat(trocar(client,SENHA,SENHA).statusCode()).isEqualTo(400);
        assertThat(usuarios.findById(id).orElseThrow().isTrocaSenhaObrigatoria()).isTrue();
        assertThat(usuarios.findById(id).orElseThrow().getSenhaHash()).isEqualTo(hash);
    }
    @Test void trocaSenhaRevogaTodosAcessosEExigeNovoLogin() throws Exception {
        login(client);var outro=novoCliente();login(outro);
        assertThat(trocar(client,SENHA,NOVA).statusCode()).isEqualTo(204);
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        assertThat(enviar(outro,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        var usuario=usuarios.findById(id).orElseThrow();
        assertThat(usuario.getSenhaHash()).startsWith("$argon2id$");
        assertThat(encoder.matches(NOVA,usuario.getSenhaHash())).isTrue();
        assertThat(usuario.isTrocaSenhaObrigatoria()).isFalse();
        assertThat(usuario.getVersaoCredencial()).isEqualTo(1);
        assertThat(login(client).statusCode()).isEqualTo(401);
        var result=enviar(client,"POST","/api/auth/login",JSON.writeValueAsString(java.util.Map.of("email",EMAIL,"senha",NOVA)),true);
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(identidade(client).get("trocaSenhaObrigatoria").asBoolean()).isFalse();
        assertThat(enviar(client,"GET","/api/nao-existe",null,false).statusCode()).isEqualTo(404);
    }
    @Test void logoutInvalidaSessaoNoBancoELimpaCookie() throws Exception {
        login(client);
        var result=enviar(client,"POST","/api/auth/logout",null,true);
        assertThat(result.statusCode()).isEqualTo(204);
        assertThat(result.headers().allValues("Set-Cookie").toString()).contains("Max-Age=0");
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM spring_session WHERE principal_name=?",Long.class,id.toString())).isZero();
    }
    @Test void expiraSessaoAposTrintaMinutosSemEsperarTempoReal() throws Exception {
        login(client);
        assertThat(jdbc.queryForObject("SELECT max_inactive_interval FROM spring_session WHERE principal_name=?",Integer.class,id.toString())).isEqualTo(1800);
        jdbc.update("UPDATE spring_session SET last_access_time=?, expiry_time=? WHERE principal_name=?",System.currentTimeMillis()-1900000,System.currentTimeMillis()-1000,id.toString());
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
    }
    @Test void trocaIdentificadorDaSessaoAoEntrar() throws Exception {
        enviar(client,"GET","/api/auth/csrf",null,false);
        var anterior=jdbc.queryForObject("SELECT session_id FROM spring_session",String.class);
        login(client);
        assertThat(jdbc.queryForObject("SELECT session_id FROM spring_session",String.class)).isNotEqualTo(anterior);
    }
    @Test void rejeitaSessaoDeCredencialRevogadaMesmoSeRegistroSobreviver() throws Exception {
        login(client);
        jdbc.update("UPDATE usuarios SET versao_credencial=versao_credencial+1 WHERE id=?",id);
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
    }
    @Test void rejeitaSessaoQuandoUsuarioEInativado() throws Exception {
        login(client);jdbc.update("UPDATE usuarios SET ativo=false WHERE id=?",id);
        assertThat(enviar(client,"GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
    }
    @Test void validaCorpoDoLoginSemVazarEntrada() throws Exception {
        var result=enviar(client,"POST","/api/auth/login","{\"email\":\"invalido\",\"senha\":\"segredo-ficticio\"}",true);
        assertThat(result.statusCode()).isEqualTo(400);
        assertThat(result.body()).doesNotContain("segredo-ficticio","stackTrace");
        assertThat(enviar(client,"POST","/api/auth/login","{",true).statusCode()).isEqualTo(400);
    }
    @Test void rejeitaMetodoEFormatoIncorretos() throws Exception {
        assertThat(enviar(client,"GET","/api/auth/login",null,false).statusCode()).isEqualTo(405);
        var csrf=JSON.readTree(enviar(client,"GET","/api/auth/csrf",null,false).body());
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/auth/login"))
                .header("Content-Type","text/plain").header(csrf.get("headerName").asText(),csrf.get("token").asText())
                .POST(HttpRequest.BodyPublishers.ofString("texto")).build();
        assertThat(client.send(request,HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(415);
    }
    @Test void inicializaAdministradorUmaVezSemSobrescreverBase() {
        usuarios.deleteAll();
        administrador.criar("Primeiro admin",EMAIL,SENHA.toCharArray());
        var usuario=usuarios.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(usuario.getPerfil()).isEqualTo(Perfil.ADMINISTRADOR);
        assertThat(usuario.isTrocaSenhaObrigatoria()).isTrue();
        assertThat(encoder.matches(SENHA,usuario.getSenhaHash())).isTrue();
        assertThatThrownBy(()->administrador.criar("Outro admin","outro@example.com",SENHA.toCharArray()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(usuarios.count()).isEqualTo(1);
    }
    @Test void inicializacaoRejeitaDadosInvalidos() {
        usuarios.deleteAll();
        assertThatThrownBy(()->administrador.criar(" ","invalido","curta".toCharArray()))
                .isInstanceOf(jakarta.validation.ValidationException.class);
        assertThat(usuarios.count()).isZero();
    }
}
