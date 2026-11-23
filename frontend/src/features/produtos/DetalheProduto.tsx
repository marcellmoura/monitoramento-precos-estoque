import { Link, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import { PaginaInterna } from '../../app/PaginaInterna';
import { formatarMoeda } from '../../shared/decimal';
import { IconeInterface } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import { quantidade, unidades, type FalhaProduto, type Produto } from './api';
import { EstadoProduto } from './EstadoProduto';
import styles from './DetalheProduto.module.css';

export function DetalheProduto() {
  const usuario = useOutletContext<Identidade>();
  const produto = useLoaderData<Produto | FalhaProduto>();
  const [params] = useSearchParams();
  const precoVenda = 'precoVenda' in produto ? produto.precoVenda : undefined;
  const compraVisivel = 'precoCompra' in produto;

  return <PaginaInterna titulo="Produto" usuario={usuario}>
    <nav aria-label="Localização" className={styles.trilha}>
      <Link to="/produtos"><IconeInterface nome="voltar" />Voltar aos produtos</Link>
      {!('erro' in produto) && <><span aria-hidden="true">/</span><span aria-current="page">{produto.codigo}</span></>}
    </nav>

    {'erro' in produto ? <p role="alert" className={styles.erro}>{produto.erro}</p> : <>
      {params.get('criado') === '1' && <p role="status" className={styles.aviso}>Produto cadastrado.</p>}
      {params.get('editado') === '1' && <p role="status" className={styles.aviso}>Produto atualizado.</p>}
      <section className={`${styles.painel} ${styles.identificacao}`} aria-labelledby="titulo-produto">
        <header className={styles.cabecalho}>
          <div className={styles.identidade}>
            <span className={styles.icone}><IconeInterface nome="caixa" /></span>
            <div>
              <p className={styles.codigo}>{produto.codigo}</p>
              <h2 id="titulo-produto">{produto.nome}</h2>
            </div>
            <EstadoProduto ativo={produto.ativo} />
          </div>
          {usuario.perfil !== 'CONSULTA' && <Link to={`/produtos/${produto.id}/editar`} className={styles.editar}>
            <IconeInterface nome="editar" />Editar produto
          </Link>}
        </header>
        <dl className={styles.metadados}>
          <div><dt>Categoria</dt><dd>{produto.categoria ?? 'Sem categoria'}</dd></div>
          <div><dt>Unidade</dt><dd>{unidades[produto.unidade]}</dd></div>
        </dl>
      </section>
      <div className={styles.paineis}>
        <section className={styles.painel} aria-labelledby="titulo-estoque-produto">
          <h2 id="titulo-estoque-produto" className={styles.tituloSecao}>
            <span className={styles.icone}><IconeInterface nome="camadas" /></span>Estoque
          </h2>
          <dl className={styles.indicadores}>
            <div>
              <dt>Estoque atual</dt>
              <dd>{quantidade(produto.estoqueAtual, produto.unidade)} <small>{produto.unidade}</small></dd>
            </div>
            <div>
              <dt>Estoque mínimo</dt>
              <dd>{quantidade(produto.estoqueMinimo, produto.unidade)} <small>{produto.unidade}</small></dd>
            </div>
          </dl>
        </section>

        {(precoVenda !== undefined || compraVisivel) && <section className={styles.painel} aria-labelledby="titulo-precos-produto">
          <h2 id="titulo-precos-produto" className={styles.tituloSecao}>
            <span className={styles.icone}><IconeInterface nome="grafico" /></span>Preços do catálogo
          </h2>
          <dl className={styles.indicadores}>
            {precoVenda !== undefined && <div><dt>Preço de venda</dt><dd>{formatarMoeda(precoVenda)}</dd></div>}
            {compraVisivel && <div>
              <dt>Preço de compra</dt>
              <dd>{produto.precoCompra == null ? <span className={styles.semPreco}>Não informado</span> : formatarMoeda(produto.precoCompra)}</dd>
            </div>}
          </dl>
        </section>}
      </div>
    </>}
  </PaginaInterna>;
}
