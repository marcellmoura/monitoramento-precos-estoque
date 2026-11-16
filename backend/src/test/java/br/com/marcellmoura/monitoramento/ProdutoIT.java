package br.com.marcellmoura.monitoramento;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
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
    @Test void retorna404EValidaIdSemOferecerEdicao() throws Exception {
        assertThat(admin.enviar("GET","/api/produtos/999999",null,false).statusCode()).isEqualTo(404);
        assertThat(admin.enviar("GET","/api/produtos/-1",null,false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("GET","/api/produtos/abc",null,false).statusCode()).isEqualTo(400);
        var p=produto();
        assertThat(admin.enviar("PATCH","/api/produtos/"+p.getId(),Map.of("precoVenda","3.00"),true).statusCode()).isEqualTo(405);
        assertThat(produtos.findById(p.getId()).orElseThrow().getPrecoVenda()).isEqualByComparingTo("4.99");
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
