package br.com.marcellmoura.monitoramento;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;
import br.com.marcellmoura.monitoramento.fornecedor.persistence.FornecedorRepository;
import br.com.marcellmoura.monitoramento.importacao.application.RecepcaoPdf;
import br.com.marcellmoura.monitoramento.support.ClienteHttpTeste;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.support.ClienteHttpTeste.json;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(PostgreSqlTestConfiguration.class)
class ImportacaoIT {
    private static final String ROTA = "/api/importacoes";
    private static final String SENHA = "Senha-sintetica-teste-123";
    private static final String ORIGINAL = "Fornecedor_1.pdf";
    @TempDir static Path pasta;
    @Value("${local.server.port}") private int porta;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private FornecedorRepository fornecedores;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecepcaoPdf recepcao;
    private ClienteHttpTeste admin;
    private Fornecedor fornecedor;
    private String hashSenha;

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry props) {
        props.add("app.importacao.pasta", () -> pasta.toString());
        props.add("app.importacao.limite-bytes", () -> "131072");
        props.add("spring.servlet.multipart.max-file-size", () -> "256KB");
        props.add("spring.servlet.multipart.max-request-size", () -> "300KB");
    }

    @BeforeEach void preparar() throws Exception {
        jdbc.update("DELETE FROM tentativas_importacao");
        jdbc.update("DELETE FROM arquivos_importacao");
        jdbc.update("DELETE FROM spring_session");
        fornecedores.deleteAll();
        usuarios.deleteAll();
        try (var arquivos = Files.list(pasta)) {
            for (Path arquivo : arquivos.toList()) Files.delete(arquivo);
        }
        hashSenha = encoder.encode(SENHA);
        admin = entrar(Perfil.ADMINISTRADOR);
        fornecedor = salvarFornecedor();
    }

    private Fornecedor salvarFornecedor() {
        return fornecedores.saveAndFlush(new Fornecedor("Distribuidora Teste", null, null, null));
    }

    private ClienteHttpTeste entrar(Perfil perfil) throws Exception {
        String email = perfil.name().toLowerCase(java.util.Locale.ROOT) + "@example.com";
        var usuario = new Usuario(perfil.name(), email, hashSenha, perfil);
        usuario.trocarSenha(hashSenha);
        usuarios.saveAndFlush(usuario);
        var cliente = new ClienteHttpTeste(porta);
        assertThat(cliente.enviar("POST", "/api/auth/login", Map.of("email", email, "senha", SENHA), true).statusCode()).isEqualTo(200);
        return cliente;
    }

    private HttpResponse<String> upload(ClienteHttpTeste cliente, Fornecedor origem, String nome, byte[] pdf) throws Exception {
        return cliente.enviarMultipart(ROTA, Map.of("fornecedorId", origem.getId().toString()), nome, pdf, true);
    }

    private byte[] ler(String arquivo) throws Exception {
        return Files.readAllBytes(Path.of("..", "fixtures", "pdfs", arquivo));
    }

    @ParameterizedTest
    @ValueSource(strings = {ORIGINAL, "Fornecedor_2.pdf", "sinteticos/tabela-v1.pdf", "sinteticos/fichas-v1.pdf",
            "sinteticos/tabela-v2.pdf", "sinteticos/fichas-v2.pdf"})
    void recebePdfTextualEGuardaExatamenteOOriginal(String arquivo) throws Exception {
        byte[] bytes = ler(arquivo);
        var resposta = upload(admin, fornecedor, Path.of(arquivo).getFileName().toString(), bytes);
        assertThat(resposta.statusCode()).isEqualTo(201);
        var dados = json(resposta);
        long id = dados.get("id").asLong();
        assertThat(resposta.headers().firstValue("Location")).contains(ROTA + "/" + id);
        assertThat(dados.get("status").asText()).isEqualTo("PENDENTE");
        assertThat(dados.get("fornecedorCodigo").asText()).isEqualTo(fornecedor.getCodigo());
        assertThat(dados.get("atorId").asLong()).isEqualTo(usuarios.findByEmailIgnoreCase("administrador@example.com").orElseThrow().getId());
        assertThat(dados.get("enviadaEm").isString()).isTrue();
        assertThat(dados.get("paginas").asInt()).isPositive();
        assertThat(dados.get("quantidadeItens").isNull()).isTrue();
        try (var caminhos = Files.list(pasta)) {
            var arquivos = caminhos.toList();
            assertThat(arquivos).hasSize(1);
            Path original = arquivos.getFirst();
            assertThat(original.getFileName().toString()).matches("[a-f0-9-]{36}\\.pdf");
            assertThat(Files.readAllBytes(original)).isEqualTo(bytes);
        }
        assertThat(json(admin.enviar("GET", ROTA + "/" + id, null, false))).isEqualTo(dados);
    }

    @Test void distingueFornecedoresHomonimosEDetectaDuplicataComOutroNome() throws Exception {
        byte[] bytes = ler(ORIGINAL);
        var outro = salvarFornecedor();
        var compras = entrar(Perfil.COMPRAS);
        var primeira = json(upload(admin, fornecedor, ORIGINAL, bytes));
        var repetida = json(upload(compras, fornecedor, "renomeado.pdf", bytes));
        var cruzada = json(upload(compras, outro, "copia.pdf", bytes));
        assertThat(repetida.get("status").asText()).isEqualTo("DUPLICADA");
        assertThat(repetida.get("importacaoExistenteId").asLong()).isEqualTo(primeira.get("id").asLong());
        assertThat(repetida.get("id").asLong()).isNotEqualTo(primeira.get("id").asLong());
        assertThat(cruzada.get("status").asText()).isEqualTo("AGUARDANDO_CONFIRMACAO");
        assertThat(cruzada.get("fornecedorNome").asText()).isEqualTo(fornecedor.getNome());
        assertThat(cruzada.get("fornecedorCodigo").asText()).isEqualTo(outro.getCodigo());
        String confirmacao = ROTA + "/" + cruzada.get("id").asLong() + "/confirmacao";
        assertThat(compras.enviar("POST", confirmacao, null, false).statusCode()).isEqualTo(403);
        var confirmada = compras.enviar("POST", confirmacao, null, true);
        assertThat(confirmada.statusCode()).isEqualTo(200);
        assertThat(json(confirmada).get("status").asText()).isEqualTo("PENDENTE");
        assertThat(compras.enviar("POST", confirmacao, null, true).statusCode()).isEqualTo(409);
        assertThat(compras.enviar("POST", ROTA + "/99999/confirmacao", null, true).statusCode()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM arquivos_importacao", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tentativas_importacao WHERE status = 'PENDENTE'", Long.class)).isEqualTo(2L);
        try (var caminhos = Files.list(pasta)) { assertThat(caminhos.count()).isEqualTo(1); }
    }

    @Test void registraOsMotivosDeRejeicaoSemContagemFicticiaNemArquivoGuardado() throws Exception {
        verificarRejeicao("orcamento.txt", ler(ORIGINAL), "extensão");
        verificarRejeicao("corrompido.pdf", "PDF inválido".getBytes(StandardCharsets.UTF_8), "corrompido");
        verificarRejeicao("grande.pdf", new byte[150_000], "limite");
        verificarRejeicao("vazio.pdf", new byte[0], "vazio");
        verificarRejeicao("sem-texto.pdf", gerarPdf(1, false), "não contém texto");
        verificarRejeicao("muitas-paginas.pdf", gerarPdf(51, false), "50 páginas");
        verificarRejeicao("protegido.pdf", gerarPdf(1, true), "protegido");
        try (var caminhos = Files.list(pasta)) { assertThat(caminhos.count()).isZero(); }
        var lista = json(admin.enviar("GET", ROTA + "?pagina=1&tamanho=3", null, false));
        assertThat(lista.get("total").asLong()).isEqualTo(7);
        assertThat(lista.get("paginas").asInt()).isEqualTo(3);
        assertThat(lista.get("itens").size()).isEqualTo(3);
        assertThat(admin.enviar("GET", ROTA + "?pagina=-1", null, false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("GET", ROTA + "/99999", null, false).statusCode()).isEqualTo(404);
    }

    private void verificarRejeicao(String nome, byte[] dados, String motivo) throws Exception {
        var resposta = upload(admin, fornecedor, nome, dados);
        assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(json(resposta).get("status").asText()).isEqualTo("REJEITADA");
        assertThat(json(resposta).get("motivo").asText()).contains(motivo);
        assertThat(json(resposta).get("quantidadeItens").isNull()).isTrue();
    }

    private byte[] gerarPdf(int paginas, boolean protegido) throws Exception {
        try (var pdf = new PDDocument(); var saida = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) pdf.addPage(new PDPage());
            if (protegido) pdf.protect(new StandardProtectionPolicy("senha-proprietario", "senha-usuario", new AccessPermission()));
            pdf.save(saida);
            return saida.toByteArray();
        }
    }

    @Test void aplicaPerfilSessaoCsrfFornecedorELimiteDeTransporte() throws Exception {
        byte[] bytes = ler(ORIGINAL);
        var campos = Map.of("fornecedorId", fornecedor.getId().toString());
        assertThat(new ClienteHttpTeste(porta).enviar("GET", ROTA, null, false).statusCode()).isEqualTo(401);
        for (Perfil perfil : java.util.List.of(Perfil.ESTOQUE, Perfil.CONSULTA)) {
            var cliente = entrar(perfil);
            assertThat(cliente.enviar("GET", ROTA, null, false).statusCode()).isEqualTo(403);
            assertThat(upload(cliente, fornecedor, ORIGINAL, bytes).statusCode()).isEqualTo(403);
        }
        assertThat(admin.enviarMultipart(ROTA, campos, ORIGINAL, bytes, false).statusCode()).isEqualTo(403);
        assertThat(admin.enviarMultipart(ROTA, Map.of(), ORIGINAL, bytes, true).statusCode()).isEqualTo(400);
        assertThat(admin.enviarMultipart(ROTA, Map.of("fornecedorId", "99999"), ORIGINAL, bytes, true).statusCode()).isEqualTo(404);
        assertThat(upload(admin, fornecedor, "grande.pdf", new byte[280_000]).statusCode()).isEqualTo(413);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tentativas_importacao", Long.class)).isZero();
    }

    @Test void umaUnicaImportacaoCanonicaMesmoComEnviosSimultaneos() throws Exception {
        var compras = entrar(Perfil.COMPRAS);
        byte[] bytes = ler(ORIGINAL);
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return json(upload(admin, fornecedor, ORIGINAL, bytes)).get("status").asText();
            });
            var segunda = executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return json(upload(compras, fornecedor, "outro-nome.pdf", bytes)).get("status").asText();
            });
            assertThat(java.util.List.of(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("PENDENTE", "DUPLICADA");
        }
        try (var caminhos = Files.list(pasta)) { assertThat(caminhos.count()).isEqualTo(1); }
    }

    @Test void removeOriginalSeTransacaoForRevertida() throws Exception {
        byte[] bytes = ler(ORIGINAL);
        try (var entrada = new ByteArrayInputStream(bytes)) {
            assertThatThrownBy(() -> recepcao.receber(fornecedor.getId(), -1, ORIGINAL, bytes.length, entrada))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM arquivos_importacao", Long.class)).isZero();
        try (var caminhos = Files.list(pasta)) { assertThat(caminhos.count()).isZero(); }
    }

    @Test void guardaTentativaQuandoArmazenamentoEstaIndisponivel() throws Exception {
        byte[] bytes = ler(ORIGINAL);
        Files.delete(pasta);
        Files.createFile(pasta);
        try {
            var resposta = upload(admin, fornecedor, ORIGINAL, bytes);
            assertThat(resposta.statusCode()).isEqualTo(201);
            assertThat(json(resposta).get("status").asText()).isEqualTo("FALHA_ENVIO");
        } finally {
            Files.delete(pasta);
            Files.createDirectory(pasta);
        }
    }
}
