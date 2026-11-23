import { Fragment, useState } from 'react';
import { Link, useLoaderData, useOutletContext } from 'react-router';
import { BannerInterno, PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface, type NomeIcone } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import { estados, type FalhaImportacao, type PaginaImportacoes, type TentativaImportacao } from './api';
import styles from './ListaImportacoes.module.css';

const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

const situacoes: Record<TentativaImportacao['status'], { tom: string; icone: NomeIcone }> = {
  PENDENTE: { tom: 'neutro', icone: 'relogio' },
  PROCESSANDO: { tom: 'info', icone: 'relogio' },
  AGUARDANDO_CONFERENCIA: { tom: 'info', icone: 'relogio' },
  CONCLUIDA_COM_PENDENCIAS: { tom: 'atencao', icone: 'atencao' },
  CONCLUIDA: { tom: 'sucesso', icone: 'confirmado' },
  FALHA_EXTRACAO: { tom: 'erro', icone: 'atencao' },
  REJEITADA: { tom: 'erro', icone: 'bloqueado' },
  FALHA_ENVIO: { tom: 'erro', icone: 'atencao' },
  DUPLICADA: { tom: 'erro', icone: 'bloqueado' },
  AGUARDANDO_CONFIRMACAO: { tom: 'info', icone: 'relogio' },
};

export function ListaImportacoes() {
  const usuario = useOutletContext<Identidade>();
  const dados = useLoaderData<PaginaImportacoes | FalhaImportacao>();
  return <PaginaInterna titulo="Importações" usuario={usuario}>
    <BannerInterno compacto titulo={<>Consultar <span>importações</span></>}>
      Acompanhe os envios de PDFs, verifique pendências e consulte os detalhes de cada importação.
    </BannerInterno>
    {'erro' in dados ? <p className={styles.falha} role="alert">{dados.erro}</p> : <Resultado dados={dados} key={dados.pagina} />}
  </PaginaInterna>;
}

function Resumo({ dados }: { dados: PaginaImportacoes }) {
  const indicadores = [
    { nome: 'Total de envios', valor: dados.total, tom: 'sucesso', icone: 'documento' },
    { nome: 'Com pendências', status: 'CONCLUIDA_COM_PENDENCIAS', ...situacoes.CONCLUIDA_COM_PENDENCIAS },
    { nome: 'Aguardando conferência', status: 'AGUARDANDO_CONFERENCIA', ...situacoes.AGUARDANDO_CONFERENCIA },
    { nome: 'Concluídas', status: 'CONCLUIDA', ...situacoes.CONCLUIDA },
  ] as const;

  return <dl className={styles.resumo} aria-label="Resumo de importações">
    {indicadores.map(item => <div className={styles.indicador} key={item.nome}>
      <dt><span className={styles.iconeIndicador} data-tom={item.tom}><IconeInterface nome={item.icone} /></span>{item.nome}{'status' in item && dados.total > dados.itens.length && <small>Nesta página</small>}</dt>
      <dd>{'valor' in item ? item.valor : dados.itens.filter(tentativa => tentativa.status === item.status).length}</dd>
    </div>)}
  </dl>;
}

