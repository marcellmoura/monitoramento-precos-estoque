package br.com.marcellmoura.monitoramento.importacao.domain;

import java.time.Instant;

public record TentativaImportacao(long id, String fornecedorCodigo, String fornecedorNome, long atorId,
        String arquivo, Instant enviadaEm, Status status, String motivo, Integer paginas, Integer quantidadeItens,
        Long importacaoExistenteId) {

    public enum Status { PENDENTE, REJEITADA, FALHA_ENVIO, DUPLICADA, AGUARDANDO_CONFIRMACAO, PROCESSANDO, AGUARDANDO_CONFERENCIA, FALHA_EXTRACAO }
}
