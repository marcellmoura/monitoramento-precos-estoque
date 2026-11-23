import { Link, useFetcher, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import { useState } from 'react';
import type { Identidade } from '../acesso/api';
import { formatarMoeda } from '../../shared/decimal';
import { destinoImportacao, unidades } from './api';
import type { ContextoPrecoCotado, FalhaProduto, PaginaProdutos, Produto } from './api';
import styles from '../../shared/gestao.module.css';

const moedaCotada = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function podeCadastrar(usuario: Identidade) {
  return usuario.perfil === 'ADMINISTRADOR' || usuario.perfil === 'COMPRAS';
}
function quantidade(valor: string, unidade: Produto['unidade']) {
  return unidade === 'UN' ? valor.split('.')[0] : valor.replace('.', ',');
}
function Falha({ mensagem }: { mensagem: string }) {
  return <p role="alert" className={styles.erro}>{mensagem}</p>;
}

function CampoUnidade({ valor = '' }: { valor?: Produto['unidade'] | '' }) {
  return <div className={styles.campo}>
    <label htmlFor="unidade-produto">Unidade</label>
    <select id="unidade-produto" name="unidade" defaultValue={valor} required>
      <option value="" disabled>Selecione</option>
      {Object.entries(unidades).map(([codigo, rotulo]) => <option key={codigo} value={codigo}>{rotulo}</option>)}
    </select>
  </div>;
}

export function Produtos() {
  const usuario = useOutletContext<Identidade>();
  const data = useLoaderData<PaginaProdutos | FalhaProduto>();
  const vendaVisivel = usuario.perfil !== 'ESTOQUE';
  const compraVisivel = podeCadastrar(usuario);
  if ('erro' in data) return <section><h2>Produtos</h2><Falha mensagem={data.erro} /><Link to="/">Voltar ao início</Link></section>;
  return <section className={styles.painel}>
    <div className={styles.cabecalho}><h2>Produtos</h2>{podeCadastrar(usuario) && <Link to="/produtos/novo">Cadastrar produto</Link>}</div>
    {data.total === 0 ? <p>Nenhum produto cadastrado.</p> : <>
      <p>{data.total} produto(s) cadastrado(s).</p><p className={styles.ajudaTabela}>Deslize a tabela para ver todas as colunas.</p>
      <div className={styles.tabela} role="region" aria-label="Catálogo de produtos" tabIndex={0}>
        <table><caption>Produtos da loja</caption><thead><tr><th scope="col">Código / produto</th><th scope="col">Unidade</th>
          {vendaVisivel && <th scope="col">Venda</th>}{compraVisivel && <th scope="col">Compra</th>}<th scope="col">Estoque atual / mínimo</th><th scope="col">Estado</th><th scope="col">Ação</th>
        </tr></thead><tbody>{data.itens.map(produto => <tr key={produto.id}>
          <td>{produto.codigo}<small>{produto.nome}</small></td><td>{produto.unidade}</td>
          {vendaVisivel && <td>{produto.precoVenda !== undefined ? formatarMoeda(produto.precoVenda) : 'Indisponível'}</td>}
          {compraVisivel && <td>{produto.precoCompra == null ? 'Não informado' : formatarMoeda(produto.precoCompra)}</td>}
          <td>{quantidade(produto.estoqueAtual, produto.unidade)} / {quantidade(produto.estoqueMinimo, produto.unidade)}</td>
          <td>{produto.ativo ? 'Ativo' : 'Inativo'}</td><td><Link to={`/produtos/${produto.id}`} aria-label={`Consultar ${produto.nome}`}>Consultar</Link></td>
        </tr>)}</tbody></table>
      </div>
      {data.itens.length === 0 && <p>Nenhum produto nesta página.</p>}
      <nav aria-label="Paginação de produtos" className={styles.paginacao}>
        {data.pagina > 0 && <Link to={`?pagina=${data.pagina - 1}`}>Página anterior</Link>}
        <span>Página {data.pagina + 1} de {Math.max(data.paginas, 1)}</span>
        {data.pagina + 1 < data.paginas && <Link to={`?pagina=${data.pagina + 1}`}>Próxima página</Link>}
      </nav>
    </>}
    <Link to="/">Voltar ao início</Link>
  </section>;
}
export function NovoProduto() {
  const [params] = useSearchParams();
  const retorno = destinoImportacao(params);
  const cotacao = useLoaderData<ContextoPrecoCotado | null>();
  const usuario = useOutletContext<Identidade>();
  const fetcher = useFetcher<FalhaProduto>();
  if (!podeCadastrar(usuario)) return <section><h2>Acesso restrito</h2><Falha mensagem="Cadastro disponível para Administrador e Compras." /><Link to="/produtos">Voltar aos produtos</Link></section>;
  return <section className={styles.painel}><h2>Cadastrar produto</h2>
    <p>Estoque atual e mínimo começam em zero. O produto fica ativo após o cadastro.</p>
    {cotacao?.preco != null && <p>
      <strong>{cotacao.conferido ? 'Preço conferido na cotação' : 'Preço cotado no PDF'}</strong>
      {' · '}{moedaCotada.format(cotacao.preco)}{cotacao.embalagem && ` · ${cotacao.embalagem}`}
    </p>}
    <fetcher.Form method="post" className={styles.formulario}>
      <label>Nome<input name="nome" maxLength={255} required /></label>
      <label>Categoria (opcional)<input name="categoria" maxLength={100} /></label>
      <CampoUnidade />
      <p id="ajuda-precos">Preços em reais. Use vírgula ou ponto para os centavos, sem separador de milhar.</p>
      <label>Preço de venda<input name="precoVenda" type="text" inputMode="decimal" maxLength={20} pattern="[0-9]{1,17}([.,][0-9]{1,2})?" aria-describedby="ajuda-precos" required /></label>
      <label>Preço de compra de referência (opcional)<input name="precoCompra" type="text" inputMode="decimal" maxLength={20}
        pattern="[0-9]{1,17}([.,][0-9]{1,2})?" aria-describedby="ajuda-referencia" /></label>
      <p id="ajuda-referencia">Valor interno do catálogo. {retorno && 'Pode ficar em branco. O valor da cotação vai para o histórico quando você confirmar os preços na importação.'}</p>
      {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
      <button type="submit" disabled={fetcher.state !== 'idle'}>{fetcher.state === 'idle' ? 'Cadastrar produto' : 'Salvando…'}</button>
    </fetcher.Form><Link to={retorno ?? "/produtos"}>{retorno ? "Voltar à importação" : "Voltar aos produtos"}</Link>
  </section>;
}
export function DetalheProduto() {
  const usuario = useOutletContext<Identidade>();
  const data = useLoaderData<Produto | FalhaProduto>();
  const [params] = useSearchParams();
  if ('erro' in data) return <section><h2>Produto</h2><Falha mensagem={data.erro} /><Link to="/produtos">Voltar aos produtos</Link></section>;
  return <section className={styles.painel}><h2>{data.nome}</h2>
    {params.get('criado') === '1' && <p role="status">Produto cadastrado.</p>}
    {params.get('editado') === '1' && <p role="status">Produto atualizado.</p>}
    <dl className={styles.dados}>
      <dt>Código</dt><dd>{data.codigo}</dd><dt>Categoria</dt><dd>{data.categoria ?? 'Sem categoria'}</dd>
      <dt>Unidade</dt><dd>{unidades[data.unidade]}</dd>
      {data.precoVenda !== undefined && <><dt>Preço de venda</dt><dd>{formatarMoeda(data.precoVenda)}</dd></>}
      {'precoCompra' in data && <><dt>Preço de compra</dt><dd>{data.precoCompra == null ? 'Não informado' : formatarMoeda(data.precoCompra)}</dd></>}
      <dt>Estoque atual</dt><dd>{quantidade(data.estoqueAtual, data.unidade)}</dd>
      <dt>Estoque mínimo</dt><dd>{quantidade(data.estoqueMinimo, data.unidade)}</dd><dt>Estado</dt><dd>{data.ativo ? 'Ativo' : 'Inativo'}</dd>
    </dl>
    <nav aria-label="Ações do produto" className={styles.paginacao}>
      {usuario.perfil !== 'CONSULTA' && <Link to={`/produtos/${data.id}/editar`}>Editar produto</Link>}
      <Link to="/produtos">Voltar aos produtos</Link>
    </nav>
  </section>;
}

export function EditarProduto() {
  const usuario = useOutletContext<Identidade>();
  const produto = useLoaderData<Produto | FalhaProduto>();
  if (usuario.perfil === 'CONSULTA') {
    return <section><h2>Acesso restrito</h2><Falha mensagem="Seu perfil não permite editar produtos." />
      <Link to="/produtos">Voltar aos produtos</Link></section>;
  }
  if ('erro' in produto) {
    return <section><h2>Editar produto</h2><Falha mensagem={produto.erro} />
      <Link to="/produtos">Voltar aos produtos</Link></section>;
  }
  return <FormularioEdicao key={`${produto.id}-${usuario.perfil}`} produto={produto} usuario={usuario} />;
}

function FormularioEdicao({ produto, usuario }: { produto: Produto; usuario: Identidade }) {
  const fetcher = useFetcher<FalhaProduto>();
  const [original] = useState(produto);
  const administrador = usuario.perfil === 'ADMINISTRADOR';
  return <section className={styles.painel}>
    <h2>Editar produto</h2>
    <p>{original.codigo} · {original.nome}</p>
    <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="versao" value={original.versao} readOnly />
      {podeCadastrar(usuario) && <>
        <label>Nome<input name="nome" defaultValue={original.nome} maxLength={255} required /></label>
        <label>Categoria (opcional)<input name="categoria" defaultValue={original.categoria ?? ''} maxLength={100} /></label>
      </>}
      {administrador && <>
        {original.unidadeEditavel ? <CampoUnidade valor={original.unidade} /> : <p>
          Unidade: {unidades[original.unidade]}. A troca direta exige estoques zerados e ausência de histórico ou vínculos.
        </p>}
        <label>Preço de venda<input name="precoVenda" defaultValue={original.precoVenda?.replace('.', ',')}
          inputMode="decimal" maxLength={20} pattern="[0-9]{1,17}([.,][0-9]{1,2})?" required /></label>
        <div className={styles.campo}><label htmlFor="estado-produto">Estado</label>
          <select id="estado-produto" name="ativo" defaultValue={String(original.ativo)}>
            <option value="true">Ativo</option><option value="false">Inativo</option>
          </select>
        </div>
      </>}
      {(administrador || usuario.perfil === 'ESTOQUE') && <>
        <p id="ajuda-estoques">Use número inteiro para UN e até três casas decimais para as demais unidades.</p>
        <label>Estoque atual<input name="estoqueAtual" defaultValue={quantidade(original.estoqueAtual, original.unidade)}
          inputMode="decimal" maxLength={20} pattern="[0-9]{1,16}([.,][0-9]{1,3})?" aria-describedby="ajuda-estoques" required /></label>
        <label>Estoque mínimo<input name="estoqueMinimo" defaultValue={quantidade(original.estoqueMinimo, original.unidade)}
          inputMode="decimal" maxLength={20} pattern="[0-9]{1,16}([.,][0-9]{1,3})?" aria-describedby="ajuda-estoques" required /></label>
      </>}
      {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
      <button type="submit" disabled={fetcher.state !== 'idle'}>
        {fetcher.state === 'idle' ? 'Salvar alterações' : 'Salvando…'}
      </button>
    </fetcher.Form>
    <Link to={`/produtos/${original.id}`}>Voltar aos detalhes</Link>
  </section>;
}
