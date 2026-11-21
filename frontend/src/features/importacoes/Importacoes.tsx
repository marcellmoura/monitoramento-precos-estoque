import { Link, useFetcher, useLoaderData, useRevalidator } from 'react-router';
import { Fragment, useEffect } from 'react';
import type { Fornecedor } from '../fornecedores/api';
import { estados } from './api';
import type { DetalheTentativa, FalhaImportacao, PaginaImportacoes, PreviaImportacao } from './api';
import styles from '../../shared/gestao.module.css';
import importacoes from './Importacoes.module.css';

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
  const tentativa = useLoaderData<DetalheTentativa | FalhaImportacao>();
  const fetcher = useFetcher<FalhaImportacao>();
  const { revalidate, state } = useRevalidator();
  const aguardando = !('erro' in tentativa) && (tentativa.status === 'PENDENTE' || tentativa.status === 'PROCESSANDO');
  useEffect(() => {
    if (!aguardando || state !== 'idle') return;
    const timer = window.setTimeout(() => void revalidate(), 2000);
    return () => window.clearTimeout(timer);
  }, [aguardando, revalidate, state]);
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
    {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
    {tentativa.status === 'AGUARDANDO_CONFIRMACAO' && <fetcher.Form method="post" className={styles.formulario}>
      <button type="submit" disabled={fetcher.state !== 'idle'}>Confirmar para este fornecedor</button>
    </fetcher.Form>}
    {tentativa.status === 'FALHA_EXTRACAO' && <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="operacao" value="reprocessar" />
      <button type="submit" disabled={fetcher.state !== 'idle'}>Tentar novamente</button>
    </fetcher.Form>}
    {tentativa.previa && <Previa previa={tentativa.previa} />}
  </section>;
}

function Previa({ previa }: { previa: PreviaImportacao }) {
  const leituraParcial = previa.itens.some(item => item.dados === null);
  return <section aria-labelledby="previa-itens">
    <h3 id="previa-itens">Prévia dos itens</h3>
    <p>Data no PDF: {previa.dataReferencia ?? 'Não informada'}</p>
    {leituraParcial && <p role="alert">Leitura parcial. Confira as linhas não reconhecidas no PDF.</p>}
    <ol className={styles.lista}>
      {previa.itens.map(({ dados, precos, textoOriginal, pendencias }, indice) => <li key={indice}>
        <strong>{dados?.descricao ?? 'Linha não reconhecida'}</strong>
        {dados && <dl className={`${styles.dados} ${importacoes.dadosItem}`}>
          <dt>Referência</dt><dd>{dados.referencia || 'Não informada'}</dd>
          <dt>Unidade</dt><dd>{dados.unidade || 'Não informada'}</dd>
          <dt>Apresentação</dt><dd>{dados.apresentacao || 'Não informada'}</dd>
          <dt>Quantidade</dt><dd>{dados.quantidade || 'Não informada'}</dd>
          {Object.entries(precos).map(([coluna, valor]) => <Fragment key={coluna}>
            <dt>{coluna}</dt><dd>{valor || 'Não informado'}</dd>
          </Fragment>)}
        </dl>}
        {!dados && <p>{textoOriginal}</p>}
        <p>{pendencias.length ? pendencias.join(' ') : 'Pendente de associação.'}</p>
      </li>)}
    </ol>
  </section>;
}
