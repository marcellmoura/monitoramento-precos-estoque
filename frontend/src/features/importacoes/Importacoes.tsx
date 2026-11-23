import { Link, useFetcher, useLoaderData, useOutletContext, useRevalidator, useSearchParams } from 'react-router';
import { Fragment, useEffect, useId, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import { AssociacaoItem } from './AssociacaoItem';
import { CorrecaoItem } from './CorrecaoItem';
import { FormularioConferencia } from './FormularioConferencia';
import { estados, formatarDataComercial } from './api';
import type { ClassificacaoPreco, ConflitoPreco, LinhaConfirmacao, PreviaGravacao, ProdutoCatalogo, SituacaoVinculo, ConferenciaCotacao, RevisaoItem, DetalheTentativa, FalhaImportacao, PreviaImportacao } from './api';
import styles from '../../shared/gestao.module.css';
import importacoes from './Importacoes.module.css';

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

function Falha({ mensagem }: { mensagem: string }) {
  return <p role="alert" className={styles.erro}>{mensagem}</p>;
}

export function DetalheImportacao() {
  const usuario = useOutletContext<Identidade>();
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
    return <PaginaInterna titulo="Detalhes da importação" usuario={usuario}>
      <section className={`${importacoes.cotacao} ${importacoes.painelCotacao}`}>
        <Falha mensagem={tentativa.erro} />
        <Link to="/importacoes">Voltar às importações</Link>
      </section>
    </PaginaInterna>;
  }
  const linhasPrecos = tentativa.precos?.itens ?? [];
  const bloqueada = linhasPrecos.some(linha => linha.estado === 'CONFIRMADO');
  return <PaginaInterna titulo="Detalhes da importação" usuario={usuario}>
    <div className={importacoes.cotacao}>
      <nav className={importacoes.trilha} aria-label="Localização">
        <Link to="/importacoes">Voltar às importações</Link>
        <span aria-hidden="true">/</span>
        <span aria-current="page">Tentativa #{tentativa.id}</span>
      </nav>
      <header className={importacoes.cabecalhoCotacao}>
        <div className={importacoes.resumoEnvio}>
          <div className={importacoes.tituloCotacao}>
            <span className={importacoes.iconeTitulo}><IconeInterface nome="documento" /></span>
            <div>
              <h2>{tentativa.fornecedorNome}</h2>
              <p className={importacoes.codigoFornecedor}>{tentativa.fornecedorCodigo}</p>
              <p className={importacoes.arquivoEnvio}><span className={importacoes.soLeitor}>Arquivo </span>{tentativa.arquivo}</p>
            </div>
          </div>
          <dl className={importacoes.dadosCotacao}>
            <div><dt>Enviado em</dt><dd><time dateTime={tentativa.enviadaEm}>{dataHora.format(new Date(tentativa.enviadaEm))}</time></dd></div>
            {tentativa.paginas != null && <div><dt>Páginas</dt><dd>{tentativa.paginas}</dd></div>}
            {tentativa.quantidadeItens != null && <div><dt>Itens</dt><dd>{tentativa.quantidadeItens}</dd></div>}
          </dl>
          <p role="status" className={importacoes.estadoCotacao} data-estado={tentativa.status}>{estados[tentativa.status]}</p>
        </div>
        {tentativa.motivo && <p className={importacoes.motivoEnvio}>{tentativa.motivo}</p>}
        <nav className={importacoes.acoesCotacao} aria-label="Ações da tentativa">
          {tentativa.importacaoExistenteId != null && <Link className={importacoes.abrirOriginal} to={`/importacoes/${tentativa.importacaoExistenteId}`}>Abrir tentativa original</Link>}
          <Link to={`/historico-precos?tentativa=${tentativa.id}`}>Ver histórico desta tentativa</Link>
        </nav>
      </header>
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
        previaGravacao={tentativa.precos?.previaGravacao}
        precos={linhasPrecos} tentativa={tentativa.id} linhaRetorno={linhaRetorno} produtoCriado={tentativa.produtoCriado}
        key={`previa-${tentativa.id}-${params.get('produto') ?? ''}`} />}
      {tentativa.conferencia && <FormularioConferencia conferencia={tentativa.conferencia} bloqueada={bloqueada}
        key={`conferencia-${tentativa.id}-${tentativa.conferencia.confirmada?.id ?? 0}`} />}
    </div>
  </PaginaInterna>;
}

