import { useEffect, useId, useRef } from 'react';
import { Link, useFetcher, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import type { Identidade } from '../acesso/api';
import { perfis } from './api';
import type { Falha, PaginaUsuarios, Usuario } from './api';
import styles from '../../shared/gestao.module.css';

type Resultado = { erro?: string; sucesso?: string };
function Mensagem({ resultado }: { resultado?: Resultado }) {
  if (resultado?.erro) return <p role="alert" className={styles.erro}>{resultado.erro}</p>;
  return resultado?.sucesso ? <p role="status">{resultado.sucesso}</p> : null;
}
function Perfil({ valor = 'CONSULTA' }: { valor?: Usuario['perfil'] }) {
  const id = useId();
  return <div className={styles.campo}><label htmlFor={id}>Perfil</label><select id={id} name="perfil" defaultValue={valor} required>
    {Object.entries(perfis).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
  </select></div>;
}
function SenhaTemporaria() {
  return <>
    <label>Senha temporária<input name="senhaTemporaria" type="password" minLength={12} autoComplete="new-password" required /></label>
    <label>Confirme a senha temporária<input name="confirmacao" type="password" minLength={12} autoComplete="new-password" required /></label>
  </>;
}
function Negado() {
  return <section><h2>Acesso restrito</h2><p role="alert">Acesso exclusivo do Administrador.</p><Link to="/">Voltar ao início</Link></section>;
}

export function Usuarios() {
  const identidade = useOutletContext<Identidade>();
  const data = useLoaderData<PaginaUsuarios | Falha>();
  if (identidade.perfil !== 'ADMINISTRADOR') return <Negado />;
  if ('erro' in data) return <section><h2>Usuários</h2><Mensagem resultado={data} /><Link to="/">Voltar ao início</Link></section>;
  return <section className={styles.painel}>
    <div className={styles.cabecalho}><h2>Usuários</h2><Link to="/usuarios/novo">Cadastrar usuário</Link></div>
    <p>{data.total} usuário(s) cadastrado(s).</p>
    <p className={styles.ajudaTabela}>Deslize a tabela para ver todas as colunas.</p>
    <div className={styles.tabela} role="region" aria-label="Contas cadastradas" tabIndex={0}><table><caption>Contas e níveis de acesso</caption>
      <thead><tr><th scope="col">Nome / e-mail</th><th scope="col">Perfil</th><th scope="col">Estado</th><th scope="col">Ação</th></tr></thead>
      <tbody>{data.itens.map(usuario => <tr key={usuario.id}>
        <td>{usuario.nome}<small>{usuario.email}</small></td><td>{perfis[usuario.perfil]}</td>
        <td>{usuario.ativo ? 'Ativo' : 'Inativo'}{usuario.trocaSenhaObrigatoria && <small>Troca de senha pendente</small>}</td>
        <td><Link to={`/usuarios/${usuario.id}`} aria-label={`Administrar ${usuario.nome}`}>Administrar</Link></td>
      </tr>)}</tbody>
    </table></div>
    {data.itens.length === 0 && <p>Nenhum usuário nesta página.</p>}
    <nav aria-label="Paginação de usuários" className={styles.paginacao}>
      {data.pagina > 0 && <Link to={`?pagina=${data.pagina - 1}`}>Página anterior</Link>}
      <span>Página {data.pagina + 1} de {Math.max(data.paginas, 1)}</span>
      {data.pagina + 1 < data.paginas && <Link to={`?pagina=${data.pagina + 1}`}>Próxima página</Link>}
    </nav>
    <Link to="/">Voltar ao início</Link>
  </section>;
}

export function NovoUsuario() {
  const identidade = useOutletContext<Identidade>();
  const fetcher = useFetcher<Resultado>();
  if (identidade.perfil !== 'ADMINISTRADOR') return <Negado />;
  return <section className={styles.painel}><h2>Cadastrar usuário</h2>
    <p>A senha temporária deve ter no mínimo 12 caracteres. O usuário deverá trocá-la no primeiro acesso.</p>
    <fetcher.Form method="post" className={styles.formulario}>
      <label>Nome<input name="nome" maxLength={120} autoComplete="off" required /></label>
      <label>E-mail<input name="email" type="email" maxLength={254} autoComplete="off" required /></label>
      <Perfil /><SenhaTemporaria /><Mensagem resultado={fetcher.data} />
      <button type="submit" disabled={fetcher.state !== 'idle'}>Cadastrar usuário</button>
    </fetcher.Form><Link to="/usuarios">Voltar aos usuários</Link>
  </section>;
}

export function AdministrarUsuario() {
  const identidade = useOutletContext<Identidade>();
  const data = useLoaderData<Usuario | Falha>();
  const [params] = useSearchParams();
  if (identidade.perfil !== 'ADMINISTRADOR') return <Negado />;
  if ('erro' in data) return <section><h2>Administrar usuário</h2><Mensagem resultado={data} /><Link to="/usuarios">Voltar aos usuários</Link></section>;
  return <section className={styles.painel}><h2>Administrar {data.nome}</h2><p>{data.email}</p>
    {params.get('criado') === '1' && <p role="status">Usuário cadastrado com senha temporária.</p>}
    <FormAcesso key={data.id} usuario={data} />
    <FormSenha />
    <Link to="/usuarios">Voltar aos usuários</Link>
  </section>;
}
function FormAcesso({ usuario }: { usuario: Usuario }) {
  const fetcher = useFetcher<Resultado>();
  return <fetcher.Form method="post" className={styles.formulario} aria-labelledby="acesso-titulo">
    <h3 id="acesso-titulo">Perfil e estado</h3><input type="hidden" name="operacao" value="acesso" />
    <Perfil valor={usuario.perfil} />
    <div className={styles.campo}><label htmlFor="estado-usuario">Estado</label><select id="estado-usuario" name="ativo" defaultValue={String(usuario.ativo)}><option value="true">Ativo</option><option value="false">Inativo</option></select></div>
    <p>Alterar o acesso encerra as sessões desse usuário. É necessário manter um Administrador ativo.</p>
    <Mensagem resultado={fetcher.data} /><button disabled={fetcher.state !== 'idle'} type="submit">Salvar acesso</button>
  </fetcher.Form>;
}
function FormSenha() {
  const fetcher = useFetcher<Resultado>();
  const form = useRef<HTMLFormElement>(null);
  useEffect(() => { if (fetcher.data?.sucesso) form.current?.reset(); }, [fetcher.data]);
  return <fetcher.Form method="post" ref={form} className={styles.formulario} aria-labelledby="senha-titulo">
    <h3 id="senha-titulo">Redefinir senha</h3><input type="hidden" name="operacao" value="senha" />
    <p>Defina uma senha temporária. Todas as sessões do usuário serão encerradas; redefinir a senha mantém o estado atual da conta.</p>
    <SenhaTemporaria /><Mensagem resultado={fetcher.data} />
    <button disabled={fetcher.state !== 'idle'} type="submit">Redefinir senha</button>
  </fetcher.Form>;
}
