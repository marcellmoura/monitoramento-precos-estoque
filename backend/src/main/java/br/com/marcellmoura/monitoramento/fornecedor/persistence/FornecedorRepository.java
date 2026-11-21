package br.com.marcellmoura.monitoramento.fornecedor.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {}
