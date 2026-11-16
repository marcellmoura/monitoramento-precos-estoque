package br.com.marcellmoura.monitoramento.acesso.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class CredenciaisConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new Argon2PasswordEncoder(16, 32, 1, 65536, 3); }
}
