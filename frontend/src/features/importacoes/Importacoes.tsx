import { Link, useFetcher, useLoaderData, useRevalidator, useSearchParams } from 'react-router';
import { Fragment, useEffect, useId, useState } from 'react';
import type { Fornecedor } from '../fornecedores/api';
import { AssociacaoItem } from './AssociacaoItem';
import { CorrecaoItem } from './CorrecaoItem';
import { FormularioConferencia } from './FormularioConferencia';
import { estados, formatarDataComercial } from './api';
import type { ClassificacaoPreco, ConflitoPreco, LinhaConfirmacao, PreviaGravacao, ProdutoCatalogo, SituacaoVinculo, ConferenciaCotacao, RevisaoItem, DetalheTentativa, FalhaImportacao, PaginaImportacoes, PreviaImportacao } from './api';
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
  const [params] = useSearchParams();
  const fetcher = useFetcher<FalhaImportacao>();
  const { revalidate, state } = useRevalidator();
  const linhaRetorno = Number(params.get('linha'));
  const idTentativa = 'erro' in tentativa ? null : tentativa.id;
  useEffect(() => {
    if (linhaRetorno > 0 && idTentativa !== null) {
      document.getElementById(`item-${linhaRetorno}`)?.scrollIntoView?.({ block: 'start' });
    }
  }, [idTentativa, linhaRetorno]);
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
  const linhasPrecos = tentativa.precos?.itens ?? [];
  const bloqueada = linhasPrecos.some(linha => linha.estado === 'CONFIRMADO');
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
      <Link to={`/historico-precos?tentativa=${tentativa.id}`}>Ver histórico desta tentativa</Link>
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
    {tentativa.precos && <ConfirmacaoItens linhas={linhasPrecos} conferencia={tentativa.conferencia}
      previa={tentativa.precos.previaGravacao} />}
    {tentativa.previa && <Previa previa={tentativa.previa}
      conferencia={tentativa.conferencia} revisoes={tentativa.revisoes ?? []} vinculos={tentativa.vinculos ?? []}
      precos={linhasPrecos} tentativa={tentativa.id} linhaRetorno={linhaRetorno} produtoCriado={tentativa.produtoCriado}
      key={`previa-${tentativa.id}-${params.get('produto') ?? ''}`} />}
    {tentativa.conferencia && <FormularioConferencia conferencia={tentativa.conferencia} bloqueada={bloqueada}
      key={`conferencia-${tentativa.id}-${tentativa.conferencia.confirmada?.id ?? 0}`} />}
  </section>;
}

function Previa({ previa, conferencia, revisoes, vinculos, precos, tentativa, linhaRetorno, produtoCriado }: {
  previa: PreviaImportacao; conferencia?: ConferenciaCotacao; revisoes: RevisaoItem[];
  vinculos: SituacaoVinculo[]; precos: LinhaConfirmacao[]; tentativa: number; linhaRetorno: number; produtoCriado?: ProdutoCatalogo;
}) {
  const precosPorLinha = new Map(precos.map(preco => [preco.linha, preco]));
  const vinculosPorLinha = new Map(vinculos.map(vinculo => [vinculo.linha, vinculo]));
  const porLinha = new Map(revisoes.map(revisao => [revisao.linha, revisao]));
  const base = conferencia?.confirmada;
  const leituraParcial = previa.itens.some(item => item.dados === null);
  return <section aria-labelledby="previa-itens">
    <h3 id="previa-itens">Prévia dos itens</h3>
    <p>Data no PDF: {previa.dataReferencia ?? 'Não informada'}</p>
    {leituraParcial && <p role="alert">Leitura parcial. Confira as linhas não reconhecidas no PDF.</p>}
    <ol className={styles.lista}>
      {previa.itens.map((item, indice) => {
        const { dados, precos, textoOriginal, pendencias } = item;
        const linha = indice + 1;
        const revisao = porLinha.get(linha);
        const situacao = vinculosPorLinha.get(linha);
        const preco = precosPorLinha.get(linha);
        const confirmado = preco?.estado === 'CONFIRMADO';
        const associado = confirmado || Boolean(situacao?.reaproveitado || (situacao?.vinculo && !situacao.revalidar));
        return <li key={linha} id={`item-${linha}`}>
          <strong>{dados?.descricao ?? 'Linha não reconhecida'}</strong>
          {revisao && <h4>Dados extraídos do PDF</h4>}
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
          {pendencias.length > 0 && <p>{pendencias.join(' ')}</p>}
          {!revisao && <>
            {base && pendencias.length === 0 && !associado && <p>Pendente de associação.</p>}
            {conferencia?.pendenciasComerciais[indice]?.map(aviso => <p key={aviso}>{aviso}</p>)}
          </>}
          {base && <CorrecaoItem item={item} linha={linha} base={base} colunas={conferencia?.colunas ?? []}
            revisao={revisao} associado={associado} bloqueado={confirmado} />}
          {situacao && !confirmado && <AssociacaoItem tentativa={tentativa} situacao={situacao} abrirAoVoltar={linha === linhaRetorno}
            produtoCriado={linha === linhaRetorno ? produtoCriado : undefined} />}
          {preco && (base || preco.estado !== 'PENDENTE' || preco.conflito) && <>
            {preco.classificacao && <p><strong>Classificação</strong> · {classificacoes[preco.classificacao]}</p>}
            <EstadoPreco linha={preco} />
          </>}
        </li>;
      })}
    </ol>
  </section>;
}

