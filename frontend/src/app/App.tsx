import { Link, Navigate, Outlet, useLoaderData, useLocation, useMatches } from 'react-router';
import type { Identidade } from '../features/acesso/api';
import styles from './App.module.css';

export function App() {
  const usuario = useLoaderData<Identidade | null>();
  const { pathname } = useLocation();
  const cotacao = useMatches().some(rota => rota.id === 'detalhe-importacao');
  if (!usuario && pathname !== '/login') return <Navigate to="/login" replace />;
  if (usuario?.trocaSenhaObrigatoria && pathname !== '/alterar-senha') {
    return <Navigate to="/alterar-senha" replace />;
  }
  if (usuario && !usuario.trocaSenhaObrigatoria && ['/login', '/alterar-senha'].includes(pathname)) {
    return <Navigate to="/" replace />;
  }
  if (cotacao) return <div className={styles.piloto}>
    <header className={styles.barraPiloto}>
      <div>
        <Link to="/" className={styles.marcaPiloto}>
          <span aria-hidden="true" className={styles.simboloPiloto} />
          <span>Monitoramento</span>
        </Link>
        <nav aria-label="Navegação principal">
          <Link to="/importacoes" className={styles.areaPiloto}>Importações</Link>
          <Link to="/historico-precos">Histórico de preços</Link>
        </nav>
      </div>
    </header>
    <main className={styles.conteudoPiloto}><Outlet context={usuario} /></main>
  </div>;
  return <main className={styles.container}>
    <p className={styles.eyebrow}>Monitoramento</p>
    <h1>Preços, estoque e compras</h1>
    <p className={styles.description}>Organize seu catálogo e acompanhe os preços dos fornecedores.</p>
    <Outlet context={usuario} />
  </main>;
}

export function FalhaDeConexao() {
  return <main className={styles.container}>
    <h1>Não foi possível acessar o sistema</h1>
    <p role="alert">Verifique sua conexão e tente novamente.</p>
    <button onClick={() => window.location.reload()}>Tentar novamente</button>
  </main>;
}
