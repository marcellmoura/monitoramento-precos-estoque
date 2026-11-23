import { Navigate, Outlet, useLoaderData, useLocation, useMatches } from 'react-router';
import type { Identidade } from '../features/acesso/api';
import styles from './App.module.css';

export function App() {
  const usuario = useLoaderData<Identidade | null>();
  const { pathname } = useLocation();
  const rota = useMatches().at(-1)?.id;
  if (!usuario && pathname !== '/login') return <Navigate to="/login" replace />;
  if (usuario?.trocaSenhaObrigatoria && pathname !== '/alterar-senha') {
    return <Navigate to="/alterar-senha" replace />;
  }
  if (usuario && !usuario.trocaSenhaObrigatoria && ['/login', '/alterar-senha'].includes(pathname)) {
    return <Navigate to="/" replace />;
  }
  if (pathname === '/' || pathname === '/login' || rota === 'lista-importacoes' ||
      rota === 'enviar-pdf' || rota === 'detalhe-importacao' || rota === 'historico-precos' ||
      rota === 'lista-produtos' || rota === 'detalhe-produto' || rota === 'novo-produto' || rota === 'editar-produto') {
    return <Outlet context={usuario} />;
  }
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
