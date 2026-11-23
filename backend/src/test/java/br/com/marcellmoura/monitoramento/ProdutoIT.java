package br.com.marcellmoura.monitoramento;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.com.marcellmoura.monitoramento.produto.domain.Produto;
import br.com.marcellmoura.monitoramento.produto.domain.Unidade;
import br.com.marcellmoura.monitoramento.produto.persistence.ProdutoRepository;
import br.com.marcellmoura.monitoramento.support.ClienteHttpTeste;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.support.ClienteHttpTeste.json;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(PostgreSqlTestConfiguration.class)
class ProdutoIT {
    private static final String SENHA = "Senha-definitiva-teste-123";
    @Value("${local.server.port}") private int porta;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProdutoRepository produtos;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    private String hash;
    private ClienteHttpTeste admin;
    @BeforeEach void preparar() throws Exception {
        jdbc.update("DELETE FROM historico_acesso_usuario");jdbc.update("DELETE FROM spring_session");
        produtos.deleteAll();usuarios.deleteAll();hash=encoder.encode(SENHA);admin=entrar(Perfil.ADMINISTRADOR);
    }
    @AfterEach void limparAjustes() { jdbc.update("DELETE FROM historico_ajustes_estoque"); }
    private ClienteHttpTeste entrar(Perfil perfil) throws Exception {
        var usuario = new Usuario(perfil.name(),perfil.name().toLowerCase()+"@example.com",hash,perfil);
        usuario.trocarSenha(hash);usuarios.saveAndFlush(usuario);
        var client = new ClienteHttpTeste(porta);
        assertThat(client.enviar("POST","/api/auth/login",Map.of("email",usuario.getEmail(),"senha",SENHA),true).statusCode()).isEqualTo(200);
        return client;
    }
    private Map<String,Object> cadastro() {
        var body = new HashMap<String,Object>();
        body.put("nome","Arroz");body.put("unidade","KG");body.put("precoVenda","12.34");return body;
    }
    private Produto produto() {
        return produtos.saveAndFlush(new Produto("Produto existente","Alimentos",Unidade.UN,new BigDecimal("2.50"),new BigDecimal("4.99")));
    }
    @ParameterizedTest @EnumSource(value=Perfil.class,names={"ADMINISTRADOR","COMPRAS"})
    void cadastraEConsultaDadosPersistidosComCodigoEValoresIniciais(Perfil perfil) throws Exception {
        var client = perfil == Perfil.ADMINISTRADOR ? admin : entrar(perfil);
        var body=cadastro();body.put("nome","  Arroz  ");body.put("categoria"," Alimentos ");body.put("precoCompra","8.25");
        var resposta=client.enviar("POST","/api/produtos",body,true);
        assertThat(resposta.statusCode()).isEqualTo(201);
        var dados=json(resposta);long id=dados.get("id").asLong();
        assertThat(resposta.headers().firstValue("Location")).contains("/api/produtos/"+id);
        assertThat(dados.get("codigo").asText()).isEqualTo("PRD-%06d".formatted(id));
        assertThat(dados.get("nome").asText()).isEqualTo("Arroz");
        assertThat(dados.get("categoria").asText()).isEqualTo("Alimentos");
        assertThat(dados.get("precoVenda").isString()).isTrue();
        assertThat(dados.get("precoVenda").asText()).isEqualTo("12.34");
        assertThat(dados.get("precoCompra").asText()).isEqualTo("8.25");
        assertThat(dados.get("estoqueAtual").asText()).isEqualTo("0.000");
        assertThat(dados.get("estoqueMinimo").asText()).isEqualTo("0.000");
        assertThat(dados.get("ativo").asBoolean()).isTrue();
        assertThat(produtos.findById(id).orElseThrow().getPrecoVenda()).isEqualByComparingTo("12.34");
        assertThat(json(client.enviar("GET","/api/produtos/"+id,null,false))).isEqualTo(dados);
    }
    @ParameterizedTest @EnumSource(Perfil.class)
    void filtraCamposNaListagemEDetalheParaCadaPerfil(Perfil perfil) throws Exception {
        var p=produto();var client=perfil==Perfil.ADMINISTRADOR ? admin : entrar(perfil);
        var detalhe=client.enviar("GET","/api/produtos/"+p.getId(),null,false);
        var lista=client.enviar("GET","/api/produtos",null,false);
        assertThat(detalhe.statusCode()).isEqualTo(200);assertThat(lista.statusCode()).isEqualTo(200);
        var dados=json(detalhe);var item=json(lista).get("itens").get(0);
        assertThat(item).isEqualTo(dados);
        assertThat(dados.has("precoCompra")).isEqualTo(perfil==Perfil.ADMINISTRADOR || perfil==Perfil.COMPRAS);
        assertThat(dados.has("precoVenda")).isEqualTo(perfil!=Perfil.ESTOQUE);
        assertThat(dados.get("nome").asText()).isEqualTo("Produto existente");
        assertThat(dados.get("unidade").asText()).isEqualTo("UN");
        if(perfil==Perfil.ESTOQUE || perfil==Perfil.CONSULTA) assertThat(detalhe.body()).doesNotContain("2.50");
        if(perfil==Perfil.ESTOQUE) assertThat(lista.body()).doesNotContain("4.99");
    }
    @ParameterizedTest @EnumSource(value=Perfil.class,names={"ESTOQUE","CONSULTA"})
    void negaCadastroAosPerfisDeConsulta(Perfil perfil) throws Exception {
        assertThat(entrar(perfil).enviar("POST","/api/produtos",cadastro(),true).statusCode()).isEqualTo(403);
        assertThat(produtos.count()).isZero();
    }
    @Test void exigeSessaoCsrfETrocaDaSenhaTemporaria() throws Exception {
        var visitante=new ClienteHttpTeste(porta);
        assertThat(visitante.enviar("GET","/api/produtos",null,false).statusCode()).isEqualTo(401);
        assertThat(visitante.enviar("POST","/api/produtos",cadastro(),true).statusCode()).isEqualTo(401);
        assertThat(admin.enviar("POST","/api/produtos",cadastro(),false).statusCode()).isEqualTo(403);
        usuarios.saveAndFlush(new Usuario("Temporario","temp@example.com",hash,Perfil.COMPRAS));
        assertThat(visitante.enviar("POST","/api/auth/login",Map.of("email","temp@example.com","senha",SENHA),true).statusCode()).isEqualTo(200);
        assertThat(visitante.enviar("POST","/api/produtos",cadastro(),true).statusCode()).isEqualTo(403);
    }
    @Test void diferenciaCompraAusenteDeZeroEPermiteVendaZero() throws Exception {
        var body=cadastro();body.put("precoVenda","0");
        var ausente=json(admin.enviar("POST","/api/produtos",body,true));
        assertThat(ausente.has("precoCompra")).isTrue();assertThat(ausente.get("precoCompra").isNull()).isTrue();
        assertThat(ausente.get("precoVenda").asText()).isEqualTo("0.00");
        body.put("precoCompra","0");var zero=json(admin.enviar("POST","/api/produtos",body,true));
        assertThat(zero.get("precoCompra").asText()).isEqualTo("0.00");
        assertThat(produtos.count()).isEqualTo(2);
    }
    @ParameterizedTest @EnumSource(Unidade.class)
    void aceitaTodasUnidadesEIgnoraTentativaDeAlterarEstadoInicial(Unidade unidade) throws Exception {
        var body=cadastro();body.put("unidade",unidade.name());body.put("estoqueAtual","-3");body.put("estoqueMinimo","8");body.put("ativo",false);
        var resposta=admin.enviar("POST","/api/produtos",body,true);assertThat(resposta.statusCode()).isEqualTo(201);
        assertThat(json(resposta).get("unidade").asText()).isEqualTo(unidade.name());
        assertThat(json(resposta).get("estoqueAtual").asText()).isEqualTo("0.000");
        assertThat(json(resposta).get("estoqueMinimo").asText()).isEqualTo("0.000");
        assertThat(json(resposta).get("ativo").asBoolean()).isTrue();
    }
    @Test void preservaPrecoMaximoSemConversaoParaPontoFlutuante() throws Exception {
        var body=cadastro();body.put("precoVenda","99999999999999999.99");body.put("precoCompra","9007199254740993.01");
        var resposta=admin.enviar("POST","/api/produtos",body,true);assertThat(resposta.statusCode()).isEqualTo(201);
        var dados=json(resposta);assertThat(dados.get("precoVenda").asText()).isEqualTo("99999999999999999.99");
        assertThat(dados.get("precoCompra").asText()).isEqualTo("9007199254740993.01");
        var consulta=json(admin.enviar("GET","/api/produtos/"+dados.get("id").asLong(),null,false));
        assertThat(consulta).isEqualTo(dados);
    }
    @Test void rejeitaCamposInvalidosSemPersistir() throws Exception {
        for(var entrada : java.util.List.of(
                Map.entry("nome"," "),Map.entry("nome","x".repeat(256)),Map.entry("categoria","x".repeat(101)),
                Map.entry("unidade","CAIXA"),Map.entry("precoVenda","-0.01"),Map.entry("precoCompra","-1"),
                Map.entry("precoVenda","1.001"),Map.entry("precoCompra","1.001"),Map.entry("precoVenda","100000000000000000.00"),
                Map.entry("precoCompra","100000000000000000.00"),Map.entry("precoVenda","NaN"))) {
            var body=cadastro();body.put(entrada.getKey(),entrada.getValue());
            assertThat(admin.enviar("POST","/api/produtos",body,true).statusCode()).isEqualTo(400);
        }
        for(var campo : java.util.List.of("nome","unidade","precoVenda")) {
            var body=cadastro();body.remove(campo);var resposta=admin.enviar("POST","/api/produtos",body,true);
            assertThat(resposta.statusCode()).isEqualTo(400);assertThat(json(resposta).get("campos").has(campo)).isTrue();
        }
        assertThat(produtos.count()).isZero();
    }
    @Test void listaVaziaPaginaOrdenadaELimites() throws Exception {
        var vazia=json(admin.enviar("GET","/api/produtos",null,false));assertThat(vazia.get("itens").size()).isZero();
        assertThat(vazia.get("tamanho").asInt()).isEqualTo(20);
        var primeiro=produto();var segundo=produto();
        var p0=json(admin.enviar("GET","/api/produtos?tamanho=1",null,false));
        var p1=json(admin.enviar("GET","/api/produtos?pagina=1&tamanho=1",null,false));
        assertThat(p0.get("itens").get(0).get("id").asLong()).isEqualTo(primeiro.getId());
        assertThat(p1.get("itens").get(0).get("id").asLong()).isEqualTo(segundo.getId());
        assertThat(p1.get("paginas").asInt()).isEqualTo(2);
        for(var query:java.util.List.of("pagina=-1","tamanho=0","tamanho=101","pagina=a")) {
            assertThat(admin.enviar("GET","/api/produtos?"+query,null,false).statusCode()).isEqualTo(400);
        }
    }
    @Test void retorna404EValidaIdentificador() throws Exception {
        assertThat(admin.enviar("GET","/api/produtos/999999",null,false).statusCode()).isEqualTo(404);
        assertThat(admin.enviar("GET","/api/produtos/-1",null,false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("GET","/api/produtos/abc",null,false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PATCH", "/api/produtos/999999", Map.of("versao", 0, "nome", "Novo"), true)
                .statusCode()).isEqualTo(404);
    }

    @Test void filtraCatalogoAntesDePaginar() throws Exception {
        produto();
        var arroz = produtos.saveAndFlush(new Produto("Arroz integral", null, Unidade.KG, null, BigDecimal.TEN));
        var outro = new Produto("Arroz branco", null, Unidade.KG, null, BigDecimal.TEN);
        outro.alterarEstado(false);
        produtos.saveAndFlush(outro);

        var resposta = admin.enviar("GET", "/api/produtos?busca=ARROZ&unidade=KG&ativo=true&tamanho=1", null, false);
        assertThat(resposta.statusCode()).isEqualTo(200);
        var resultado = json(resposta);
        assertThat(resultado.get("total").asInt()).isEqualTo(1);
        assertThat(resultado.get("itens").get(0).get("id").asLong()).isEqualTo(arroz.getId());

        var porCodigo = json(admin.enviar("GET", "/api/produtos?busca=" + arroz.getCodigo().toLowerCase(java.util.Locale.ROOT), null, false));
        assertThat(porCodigo.get("total").asInt()).isEqualTo(1);
        assertThat(porCodigo.get("itens").get(0).get("id").asLong()).isEqualTo(arroz.getId());
    }

    @Test void administradorEditaCadastroVendaEstadoEEstoqueComHistorico() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        var resposta = admin.enviar("PATCH", caminho, Map.of("versao", 0, "nome", "  Arroz novo  ", "categoria", " ",
                "unidade", "KG", "precoVenda", "0", "ativo", false, "estoqueAtual", "8.125", "estoqueMinimo", "1.25"), true);
        assertThat(resposta.statusCode()).isEqualTo(200);
        var dados = json(resposta);
        assertThat(dados.get("nome").asText()).isEqualTo("Arroz novo");
        assertThat(dados.get("categoria").isNull()).isTrue();
        assertThat(dados.get("ativo").asBoolean()).isFalse();
        assertThat(dados.get("codigo").asText()).isEqualTo(p.getCodigo());
        assertThat(dados.get("unidade").asText()).isEqualTo("KG");
        assertThat(dados.get("precoVenda").asText()).isEqualTo("0.00");
        assertThat(dados.get("precoCompra").asText()).isEqualTo("2.50");
        assertThat(dados.get("estoqueAtual").asText()).isEqualTo("8.125");
        assertThat(dados.get("estoqueMinimo").asText()).isEqualTo("1.250");
        assertThat(dados.get("versao").asLong()).isEqualTo(1);
        assertThat(dados.get("unidadeEditavel").asBoolean()).isFalse();
        assertThat(json(admin.enviar("GET", caminho, null, false))).isEqualTo(dados);
        var ajuste = jdbc.queryForMap("SELECT * FROM historico_ajustes_estoque WHERE produto_id = ?", p.getId());
        assertThat(ajuste).containsEntry("unidade", "KG")
                .containsEntry("ator_id", usuarios.findByEmailIgnoreCase("administrador@example.com").orElseThrow().getId())
                .containsEntry("atual_anterior", new BigDecimal("0.000")).containsEntry("atual_novo", new BigDecimal("8.125"))
                .containsEntry("minimo_anterior", new BigDecimal("0.000")).containsEntry("minimo_novo", new BigDecimal("1.250"));
        assertThat(ajuste.get("ocorrido_em")).isNotNull();
    }

    @Test void aplicaPermissoesPorCampoNaApiSemAlteracaoParcial() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        var compras = entrar(Perfil.COMPRAS);
        var estoque = entrar(Perfil.ESTOQUE);
        var consulta = entrar(Perfil.CONSULTA);
        var proibidos = Map.of("precoVenda", "1", "unidade", "KG", "ativo", false, "estoqueAtual", "5", "estoqueMinimo", "1");
        for (var entrada : proibidos.entrySet()) {
            assertThat(compras.enviar("PATCH", caminho, Map.of("versao", 0, "nome", "Tentativa", entrada.getKey(), entrada.getValue()), true)
                    .statusCode()).isEqualTo(403);
        }
        assertThat(estoque.enviar("PATCH", caminho, Map.of("versao", 0, "estoqueAtual", "6", "nome", "Tentativa"), true)
                .statusCode()).isEqualTo(403);
        assertThat(consulta.enviar("PATCH", caminho, Map.of("versao", 0, "nome", "Tentativa"), true).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 0, "precoCompra", "1"), true).statusCode()).isEqualTo(400);
        assertThat(produtos.findById(p.getId()).orElseThrow().getNome()).isEqualTo("Produto existente");
        assertThat(compras.enviar("PATCH", caminho, Map.of("versao", 0, "nome", "Novo nome", "categoria", "Nova categoria"), true)
                .statusCode()).isEqualTo(200);
        var resposta = estoque.enviar("PATCH", caminho, Map.of("versao", 1, "estoqueAtual", "6"), true);
        assertThat(resposta.statusCode()).isEqualTo(200);
        var dados = json(resposta);
        assertThat(dados.get("estoqueAtual").asText()).isEqualTo("6.000");
        assertThat(dados.get("nome").asText()).isEqualTo("Novo nome");
        assertThat(dados.has("precoCompra")).isFalse();
        assertThat(dados.has("precoVenda")).isFalse();
        assertThat(jdbc.queryForObject("SELECT ator_id FROM historico_ajustes_estoque WHERE produto_id = ?", Long.class, p.getId()))
                .isEqualTo(usuarios.findByEmailIgnoreCase("estoque@example.com").orElseThrow().getId());
        assertThat(estoque.enviar("PATCH", caminho, Map.of("versao", 2, "estoqueAtual", "6.000"), true).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_ajustes_estoque WHERE produto_id = ?", Long.class, p.getId())).isEqualTo(1L);
    }

    @Test void impedeTrocaDeUnidadeComEstoqueOuHistoricoMesmoAposZerar() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        jdbc.update("UPDATE produtos SET estoque_atual = 5 WHERE id = ?", p.getId());
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 0, "unidade", "KG"), true).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 0, "estoqueMinimo", "2"), true).statusCode()).isEqualTo(200);
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 1, "unidade", "KG", "estoqueMinimo", "0"), true)
                .statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 1, "estoqueMinimo", "0", "estoqueAtual", "0"), true)
                .statusCode()).isEqualTo(200);
        var resposta = admin.enviar("PATCH", caminho, Map.of("versao", 2, "unidade", "KG"), true);
        assertThat(resposta.statusCode()).isEqualTo(400);
        assertThat(json(resposta).get("mensagem").asText()).contains("sem histórico ou vínculos");
        assertThat(produtos.findById(p.getId()).orElseThrow().getUnidade()).isEqualTo(Unidade.UN);
    }

    @Test void rejeitaEdicaoInvalidaSemPersistirNemRegistrarAjuste() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        for (var entrada : java.util.List.of(Map.entry("nome", " "), Map.entry("categoria", "x".repeat(101)),
                Map.entry("precoVenda", "1.001"), Map.entry("unidade", "CAIXA"), Map.entry("estoqueAtual", "-1"),
                Map.entry("estoqueAtual", "0.1"), Map.entry("estoqueMinimo", "1.0001"),
                Map.entry("estoqueMinimo", "10000000000000000"), Map.entry("ativo", "false"))) {
            assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 0, entrada.getKey(), entrada.getValue()), true)
                    .statusCode()).isEqualTo(400);
        }
        assertThat(admin.enviar("PATCH", caminho, Map.of("nome", "Sem versão"), true).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PATCH", caminho, Map.of("versao", 0, "nome", "Não salvar", "estoqueAtual", "0.1"), true)
                .statusCode()).isEqualTo(400);
        var dados = produtos.findById(p.getId()).orElseThrow();
        assertThat(dados.getNome()).isEqualTo(p.getNome());
        assertThat(dados.getVersao()).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_ajustes_estoque", Long.class)).isZero();
    }

    @Test void exigeSessaoCsrfERejeitaFormularioDesatualizado() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        var primeira = Map.of("versao", 0, "estoqueAtual", "4");
        assertThat(new ClienteHttpTeste(porta).enviar("PATCH", caminho, primeira, true).statusCode()).isEqualTo(401);
        assertThat(admin.enviar("PATCH", caminho, primeira, false).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PATCH", caminho, primeira, true).statusCode()).isEqualTo(200);
        var conflito = admin.enviar("PATCH", caminho, Map.of("versao", 0, "estoqueAtual", "8"), true);
        assertThat(conflito.statusCode()).isEqualTo(409);
        assertThat(json(conflito).get("codigo").asText()).isEqualTo("PRODUTO_DESATUALIZADO");
        assertThat(produtos.findById(p.getId()).orElseThrow().getEstoqueAtual()).isEqualByComparingTo("4");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_ajustes_estoque WHERE produto_id = ?", Long.class, p.getId())).isEqualTo(1L);
    }
    @Test void edicoesConcorrentesRegistramSomenteOAjusteQueFoiSalvo() throws Exception {
        var p = produto();
        var caminho = "/api/produtos/" + p.getId();
        var estoque = entrar(Perfil.ESTOQUE);
        var inicio = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return admin.enviar("PATCH", caminho, Map.of("versao", 0, "estoqueAtual", "4"), true).statusCode();
            });
            var segunda = executor.submit(() -> {
                inicio.await(5, TimeUnit.SECONDS);
                return estoque.enviar("PATCH", caminho, Map.of("versao", 0, "estoqueAtual", "8"), true).statusCode();
            });
            assertThat(java.util.List.of(primeira.get(15, TimeUnit.SECONDS), segunda.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        var atual = produtos.findById(p.getId()).orElseThrow().getEstoqueAtual();
        assertThat(jdbc.queryForObject("SELECT atual_novo FROM historico_ajustes_estoque WHERE produto_id = ?", BigDecimal.class, p.getId()))
                .isEqualByComparingTo(atual);
    }

    @Test void bancoImpedeNegativosEFracaoParaUnidadeInteira() {
        var p=produto();
        for(var sql:java.util.List.of("UPDATE produtos SET preco_compra=-1 WHERE id=?","UPDATE produtos SET estoque_atual=-1 WHERE id=?",
                "UPDATE produtos SET estoque_minimo=-1 WHERE id=?","UPDATE produtos SET estoque_atual=0.125 WHERE id=?")) {
            assertThatThrownBy(()->jdbc.update(sql,p.getId())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        }
        assertThat(produtos.findById(p.getId()).orElseThrow().getEstoqueAtual()).isEqualByComparingTo("0");
    }
}
