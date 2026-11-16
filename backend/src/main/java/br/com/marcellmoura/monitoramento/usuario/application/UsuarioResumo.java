package br.com.marcellmoura.monitoramento.usuario.application;

import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;

public record UsuarioResumo(Long id, String nome, String email, Perfil perfil, boolean ativo,
        boolean trocaSenhaObrigatoria) {
    public static UsuarioResumo de(Usuario u) {
        return new UsuarioResumo(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(), u.isAtivo(),
                u.isTrocaSenhaObrigatoria());
    }
}
