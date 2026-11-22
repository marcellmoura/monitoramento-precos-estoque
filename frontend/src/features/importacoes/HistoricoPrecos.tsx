import { Form, Link, useLoaderData, useSearchParams } from 'react-router';
import { formatarDataComercial } from './api';
import type { FalhaImportacao, PaginaHistorico, PrecoHistorico, VinculoSubstituicao } from './api';
import styles from '../../shared/gestao.module.css';

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function HistoricoPrecos() {
  const dados = useLoaderData<PaginaHistorico | FalhaImportacao>();
  const [filtros] = useSearchParams();

  function destinoPagina(pagina: number) {
    const parametros = new URLSearchParams(filtros);
    parametros.set('pagina', String(pagina));
    return `?${parametros}`;
  }

  return <section className={styles.painel}>
    <h2>Histórico de preços</h2>
    <Form method="get" key={filtros.toString()} className={styles.formulario}>
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
      <button type="submit">Filtrar histórico</button>
      {filtros.size > 0 && <Link to="/historico-precos">Limpar filtros</Link>}
    </Form>
    {'erro' in dados ? <p role="alert" className={styles.erro}>{dados.erro}</p> : <>
      {dados.itens.length > 0 ? <TabelaHistorico itens={dados.itens} /> :
        <p>{dados.total === 0 ? 'Nenhum preço encontrado para os filtros informados.' : 'Nenhum preço nesta página.'}</p>}
      {(dados.pagina > 0 || dados.paginas > 1) && <nav className={styles.paginacao} aria-label="Paginação do histórico de preços">
        {dados.pagina > 0 && <Link to={destinoPagina(dados.pagina - 1)}>Página anterior</Link>}
        {dados.pagina + 1 < dados.paginas && <Link to={destinoPagina(dados.pagina + 1)}>Próxima página</Link>}
      </nav>}
    </>}
    <nav className={styles.paginacao} aria-label="Navegação do histórico">
      <Link to="/importacoes">Consultar importações e pendências</Link>
      <Link to="/">Voltar ao início</Link>
    </nav>
  </section>;
}

function TabelaHistorico({ itens }: { itens: PrecoHistorico[] }) {
  return <>
    <p className={styles.ajudaTabela}>Deslize a tabela para ver a origem e as substituições.</p>
    <div className={styles.tabela} role="region" aria-label="Resultados do histórico de preços" tabIndex={0}>
      <table>
        <caption>Preços registrados</caption>
        <thead>
          <tr>
            <th scope="col">Data comercial</th>
            <th scope="col">Produto</th>
            <th scope="col">Fornecedor</th>
            <th scope="col">Apresentação</th>
            <th scope="col">Preço</th>
            <th scope="col">Origem e substituições</th>
          </tr>
        </thead>
        <tbody>
          {itens.map(item => <tr key={item.id}>
            <td>{formatarDataComercial(item.dataComercial)}
              {item.ordem !== null && <small>Posição {item.ordem} no dia</small>}
            </td>
            <td>{item.produto.nome}<small>{item.produto.codigo}</small></td>
            <td>{item.fornecedor.nome}<small>{item.fornecedor.codigo}</small></td>
            <td>{item.unidade} · {item.embalagem}<small>{item.modalidade === 'VERIFICACAO_MANUAL' ? 'Verificação manual' : item.modalidade}</small></td>
            <td>{moeda.format(item.preco)}<small>{item.ordem === null ? 'Substituído' : 'Vigente'}</small></td>
            <td>
              <Link to={`/importacoes/${item.origem.tentativa}?linha=${item.origem.linha}`}>Tentativa #{item.origem.tentativa}</Link>
              <small>{item.origem.arquivo}</small>
              {item.substituiu && <Substituicao rotulo="Substituiu" vinculo={item.substituiu} />}
              {item.substituidoPor && <Substituicao rotulo="Substituído por" vinculo={item.substituidoPor} />}
            </td>
          </tr>)}
        </tbody>
      </table>
    </div>
  </>;
}

function Substituicao({ rotulo, vinculo }: { rotulo: string; vinculo: VinculoSubstituicao }) {
  return <small>
    {rotulo} {moeda.format(vinculo.preco)} ·{' '}
    <Link to={`/importacoes/${vinculo.tentativa}?linha=${vinculo.linha}`}>Tentativa #{vinculo.tentativa}</Link>
    <br />Motivo · {vinculo.motivo}
  </small>;
}