const classificacoes: Record<ClassificacaoPreco, string> = {
  AUMENTOU: 'Aumentou',
  DIMINUIU: 'Diminuiu',
  SEM_ALTERACAO: 'Sem alteração',
  NOVO: 'Novo',
  PENDENTE: 'Pendente',
};

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const percentual = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'exceptZero',
});

function EstadoPreco({ linha }: { linha: LinhaConfirmacao }) {
  const { confirmado, preco, variacao } = linha;
  if (preco === null) return <>
    <p>Pendência para confirmar · {linha.motivos.join(' ')}</p>
    {linha.conflito && <ResolverConflito linha={linha.linha} conflito={linha.conflito} />}
  </>;

  if (!confirmado) return <>
    <p>Preço pronto para confirmar · {moeda.format(preco)}</p>
    <p>{linha.motivos.join(' ')}</p>
  </>;
  if (confirmado.substituido) return <p>Preço substituído no histórico · {moeda.format(preco)}</p>;
  const anterior = confirmado.anterior;
  return <>
    <p>Preço confirmado no histórico · {moeda.format(preco)}</p>
    {anterior ? <p>
      Último preço válido anterior · {moeda.format(anterior.preco)} · {formatarDataComercial(anterior.dataComercial)} ·{' '}
      <Link to={`/importacoes/${anterior.tentativa}`}>Tentativa #{anterior.tentativa}</Link>
    </p> : <p>Sem preço válido anterior.</p>}
    {variacao && <dl className={`${styles.dados} ${importacoes.dadosItem}`}>
      <dt>Diferença em reais</dt><dd>{moeda.format(variacao.diferencaAbsoluta)}</dd>
      <dt>Variação percentual</dt><dd>{percentual.format(variacao.percentual)}%</dd>
    </dl>}
  </>;
}

