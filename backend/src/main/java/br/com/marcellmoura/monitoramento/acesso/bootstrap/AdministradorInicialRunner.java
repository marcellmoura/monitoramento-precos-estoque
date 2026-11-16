package br.com.marcellmoura.monitoramento.acesso.bootstrap;

import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import br.com.marcellmoura.monitoramento.acesso.application.AdministradorInicialService;

@Component
@Profile("bootstrap-admin")
public class AdministradorInicialRunner implements CommandLineRunner {
    private final AdministradorInicialService service;
    public AdministradorInicialRunner(AdministradorInicialService service) { this.service = service; }
    @Override public void run(String... args) {
        var console = System.console();
        if (console == null) { throw new IllegalStateException("Execute o JAR em terminal interativo; senha não aceita por argumento ou arquivo."); }
        String nome = console.readLine("Nome do primeiro administrador: ");
        String email = console.readLine("E-mail: ");
        char[] senha = console.readPassword("Senha temporária (mínimo 12 caracteres): ");
        char[] confirmacao = console.readPassword("Confirme a senha: ");
        try {
            if (senha == null || confirmacao == null || !Arrays.equals(senha, confirmacao)) {
                throw new IllegalArgumentException("As senhas não conferem.");
            }
            service.criar(nome == null ? "" : nome, email == null ? "" : email, senha);
            console.printf("Administrador criado. A troca de senha será obrigatória no primeiro acesso.%n");
        } finally {
            if (senha != null) { Arrays.fill(senha, '\0'); }
            if (confirmacao != null) { Arrays.fill(confirmacao, '\0'); }
        }
    }
}
