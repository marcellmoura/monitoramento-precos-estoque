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
import org.apache.pdfbox.Loader;
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
import br.com.marcellmoura.monitoramento.importacao.application.ProcessamentoPdf;
import br.com.marcellmoura.monitoramento.importacao.domain.PreviaImportacao;
import br.com.marcellmoura.monitoramento.importacao.persistence.ExtracaoRepository;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import br.com.marcellmoura.monitoramento.support.ClienteHttpTeste;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.support.ClienteHttpTeste.json;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = "app.importacao.processamento-ativo=false")
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
    @Autowired private ProcessamentoPdf processamento;
    @Autowired private ExtracaoRepository extracoes;
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
        jdbc.update("DELETE FROM precos_confirmados");
        jdbc.update("DELETE FROM vinculos_itens_cotacao");
        jdbc.update("DELETE FROM produtos");
        jdbc.update("DELETE FROM revisoes_itens_cotacao");
        jdbc.update("DELETE FROM conferencias_cotacao");
        jdbc.update("UPDATE tentativas_importacao SET execucao_atual = NULL");
        jdbc.update("DELETE FROM execucoes_extracao");
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
    @ParameterizedTest
    @ValueSource(strings = { ORIGINAL, "Fornecedor_2.pdf", "sinteticos/tabela-v1.pdf", "sinteticos/tabela-v2.pdf",
            "sinteticos/fichas-v1.pdf", "sinteticos/fichas-v2.pdf" })
    void extraiOsFormatosENaoDeduzCamposAusentes(String arquivo) throws Exception {
        long id = json(upload(admin, fornecedor, "cotacao-renomeada.pdf", ler(arquivo))).get("id").asLong();
        String rota = ROTA + "/" + id;
        assertThat(admin.enviar("GET", rota + "/previa", null, false).statusCode()).isEqualTo(409);
        processamento.executarFila();
        var dados = json(admin.enviar("GET", rota, null, false));
        assertThat(dados.get("status").asText()).isEqualTo("AGUARDANDO_CONFERENCIA");
        var previa = json(admin.enviar("GET", rota + "/previa", null, false));
        var itens = previa.get("itens");
        assertThat(dados.get("quantidadeItens").asInt()).isEqualTo(itens.size());
        if (arquivo.startsWith("Fornecedor")) {
            boolean kgFardo = ORIGINAL.equals(arquivo);
            assertThat(itens.size()).isEqualTo(2);
            assertThat(previa.get("dataReferencia").isNull()).isTrue();
            assertThat(previa.get("layout").asText()).isEqualTo(kgFardo ? "KG_FARDO" : "ATACADO_VAREJO");
            var primeira = itens.get(0).get("dados");
            assertThat(primeira.get("descricao").asText()).isEqualTo("ACUCAR DE COCO");
            assertThat(primeira.get("quantidade").isNull()).isTrue();
            assertThat(primeira.get("referencia").isNull()).isEqualTo(!kgFardo);
            assertThat(primeira.get("unidade").isNull()).isEqualTo(!kgFardo);
            for (String coluna : kgFardo ? java.util.List.of("Preço KG", "Preço Fardo") : java.util.List.of("ATACADO", "VAREJO")) {
                assertThat(itens.get(0).get("precos").get(coluna).asText()).isEqualTo("32");
                assertThat(itens.get(1).get("precos").get(coluna).asText()).isEqualTo("78");
            }
        } else {
            verificarSintetico(arquivo, previa);
        }
        assertThat(admin.enviar("POST", rota + "/reprocessamento", null, true).statusCode()).isEqualTo(409);
    }

    private void verificarSintetico(String arquivo, tools.jackson.databind.JsonNode previa) throws Exception {
        var esperados = tools.jackson.databind.json.JsonMapper.builder().build().readTree(ler("sinteticos/dados-esperados.json"));
        var itens = previa.get("itens");
        boolean fichas = arquivo.contains("fichas");
        assertThat(previa.get("layout").asText()).isEqualTo(fichas ? "FICHAS" : "TABELA");
        assertThat(previa.get("dataReferencia").asText()).isEqualTo(arquivo.contains("v1") ? "01/11/2026" : "08/11/2026");
        for (var amostra : esperados) {
            if (!arquivo.endsWith(amostra.get("arquivo").asText())) continue;
            var linhas = amostra.get("linhas");
            assertThat(itens.size()).isEqualTo(linhas.size());
            for (int i = 0; i < linhas.size(); i++) {
                var origem = linhas.get(i);
                var item = itens.get(i);
                var dados = item.get("dados");
                assertThat(dados.get("descricao").asText()).isEqualTo(origem.get("descricao").asText());
                assertThat(dados.get("referencia").asText()).isEqualTo(origem.get("codigo").asText());
                assertThat(dados.get("unidade").asText()).isEqualTo(origem.get("unidade").asText());
                assertThat(dados.get("apresentacao").asText()).isEqualTo(origem.get("apresentacao").asText());
                assertThat(dados.get("quantidade").asText()).isEqualTo(origem.get("quantidade").asText());
                assertThat(item.get("precos").get(fichas ? "Valor de compra" : "Preço").asText().replace("R$ ", ""))
                        .isEqualTo(origem.get("preco_brl").asText());
                if ("0,00".equals(origem.get("preco_brl").asText()) || "Não informada".equals(origem.get("apresentacao").asText())) {
                    assertThat(item.get("pendencias").size()).isPositive();
                }
            }
        }
    }

    @Test void confirmaBaseComercialSemAlterarPreviaEProtegeContraRevisaoDesatualizada() throws Exception {
        var rota = processar(fornecedor, ORIGINAL, ler(ORIGINAL));
        var previa = json(admin.enviar("GET", rota + "/previa", null, false));
        var inicial = json(admin.enviar("GET", rota + "/conferencia", null, false));
        assertThat(inicial.get("colunas").toString()).isEqualTo("[\"Preço Fardo\",\"Preço KG\"]");
        assertThat(inicial.get("confirmada").isNull()).isTrue();
        var envio = java.time.Instant.parse(json(admin.enviar("GET", rota, null, false)).get("enviadaEm").asText());
        assertThat(inicial.get("dataSugerida").asText())
                .isEqualTo(envio.atZone(java.time.ZoneId.of("America/Fortaleza")).toLocalDate().toString());

        var invalida = escolha(0, "Preço KG", "UN", "A granel", "2026-11-20");
        assertThat(admin.enviar("PUT", rota + "/conferencia", invalida, true).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Outra coluna", "KG", "A granel", "2026-11-20"), true).statusCode()).isEqualTo(400);
        var dados = escolha(0, "Preço KG", "KG", "A granel", "2026-11-20");
        assertThat(admin.enviar("PUT", rota + "/conferencia", dados, false).statusCode()).isEqualTo(403);
        assertThat(entrar(Perfil.CONSULTA).enviar("PUT", rota + "/conferencia", dados, true).statusCode()).isEqualTo(403);
        var salva = json(admin.enviar("PUT", rota + "/conferencia", dados, true));
        long revisao = salva.get("confirmada").get("id").asLong();
        assertThat(revisao).isPositive();
        assertThat(salva.get("pendenciasComerciais").get(0).size()).isEqualTo(1);
        assertThat(salva.get("pendenciasComerciais").get(1).toString()).contains("unidade", "embalagem");
        assertThat(json(admin.enviar("PUT", rota + "/conferencia", dados, true))).isEqualTo(salva);
        assertThat(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Preço Fardo", "KG", "Fardo", "2026-11-21"), true).statusCode()).isEqualTo(409);
        var alterada = json(admin.enviar("PUT", rota + "/conferencia",
                escolha(revisao, "Preço Fardo", "KG", "Fardo", "2026-11-21"), true));
        assertThat(alterada.get("confirmada").get("id").asLong()).isGreaterThan(revisao);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM conferencias_cotacao", Integer.class)).isEqualTo(2);
        assertThat(json(admin.enviar("GET", rota + "/previa", null, false))).isEqualTo(previa);
        assertThat(json(admin.enviar("GET", rota, null, false)).get("status").asText()).isEqualTo("AGUARDANDO_CONFERENCIA");
    }

    @Test void sugereBaseSomenteComMesmoFornecedorLayoutEItensCompativeis() throws Exception {
        var arquivo = "sinteticos/tabela-v1.pdf";
        var original = ler(arquivo);
        var rota = processar(fornecedor, "tabela.pdf", original);
        var leitura = json(admin.enviar("GET", rota + "/conferencia", null, false));
        assertThat(leitura.get("dataSugerida").asText()).isEqualTo("2026-11-01");
        var dados = escolha(0, "Preço", "KG", "Saco de 1 kg", "2026-11-03");
        var salva = json(admin.enviar("PUT", rota + "/conferencia", dados, true));
        assertThat(salva.get("pendenciasComerciais").get(0).size()).isZero();
        byte[] variacao;
        try (var documento = Loader.loadPDF(original); var saida = new ByteArrayOutputStream()) {
            documento.getDocumentInformation().setTitle("Nova cotação");
            documento.save(saida);
            variacao = saida.toByteArray();
        }
        var compativel = processar(fornecedor, "nova-cotacao.pdf", variacao);
        var sugerida = json(admin.enviar("GET", compativel + "/conferencia", null, false));
        assertThat(sugerida.get("sugestao")).isEqualTo(salva.get("confirmada").get("base"));
        assertThat(sugerida.get("confirmada").isNull()).isTrue();
        assertThat(sugerida.get("dataSugerida").asText()).isEqualTo("2026-11-01");
        var outroLayout = processar(fornecedor, "fichas.pdf", ler("sinteticos/fichas-v1.pdf"));
        var outrosItens = processar(fornecedor, "tabela-v2.pdf", ler("sinteticos/tabela-v2.pdf"));
        var outroFornecedor = processar(salvarFornecedor(), "nova-cotacao.pdf", variacao);
        assertThat(admin.enviar("POST", outroFornecedor + "/confirmacao", null, true).statusCode()).isEqualTo(200);
        processamento.executarFila();
        for (var tentativa : java.util.List.of(outroLayout, outrosItens, outroFornecedor)) {
            assertThat(json(admin.enviar("GET", tentativa + "/conferencia", null, false)).get("sugestao").isNull()).isTrue();
        }
    }

    @Test void revisaItensDaPreviaPreservandoOrigemEHistorico() throws Exception {
        var rota = processar(fornecedor, ORIGINAL, ler(ORIGINAL));
        var previa = json(admin.enviar("GET", rota + "/previa", null, false));
        var revisoes = rota + "/itens";
        var primeira = revisoes + "/1";
        var segunda = revisoes + "/2";
        assertThat(json(admin.enviar("GET", revisoes, null, false)).size()).isZero();
        var incompleta = correcao(0, 1, "KG", "Preço KG", null, null);
        assertThat(admin.enviar("PUT", primeira, incompleta, true).statusCode()).isEqualTo(409);

        var base = json(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Preço KG", "KG", "A granel", "2026-11-21"), true));
        long conferencia = base.get("confirmada").get("id").asLong();
        incompleta = correcao(0, conferencia, "KG", "Preço KG", null, null);
        assertThat(entrar(Perfil.CONSULTA).enviar("PUT", primeira, incompleta, true).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PUT", primeira, incompleta, false).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PUT", revisoes + "/3", incompleta, true).statusCode()).isEqualTo(404);
        var parcial = json(entrar(Perfil.COMPRAS).enviar("PUT", primeira, incompleta, true));
        assertThat(parcial.get("pendencias").size()).isEqualTo(2);
        long versao = parcial.get("id").asLong();

        var corrigida = correcao(versao, conferencia, "KG", "Preço KG", "A granel", "32.00");
        var finalizada = json(admin.enviar("PUT", primeira, corrigida, true));
        assertThat(finalizada.get("pendencias").size()).isZero();
        assertThat(finalizada.get("dados").get("embalagem").asText()).isEqualTo("A granel");
        assertThat(finalizada.get("revisadaEm").asText()).isNotBlank();
        assertThat(finalizada.get("atorId").asLong()).isNotEqualTo(parcial.get("atorId").asLong());
        assertThat(json(admin.enviar("PUT", primeira, corrigida, true))).isEqualTo(finalizada);
        assertThat(admin.enviar("PUT", primeira, incompleta, true).statusCode()).isEqualTo(409);

        assertThat(admin.enviar("PUT", segunda, correcao(0, conferencia, "UN", "Preço KG", "Pote", "78"), true)
                .statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PUT", segunda, correcao(0, conferencia, "UN", "Outra", "Pote", "78"), true)
                .statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PUT", segunda, correcao(0, conferencia, "UN", null, "Pote", "0"), true)
                .statusCode()).isEqualTo(400);
        var manual = json(admin.enviar("PUT", segunda, correcao(0, conferencia, "UN", null, "Pote", "78"), true));
        assertThat(manual.get("pendencias").size()).isZero();
        assertThat(manual.get("dados").get("coluna").isNull()).isTrue();
        assertThat(json(admin.enviar("GET", revisoes, null, false)).size()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM revisoes_itens_cotacao", Integer.class)).isEqualTo(3);
        assertThat(json(admin.enviar("GET", rota + "/previa", null, false))).isEqualTo(previa);

        assertThat(admin.enviar("PUT", rota + "/conferencia",
                escolha(conferencia, "Preço Fardo", "KG", "Saco", "2026-11-22"), true).statusCode()).isEqualTo(200);
        assertThat(admin.enviar("PUT", primeira, corrigida, true).statusCode()).isEqualTo(409);
    }

    @Test void associaProdutoEscolhidoERevalidaMudancaDeEmbalagem() throws Exception {
        var rota = processar(fornecedor, ORIGINAL, ler(ORIGINAL));
        var primeira = rota + "/itens/1";
        var confirmar = primeira + "/vinculo";
        long produto = cadastrarProduto("Açúcar de coco", "KG");
        long homonimo = cadastrarProduto("Açúcar de coco", "KG");
        long unidadeDiferente = cadastrarProduto("Açúcar de coco", "UN");
        var compras = entrar(Perfil.COMPRAS);
        var consulta = entrar(Perfil.CONSULTA);
        var catalogo = ROTA + "/catalogo?unidade=KG&nome=A%C3%A7%C3%BAcar";
        var candidatos = json(compras.enviar("GET", catalogo, null, false)).get("itens");
        assertThat(candidatos.size()).isEqualTo(2);
        assertThat(candidatos.get(0).get("codigo").asText()).isNotEqualTo(candidatos.get(1).get("codigo").asText());
        assertThat(consulta.enviar("GET", catalogo, null, false).statusCode()).isEqualTo(403);

        var base = json(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Preço KG", "KG", "A granel", "2026-11-21"), true));
        long conferencia = base.get("confirmada").get("id").asLong();
        var pendente = Map.of("produto", produto, "conferencia", conferencia, "revisaoItem", 0, "vinculo", 0);
        assertThat(admin.enviar("PUT", confirmar, pendente, true).statusCode()).isEqualTo(409);
        var conferido = json(admin.enviar("PUT", primeira,
                correcao(0, conferencia, "KG", "Preço KG", "A granel", "32"), true));
        long revisao = conferido.get("id").asLong();
        var errado = Map.of("produto", unidadeDiferente, "conferencia", conferencia, "revisaoItem", revisao, "vinculo", 0);
        assertThat(admin.enviar("PUT", confirmar, errado, true).statusCode()).isEqualTo(409);
        var vincular = Map.of("produto", homonimo, "conferencia", conferencia, "revisaoItem", revisao, "vinculo", 0);
        assertThat(consulta.enviar("PUT", confirmar, vincular, true).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PUT", confirmar, vincular, false).statusCode()).isEqualTo(403);
        var escolhido = json(compras.enviar("PUT", confirmar, vincular, true));
        long vinculo = escolhido.get("id").asLong();
        assertThat(escolhido.get("produto").get("id").asLong()).isEqualTo(homonimo);
        assertThat(json(compras.enviar("PUT", confirmar, vincular, true))).isEqualTo(escolhido);
        var estoque = json(admin.enviar("GET", "/api/produtos/" + homonimo, null, false));
        assertThat(estoque.get("unidadeEditavel").asBoolean()).isFalse();
        assertThat(estoque.get("precoVenda").asText()).isEqualTo("42.00");
        assertThat(estoque.get("estoqueAtual").asText()).isEqualTo("0.000");

        var ajustada = json(admin.enviar("PUT", primeira,
                correcao(revisao, conferencia, "KG", "Preço KG", "Saco", "32"), true));
        long novaRevisao = ajustada.get("id").asLong();
        var situacao = json(admin.enviar("GET", rota + "/vinculos", null, false)).get(0);
        assertThat(situacao.get("revalidar").asBoolean()).isTrue();
        assertThat(situacao.get("vinculo").get("id").asLong()).isEqualTo(vinculo);
        assertThat(admin.enviar("PUT", confirmar, vincular, true).statusCode()).isEqualTo(409);
        var novaEscolha = Map.of("produto", produto, "conferencia", conferencia, "revisaoItem", novaRevisao, "vinculo", vinculo);
        assertThat(admin.enviar("PUT", confirmar, novaEscolha, true).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vinculos_itens_cotacao", Integer.class)).isEqualTo(2);
    }

    @Test void distingueReferenciaRepetidaPorFornecedorEApresentacao() throws Exception {
        long produto = cadastrarProduto("Hibisco", "KG");
        var arquivo = ler("sinteticos/tabela-v1.pdf");
        var primeira = processar(fornecedor, "tabela.pdf", arquivo);
        var outroFornecedor = salvarFornecedor();
        var segunda = processar(outroFornecedor, "tabela.pdf", arquivo);
        assertThat(admin.enviar("POST", segunda + "/confirmacao", null, true).statusCode()).isEqualTo(200);
        processamento.executarFila();
        long base1 = selecionarBaseTabela(primeira);
        long base2 = selecionarBaseTabela(segunda);
        var corrigida = correcao(0, base2, "KG", "Preço", "Saco de 5 kg", "33");
        corrigida.put("referencia", "H01");
        var revisao = json(admin.enviar("PUT", segunda + "/itens/1", corrigida, true)).get("id").asLong();
        assertThat(admin.enviar("PUT", primeira + "/itens/1/vinculo",
                Map.of("produto", produto, "conferencia", base1, "revisaoItem", 0, "vinculo", 0), true).statusCode())
                .isEqualTo(200);
        assertThat(admin.enviar("PUT", segunda + "/itens/1/vinculo",
                Map.of("produto", produto, "conferencia", base2, "revisaoItem", revisao, "vinculo", 0), true).statusCode())
                .isEqualTo(200);
        var linhas = jdbc.queryForList("""
                SELECT fornecedor_id, embalagem FROM vinculos_itens_cotacao
                WHERE referencia = 'H01' ORDER BY id
                """);
        assertThat(linhas).containsExactly(Map.of("fornecedor_id", fornecedor.getId(), "embalagem", "Saco de 1 kg"),
                Map.of("fornecedor_id", outroFornecedor.getId(), "embalagem", "Saco de 5 kg"));
    }

    @Test void reaproveitaProdutoDaMesmaIdentidadeMesmoAposCotacaoSemOItem() throws Exception {
        long produto = cadastrarProduto("Hibisco", "KG");
        var origem = processar(fornecedor, "antiga.pdf", ler("sinteticos/tabela-v1.pdf"));
        long base = selecionarBaseTabela(origem);
        long vinculo = gravarVinculo(origem, produto, base, 0);
        processar(fornecedor, "outro-layout.pdf", ler("sinteticos/fichas-v1.pdf"));

        var nova = processar(fornecedor, "nova.pdf", ler("sinteticos/tabela-v2.pdf"));
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        long baseNova = selecionarBaseTabela(nova);
        var primeira = vinculos(nova).get(0);
        var reutilizado = primeira.get("reaproveitado");
        assertThat(reutilizado.get("produto").get("id").asLong()).isEqualTo(produto);
        assertThat(reutilizado.get("vinculoOrigem").asLong()).isEqualTo(vinculo);
        assertThat(reutilizado.get("tentativaOrigem").asLong()).isEqualTo(Long.parseLong(origem.substring(ROTA.length() + 1)));
        assertThat(primeira.get("vinculo").isNull()).isTrue();
        assertThat(vinculos(nova).get(1).get("reaproveitado").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM vinculos_itens_cotacao", Integer.class)).isEqualTo(1);

        long revisao = revisarHibisco(nova, baseNova, 0, "H01", " HIBÍSCO ", "KG", "Saco de 1 KG", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").get("vinculoOrigem").asLong()).isEqualTo(vinculo);
        long novo = gravarVinculo(nova, produto, baseNova, revisao);
        assertThat(vinculos(nova).get(0).get("vinculo").get("id").asLong()).isEqualTo(novo);
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
    }

    @Test void impedeReaproveitamentoIncertoERevalidaQuandoDadosMudam() throws Exception {
        long produto = cadastrarProduto("Hibisco", "KG");
        var origem = processar(fornecedor, "antiga.pdf", ler("sinteticos/tabela-v1.pdf"));
        long baseOrigem = selecionarBaseTabela(origem);
        gravarVinculo(origem, produto, baseOrigem, 0);

        var rotaOutra = processar(salvarFornecedor(), "outro.pdf", varianteTabela("Outro fornecedor"));
        selecionarBaseTabela(rotaOutra);
        assertThat(vinculos(rotaOutra).get(0).get("reaproveitado").isNull()).isTrue();

        var nova = processar(fornecedor, "nova.pdf", ler("sinteticos/tabela-v2.pdf"));
        long baseNova = selecionarBaseTabela(nova);
        long revisao = revisarHibisco(nova, baseNova, 0, "H01", "Hibisco", "KG", "Saco de 5 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        revisao = revisarHibisco(nova, baseNova, revisao, "H01", "Hibisco", "UN", "Saco de 1 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        revisao = revisarHibisco(nova, baseNova, revisao, "H01", "Hibisco", "KG", "Saco de 1 kg", null);
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        revisao = revisarHibisco(nova, baseNova, revisao, null, "Hibisco", "KG", "Saco de 1 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        revisao = revisarHibisco(nova, baseNova, revisao, "H01", "Outro item", "KG", "Saco de 1 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        revisarHibisco(nova, baseNova, revisao, "H01", "Hibisco", "KG", "Saco de 1 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isObject()).isTrue();

        revisarHibisco(origem, baseOrigem, 0, "H01", "Hibisco", "KG", "Saco de 10 kg", "Preço");
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
    }

    @Test void bloqueiaConflitoDeProdutosEProdutoInativo() throws Exception {
        long produto = cadastrarProduto("Hibisco", "KG");
        long outro = cadastrarProduto("Hibisco", "KG");
        var origem = processar(fornecedor, "antiga.pdf", ler("sinteticos/tabela-v1.pdf"));
        long baseOrigem = selecionarBaseTabela(origem);
        gravarVinculo(origem, produto, baseOrigem, 0);
        var intermediaria = processar(fornecedor, "intermediaria.pdf", varianteTabela("Segunda entrega"));
        long baseInt = selecionarBaseTabela(intermediaria);
        var nova = processar(fornecedor, "nova.pdf", ler("sinteticos/tabela-v2.pdf"));
        selecionarBaseTabela(nova);
        assertThat(vinculos(nova).get(0).get("reaproveitado").isObject()).isTrue();
        jdbc.update("UPDATE produtos SET ativo = false WHERE id = ?", produto);
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
        jdbc.update("UPDATE produtos SET ativo = true WHERE id = ?", produto);
        gravarVinculo(intermediaria, outro, baseInt, 0);
        assertThat(vinculos(nova).get(0).get("reaproveitado").isNull()).isTrue();
    }

    @Test void confirmaParcialmenteECorrigeSomenteOsItensRestantes() throws Exception {
        var rota = processar(fornecedor, ORIGINAL, ler(ORIGINAL));
        var confirmar = rota + "/confirmar-itens";
        var primeira = rota + "/itens/1";
        var segunda = rota + "/itens/2";
        assertThat(json(admin.enviar("GET", rota + "/precos", null, false)).get("itens").get(0)
                .get("motivos").toString()).contains("Salve a conferência comercial");
        long produto = cadastrarProduto("Açúcar", "KG");
        long adocante = cadastrarProduto("Adoçante", "UN");
        long base = json(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Preço KG", "KG", "A granel", "2026-10-31"), true)).get("confirmada").get("id").asLong();
        long revisao = json(admin.enviar("PUT", primeira,
                correcao(0, base, "KG", "Preço KG", "A granel", "32"), true)).get("id").asLong();
        long vinculo = gravarVinculo(rota, produto, base, revisao);

        assertThat(admin.enviar("POST", confirmar, null, false).statusCode()).isEqualTo(403);
        assertThat(entrar(Perfil.ESTOQUE).enviar("POST", confirmar, null, true).statusCode()).isEqualTo(403);
        var resultado = json(entrar(Perfil.COMPRAS).enviar("POST", confirmar, null, true)).get("itens");
        var confirmado = resultado.get(0).get("confirmado");
        long precoId = confirmado.get("id").asLong();
        assertThat(confirmado.get("vinculoOrigem").asLong()).isEqualTo(vinculo);
        assertThat(confirmado.get("dataComercial").asText()).isEqualTo("2026-10-31");
        assertThat(resultado.get(1).get("estado").asText()).isEqualTo("PENDENTE");
        assertThat(statusDaTentativa(rota)).isEqualTo("CONCLUIDA_COM_PENDENCIAS");
        assertThat(json(admin.enviar("POST", confirmar, null, true)).get("itens").get(0).get("confirmado")
                .get("id").asLong()).isEqualTo(precoId);

        assertThat(admin.enviar("PUT", primeira, correcao(revisao, base, "KG", "Preço KG", "A granel", "40"), true)
                .statusCode()).isEqualTo(409);
        assertThat(admin.enviar("PUT", primeira + "/vinculo",
                Map.of("produto", produto, "conferencia", base, "revisaoItem", revisao, "vinculo", vinculo), true)
                .statusCode()).isEqualTo(409);
        assertThat(admin.enviar("PUT", rota + "/conferencia", escolha(base, "Preço KG", "KG", "A granel", "2026-11-01"), true)
                .statusCode()).isEqualTo(409);
        assertThat(admin.enviar("PUT", segunda, correcao(0, base, "UN", null, "Pote", "0"), true).statusCode()).isEqualTo(400);
        long novaRevisao = json(admin.enviar("PUT", segunda, correcao(0, base, "UN", null, "Pote", "78"), true))
                .get("id").asLong();
        assertThat(admin.enviar("PUT", segunda + "/vinculo",
                Map.of("produto", adocante, "conferencia", base, "revisaoItem", novaRevisao, "vinculo", 0), true)
                .statusCode()).isEqualTo(200);
        var completo = json(admin.enviar("POST", confirmar, null, true)).get("itens");
        assertThat(completo.get(0).get("confirmado").get("id").asLong()).isEqualTo(precoId);
        assertThat(completo.get(1).get("estado").asText()).isEqualTo("CONFIRMADO");
        assertThat(statusDaTentativa(rota)).isEqualTo("CONCLUIDA");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM precos_confirmados", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT preco_venda FROM produtos WHERE id = ?", java.math.BigDecimal.class, produto))
                .isEqualByComparingTo("42.00");
    }

    @Test void preservaAOrigemReaproveitadaEDeixaConflitoNaMesmaDataPendente() throws Exception {
        long produto = cadastrarProduto("Hibisco", "KG");
        var origem = processar(fornecedor, "antiga.pdf", ler("sinteticos/tabela-v1.pdf"));
        long base = selecionarBaseTabela(origem);
        long vinculo = gravarVinculo(origem, produto, base, 0);
        assertThat(admin.enviar("POST", origem + "/confirmar-itens", null, true).statusCode()).isEqualTo(200);

        var conflito = processar(fornecedor, "mesma-data.pdf", varianteTabela("Outra cotação da mesma data"));
        long baseRepetida = selecionarBaseTabela(conflito);
        var resultado = json(admin.enviar("POST", conflito + "/confirmar-itens", null, true)).get("itens");
        assertThat(resultado.get(0).get("motivos").toString()).contains("Já há outro preço");
        assertThat(statusDaTentativa(conflito)).isEqualTo("AGUARDANDO_CONFERENCIA");

        assertThat(admin.enviar("PUT", conflito + "/conferencia",
                escolha(baseRepetida, "Preço", "KG", "Saco de 1 kg", "2026-10-31"), true).statusCode()).isEqualTo(200);
        var retroativo = json(admin.enviar("POST", conflito + "/confirmar-itens", null, true)).get("itens").get(0).get("confirmado");
        assertThat(retroativo.get("vinculoOrigem").asLong()).isEqualTo(vinculo);
        assertThat(retroativo.get("dataComercial").asText()).isEqualTo("2026-10-31");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM precos_confirmados", Integer.class)).isEqualTo(2);
    }

    @Test void desfazTudoNaFalhaEConfirmaSemDuplicacaoEmSolicitacoesSimultaneas() throws Exception {
        var rota = processar(fornecedor, "misto.pdf", ler("sinteticos/tabela-v1.pdf"));
        long base = selecionarBaseTabela(rota);
        gravarVinculo(rota, cadastrarProduto("Hibisco", "KG"), base, 0);
        long alecrim = cadastrarProduto("Alecrim", "KG");
        assertThat(admin.enviar("PUT", rota + "/itens/2/vinculo",
                Map.of("produto", alecrim, "conferencia", base, "revisaoItem", 0, "vinculo", 0), true).statusCode()).isEqualTo(200);
        var confirmar = rota + "/confirmar-itens";
        jdbc.execute("ALTER TABLE precos_confirmados ADD CONSTRAINT falha_de_teste CHECK (linha <> 2)");
        try {
            assertThat(admin.enviar("POST", confirmar, null, true).statusCode()).isEqualTo(500);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM precos_confirmados", Integer.class)).isZero();
            assertThat(statusDaTentativa(rota)).isEqualTo("AGUARDANDO_CONFERENCIA");
        } finally {
            jdbc.execute("ALTER TABLE precos_confirmados DROP CONSTRAINT falha_de_teste");
        }
        var outro = entrar(Perfil.COMPRAS);
        var partida = new CyclicBarrier(2);
        try (var executores = Executors.newFixedThreadPool(2)) {
            var primeiro = executores.submit(() -> {
                partida.await(5, TimeUnit.SECONDS);
                return admin.enviar("POST", confirmar, null, true).statusCode();
            });
            var segundo = executores.submit(() -> {
                partida.await(5, TimeUnit.SECONDS);
                return outro.enviar("POST", confirmar, null, true).statusCode();
            });
            assertThat(primeiro.get(10, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(segundo.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM precos_confirmados", Integer.class)).isEqualTo(2);
        assertThat(statusDaTentativa(rota)).isEqualTo("CONCLUIDA_COM_PENDENCIAS");
    }

    private String statusDaTentativa(String rota) throws Exception {
        return json(admin.enviar("GET", rota, null, false)).get("status").asText();
    }

    private byte[] varianteTabela(String titulo) throws Exception {
        try (var doc = Loader.loadPDF(ler("sinteticos/tabela-v1.pdf")); var saida = new ByteArrayOutputStream()) {
            doc.getDocumentInformation().setTitle(titulo);
            doc.save(saida);
            return saida.toByteArray();
        }
    }

    private tools.jackson.databind.JsonNode vinculos(String rota) throws Exception {
        return json(admin.enviar("GET", rota + "/vinculos", null, false));
    }

    private long gravarVinculo(String rota, long produto, long conferencia, long revisao) throws Exception {
        var resposta = admin.enviar("PUT", rota + "/itens/1/vinculo",
                Map.of("produto", produto, "conferencia", conferencia, "revisaoItem", revisao, "vinculo", 0), true);
        assertThat(resposta.statusCode()).isEqualTo(200);
        return json(resposta).get("id").asLong();
    }

    private long revisarHibisco(String rota, long base, long revisao, String referencia, String descricao,
            String unidade, String embalagem, String coluna) throws Exception {
        var dados = correcao(revisao, base, unidade, coluna, embalagem, "35");
        dados.put("referencia", referencia);
        dados.put("descricao", descricao);
        var resposta = admin.enviar("PUT", rota + "/itens/1", dados, true);
        assertThat(resposta.statusCode()).isEqualTo(200);
        return json(resposta).get("id").asLong();
    }

    private long selecionarBaseTabela(String rota) throws Exception {
        var base = json(admin.enviar("PUT", rota + "/conferencia",
                escolha(0, "Preço", "KG", "Saco de 1 kg", "2026-11-21"), true));
        return base.get("confirmada").get("id").asLong();
    }

    private long cadastrarProduto(String nome, String unidade) throws Exception {
        var resposta = admin.enviar("POST", "/api/produtos", Map.of("nome", nome, "unidade", unidade, "precoVenda", "42"), true);
        assertThat(resposta.statusCode()).isEqualTo(201);
        return json(resposta).get("id").asLong();
    }

    private Map<String, Object> correcao(long revisao, long conferencia, String unidade, String coluna, String embalagem, String preco) {
        var dados = new java.util.HashMap<String, Object>();
        dados.put("revisao", revisao);
        dados.put("conferencia", conferencia);
        dados.put("descricao", "Item conferido");
        dados.put("referencia", "1076");
        dados.put("quantidade", "1.5");
        dados.put("unidade", unidade);
        dados.put("coluna", coluna);
        dados.put("embalagem", embalagem);
        dados.put("preco", preco);
        dados.put("motivo", "Conferência com a cotação");
        return dados;
    }

    private String processar(Fornecedor origem, String nome, byte[] pdf) throws Exception {
        long id = json(upload(admin, origem, nome, pdf)).get("id").asLong();
        processamento.executarFila();
        return ROTA + "/" + id;
    }

    private Map<String, Object> escolha(long revisao, String coluna, String unidade, String embalagem, String data) {
        return Map.of("revisao", revisao, "coluna", coluna, "unidade", unidade,
                "embalagem", embalagem, "dataComercial", data);
    }

    @Test void reprocessaFalhaDeLeituraSemNovoEnvioNemDuplicacao() throws Exception {
        byte[] original = ler(ORIGINAL);
        long id = json(upload(admin, fornecedor, ORIGINAL, original)).get("id").asLong();
        Path pdf;
        try (var arquivos = Files.list(pasta)) { pdf = arquivos.findFirst().orElseThrow(); }
        Files.delete(pdf);
        processamento.executarFila();
        String rota = ROTA + "/" + id;
        var falha = json(admin.enviar("GET", rota, null, false));
        assertThat(falha.get("status").asText()).isEqualTo("FALHA_EXTRACAO");
        assertThat(falha.get("motivo").asText()).contains("ler o PDF");
        assertThat(entrar(Perfil.CONSULTA).enviar("POST", rota + "/reprocessamento", null, true).statusCode()).isEqualTo(403);
        Files.write(pdf, original);
        assertThat(json(admin.enviar("POST", rota + "/reprocessamento", null, true)).get("status").asText()).isEqualTo("PENDENTE");
        processamento.executarFila();
        processamento.executarFila();
        assertThat(json(admin.enviar("GET", rota + "/previa", null, false)).get("itens").size()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM execucoes_extracao WHERE tentativa_id = ?", Long.class, id)).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tentativas_importacao", Long.class)).isEqualTo(1L);
    }

    @Test void expiraExecucaoEImpedePublicacaoAtrasada() throws Exception {
        long id = json(upload(admin, fornecedor, ORIGINAL, ler(ORIGINAL))).get("id").asLong();
        var trabalho = extracoes.reivindicar(60).orElseThrow();
        assertThat(trabalho.tentativa()).isEqualTo(id);
        assertThat(extracoes.reivindicar(60)).isEmpty();
        jdbc.update("UPDATE execucoes_extracao SET expira_em = clock_timestamp() - INTERVAL '1 second' WHERE id = ?", trabalho.execucao());
        var previaAtrasada = new PreviaImportacao("ATRASADA", null, java.util.List.of());
        assertThat(extracoes.concluir(trabalho, previaAtrasada)).isFalse();
        processamento.executarFila();
        String rota = ROTA + "/" + id;
        var falha = json(admin.enviar("GET", rota, null, false));
        assertThat(falha.get("status").asText()).isEqualTo("FALHA_EXTRACAO");
        assertThat(falha.get("motivo").asText()).contains("interrompido");
        assertThat(admin.enviar("POST", rota + "/reprocessamento", null, true).statusCode()).isEqualTo(200);
        processamento.executarFila();
        assertThat(extracoes.concluir(trabalho, previaAtrasada)).isFalse();
        assertThat(json(admin.enviar("GET", rota + "/previa", null, false)).get("layout").asText()).isEqualTo("KG_FARDO");
    }

    @Test void layoutDesconhecidoMantemFalhaExplicita() throws Exception {
        byte[] conteudo;
        try (var documento = new PDDocument(); var saida = new ByteArrayOutputStream()) {
            var pagina = new PDPage();
            documento.addPage(pagina);
            try (var escrita = new PDPageContentStream(documento, pagina)) {
                escrita.beginText();
                escrita.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                escrita.newLineAtOffset(50, 700);
                escrita.showText("Cotacao textual sem tabela de produtos");
                escrita.endText();
            }
            documento.save(saida);
            conteudo = saida.toByteArray();
        }
        long id = json(upload(admin, fornecedor, "formato.pdf", conteudo)).get("id").asLong();
        processamento.executarFila();
        var falha = json(admin.enviar("GET", ROTA + "/" + id, null, false));
        assertThat(falha.get("status").asText()).isEqualTo("FALHA_EXTRACAO");
        assertThat(falha.get("motivo").asText()).contains("Layout não reconhecido");
    }

}
