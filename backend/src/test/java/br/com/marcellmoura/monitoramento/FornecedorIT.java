package br.com.marcellmoura.monitoramento;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.marcellmoura.monitoramento.fornecedor.persistence.FornecedorRepository;
import br.com.marcellmoura.monitoramento.support.ClienteHttpTeste;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.support.ClienteHttpTeste.json;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(PostgreSqlTestConfiguration.class)
class FornecedorIT {
    private static final String ROTA = "/api/fornecedores";
    private static final String SENHA = "Senha-definitiva-teste-123";
    @Value("${local.server.port}") private int porta;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private PasswordEncoder encoder;

    @BeforeEach
    void limparFornecedores() {
        fornecedores.deleteAll();
    }

    private ClienteHttpTeste entrar(Perfil perfil, boolean temporaria) throws Exception {
        var usuario = new Usuario(perfil.name(), java.util.UUID.randomUUID() + "@example.com",
                encoder.encode(SENHA), perfil);
        if (!temporaria) {
            usuario.trocarSenha(usuario.getSenhaHash());
        }
        usuarios.saveAndFlush(usuario);
        var cliente = new ClienteHttpTeste(porta);
        assertThat(cliente.enviar("POST", "/api/auth/login",
                Map.of("email", usuario.getEmail(), "senha", SENHA), true).statusCode()).isEqualTo(200);
        return cliente;
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"ADMINISTRADOR", "COMPRAS"})
    void cadastraEConsultaComCodigoEContatosNormalizados(Perfil perfil) throws Exception {
        var cliente = entrar(perfil, false);
        var resposta = cliente.enviar("POST", ROTA, Map.of("nome", "  Distribuidora Teste  ",
                "cnpj", "  AB.123.456/0001-01 ", "telefone", " (83) 99999-9999 ",
                "email", " Vendas@example.com "), true);
        assertThat(resposta.statusCode()).isEqualTo(201);
        var dados = json(resposta);
        long id = dados.get("id").asLong();
        assertThat(resposta.headers().firstValue("Location")).contains(ROTA + "/" + id);
        assertThat(dados.get("codigo").asText()).isEqualTo("FOR-%06d".formatted(id));
        assertThat(dados.get("nome").asText()).isEqualTo("Distribuidora Teste");
        assertThat(dados.get("cnpj").asText()).isEqualTo("AB.123.456/0001-01");
        assertThat(dados.get("telefone").asText()).isEqualTo("(83) 99999-9999");
        assertThat(dados.get("email").asText()).isEqualTo("Vendas@example.com");
        assertThat(json(cliente.enviar("GET", ROTA + "/" + id, null, false))).isEqualTo(dados);
        assertThat(json(cliente.enviar("GET", ROTA, null, false)).get("itens").get(0)).isEqualTo(dados);
    }

    @Test
    void paginaHomônimosEConservaContatosAusentes() throws Exception {
        var cliente = entrar(Perfil.COMPRAS, false);
        var vazio = json(cliente.enviar("GET", ROTA, null, false));
        assertThat(vazio.get("total").asLong()).isZero();
        assertThat(vazio.get("tamanho").asInt()).isEqualTo(20);
        var primeiro = json(cliente.enviar("POST", ROTA,
                Map.of("nome", "Mesmo Nome", "telefone", " ", "cnpj", "", "email", "  "), true));
        var segundo = json(cliente.enviar("POST", ROTA,
                Map.of("nome", "Mesmo Nome", "telefone", "(83) 3333-4444"), true));
        assertThat(primeiro.get("codigo").asText()).isNotEqualTo(segundo.get("codigo").asText());
        for (var campo : List.of("cnpj", "telefone", "email")) {
            assertThat(primeiro.get(campo).isNull()).isTrue();
        }
        var pagina = json(cliente.enviar("GET", ROTA + "?pagina=1&tamanho=1", null, false));
        assertThat(pagina.get("total").asLong()).isEqualTo(2);
        assertThat(pagina.get("paginas").asInt()).isEqualTo(2);
        assertThat(pagina.get("itens").get(0)).isEqualTo(segundo);
        assertThat(cliente.enviar("GET", ROTA + "?tamanho=101", null, false).statusCode()).isEqualTo(400);
        assertThat(cliente.enviar("GET", ROTA + "?pagina=-1", null, false).statusCode()).isEqualTo(400);
        assertThat(cliente.enviar("GET", ROTA + "/0", null, false).statusCode()).isEqualTo(400);
        assertThat(cliente.enviar("GET", ROTA + "/999999", null, false).statusCode()).isEqualTo(404);
    }

    @Test
    void rejeitaFormatosInvalidosSemGravar() throws Exception {
        var cliente = entrar(Perfil.ADMINISTRADOR, false);
        for (var telefone : List.of("+55 (83) 99999-9999", "5583999999999", "99999-9999",
                "(83 99999-9999", "83 99A99-9999", "8 99999-9999")) {
            var resposta = cliente.enviar("POST", ROTA, Map.of("nome", "Teste", "telefone", telefone), true);
            assertThat(resposta.statusCode()).isEqualTo(400);
            assertThat(json(resposta).get("campos").has("telefone")).isTrue();
        }
        for (var dados : List.of(Map.of("nome", "  "), Map.of("nome", "x".repeat(256)),
                Map.of("nome", "Teste", "cnpj", "x".repeat(33)), Map.of("nome", "Teste", "email", "inválido"))) {
            assertThat(cliente.enviar("POST", ROTA, dados, true).statusCode()).isEqualTo(400);
        }
        assertThat(fornecedores.count()).isZero();
        assertThat(cliente.enviar("POST", ROTA,
                Map.of("nome", "Teste", "telefone", "83999999999"), true).statusCode()).isEqualTo(201);
    }

    @ParameterizedTest
    @EnumSource(value = Perfil.class, names = {"ESTOQUE", "CONSULTA"})
    void bloqueiaListaDetalheECadastroParaOutrosPerfis(Perfil perfil) throws Exception {
        var cliente = entrar(perfil, false);
        assertThat(cliente.enviar("GET", ROTA, null, false).statusCode()).isEqualTo(403);
        assertThat(cliente.enviar("GET", ROTA + "/1", null, false).statusCode()).isEqualTo(403);
        assertThat(cliente.enviar("POST", ROTA, Map.of("nome", "Teste"), true).statusCode()).isEqualTo(403);
    }

    @Test
    void exigeSessaoSenhaDefinitivaECsrf() throws Exception {
        var visitante = new ClienteHttpTeste(porta);
        assertThat(visitante.enviar("GET", ROTA, null, false).statusCode()).isEqualTo(401);
        var temporaria = entrar(Perfil.COMPRAS, true);
        assertThat(temporaria.enviar("GET", ROTA, null, false).statusCode()).isEqualTo(403);
        var admin = entrar(Perfil.ADMINISTRADOR, false);
        assertThat(admin.enviar("POST", ROTA, Map.of("nome", "Teste"), false).statusCode()).isEqualTo(403);
        assertThat(fornecedores.count()).isZero();
    }
}
