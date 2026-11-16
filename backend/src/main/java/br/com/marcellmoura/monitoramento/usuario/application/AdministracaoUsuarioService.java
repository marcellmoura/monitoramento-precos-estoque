package br.com.marcellmoura.monitoramento.usuario.application;

import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;
import static br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException.Motivo.ACESSO_REVOGADO;
import static br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException.Motivo.EMAIL_DUPLICADO;
import static br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException.Motivo.NAO_ENCONTRADO;
import static br.com.marcellmoura.monitoramento.usuario.application.FalhaUsuarioException.Motivo.ULTIMO_ADMINISTRADOR;

@Service
@Transactional(readOnly = true)
public class AdministracaoUsuarioService {
    private final UsuarioRepository usuarios;
    private final EntityManager entityManager;
    private final PasswordEncoder encoder;
    private final JdbcTemplate jdbc;

    public AdministracaoUsuarioService(UsuarioRepository usuarios, EntityManager entityManager,
            PasswordEncoder encoder, JdbcTemplate jdbc) {
        this.usuarios = usuarios; this.entityManager = entityManager; this.encoder = encoder; this.jdbc = jdbc;
    }
    public record Pagina(List<UsuarioResumo> itens, int pagina, int tamanho, long total, int paginas) {}

    public Pagina listar(int pagina, int tamanho) {
        var result = usuarios.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
        return new Pagina(result.map(UsuarioResumo::de).getContent(), pagina, tamanho,
                result.getTotalElements(), result.getTotalPages());
    }
    public UsuarioResumo consultar(Long id) {
        return UsuarioResumo.de(usuarios.findById(id).orElseThrow(() -> new FalhaUsuarioException(NAO_ENCONTRADO)));
    }
    @Transactional
    public UsuarioResumo criar(Long atorId, long versaoAtor, String nome, String email, Perfil perfil, String senha) {
        validarAtorComBloqueio(atorId, versaoAtor);
        if (usuarios.existsByEmailIgnoreCase(email)) { throw new FalhaUsuarioException(EMAIL_DUPLICADO); }
        return UsuarioResumo.de(usuarios.saveAndFlush(new Usuario(nome, email, encoder.encode(senha), perfil)));
    }
    @Transactional
    public UsuarioResumo alterarAcesso(Long atorId, long versaoAtor, Long id, Perfil perfil, boolean ativo) {
        validarAtorComBloqueio(atorId, versaoAtor);
        var usuario = bloquearUsuario(id);
        protegerUltimoAdministrador(usuario, perfil, ativo);
        if (usuario.alterarAcesso(perfil, ativo)) { revogarSessoes(id); }
        return UsuarioResumo.de(usuario);
    }
    @Transactional
    public void redefinirSenha(Long atorId, long versaoAtor, Long id, String senha) {
        validarAtorComBloqueio(atorId, versaoAtor);
        var usuario = bloquearUsuario(id);
        usuario.redefinirSenha(encoder.encode(senha));
        revogarSessoes(id);
        jdbc.update("INSERT INTO historico_acesso_usuario (ator_id, usuario_id, tipo) VALUES (?, ?, 'SENHA_REDEFINIDA')",
                atorId, id);
    }
    private void validarAtorComBloqueio(Long id, long versao) {
        // Lock transacional compartilhado por todas as alterações administrativas, inclusive entre instâncias.
        jdbc.execute("SELECT pg_advisory_xact_lock(170031)");
        var ator = bloquearUsuario(id);
        if (!ator.isAtivo() || ator.getPerfil() != Perfil.ADMINISTRADOR || ator.isTrocaSenhaObrigatoria()
                || ator.getVersaoCredencial() != versao) {
            throw new FalhaUsuarioException(ACESSO_REVOGADO);
        }
    }
    private Usuario bloquearUsuario(Long id) {
        var usuario = entityManager.find(Usuario.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (usuario == null) { throw new FalhaUsuarioException(NAO_ENCONTRADO); }
        return usuario;
    }
    private void protegerUltimoAdministrador(Usuario usuario, Perfil perfil, boolean ativo) {
        if (usuario.isAtivo() && usuario.getPerfil() == Perfil.ADMINISTRADOR
                && (!ativo || perfil != Perfil.ADMINISTRADOR)
                && usuarios.countByPerfilAndAtivoTrue(Perfil.ADMINISTRADOR) <= 1) {
            throw new FalhaUsuarioException(ULTIMO_ADMINISTRADOR);
        }
    }
    private void revogarSessoes(Long id) {
        jdbc.update("DELETE FROM spring_session WHERE principal_name = ?", id.toString());
    }
}
