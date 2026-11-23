import { Form, Link, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import { BannerInterno, PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import { formatarDataComercial } from './api';
import type { FalhaImportacao, PaginaHistorico, PrecoHistorico, VinculoSubstituicao } from './api';
import styles from './HistoricoPrecos.module.css';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function HistoricoPrecos() {
  const usuario = useOutletContext<Identidade>();
  const dados = useLoaderData<PaginaHistorico | FalhaImportacao>();
  const [filtros] = useSearchParams();

  function destinoPagina(pagina: number) {
    const parametros = new URLSearchParams(filtros);
    parametros.set('pagina', String(pagina));
    return `?${parametros}`;
  }

  return <PaginaInterna titulo="Preços" usuario={usuario}>
    <BannerInterno compacto titulo={<>Histórico de <span>preços</span></>}>
      Consulte os preços confirmados, veja de qual cotação vieram e acompanhe as substituições.
    </BannerInterno>

    <section className={styles.painel} aria-labelledby="titulo-filtros">
      <h2 id="titulo-filtros" className={styles.titulo}><span><IconeInterface nome="filtro" /></span>Filtrar histórico</h2>
      <Form method="get" key={filtros.toString()} className={styles.filtros}>
        <label>Código do produto
          <input name="produto" maxLength={23} defaultValue={filtros.get('produto') ?? ''} placeholder="PRD-000001" />
        </label>
        <label>Código do fornecedor
          <input name="fornecedor" maxLength={23} defaultValue={filtros.get('fornecedor') ?? ''} placeholder="FOR-000001" />
        </label>
        <label>Número da tentativa
          <input name="tentativa" inputMode="numeric" pattern="[1-9][0-9]*" maxLength={19}
            defaultValue={filtros.get('tentativa') ?? ''} />
        </label>
        <label>Data comercial inicial
          <input type="date" name="inicio" max="9999-12-31" defaultValue={filtros.get('inicio') ?? ''} />
        </label>
        <label>Data comercial final
          <input type="date" name="fim" max="9999-12-31" defaultValue={filtros.get('fim') ?? ''} />
        </label>
        <button type="submit"><IconeInterface nome="filtro" />Filtrar histórico</button>
        {filtros.size > 0 && <Link className={styles.limpar} to="/historico-precos">Limpar filtros</Link>}
      </Form>
      {'erro' in dados && <p role="alert" className={styles.erro}>{dados.erro}</p>}
    </section>

    <section className={styles.painel} aria-labelledby="titulo-precos-registrados">
      <h2 id="titulo-precos-registrados" className={styles.titulo}><span><IconeInterface nome="camadas" /></span>Preços registrados</h2>
      {!('erro' in dados) && <>
        {dados.itens.length > 0 ? <TabelaHistorico itens={dados.itens} /> :
          <p className={styles.vazio}>{dados.total === 0 ? 'Nenhum preço encontrado para os filtros informados.' : 'Nenhum preço nesta página.'}</p>}
        {(dados.pagina > 0 || dados.paginas > 1) && <nav className={styles.paginacao} aria-label="Paginação do histórico de preços">
          {dados.pagina > 0 && <Link to={destinoPagina(dados.pagina - 1)}>Página anterior</Link>}
          <span>Página {dados.pagina + 1}{dados.paginas > 0 && ` de ${dados.paginas}`}</span>
          {dados.pagina + 1 < dados.paginas && <Link to={destinoPagina(dados.pagina + 1)}>Próxima página</Link>}
        </nav>}
      </>}
      <nav className={styles.links} aria-label="Navegação do histórico">
        <Link to="/importacoes">Consultar importações e pendências</Link>
        <Link to="/">Voltar ao início</Link>
      </nav>
    </section>
  </PaginaInterna>;
}

function TabelaHistorico({ itens }: { itens: PrecoHistorico[] }) {
  return <div className={styles.moldura}>
    <table className={styles.tabela} aria-labelledby="titulo-precos-registrados" role="table">
      <thead role="rowgroup">
        <tr role="row">
          <th scope="col" role="columnheader">Data comercial</th>
          <th scope="col" role="columnheader">Produto</th>
          <th scope="col" role="columnheader">Fornecedor</th>
          <th scope="col" role="columnheader">Apresentação</th>
          <th scope="col" role="columnheader">Preço</th>
          <th scope="col" role="columnheader">Origem e substituições</th>
        </tr>
      </thead>
      <tbody role="rowgroup">
        {itens.map(item => <tr key={item.id} role="row">
          <td role="cell">
            <span className={styles.rotuloMovel} aria-hidden="true">Data comercial</span>
            <time dateTime={item.dataComercial}>{formatarDataComercial(item.dataComercial)}</time>
            {item.ordem !== null && <small>Posição {item.ordem} no dia</small>}
          </td>
          <td role="cell">
            <span className={styles.rotuloMovel} aria-hidden="true">Produto</span>
            <strong>{item.produto.nome}</strong><small>{item.produto.codigo}</small>
          </td>
          <td role="cell">
            <span className={styles.rotuloMovel} aria-hidden="true">Fornecedor</span>
            {item.fornecedor.nome}<small>{item.fornecedor.codigo}</small>
          </td>
          <td role="cell">
            <span className={styles.rotuloMovel} aria-hidden="true">Apresentação</span>
            {item.unidade} · {item.embalagem}<small>{item.modalidade === 'VERIFICACAO_MANUAL' ? 'Verificação manual' : item.modalidade}</small>
          </td>
          <td role="cell" className={styles.preco}>
            <span className={styles.rotuloMovel} aria-hidden="true">Preço</span>
            <strong>{moeda.format(item.preco)}</strong>
            <span className={styles.estado} data-substituido={item.ordem === null}>
              <IconeInterface nome={item.ordem === null ? 'bloqueado' : 'confirmado'} />
              {item.ordem === null ? 'Substituído' : 'Vigente'}
            </span>
          </td>
          <td role="cell" className={styles.origem}>
            <span className={styles.rotuloMovel} aria-hidden="true">Origem e substituições</span>
            <Link to={`/importacoes/${item.origem.tentativa}?linha=${item.origem.linha}`}>Tentativa #{item.origem.tentativa}</Link>
            <small>{item.origem.arquivo}</small>
            {item.substituiu && <Substituicao rotulo="Substituiu" vinculo={item.substituiu} />}
            {item.substituidoPor && <Substituicao rotulo="Substituído por" vinculo={item.substituidoPor} />}
          </td>
        </tr>)}
      </tbody>
    </table>
  </div>;
}

function Substituicao({ rotulo, vinculo }: { rotulo: string; vinculo: VinculoSubstituicao }) {
  return <div className={styles.substituicao}>
    {rotulo} {moeda.format(vinculo.preco)} ·{' '}
    <Link to={`/importacoes/${vinculo.tentativa}?linha=${vinculo.linha}`}>Tentativa #{vinculo.tentativa}</Link>
    <small>Motivo · {vinculo.motivo}</small>
  </div>;
}
