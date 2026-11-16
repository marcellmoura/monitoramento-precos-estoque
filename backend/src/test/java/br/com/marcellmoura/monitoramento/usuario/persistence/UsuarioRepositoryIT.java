package br.com.marcellmoura.monitoramento.usuario.persistence;

import java.time.Instant;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import br.com.marcellmoura.monitoramento.support.PostgreSqlTestConfiguration;
import br.com.marcellmoura.monitoramento.usuario.domain.Perfil;
import br.com.marcellmoura.monitoramento.usuario.domain.Usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
@AutoConfigureMockMvc
@Import(PostgreSqlTestConfiguration.class)
@Transactional
class UsuarioRepositoryIT {

	private static final String HASH_FICTICIO = "hash-ficticio-exclusivo-do-teste";

	@Autowired
	private UsuarioRepository repository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbc;

	@ParameterizedTest
	@EnumSource(Perfil.class)
	void persisteEConsultaOsPerfisComIdentidadeEDatas(Perfil perfil) {
		Usuario salvo = repository.saveAndFlush(new Usuario(
				"  Pessoa de Teste  ", "  Pessoa@Example.com  ", HASH_FICTICIO, perfil));
		Long id = salvo.getId();
		entityManager.clear();

		Usuario consultado = repository.findByEmailIgnoreCase("PESSOA@EXAMPLE.COM").orElseThrow();

		assertThat(consultado.getId()).isNotNull().isEqualTo(id);
		assertThat(consultado.getNome()).isEqualTo("Pessoa de Teste");
		assertThat(consultado.getEmail()).isEqualTo("pessoa@example.com");
		assertThat(consultado.getSenhaHash()).isEqualTo(HASH_FICTICIO);
		assertThat(consultado.getPerfil()).isEqualTo(perfil);
		assertThat(consultado.isAtivo()).isTrue();
		assertThat(consultado.isTrocaSenhaObrigatoria()).isTrue();
		assertThat(consultado.getCriadoEm()).isNotNull();
		assertThat(consultado.getAtualizadoEm()).isEqualTo(consultado.getCriadoEm());
	}

	@Test
	void atualizaSemMudarIdentidadeOuDataDeCriacao() {
		Usuario salvo = repository.saveAndFlush(new Usuario(
				"Nome inicial", "atualizacao@example.com", HASH_FICTICIO, Perfil.CONSULTA));
		Long id = salvo.getId();
		entityManager.clear();
		Usuario usuario = repository.findById(id).orElseThrow();
		Instant criacao = usuario.getCriadoEm();
		Instant atualizacaoAnterior = usuario.getAtualizadoEm();

		usuario.atualizarNome("  Nome atualizado  ");
		repository.flush();
		entityManager.clear();
		Usuario atualizado = repository.findById(id).orElseThrow();

		assertThat(atualizado.getNome()).isEqualTo("Nome atualizado");
		assertThat(atualizado.getCriadoEm()).isEqualTo(criacao);
		assertThat(atualizado.getAtualizadoEm()).isAfter(atualizacaoAnterior);
	}

	@Test
	void bancoImpedeEmailDuplicadoMesmoSemNormalizacaoDaAplicacao() {
		repository.saveAndFlush(new Usuario(
				"Pessoa", "unico@example.com", HASH_FICTICIO, Perfil.COMPRAS));

		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO usuarios (nome, email, senha_hash, perfil, ativo, troca_senha_obrigatoria)
				VALUES ('Outra pessoa', 'UNICO@EXAMPLE.COM', ?, 'CONSULTA', true, true)
				""", HASH_FICTICIO))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_usuarios_email");
	}

	@Test
	void bancoImpedePerfilForaDaMatrizDeAcesso() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO usuarios (nome, email, senha_hash, perfil, ativo, troca_senha_obrigatoria)
				VALUES ('Pessoa', 'perfil@example.com', ?, 'INVALIDO', true, true)
				""", HASH_FICTICIO))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_usuarios_perfil");
	}

	@Test
	void bancoImpedeNomeVazio() {
		assertThatThrownBy(() -> jdbc.update("""
				INSERT INTO usuarios (nome, email, senha_hash, perfil, ativo, troca_senha_obrigatoria)
				VALUES ('   ', 'nome@example.com', ?, 'CONSULTA', true, true)
				""", HASH_FICTICIO))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_usuarios_nome");
	}

}
