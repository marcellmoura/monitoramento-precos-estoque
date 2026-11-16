package br.com.marcellmoura.monitoramento.usuario.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "senha_hash", nullable = false, length = 255)
	private String senhaHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Perfil perfil;

	@Column(nullable = false)
	private boolean ativo;

	@Column(name = "troca_senha_obrigatoria", nullable = false)
	private boolean trocaSenhaObrigatoria;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private Instant criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private Instant atualizadoEm;

	protected Usuario() {
	}

	public Usuario(String nome, String email, String senhaHash, Perfil perfil) {
		this.nome = Objects.requireNonNull(nome).strip();
		this.email = Objects.requireNonNull(email).strip().toLowerCase(Locale.ROOT);
		this.senhaHash = Objects.requireNonNull(senhaHash);
		this.perfil = Objects.requireNonNull(perfil);
		this.ativo = true;
		this.trocaSenhaObrigatoria = true;
	}

	public void atualizarNome(String nome) {
		this.nome = Objects.requireNonNull(nome).strip();
	}

	@PrePersist
	void registrarCriacao() {
		Instant agora = Instant.now();
		this.criadoEm = agora;
		this.atualizadoEm = agora;
	}

	@PreUpdate
	void registrarAtualizacao() {
		this.atualizadoEm = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public String getEmail() {
		return email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public Perfil getPerfil() {
		return perfil;
	}

	public boolean isAtivo() {
		return ativo;
	}

	public boolean isTrocaSenhaObrigatoria() {
		return trocaSenhaObrigatoria;
	}

	public Instant getCriadoEm() {
		return criadoEm;
	}

	public Instant getAtualizadoEm() {
		return atualizadoEm;
	}

}