function Resultado({ dados }: { dados: PaginaImportacoes }) {
  const [busca, setBusca] = useState('');
  const [status, setStatus] = useState('');
  const [fornecedor, setFornecedor] = useState('');
  const fornecedores = [...new Map(dados.itens.map(item => [item.fornecedorCodigo, item.fornecedorNome]))];
  const consulta = busca.trim().toLocaleLowerCase('pt-BR');
  const tentativas = dados.itens.filter(item => item.arquivo.toLocaleLowerCase('pt-BR').includes(consulta) &&
    (!status || item.status === status) && (!fornecedor || item.fornecedorCodigo === fornecedor));

  return <>
    <Resumo dados={dados} />
    <section className={styles.painel} aria-label="Envios de PDF">
      <div className={styles.barra}>
        {dados.itens.length > 0 && <fieldset className={styles.filtros}>
          <legend>{dados.paginas > 1 ? 'Filtrar esta página' : 'Filtrar envios'}</legend>
          <label>Arquivo
            <span className={styles.busca}>
              <IconeInterface nome="buscar" />
              <input type="search" value={busca} onChange={evento => setBusca(evento.target.value)} placeholder="Buscar por nome do arquivo" />
            </span>
          </label>
          <label>Status
            <select value={status} onChange={evento => setStatus(evento.target.value)}>
              <option value="">Todos os status</option>
              {Object.entries(estados).map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
            </select>
          </label>
          <label>Fornecedor
            <select value={fornecedor} onChange={evento => setFornecedor(evento.target.value)}>
              <option value="">Todos os fornecedores</option>
              {fornecedores.map(([codigo, nome]) => <option key={codigo} value={codigo}>{codigo} · {nome}</option>)}
            </select>
          </label>
        </fieldset>}
        <Link className={styles.enviar} to="/importacoes/nova"><IconeInterface nome="enviar" />Enviar PDF</Link>
      </div>

      {dados.itens.length === 0 && <p className={styles.vazio}>{dados.total === 0 ? 'Nenhum envio registrado.' : 'Nenhum envio nesta página.'}</p>}
      {dados.itens.length > 0 && tentativas.length === 0 && <p className={styles.vazio} role="status">Nenhum envio corresponde aos filtros desta página.</p>}
      {tentativas.length > 0 && <Tabela tentativas={tentativas} />}
      {(dados.pagina > 0 || dados.paginas > 1) && <nav className={styles.paginacao} aria-label="Paginação de importações">
        {dados.pagina > 0 && <Link to={`?pagina=${dados.pagina - 1}`}>Página anterior</Link>}
        <span>Página {dados.pagina + 1}{dados.paginas > 0 && ` de ${dados.paginas}`}</span>
        {dados.pagina + 1 < dados.paginas && <Link to={`?pagina=${dados.pagina + 1}`}>Próxima página</Link>}
      </nav>}
    </section>
  </>;
}

function Tabela({ tentativas }: { tentativas: TentativaImportacao[] }) {
  return <div className={styles.moldura}>
    <table className={styles.tabela} aria-label="Importações de PDF" role="table">
      <thead role="rowgroup"><tr role="row">
        <th scope="col" role="columnheader">Arquivo PDF</th><th scope="col" role="columnheader">Fornecedor</th>
        <th scope="col" role="columnheader">Enviado em</th><th scope="col" role="columnheader">Status</th>
        <th scope="col" role="columnheader">Itens</th><th scope="col" role="columnheader">Ações</th>
      </tr></thead>
      <tbody role="rowgroup">{tentativas.map(item => {
        const situacao = situacoes[item.status];
        return <tr role="row" key={item.id}>
          <td role="cell" className={styles.arquivo}>
            <Link to={`/importacoes/${item.id}`} aria-label={`#${item.id} · ${item.arquivo}`}>
              <IconeInterface nome="documento" />
              <span>#{item.id} · {item.arquivo.split(/(?<=[_-])/u).map((trecho, indice) =>
                <Fragment key={indice}>{indice > 0 && <wbr />}{trecho}</Fragment>)}</span>
            </Link>
          </td>
          <td role="cell"><small className={styles.codigo}>{item.fornecedorCodigo}</small>{item.fornecedorNome}</td>
          <td role="cell"><time dateTime={item.enviadaEm}>{dataHora.format(new Date(item.enviadaEm))}</time></td>
          <td role="cell"><span className={styles.status} data-tom={situacao.tom}>
            <IconeInterface nome={situacao.icone} />{estados[item.status]}
          </span></td>
          <td role="cell">{item.quantidadeItens == null ? '—' : `${item.quantidadeItens} ${item.quantidadeItens === 1 ? 'item' : 'itens'}`}</td>
          <td role="cell"><Link className={styles.detalhes} to={`/importacoes/${item.id}`} aria-label={`Ver detalhes da importação #${item.id}`}>Ver detalhes</Link></td>
        </tr>;
      })}</tbody>
    </table>
  </div>;
}
