import { Link, useFetcher, useLoaderData } from 'react-router';
import type { Fornecedor } from '../fornecedores/api';
import { estados } from './api';
import type { FalhaImportacao, PaginaImportacoes, TentativaImportacao } from './api';
import styles from '../../shared/gestao.module.css';

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

function Falha({ mensagem }: { mensagem: string }) {
  return <p role="alert" className={styles.erro}>{mensagem}</p>;
}

export function Importacoes() {
  const dados = useLoaderData<PaginaImportacoes | FalhaImportacao>();
  if ('erro' in dados) {
    return <section><h2>Importações</h2><Falha mensagem={dados.erro} /><Link to="/">Voltar ao início</Link></section>;
  }
  return <section className={styles.painel}>
    <div className={styles.cabecalho}><h2>Importações</h2><Link to="/importacoes/nova">Enviar PDF</Link></div>
    {dados.itens.length === 0 ? <p>{dados.total === 0 ? 'Nenhum envio registrado.' : 'Nenhum envio nesta página.'}</p> :
      <ul className={styles.lista}>
        {dados.itens.map(item => <li key={item.id}>
          <Link to={`/importacoes/${item.id}`}>#{item.id} · {item.arquivo}</Link>
          <p>{item.fornecedorCodigo} · {item.fornecedorNome} · {dataHora.format(new Date(item.enviadaEm))}</p>
          <p>{estados[item.status]}{item.quantidadeItens != null && ` · ${item.quantidadeItens} item(ns)`}</p>
        </li>)}
      </ul>}
    {(dados.pagina > 0 || dados.paginas > 1) && <nav className={styles.paginacao} aria-label="Paginação de importações">
      {dados.pagina > 0 && <Link to={`?pagina=${dados.pagina - 1}`}>Página anterior</Link>}
      {dados.pagina + 1 < dados.paginas && <Link to={`?pagina=${dados.pagina + 1}`}>Próxima página</Link>}
    </nav>}
    <Link to="/">Voltar ao início</Link>
  </section>;
}

export function NovoEnvio() {
  const opcoes = useLoaderData<Fornecedor[] | FalhaImportacao>();
  return <section className={styles.painel}>
    <h2>Enviar PDF</h2>
    {'erro' in opcoes ? <Falha mensagem={opcoes.erro} /> : <FormularioEnvio fornecedores={opcoes} />}
    <Link to="/importacoes">Voltar às importações</Link>
  </section>;
}

function FormularioEnvio({ fornecedores }: { fornecedores: Fornecedor[] }) {
  const fetcher = useFetcher<FalhaImportacao>();
  if (fornecedores.length === 0) {
    return <p>Cadastre um fornecedor antes de enviar um PDF. <Link to="/fornecedores/novo">Cadastrar fornecedor</Link></p>;
  }
  return <fetcher.Form method="post" encType="multipart/form-data" className={styles.formulario}>
    <div className={styles.campo}>
      <label htmlFor="fornecedor-envio">Fornecedor</label>
      <select id="fornecedor-envio" name="fornecedorId" defaultValue="" required>
        <option value="" disabled>Selecione</option>
        {fornecedores.map(fornecedor => <option key={fornecedor.id} value={fornecedor.id}>{fornecedor.codigo} · {fornecedor.nome}</option>)}
      </select>
    </div>
    <label>Arquivo PDF<input type="file" name="arquivo" accept=".pdf,application/pdf" required /></label>
    {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
    <button type="submit" disabled={fetcher.state !== 'idle'}>{fetcher.state === 'idle' ? 'Enviar PDF' : 'Enviando…'}</button>
  </fetcher.Form>;
}

export function DetalheImportacao() {
  const tentativa = useLoaderData<TentativaImportacao | FalhaImportacao>();
  const fetcher = useFetcher<FalhaImportacao>();
  if ('erro' in tentativa) {
    return <section><h2>Tentativa de importação</h2><Falha mensagem={tentativa.erro} />
      <Link to="/importacoes">Voltar às importações</Link></section>;
  }
  return <section className={styles.painel}>
    <h2>Tentativa #{tentativa.id}</h2>
    <p role="status">{estados[tentativa.status]}</p>
    {tentativa.motivo && <p>{tentativa.motivo}</p>}
    <dl className={styles.dados}>
      <dt>Fornecedor</dt><dd>{tentativa.fornecedorCodigo} · {tentativa.fornecedorNome}</dd>
      <dt>Arquivo</dt><dd>{tentativa.arquivo}</dd>
      <dt>Enviado em</dt><dd>{dataHora.format(new Date(tentativa.enviadaEm))}</dd>
      {tentativa.paginas != null && <><dt>Páginas</dt><dd>{tentativa.paginas}</dd></>}
      {tentativa.quantidadeItens != null && <><dt>Itens</dt><dd>{tentativa.quantidadeItens}</dd></>}
    </dl>
    <nav className={styles.paginacao} aria-label="Ações da tentativa">
      {tentativa.importacaoExistenteId != null && <Link to={`/importacoes/${tentativa.importacaoExistenteId}`}>Abrir tentativa original</Link>}
      <Link to="/importacoes">Voltar às importações</Link>
    </nav>
    {tentativa.status === 'AGUARDANDO_CONFIRMACAO' && <fetcher.Form method="post" className={styles.formulario}>
      {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
      <button type="submit" disabled={fetcher.state !== 'idle'}>Confirmar para este fornecedor</button>
    </fetcher.Form>}
  </section>;
}
