package br.com.marcellmoura.monitoramento;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@Import(PostgreSqlTestConfiguration.class)
class FlywayMigrationIT {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	@Test
	void aplicaUmaVezPreservaDadosERejeitaMigrationAlterada(@TempDir Path diretorio) throws Exception {
		Path copia = diretorio.resolve("V1__criar_usuarios.sql");
		try (var original = new ClassPathResource("db/migration/V1__criar_usuarios.sql").getInputStream()) {
			Files.copy(original, copia);
		}
		Flyway flyway = Flyway.configure().dataSource(dataSource)
				.schemas("teste_migrations").defaultSchema("teste_migrations")
				.locations("filesystem:" + diretorio).cleanDisabled(true).load();

		assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
		jdbc.update("""
				INSERT INTO teste_migrations.usuarios
				(nome, email, senha_hash, perfil, ativo, troca_senha_obrigatoria)
				VALUES ('Teste migration', 'migration@example.com', 'hash-ficticio', 'CONSULTA', true, true)
				""");
		assertThat(flyway.migrate().migrationsExecuted).isZero();
		assertThat(jdbc.queryForObject("SELECT count(*) FROM teste_migrations.usuarios", Long.class))
				.isEqualTo(1L);

		Files.writeString(copia, "\n-- Alteracao simulada somente na copia de teste\n", StandardOpenOption.APPEND);
		assertThatThrownBy(flyway::validate).isInstanceOf(FlywayValidateException.class);
	}

}
