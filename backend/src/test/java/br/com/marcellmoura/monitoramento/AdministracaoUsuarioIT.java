package br.com.marcellmoura.monitoramento;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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
import br.com.marcellmoura.monitoramento.support.ClienteHttpTeste;
import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.application.AdministracaoUsuarioService;
import br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.support.ClienteHttpTeste.json;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@Import(PostgreSqlTestConfiguration.class)
class AdministracaoUsuarioIT {
    private static final String SENHA = "Senha-definitiva-teste-123";
    private static final String TEMPORARIA = "Senha-temporaria-teste-456";
    @Value("${local.server.port}") private int porta;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AdministracaoUsuarioService service;
    private ClienteHttpTeste admin;
    private Usuario administrador;
    private String hash;

    @BeforeEach void preparar() throws Exception {
        jdbc.update("DELETE FROM historico_acesso_usuario");
        jdbc.update("DELETE FROM spring_session");
        usuarios.deleteAll();
        hash = encoder.encode(SENHA);
        administrador = criar("admin", Perfil.ADMINISTRADOR);
        admin = entrar(administrador);
    }
    @AfterEach void limparHistorico() { jdbc.update("DELETE FROM historico_acesso_usuario"); }
    private Usuario criar(String nome, Perfil perfil) {
        var usuario = new Usuario(nome, nome+"@example.com",hash,perfil);
        usuario.trocarSenha(hash);
        return usuarios.saveAndFlush(usuario);
    }
    private ClienteHttpTeste entrar(Usuario usuario) throws Exception {
        var client = new ClienteHttpTeste(porta);
        assertThat(client.enviar("POST","/api/auth/login",Map.of("email",usuario.getEmail(),"senha",SENHA),true).statusCode()).isEqualTo(200);
        return client;
    }
    private Map<String,Object> cadastro(String email, Perfil perfil) {
        return Map.of("nome","Conta de teste","email",email,"perfil",perfil.name(),"senhaTemporaria",TEMPORARIA);
    }
    private String acesso(Usuario usuario) { return "/api/usuarios/"+usuario.getId()+"/acesso"; }
    private String reset(Usuario usuario) { return "/api/usuarios/"+usuario.getId()+"/redefinir-senha"; }

