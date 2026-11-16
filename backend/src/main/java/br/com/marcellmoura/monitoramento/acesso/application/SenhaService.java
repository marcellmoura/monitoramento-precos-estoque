package br.com.marcellmoura.monitoramento.acesso.application;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;

@Service
public class SenhaService {
    private final EntityManager entityManager;
    private final PasswordEncoder encoder;
    private final JdbcTemplate sessoes;
    public SenhaService(EntityManager entityManager, PasswordEncoder encoder, JdbcTemplate sessoes) {
        this.entityManager = entityManager; this.encoder = encoder; this.sessoes = sessoes;
    }
    @Transactional
    public void trocar(Long id, String atual, String nova) {
        var usuario = entityManager.find(Usuario.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (!encoder.matches(atual, usuario.getSenhaHash())) {
            throw new SenhaInvalidaException("A senha atual está incorreta.");
        }
        if (nova.equals(atual)) { throw new SenhaInvalidaException("A nova senha deve ser diferente da atual."); }
        usuario.trocarSenha(encoder.encode(nova));
        sessoes.update("DELETE FROM spring_session WHERE principal_name = ?", id.toString());
    }
}
