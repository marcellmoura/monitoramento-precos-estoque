package br.com.marcellmoura.monitoramento.importacao.application;

import java.io.IOException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.com.marcellmoura.monitoramento.importacao.domain.EnvioPdf;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao;
import br.com.marcellmoura.monitoramento.importacao.domain.TentativaImportacao.Status;
import br.com.marcellmoura.monitoramento.importacao.persistence.ImportacaoRepository;

@Service
@Transactional(readOnly = true)
public class RegistroImportacoes {
    private final ImportacaoRepository registros;
    private final ArmazenamentoPdf armazenamento;

    RegistroImportacoes(ImportacaoRepository registros, ArmazenamentoPdf armazenamento) {
        this.registros = registros;
        this.armazenamento = armazenamento;
    }

    public Page<TentativaImportacao> listar(int pagina, int tamanho) { return registros.listar(pagina, tamanho); }

    public TentativaImportacao consultar(long id) {
        return registros.consultar(id).orElseThrow(() ->
                new ImportacaoException(404, "IMPORTACAO_NAO_ENCONTRADA", "Tentativa de importação não encontrada."));
    }

    @Transactional
    TentativaImportacao registrarFalha(EnvioPdf envio, Status status, String motivo) {
        return consultar(registros.inserir(envio, null, status, motivo, null));
    }

    @Transactional(rollbackFor = IOException.class)
    TentativaImportacao registrarArquivo(EnvioPdf envio, ArmazenamentoPdf.Temporario arquivo, int paginas) throws IOException {
        String hash = arquivo.hash();
        registros.bloquearConteudo(hash);
        var existente = registros.existente(envio.fornecedorId(), hash);
        if (existente.isPresent()) {
            return consultar(registros.inserir(envio, hash, Status.DUPLICADA,
                    "Este conteúdo já foi enviado para o fornecedor selecionado.", existente.get()));
        }
        if (registros.arquivoRegistrado(hash)) {
            return consultar(registros.inserir(envio, hash, Status.AGUARDANDO_CONFIRMACAO,
                    "Este conteúdo já foi enviado para outro fornecedor. Confira o fornecedor e confirme para prosseguir.", null));
        }
        registros.inserirArquivo(hash, armazenamento.guardar(arquivo), arquivo.tamanho(), paginas);
        return consultar(registros.inserir(envio, hash, Status.PENDENTE, null, null));
    }

    @Transactional
    public TentativaImportacao confirmar(long id, long atorId) {
        if (registros.confirmar(id, atorId)) return consultar(id);
        consultar(id);
        throw new ImportacaoException(409, "IMPORTACAO_SEM_CONFIRMACAO", "Esta tentativa não está aguardando confirmação.");
    }
}