    @Test void exigeSessaoECsrfNasOperacoesAdministrativas() throws Exception {
        var visitante = new ClienteHttpTeste(porta);
        assertThat(visitante.enviar("GET","/api/usuarios",null,false).statusCode()).isEqualTo(401);
        assertThat(admin.enviar("POST","/api/usuarios",cadastro("novo@example.com",Perfil.CONSULTA),false).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("PATCH",acesso(administrador),Map.of("perfil","ADMINISTRADOR","ativo",true),false).statusCode()).isEqualTo(403);
        assertThat(admin.enviar("POST",reset(administrador),Map.of("senhaTemporaria",TEMPORARIA),false).statusCode()).isEqualTo(403);
    }
    @ParameterizedTest @EnumSource(value=Perfil.class, names={"COMPRAS","ESTOQUE","CONSULTA"})
    void negaTodasOperacoesAOutrosPerfis(Perfil perfil) throws Exception {
        var usuario=criar("operador",perfil); var client=entrar(usuario);
        assertThat(client.enviar("GET","/api/usuarios",null,false).statusCode()).isEqualTo(403);
        assertThat(client.enviar("GET","/api/usuarios/"+administrador.getId(),null,false).statusCode()).isEqualTo(403);
        assertThat(client.enviar("POST","/api/usuarios",cadastro("novo@example.com",Perfil.ADMINISTRADOR),true).statusCode()).isEqualTo(403);
        assertThat(client.enviar("PATCH",acesso(usuario),Map.of("perfil","ADMINISTRADOR","ativo",true),true).statusCode()).isEqualTo(403);
        assertThat(client.enviar("POST",reset(administrador),Map.of("senhaTemporaria",TEMPORARIA),true).statusCode()).isEqualTo(403);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().getPerfil()).isEqualTo(perfil);
    }
    @ParameterizedTest @EnumSource(Perfil.class)
    void cadastraPerfilComHashETrocaObrigatoriaSemRetornarCredencial(Perfil perfil) throws Exception {
        var response=admin.enviar("POST","/api/usuarios",cadastro(" NOVO@EXAMPLE.COM ",perfil),true);
        assertThat(response.statusCode()).isEqualTo(201);
        var id=json(response).get("id").asLong();
        assertThat(response.headers().firstValue("Location")).contains("/api/usuarios/"+id);
        assertThat(response.body()).doesNotContain(TEMPORARIA,"senhaHash","senhaTemporaria","password");
        var usuario=usuarios.findById(id).orElseThrow();
        assertThat(usuario.isTrocaSenhaObrigatoria()).isTrue();
        assertThat(usuario.isAtivo()).isTrue();
        assertThat(usuario.getEmail()).isEqualTo("novo@example.com");
        assertThat(usuario.getSenhaHash()).startsWith("$argon2id$");
        assertThat(encoder.matches(TEMPORARIA,usuario.getSenhaHash())).isTrue();
        var consulta=admin.enviar("GET","/api/usuarios/"+id,null,false);
        assertThat(consulta.statusCode()).isEqualTo(200);
        assertThat(json(consulta).get("perfil").asText()).isEqualTo(perfil.name());
        assertThat(consulta.body()).doesNotContain(usuario.getSenhaHash(),TEMPORARIA);
    }
    @Test void validaCadastroDuplicidadeCamposPerfilESenha() throws Exception {
        var duplicado=admin.enviar("POST","/api/usuarios",cadastro(" ADMIN@EXAMPLE.COM ",Perfil.CONSULTA),true);
        assertThat(duplicado.statusCode()).isEqualTo(409);
        assertThat(json(duplicado).get("codigo").asText()).isEqualTo("EMAIL_DUPLICADO");
        for (var body : java.util.List.of(
                Map.of("nome"," ","email","a@example.com","perfil","CONSULTA","senhaTemporaria",TEMPORARIA),
                Map.of("nome","Teste","email","invalido","perfil","CONSULTA","senhaTemporaria",TEMPORARIA),
                Map.of("nome","Teste","email","a@example.com","perfil","ROOT","senhaTemporaria",TEMPORARIA),
                Map.of("nome","Teste","email","a@example.com","perfil","CONSULTA","senhaTemporaria","curta"),
                Map.of("email","a@example.com"))) {
            assertThat(admin.enviar("POST","/api/usuarios",body,true).statusCode()).isEqualTo(400);
        }
        var camposInvalidos=admin.enviar("POST","/api/usuarios",Map.of("nome"," ","email","invalido","perfil","CONSULTA","senhaTemporaria","curta"),true);
        assertThat(json(camposInvalidos).get("campos").has("nome")).isTrue();
        assertThat(json(camposInvalidos).get("campos").has("email")).isTrue();
        assertThat(json(camposInvalidos).get("campos").has("senhaTemporaria")).isTrue();
        assertThat(camposInvalidos.body()).doesNotContain("curta");
        assertThat(usuarios.count()).isEqualTo(1);
    }
    @Test void paginaOrdenaEValidaLimitesDaListagem() throws Exception {
        criar("primeiro",Perfil.CONSULTA);criar("segundo",Perfil.ESTOQUE);
        var padrao=admin.enviar("GET","/api/usuarios",null,false);
        assertThat(padrao.statusCode()).isEqualTo(200);
        assertThat(json(padrao).get("tamanho").asInt()).isEqualTo(20);
        assertThat(json(padrao).get("total").asLong()).isEqualTo(3);
        var segunda=json(admin.enviar("GET","/api/usuarios?pagina=1&tamanho=2",null,false));
        assertThat(segunda.get("itens").size()).isEqualTo(1);
        assertThat(segunda.get("pagina").asInt()).isEqualTo(1);
        assertThat(segunda.get("paginas").asInt()).isEqualTo(2);
        assertThat(padrao.body()).doesNotContain(hash,SENHA);
        for(var query : java.util.List.of("pagina=-1","tamanho=0","tamanho=101","pagina=invalida")) {
            assertThat(admin.enviar("GET","/api/usuarios?"+query,null,false).statusCode()).isEqualTo(400);
        }
    }
    @Test void retorna404ParaContaAusenteE400ParaIdentificadorOuAcessoInvalidos() throws Exception {
        assertThat(admin.enviar("GET","/api/usuarios/999999",null,false).statusCode()).isEqualTo(404);
        assertThat(admin.enviar("GET","/api/usuarios/-1",null,false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("GET","/api/usuarios/abc",null,false).statusCode()).isEqualTo(400);
        assertThat(admin.enviar("PATCH","/api/usuarios/999999/acesso",Map.of("perfil","CONSULTA","ativo",true),true).statusCode()).isEqualTo(404);
        assertThat(admin.enviar("POST","/api/usuarios/999999/redefinir-senha",Map.of("senhaTemporaria",TEMPORARIA),true).statusCode()).isEqualTo(404);
        assertThat(admin.enviar("PATCH",acesso(administrador),Map.of("perfil","CONSULTA"),true).statusCode()).isEqualTo(400);
    }
    @Test void protegeUltimoAdministradorContraDesativacaoERebaixamento() throws Exception {
        for(var body : java.util.List.of(Map.of("perfil","ADMINISTRADOR","ativo",false), Map.of("perfil","CONSULTA","ativo",true))) {
            var response=admin.enviar("PATCH",acesso(administrador),body,true);
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(json(response).get("codigo").asText()).isEqualTo("ULTIMO_ADMINISTRADOR");
        }
        assertThat(admin.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(200);
        assertThat(usuarios.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)).isEqualTo(1);
    }
    @Test void alterarMesmosValoresPreservaSessao() throws Exception {
        assertThat(admin.enviar("PATCH",acesso(administrador),Map.of("perfil","ADMINISTRADOR","ativo",true),true).statusCode()).isEqualTo(200);
        assertThat(admin.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(200);
        assertThat(usuarios.findById(administrador.getId()).orElseThrow().getVersaoCredencial()).isEqualTo(1);
    }
    @Test void mudaPerfilDesativaEReativaRevogandoSessaoDoAfetado() throws Exception {
        var usuario=criar("operador",Perfil.COMPRAS);var sessao=entrar(usuario);
        assertThat(admin.enviar("PATCH",acesso(usuario),Map.of("perfil","ESTOQUE","ativo",true),true).statusCode()).isEqualTo(200);
        assertThat(sessao.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        sessao=entrar(usuarios.findById(usuario.getId()).orElseThrow());
        assertThat(json(sessao.enviar("GET","/api/auth/me",null,false)).get("perfil").asText()).isEqualTo("ESTOQUE");
        assertThat(admin.enviar("PATCH",acesso(usuario),Map.of("perfil","ESTOQUE","ativo",false),true).statusCode()).isEqualTo(200);
        assertThat(sessao.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        assertThat(sessao.enviar("POST","/api/auth/login",Map.of("email",usuario.getEmail(),"senha",SENHA),true).statusCode()).isEqualTo(401);
        assertThat(admin.enviar("PATCH",acesso(usuario),Map.of("perfil","ESTOQUE","ativo",true),true).statusCode()).isEqualTo(200);
        entrar(usuarios.findById(usuario.getId()).orElseThrow());
    }
    @Test void permiteMudarProprioPerfilComOutroAdministradorEEncerraAcesso() throws Exception {
        criar("outroadmin",Perfil.ADMINISTRADOR);
        assertThat(admin.enviar("PATCH",acesso(administrador),Map.of("perfil","CONSULTA","ativo",true),true).statusCode()).isEqualTo(200);
        assertThat(admin.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
    }
    @Test void redefineSenhaRevogaSessoesRegistraAtorEExigeTroca() throws Exception {
        var usuario=criar("operador",Perfil.COMPRAS);var sessao=entrar(usuario);var outra=entrar(usuario);
        assertThat(admin.enviar("POST",reset(usuario),Map.of("senhaTemporaria",TEMPORARIA),true).statusCode()).isEqualTo(204);
        assertThat(sessao.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        assertThat(outra.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(401);
        var redefinido=usuarios.findById(usuario.getId()).orElseThrow();
        assertThat(redefinido.isTrocaSenhaObrigatoria()).isTrue();
        assertThat(encoder.matches(TEMPORARIA,redefinido.getSenhaHash())).isTrue();
        var evento=jdbc.queryForMap("SELECT ator_id, usuario_id, tipo, ocorrido_em FROM historico_acesso_usuario");
        assertThat(evento.get("ator_id")).isEqualTo(administrador.getId());
        assertThat(evento.get("usuario_id")).isEqualTo(usuario.getId());
        assertThat(evento.get("tipo")).isEqualTo("SENHA_REDEFINIDA");
        assertThat(evento.get("ocorrido_em")).isNotNull();
        var novo=new ClienteHttpTeste(porta);
        assertThat(novo.enviar("POST","/api/auth/login",Map.of("email",usuario.getEmail(),"senha",TEMPORARIA),true).statusCode()).isEqualTo(200);
        assertThat(novo.enviar("GET","/api/usuarios",null,false).statusCode()).isEqualTo(403);
        assertThat(novo.enviar("PUT","/api/auth/senha",Map.of("senhaAtual",TEMPORARIA,"novaSenha",SENHA),true).statusCode()).isEqualTo(204);
        entrar(usuarios.findById(usuario.getId()).orElseThrow());
    }
    @Test void resetNaoReativaContaInativaERejeitaSenhaCurtaSemHistorico() throws Exception {
        var usuario=criar("inativo",Perfil.CONSULTA);
        admin.enviar("PATCH",acesso(usuario),Map.of("perfil","CONSULTA","ativo",false),true);
        assertThat(admin.enviar("POST",reset(usuario),Map.of("senhaTemporaria","curta"),true).statusCode()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_acesso_usuario",Long.class)).isZero();
        assertThat(admin.enviar("POST",reset(usuario),Map.of("senhaTemporaria",TEMPORARIA),true).statusCode()).isEqualTo(204);
        assertThat(usuarios.findById(usuario.getId()).orElseThrow().isAtivo()).isFalse();
    }
    @Test void primeiroAcessoDeAdministradorTambemBloqueiaAdministracao() throws Exception {
        var usuario=usuarios.saveAndFlush(new Usuario("Temporario","temp@example.com",hash,Perfil.ADMINISTRADOR));
        var client=entrar(usuario);
        assertThat(client.enviar("GET","/api/usuarios",null,false).statusCode()).isEqualTo(403);
    }
    @Test void duasAlteracoesSimultaneasNuncaRemovemTodosAdministradores() throws Exception {
        var outro=criar("outroadmin",Perfil.ADMINISTRADOR);var clientOutro=entrar(outro);
        var pronto=new CountDownLatch(2);var iniciar=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{pronto.countDown();iniciar.await();return admin.enviar("PATCH",acesso(administrador),Map.of("perfil","CONSULTA","ativo",true),true).statusCode();});
            var b=executor.submit(()->{pronto.countDown();iniciar.await();return clientOutro.enviar("PATCH",acesso(outro),Map.of("perfil","ADMINISTRADOR","ativo",false),true).statusCode();});
            assertThat(pronto.await(5,TimeUnit.SECONDS)).isTrue();iniciar.countDown();
            assertThat(java.util.List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(usuarios.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR)).isEqualTo(1);
    }
    @Test void falhaNoHistoricoReverteSenhaESessoesSemExporCredencial() throws Exception {
        var usuario=criar("operador",Perfil.COMPRAS);var sessao=entrar(usuario);
        jdbc.execute("ALTER TABLE historico_acesso_usuario ADD CONSTRAINT teste_falha_evento CHECK (false)");
        try {
            var result=admin.enviar("POST",reset(usuario),Map.of("senhaTemporaria",TEMPORARIA),true);
            assertThat(result.statusCode()).isEqualTo(500);
            assertThat(result.body()).contains("ERRO_INTERNO","identificador").doesNotContain(TEMPORARIA,hash,"SQLException");
            assertThat(usuarios.findById(usuario.getId()).orElseThrow().getSenhaHash()).isEqualTo(hash);
            assertThat(sessao.enviar("GET","/api/auth/me",null,false).statusCode()).isEqualTo(200);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM historico_acesso_usuario",Long.class)).isZero();
        } finally { jdbc.execute("ALTER TABLE historico_acesso_usuario DROP CONSTRAINT teste_falha_evento"); }
    }
    @Test void recusaEscritaQuandoAtorFoiRevogadoAposAutenticacao() {
        jdbc.update("UPDATE usuarios SET versao_credencial=versao_credencial+1 WHERE id=?",administrador.getId());
        assertThatThrownBy(()->service.criar(administrador.getId(),1,"Teste","novo@example.com",Perfil.CONSULTA,TEMPORARIA))
                .isInstanceOfSatisfying(FalhaUsuarioException.class, ex->assertThat(ex.getMotivo()).isEqualTo(FalhaUsuarioException.Motivo.ACESSO_REVOGADO));
        assertThat(usuarios.count()).isEqualTo(1);
    }
}
