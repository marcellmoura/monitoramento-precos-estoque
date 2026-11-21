import { podeAcessarFornecedores } from '../fornecedores/api';
import { Link, useFetcher, useOutletContext, useSearchParams } from 'react-router';
import type { Identidade } from './api';
import styles from './acesso.module.css';

function Erro({ mensagem }: { mensagem?: string }) {
  return mensagem ? <p role="alert" className={styles.erro}>{mensagem}</p> : null;
}

export function Login() {
  const fetcher = useFetcher<{ erro: string }>();
  const [params] = useSearchParams();
  return <section className={styles.painel} aria-labelledby="titulo-login">
    <h2 id="titulo-login">Entrar</h2>
    <p>Use seu e-mail e senha para acessar o sistema.</p>
    {params.get('senha') === 'alterada' && <p role="status">Senha alterada. Entre com a nova senha.</p>}
    <fetcher.Form method="post" className={styles.formulario}>
      <label>E-mail<input name="email" type="email" autoComplete="username" maxLength={254} required /></label>
      <label>Senha<input name="senha" type="password" autoComplete="current-password" required /></label>
      <Erro mensagem={fetcher.data?.erro} />
      <button disabled={fetcher.state !== 'idle'} type="submit">{fetcher.state !== 'idle' ? 'Entrando…' : 'Entrar'}</button>
    </fetcher.Form>
  </section>;
}

export function AlterarSenha() {
  const fetcher = useFetcher<{ erro: string }>();
  return <section className={styles.painel} aria-labelledby="titulo-senha">
    <h2 id="titulo-senha">Defina sua senha</h2>
    <p>Antes de continuar, substitua a senha temporária por uma senha de no mínimo 12 caracteres.</p>
    <fetcher.Form method="post" className={styles.formulario}>
      <label>Senha atual<input name="senhaAtual" type="password" autoComplete="current-password" required /></label>
      <label>Nova senha<input name="novaSenha" type="password" autoComplete="new-password" minLength={12} required /></label>
      <label>Confirme a nova senha<input name="confirmacao" type="password" autoComplete="new-password" minLength={12} required /></label>
      <Erro mensagem={fetcher.data?.erro} />
      <button disabled={fetcher.state !== 'idle'} type="submit">{fetcher.state !== 'idle' ? 'Salvando…' : 'Salvar nova senha'}</button>
    </fetcher.Form>
    <Sair />
  </section>;
}

export function Sair() {
  const fetcher = useFetcher<{ erro: string }>();
  return <fetcher.Form method="post" action="/sair" className={styles.saida}>
    <button type="submit" disabled={fetcher.state !== 'idle'}>Sair</button>
    <Erro mensagem={fetcher.data?.erro} />
  </fetcher.Form>;
}

export function Inicio() {
  const usuario = useOutletContext<Identidade>();
  const perfis = { ADMINISTRADOR: 'Administrador', COMPRAS: 'Compras', ESTOQUE: 'Estoque', CONSULTA: 'Consulta' };
  return <section className={styles.painel}>
    <h2>Olá, {usuario.nome}</h2>
    <p>Perfil: {perfis[usuario.perfil]}</p>
    <p><Link to="/produtos">Consultar produtos</Link></p>
    {podeAcessarFornecedores(usuario.perfil) && <p><Link to="/fornecedores">Consultar fornecedores</Link></p>}
    {usuario.perfil === 'ADMINISTRADOR'  && <Link to="/usuarios">Administrar usuários</Link>}
    <Sair />
  </section>;
}