function ResolverConflito({ linha, conflito }: { linha: number; conflito: ConflitoPreco }) {
  const fetcher = useFetcher<FalhaImportacao>();
  const titulo = useId();
  return <section aria-labelledby={titulo}>
    <h4 id={titulo}>Resolver conflito de preço</h4>
    <p>Data comercial · {formatarDataComercial(conflito.dataComercial)}</p>
    <dl className={styles.dados}>
      <dt>Preço desta cotação</dt><dd>{moeda.format(conflito.preco)}</dd>
      {conflito.anteriores.map(preco => <Fragment key={preco.id}>
        <dt>Preço confirmado · posição {preco.ordem}</dt>
        <dd>{moeda.format(preco.preco)} · <Link to={`/importacoes/${preco.tentativa}`}>Tentativa #{preco.tentativa}</Link></dd>
      </Fragment>)}
    </dl>
    {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
    <FormularioConflito key={conflito.revisao} linha={linha} conflito={conflito} fetcher={fetcher} />
  </section>;
}

function FormularioConflito({ linha, conflito, fetcher }: {
  linha: number;
  conflito: ConflitoPreco;
  fetcher: ReturnType<typeof useFetcher<FalhaImportacao>>;
}) {
  const id = useId();
  const [decisao, decidir] = useState('');
  return <fetcher.Form method="post" className={styles.formulario}>
    <input type="hidden" name="operacao" value="resolver-conflito-preco" />
    <input type="hidden" name="linha" value={linha} />
    <input type="hidden" name="revisao" value={conflito.revisao} />
    <label htmlFor={`${id}-decisao`}>Resolução do conflito</label>
    <select id={`${id}-decisao`} name="decisao" required value={decisao} onChange={evento => decidir(evento.target.value)}>
      <option value="">Selecione</option>
      <option value="MANTER">Manter os dois preços</option>
      <option value="SUBSTITUIR">Substituir preço confirmado</option>
    </select>
    {decisao && <>
      <label htmlFor={`${id}-preco`}>Preço confirmado de referência</label>
      <select id={`${id}-preco`} name="precoReferencia" required>
        <option value="">Selecione</option>
        {conflito.anteriores.map(preco => <option key={preco.id} value={preco.id}>
          {moeda.format(preco.preco)} · Tentativa #{preco.tentativa} · Posição {preco.ordem}
        </option>)}
      </select>
    </>}
    {decisao === 'MANTER' && <>
      <label htmlFor={`${id}-ordem`}>Posição do novo preço</label>
      <select id={`${id}-ordem`} name="ordem" required>
        <option value="">Selecione</option>
        <option value="ANTES">Antes do preço selecionado</option>
        <option value="DEPOIS">Depois do preço selecionado</option>
      </select>
    </>}
    {decisao === 'SUBSTITUIR' && <>
      <label htmlFor={`${id}-motivo`}>Motivo da substituição</label>
      <input id={`${id}-motivo`} name="motivo" required maxLength={500} />
    </>}
    <button type="submit" disabled={!decisao || fetcher.state !== 'idle'}>Confirmar resolução</button>
  </fetcher.Form>;
}

function ResumoGravacao({ previa }: { previa: PreviaGravacao }) {
  return <div className={importacoes.resumoGravacao}>
    <p><strong>{previa.prontos.length === 1 ? '1 preço pronto para salvar' : `${previa.prontos.length} preços prontos para salvar`}</strong></p>
    {previa.prontos.length > 0 && <ul className={styles.lista}>
      {previa.prontos.map(item => <li key={item.linha}>
        <a href={`#item-${item.linha}`}>{[item.descricao, item.referencia, item.embalagem].filter(Boolean).join(' · ')}</a>
        <p>{item.anterior ? <>De {moeda.format(item.anterior.preco)} para </> : null}
          <strong>{moeda.format(item.preco)}</strong>{item.variacao && ` (${percentual.format(item.variacao.percentual)}%)`}
        </p>
      </li>)}
    </ul>}
    {previa.pendentes > 0 && <p>{previa.pendentes === 1 ? '1 item ficará pendente.' : `${previa.pendentes} itens ficarão pendentes.`}</p>}
  </div>;
}

function ConfirmacaoItens({ linhas, conferencia, previa }: {
  linhas: LinhaConfirmacao[]; conferencia?: ConferenciaCotacao; previa?: PreviaGravacao | null;
}) {
  const fetcher = useFetcher<FalhaImportacao>();
  const prontos = linhas.filter(linha => linha.estado === 'PRONTO').length;
  const confirmados = linhas.filter(linha => linha.estado === 'CONFIRMADO').length;
  const pendentes = linhas.length - prontos - confirmados;
  const exigeConferencia = conferencia && !conferencia.confirmada;
  const sugestao = exigeConferencia && conferencia.sugestao
    ? { ...conferencia.sugestao, dataComercial: conferencia.dataSugerida }
    : null;
  return <section className={importacoes.proximoPasso} aria-labelledby="confirmar-precos">
    <h3 id="confirmar-precos">Confirmação de preços</h3>
    {exigeConferencia ? <>
      {sugestao ? <>
        <p>Podemos reaproveitar os dados de compra da última cotação deste fornecedor.</p>
        <p>{sugestao.coluna} · {sugestao.unidade} · {sugestao.embalagem} · {formatarDataComercial(sugestao.dataComercial)}</p>
        {previa?.prontos.length === 0 && <p>Revise os dados de compra para salvar a conferência e tratar as pendências.</p>}
      </> : <p>Confira a prévia abaixo e confirme os dados de compra para vincular os itens ao catálogo.</p>}
      <a href="#conferencia-comercial">Revisar dados de compra</a>
    </> : <>
      <p>{prontos} pronto(s) · {confirmados} confirmado(s) · {pendentes} pendente(s)</p>
      {prontos === 0 && pendentes > 0 && <p>Veja o que falta em cada item da prévia.</p>}
    </>}
    {previa && (sugestao || prontos > 0) && <ResumoGravacao previa={previa} />}
    {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
    {previa && previa.prontos.length > 0 && <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="operacao" value="confirmar-itens" />
      <input type="hidden" name="revisaoPrevia" value={previa.revisao} />
      {sugestao && <>
        <input type="hidden" name="revisao" value={0} />
        <input type="hidden" name="coluna" value={sugestao.coluna} />
        <input type="hidden" name="unidade" value={sugestao.unidade} />
        <input type="hidden" name="embalagem" value={sugestao.embalagem} />
        <input type="hidden" name="dataComercial" value={sugestao.dataComercial} />
      </>}
      <button type="submit" disabled={fetcher.state !== 'idle'}>
        {fetcher.state === 'idle' ? 'Salvar preços válidos' : 'Salvando…'}
      </button>
    </fetcher.Form>}
  </section>;
}
