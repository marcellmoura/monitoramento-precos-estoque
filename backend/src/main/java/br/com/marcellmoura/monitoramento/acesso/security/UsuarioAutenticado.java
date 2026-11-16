package br.com.marcellmoura.monitoramento.acesso.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;

public final class UsuarioAutenticado implements UserDetails, CredentialsContainer {
    @Serial private static final long serialVersionUID = 1L;
    private final Long id;
    private final long versaoCredencial;
    private final String perfil;
    private final boolean ativo;
    private transient String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        id = usuario.getId();
        versaoCredencial = usuario.getVersaoCredencial();
        perfil = usuario.getPerfil().name();
        ativo = usuario.isAtivo();
        senhaHash = usuario.getSenhaHash();
    }
    public Perfil getPerfil() {
        return Perfil.valueOf(perfil);
    }
    public Long getId() { return id; }
    public long getVersaoCredencial() { return versaoCredencial; }
    @Override public String getUsername() { return id.toString(); }
    @Override public String getPassword() { return senhaHash; }
    @Override public boolean isEnabled() { return ativo; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + perfil));
    }
    @Override public void eraseCredentials() { senhaHash = null; }
}
