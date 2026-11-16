package br.com.marcellmoura.monitoramento.acesso.application;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.persistence.UsuarioRepository;

@Service
public class AdministradorInicialService {
    private final EntityManager entityManager;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final Validator validator;
    public AdministradorInicialService(EntityManager entityManager, UsuarioRepository usuarios,
            PasswordEncoder encoder, Validator validator) {
        this.entityManager = entityManager; this.usuarios = usuarios; this.encoder = encoder; this.validator = validator;
    }
    public record Dados(@NotBlank @Size(max=120) String nome, @NotBlank @Email @Size(max=254) String email,
            @NotBlank @Size(min=12) String senha) {}

    @Transactional
    public void criar(String nome, String email, char[] senha) {
        var dados = new Dados(nome.strip(), email.strip(), new String(senha));
        if (!validator.validate(dados).isEmpty()) {
            throw new ValidationException("Nome, e-mail e senha de no mínimo 12 caracteres são obrigatórios.");
        }
        // Também protege contra duas inicializações locais simultâneas.
        entityManager.createNativeQuery("LOCK TABLE usuarios IN EXCLUSIVE MODE").executeUpdate();
        if (usuarios.count() != 0) { throw new IllegalStateException("A base já possui usuários; inicialização recusada."); }
        usuarios.save(new Usuario(dados.nome(), dados.email(), encoder.encode(dados.senha()), Perfil.ADMINISTRADOR));
    }
}
