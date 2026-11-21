package br.com.marcellmoura.monitoramento.fornecedor.domain;

import java.util.Locale;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fornecedores")
public class Fornecedor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 255)
    private String nome;
    @Column(length = 32)
    private String cnpj;
    @Column(length = 40)
    private String telefone;
    @Column(length = 254)
    private String email;

    protected Fornecedor() {}

    public Fornecedor(String nome, String cnpj, String telefone, String email) {
        this.nome = nome;
        this.cnpj = cnpj;
        this.telefone = telefone;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getCodigo() { return String.format(Locale.ROOT, "FOR-%06d", id); }
    public String getNome() { return nome; }
    public String getCnpj() { return cnpj; }
    public String getTelefone() { return telefone; }
    public String getEmail() { return email; }
}
