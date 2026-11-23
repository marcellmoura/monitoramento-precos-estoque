import { Form, Link, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import { BannerInterno, PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface } from '../../shared/IconeInterface';
import { formatarMoeda } from '../../shared/decimal';
import type { Identidade } from '../acesso/api';
import { podeCadastrar, quantidade, unidades, type FalhaProduto, type PaginaProdutos, type Produto } from './api';
import { EstadoProduto } from './EstadoProduto';
import styles from './ListaProdutos.module.css';

export function ListaProdutos() {
  const usuario = useOutletContext<Identidade>();
  const dados = useLoaderData<PaginaProdutos | FalhaProduto>();
  const [parametros] = useSearchParams();
  const filtrado = ['busca', 'ativo', 'unidade'].some(chave => Boolean(parametros.get(chave)?.trim()));
  const cadastroVisivel = podeCadastrar(usuario);

  function destinoPagina(pagina: number) {
    const destino = new URLSearchParams(parametros);
    destino.set('pagina', String(pagina));
    return `?${destino}`;
  }

  return <PaginaInterna titulo="Produtos" usuario={usuario}>
    <BannerInterno compacto titulo={<>Produtos <span>cadastrados</span></>}>
      Consulte o catálogo, as unidades e o estoque dos produtos da loja.
    </BannerInterno>
    <section className={styles.painel} aria-labelledby="titulo-catalogo">
      <header className={styles.cabecalho}>
        <div className={styles.titulo}>
          <span className={styles.icone}><IconeInterface nome="caixa" /></span>
          <div>
            <h2 id="titulo-catalogo">Produtos da loja</h2>
            {!('erro' in dados) && dados.total > 0 && <p>
              {dados.total === 1 ? '1 produto' : `${dados.total} produtos`} {filtrado
                ? (dados.total === 1 ? 'encontrado' : 'encontrados')
                : (dados.total === 1 ? 'cadastrado' : 'cadastrados')}.
            </p>}
          </div>
        </div>
        {cadastroVisivel && <Link to="/produtos/novo" className={styles.cadastrar}><IconeInterface nome="adicionar" />Cadastrar produto</Link>}
      </header>

      <Form method="get" key={parametros.toString()} className={styles.filtros} aria-label="Filtrar catálogo">
        <label>Buscar por código ou nome
          <span className={styles.busca}>
            <IconeInterface nome="buscar" />
            <input type="search" name="busca" maxLength={255} placeholder="Código ou nome do produto" defaultValue={parametros.get('busca') ?? ''} />
          </span>
        </label>
        <label>Estado
          <select name="ativo" defaultValue={parametros.get('ativo') ?? ''}>
            <option value="">Todos</option>
            <option value="true">Ativos</option>
            <option value="false">Inativos</option>
          </select>
        </label>
        <label>Unidade
          <select name="unidade" defaultValue={parametros.get('unidade') ?? ''}>
            <option value="">Todas</option>
            {Object.entries(unidades).map(([codigo, nome]) => <option key={codigo} value={codigo}>{nome}</option>)}
          </select>
        </label>
        <button type="submit" className={styles.filtrar}>Buscar</button>
        {filtrado && <Link to="/produtos" className={styles.limpar}>Limpar filtros</Link>}
      </Form>

      {'erro' in dados ? <p role="alert" className={styles.erro}>{dados.erro}</p> : <>
        {dados.itens.length > 0 && <TabelaProdutos itens={dados.itens} vendaVisivel={usuario.perfil !== 'ESTOQUE'} compraVisivel={cadastroVisivel} />}
        {dados.itens.length === 0 && <p className={styles.vazio}>{dados.total > 0 ? 'Nenhum produto nesta página.'
          : filtrado ? 'Nenhum produto corresponde aos filtros.' : 'Nenhum produto cadastrado.'}</p>}
        {dados.total > 0 && <nav aria-label="Paginação de produtos" className={styles.paginacao}>
          {dados.pagina > 0 && <Link to={destinoPagina(dados.pagina - 1)}>Página anterior</Link>}
          <span>Página {dados.pagina + 1} de {Math.max(dados.paginas, 1)}</span>
          {dados.pagina + 1 < dados.paginas && <Link to={destinoPagina(dados.pagina + 1)}>Próxima página</Link>}
        </nav>}
      </>}
      <Link className={styles.voltar} to="/">Voltar ao início</Link>
    </section>
  </PaginaInterna>;
}

function TabelaProdutos({ itens, vendaVisivel, compraVisivel }: { itens: Produto[]; vendaVisivel: boolean; compraVisivel: boolean }) {
  return <div className={styles.moldura}>
    <table className={styles.tabela} aria-labelledby="titulo-catalogo" role="table">
      <thead role="rowgroup"><tr role="row">
        <th scope="col" role="columnheader" className={styles.colunaProduto}>Código / produto</th>
        <th scope="col" role="columnheader">Unidade</th>
        {vendaVisivel && <th scope="col" role="columnheader">Venda</th>}
        {compraVisivel && <th scope="col" role="columnheader">Compra</th>}
        <th scope="col" role="columnheader">Estoque atual / mínimo</th>
        <th scope="col" role="columnheader">Estado</th>
        <th scope="col" role="columnheader">Ação</th>
      </tr></thead>
      <tbody role="rowgroup">{itens.map(produto => <tr key={produto.id} role="row">
        <th scope="row" role="rowheader" className={styles.produto}><strong>{produto.codigo}</strong><small>{produto.nome}</small></th>
        <td role="cell"><span className={styles.rotuloMovel} aria-hidden="true">Unidade</span>{produto.unidade}</td>
        {vendaVisivel && <td role="cell"><span className={styles.rotuloMovel} aria-hidden="true">Venda</span>
          {produto.precoVenda !== undefined ? formatarMoeda(produto.precoVenda) : 'Indisponível'}
        </td>}
        {compraVisivel && <td role="cell"><span className={styles.rotuloMovel} aria-hidden="true">Compra</span>
          {produto.precoCompra == null ? 'Não informado' : formatarMoeda(produto.precoCompra)}
        </td>}
        <td role="cell" className={styles.estoque}><span className={styles.rotuloMovel} aria-hidden="true">Estoque atual / mínimo</span>
          {quantidade(produto.estoqueAtual, produto.unidade)} / {quantidade(produto.estoqueMinimo, produto.unidade)}
        </td>
        <td role="cell"><span className={styles.rotuloMovel} aria-hidden="true">Estado</span>
          <EstadoProduto ativo={produto.ativo} />
        </td>
        <td role="cell"><Link className={styles.consultar} to={`/produtos/${produto.id}`} aria-label={`Consultar ${produto.nome}`}>Consultar</Link></td>
      </tr>)}</tbody>
    </table>
  </div>;
}
