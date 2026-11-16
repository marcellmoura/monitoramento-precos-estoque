package br.com.marcellmoura.monitoramento.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSqlTestConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresql() {
		// Imagem oficial fixada por digest; preservar sua identidade no Testcontainers.
		return new PostgreSQLContainer(DockerImageName.parse(
				"postgres:18.7-bookworm@sha256:ece55f7868da684e400f82f1030a050718adbdd65c23aa500c486f6e2950dc0f").asCompatibleSubstituteFor("postgres"));
	}

}