function abrirDetalhesItem(id: string) {
  const detalhes = document.getElementById(id)?.querySelector('details');
  if (!detalhes) return;
  detalhes.open = true;
  detalhes.querySelector('summary')?.focus({ preventScroll: true });
}

function DetalhesItem({ abrir, children }: { abrir: boolean; children: ReactNode }) {
  const detalhes = useRef<HTMLDetailsElement>(null);
  useLayoutEffect(() => {
    if (abrir && detalhes.current) detalhes.current.open = true;
  }, [abrir]);
  return <details ref={detalhes}>{children}</details>;
}

function Previa({ previa, conferencia, revisoes, vinculos, precos, previaGravacao, tentativa, linhaRetorno, produtoCriado }: {
  previa: PreviaImportacao; conferencia?: ConferenciaCotacao; revisoes: RevisaoItem[];
  vinculos: SituacaoVinculo[]; precos: LinhaConfirmacao[]; previaGravacao?: PreviaGravacao | null;
  tentativa: number; linhaRetorno: number; produtoCriado?: ProdutoCatalogo;
}) {
  const precosPorLinha = new Map(precos.map(preco => [preco.linha, preco]));
  const prontosPorLinha = new Map(previaGravacao?.prontos.map(item => [item.linha, item.preco]));
  const vinculosPorLinha = new Map(vinculos.map(vinculo => [vinculo.linha, vinculo]));
  const porLinha = new Map(revisoes.map(revisao => [revisao.linha, revisao]));
  const base = conferencia?.confirmada;
  const leituraParcial = previa.itens.some(item => item.dados === null);
  useEffect(() => {
    function abrirItemDaUrl() {
      const alvo = window.location.hash.slice(1);
      if (/^item-\d+$/.test(alvo)) abrirDetalhesItem(alvo);
    }
    abrirItemDaUrl();
    window.addEventListener('hashchange', abrirItemDaUrl);
    return () => window.removeEventListener('hashchange', abrirItemDaUrl);
  }, [tentativa]);
  return <section className={importacoes.painelCotacao} aria-labelledby="previa-itens">
    <div className={importacoes.cabecalhoPrevia}>
      <h2 id="previa-itens" tabIndex={-1} className={importacoes.tituloSecao}><IconeInterface nome="caixa" />Prévia dos itens</h2>
      <p>Data no PDF: {previa.dataReferencia ?? 'Não informada'}</p>
    </div>
    {leituraParcial && <p role="alert">Leitura parcial. Confira as linhas não reconhecidas no PDF.</p>}
    <ol role="list" className={importacoes.listaItens}>
      {previa.itens.map((item, indice) => {
        const { dados, precos, textoOriginal, pendencias } = item;
        const linha = indice + 1;
        const revisao = porLinha.get(linha);
        const situacao = vinculosPorLinha.get(linha);
        const preco = precosPorLinha.get(linha);
        const confirmado = preco?.estado === 'CONFIRMADO';
        const substituido = preco?.confirmado?.substituido === true;
        const associado = confirmado || Boolean(situacao?.reaproveitado || (situacao?.vinculo && !situacao.revalidar));
        const avisosComerciais = revisao ? [] : conferencia?.pendenciasComerciais[indice] ?? [];
        const precoSugerido = base ? undefined : prontosPorLinha.get(linha);
        const pronto = preco?.estado === 'PRONTO' || precoSugerido !== undefined;
        const temAvisos = pendencias.length > 0 || avisosComerciais.length > 0 || Boolean(base && !associado);
        const pendente = !confirmado && !pronto && (!dados || preco?.estado === 'PENDENTE' || temAvisos);
        const estado = confirmado ? (substituido ? 'Substituído' : 'Confirmado') : pronto ? 'Pronto para salvar' : pendente ? 'Pendente' : 'Extraído do PDF';
        const valor = precoSugerido ?? preco?.preco;
        return <li key={linha} id={`item-${linha}`} className={importacoes.itemCotacao} data-pendente={pendente}>
          <DetalhesItem abrir={pendente || substituido || Boolean(base && !confirmado && temAvisos) || linha === linhaRetorno}>
            <summary className={importacoes.resumoItem}>
              <span className={importacoes.nomeItem}>
                <strong>{dados?.descricao ?? 'Linha não reconhecida'}</strong>
                {dados && <small>Ref. {dados.referencia || 'não informada'} · {dados.apresentacao || 'Apresentação não informada'}</small>}
              </span>
              {dados && <span className={importacoes.medidasItem}>
                <span><small>Unidade</small><strong>{dados.unidade || '—'}</strong></span>
                {dados.quantidade && <span><small>Quantidade</small><strong>{dados.quantidade}</strong></span>}
                {Object.entries(precos).map(([coluna, valor]) => <span key={coluna} className={importacoes.valorItem}>
                  <small>{coluna}</small><strong>{valor || '—'}</strong>
                </span>)}
              </span>}
              <span className={importacoes.estadoItem} data-pendente={pendente}>
                {estado}{valor != null && <small>{moeda.format(valor)}</small>}
              </span>
              <span className={importacoes.setaItem}><IconeInterface nome="seta" /></span>
            </summary>
            <div className={importacoes.detalhesItem}>
              {revisao && <p className={importacoes.notaItem}>A linha acima mostra os dados originais do PDF.</p>}
              {!dados && <p>{textoOriginal}</p>}
              {pendencias.length > 0 && <p className={importacoes.avisoCotacao}>{pendencias.join(' ')}</p>}
              {pendente && conferencia && !base && pendencias.length === 0 && avisosComerciais.length === 0 && !preco?.conflito &&
                <p className={importacoes.avisoCotacao}>
                  <a href="#conferencia-comercial">Revise os dados de compra</a> para continuar a conferência deste item.
                </p>}
              {!revisao && <>
                {base && !preco && pendencias.length === 0 && !associado && <p>Pendente de associação.</p>}
                {avisosComerciais.map(aviso => <p className={importacoes.avisoCotacao} key={aviso}>{aviso}</p>)}
              </>}
              {base && <CorrecaoItem item={item} linha={linha} base={base} colunas={conferencia?.colunas ?? []}
                revisao={revisao} associado={associado} bloqueado={confirmado} />}
              {situacao && !confirmado && <AssociacaoItem tentativa={tentativa} situacao={situacao} abrirAoVoltar={linha === linhaRetorno}
                produtoCriado={linha === linhaRetorno ? produtoCriado : undefined} />}
              {preco && (base || preco.estado !== 'PENDENTE' || preco.conflito) && <>
                {preco.classificacao && <p className={importacoes.classificacao} data-classificacao={preco.classificacao}>
                  <strong>Classificação</strong> · {classificacoes[preco.classificacao]}
                </p>}
                <EstadoPreco linha={preco} />
              </>}
            </div>
          </DetalhesItem>
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
    <p className={importacoes.avisoCotacao}>Pendência para confirmar · {linha.motivos.join(' ')}</p>
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
    <h3>{previa.prontos.length === 1 ? '1 preço pronto para salvar' : `${previa.prontos.length} preços prontos para salvar`}</h3>
    {previa.prontos.length > 0 && <ul role="list" className={importacoes.listaPrecos}>
      {previa.prontos.map(item => <li key={item.linha}>
        <a href={`#item-${item.linha}`} onClick={() => abrirDetalhesItem(`item-${item.linha}`)}>{[item.descricao, item.referencia, item.embalagem].filter(Boolean).join(' · ')}</a>
        <p>{item.anterior ? <>De {moeda.format(item.anterior.preco)} para </> : null}
          <strong>{moeda.format(item.preco)}</strong>{item.variacao &&
            <span className={importacoes.variacao}>{` (${percentual.format(item.variacao.percentual)}%)`}</span>}
        </p>
      </li>)}
    </ul>}
    {previa.pendentes > 0 && <p className={importacoes.avisoCotacao}>
      {previa.pendentes === 1 ? '1 item ficará pendente.' : `${previa.pendentes} itens ficarão pendentes.`}
    </p>}
  </div>;
}

function ConfirmacaoItens({ linhas, conferencia, previa }: {
  linhas: LinhaConfirmacao[]; conferencia?: ConferenciaCotacao; previa?: PreviaGravacao | null;
}) {
  const fetcher = useFetcher<FalhaImportacao>();
  const prontos = linhas.filter(linha => linha.estado === 'PRONTO').length;
  const confirmados = linhas.filter(linha => linha.estado === 'CONFIRMADO').length;
  const pendentes = linhas.length - prontos - confirmados;
  const temPendencias = (previa?.pendentes ?? pendentes) > 0;
  const exigeConferencia = conferencia && !conferencia.confirmada;
  const sugestao = exigeConferencia && conferencia.sugestao
    ? { ...conferencia.sugestao, dataComercial: conferencia.dataSugerida }
    : null;
  const calculoSugerido = sugestao ? previa : null;
  return <section className={importacoes.proximoPasso} aria-labelledby="confirmar-precos">
    <h2 id="confirmar-precos" className={importacoes.tituloSecao}><IconeInterface nome="grafico" />Confirmação de preços</h2>
    {exigeConferencia && <>
      {sugestao ? <>
        <p>Podemos reaproveitar os dados de compra da última cotação deste fornecedor.</p>
        <p>{sugestao.coluna} · {sugestao.unidade} · {sugestao.embalagem} · {formatarDataComercial(sugestao.dataComercial)}</p>
        {previa?.prontos.length === 0 && <p>Revise os dados de compra para salvar a conferência e tratar as pendências.</p>}
      </> : <p>Confira a prévia abaixo e confirme os dados de compra para vincular os itens ao catálogo.</p>}
    </>}
    {(!exigeConferencia || calculoSugerido) && <>
      <dl className={importacoes.contadores}>
        <div><dt><IconeInterface nome="confirmado" />Prontos</dt><dd>{calculoSugerido?.prontos.length ?? prontos}</dd></div>
        <div><dt><IconeInterface nome="documento" />Confirmados</dt><dd>{confirmados}</dd></div>
        <div className={temPendencias ? importacoes.contadorPendente : undefined}>
          <dt><IconeInterface nome="atencao" />Pendentes</dt><dd>{calculoSugerido?.pendentes ?? pendentes}</dd>
        </div>
      </dl>
    </>}
    {previa && (sugestao || prontos > 0) && <ResumoGravacao previa={previa} />}
    {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
    {(exigeConferencia || temPendencias || Boolean(previa?.prontos.length)) && <div className={importacoes.acoesPrecos}>
      <div className={importacoes.atalhosPrecos}>
        {exigeConferencia && <a className={importacoes.revisar} href="#conferencia-comercial">Revisar dados de compra</a>}
        {temPendencias && <a href="#previa-itens">Ver pendências na prévia</a>}
      </div>
      {previa && previa.prontos.length > 0 && <fetcher.Form method="post" className={`${styles.formulario} ${importacoes.salvarPrecos}`}>
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
    </div>}
  </section>;
}
