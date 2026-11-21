import { Link, useFetcher, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import type { Identidade } from '../acesso/api';
import { podeAcessarFornecedores } from './api';
import type { FalhaFornecedor, Fornecedor, PaginaFornecedores } from './api';
import styles from '../../shared/gestao.module.css';

function Falha({ mensagem }: { mensagem: string }) {
  return <p role="alert" className={styles.erro}>{mensagem}</p>;
}

export function Fornecedores() {
  const data = useLoaderData<PaginaFornecedores | FalhaFornecedor>();
  if ('erro' in data) {
    return <section><h2>Fornecedores</h2><Falha mensagem={data.erro} /><Link to="/">Voltar ao início</Link></section>;
  }

  return <section className={styles.painel}>
    <div className={styles.cabecalho}><h2>Fornecedores</h2><Link to="/fornecedores/novo">Cadastrar fornecedor</Link></div>
    {data.itens.length === 0 ? <p>{data.total === 0 ? 'Nenhum fornecedor cadastrado.' : 'Nenhum fornecedor nesta página.'}</p> :
      <ul className={styles.lista}>
        {data.itens.map(fornecedor => (
          <li key={fornecedor.id}><Link to={'/fornecedores/' + fornecedor.id}>{fornecedor.codigo} · {fornecedor.nome}</Link></li>
        ))}
      </ul>}
    {(data.paginas > 1 || data.pagina > 0) && <nav aria-label="Paginação de fornecedores" className={styles.paginacao}>
      {data.pagina > 0 && <Link to={'?pagina=' + (data.pagina - 1)}>Página anterior</Link>}
      {data.pagina < data.paginas - 1 && <Link to={'?pagina=' + (data.pagina + 1)}>Próxima página</Link>}
    </nav>}
    <Link to="/">Voltar ao início</Link>
  </section>;
}

export function NovoFornecedor() {
  const usuario = useOutletContext<Identidade>();
  const fetcher = useFetcher<FalhaFornecedor>();
  if (!podeAcessarFornecedores(usuario.perfil)) {
    return <section><h2>Acesso restrito</h2>
      <Falha mensagem="Acesso disponível para Administrador e Compras." /><Link to="/">Voltar ao início</Link>
    </section>;
  }
  return <section className={styles.painel}>
    <h2>Cadastrar fornecedor</h2>
    <fetcher.Form method="post" className={styles.formulario}>
      <label>Nome<input name="nome" maxLength={255} required /></label>
      <label>CNPJ (opcional)<input name="cnpj" maxLength={32} /></label>
      <label>Telefone (opcional)
        <input name="telefone" type="tel" maxLength={40} aria-describedby="ajuda-telefone"
          pattern="(?:\([0-9]{2}\)|[0-9]{2}) *[0-9]{4,5}(?:-| )?[0-9]{4}" />
      </label>
      <small id="ajuda-telefone">Informe DDD e número, como (83) 99999-9999.</small>
      <label>E-mail (opcional)<input name="email" type="email" maxLength={254} /></label>
      {fetcher.data?.erro && <Falha mensagem={fetcher.data.erro} />}
      <button type="submit" disabled={fetcher.state !== 'idle'}>Cadastrar fornecedor</button>
    </fetcher.Form>
    <Link to="/fornecedores">Voltar aos fornecedores</Link>
  </section>;
}

export function DetalheFornecedor() {
  const fornecedor = useLoaderData<Fornecedor | FalhaFornecedor>();
  const [params] = useSearchParams();
  if ('erro' in fornecedor) {
    return <section><h2>Fornecedor</h2><Falha mensagem={fornecedor.erro} /><Link to="/fornecedores">Voltar aos fornecedores</Link></section>;
  }
  return <section className={styles.painel}>
    <h2>{fornecedor.nome}</h2>
    {params.get('criado') === '1' && <p role="status">Fornecedor cadastrado.</p>}
    <dl className={styles.dados}>
      <dt>Código</dt><dd>{fornecedor.codigo}</dd>
      <dt>CNPJ</dt><dd>{fornecedor.cnpj ?? 'Não informado'}</dd>
      <dt>Telefone</dt><dd>{fornecedor.telefone ?? 'Não informado'}</dd>
      <dt>E-mail</dt><dd>{fornecedor.email ?? 'Não informado'}</dd>
    </dl>
    <Link to="/fornecedores">Voltar aos fornecedores</Link>
  </section>;
}
