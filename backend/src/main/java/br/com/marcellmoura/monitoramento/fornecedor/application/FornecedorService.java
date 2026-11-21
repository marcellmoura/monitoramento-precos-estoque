package br.com.marcellmoura.monitoramento.fornecedor.application;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.fornecedor.domain.Fornecedor;
import br.com.marcellmoura.monitoramento.fornecedor.persistence.FornecedorRepository;

@Service
@Transactional(readOnly = true)
public class FornecedorService {
    private final FornecedorRepository fornecedores;

    public FornecedorService(FornecedorRepository fornecedores) {
        this.fornecedores = fornecedores;
    }

    public Page<Fornecedor> listar(int pagina, int tamanho) {
        return fornecedores.findAll(PageRequest.of(pagina, tamanho, Sort.by("id")));
    }

    public Optional<Fornecedor> consultar(Long id) {
        return fornecedores.findById(id);
    }

    @Transactional
    public Fornecedor cadastrar(String nome, String cnpj, String telefone, String email) {
        return fornecedores.saveAndFlush(new Fornecedor(nome, cnpj, telefone, email));
    }
}
