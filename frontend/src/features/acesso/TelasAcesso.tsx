import { useState } from 'react';
import { useFetcher, useSearchParams } from 'react-router';
import { IconeInterface } from '../../shared/IconeInterface';
import styles from './acesso.module.css';
import loginStyles from './Login.module.css';

function Erro({ mensagem }: { mensagem?: string }) {
  return mensagem ? <p role="alert" className={styles.erro}>{mensagem}</p> : null;
}

export function Login() {
  const fetcher = useFetcher<{ erro: string }>();
  const [params] = useSearchParams();
  const [senhaVisivel, definirSenhaVisivel] = useState(false);
  const entrando = fetcher.state !== 'idle';

  return <main className={loginStyles.pagina}>
    <div className={loginStyles.cartao}>
      <header className={loginStyles.identidade}>
        <p className={loginStyles.marca}>
          <span className={loginStyles.simbolo}><IconeInterface nome="caixa" /></span>
          Monitoramento
        </p>
        <div className={loginStyles.apresentacao}>
          <p className={loginStyles.tituloProduto}>Preços, estoque e <span>compras</span></p>
          <p className={loginStyles.descricao}>Importe cotações em PDF, organize seu catálogo e acompanhe os preços dos fornecedores.</p>
        </div>
        <ul className={loginStyles.recursos}>
          <li><IconeInterface nome="documento" />Importação de PDFs</li>
          <li><IconeInterface nome="caixa" />Catálogo de produtos</li>
          <li><IconeInterface nome="grafico" />Histórico de preços</li>
        </ul>
      </header>
      <section className={loginStyles.acesso} aria-labelledby="titulo-login">
        <h1 id="titulo-login">Entrar</h1>
        {params.get('senha') === 'alterada' && <p className={loginStyles.aviso} role="status">Senha alterada. Entre com a nova senha.</p>}
        <fetcher.Form method="post" className={loginStyles.formulario}>
          <div className={loginStyles.campo}>
            <label className={loginStyles.rotuloAcessivel} htmlFor="login-email">E-mail</label>
            <span className={loginStyles.iconeCampo}><IconeInterface nome="email" /></span>
            <input id="login-email" name="email" type="email" placeholder="E-mail" autoComplete="username" autoCapitalize="none" spellCheck={false} maxLength={254} required />
          </div>
          <div className={`${loginStyles.campo} ${loginStyles.campoSenha}`}>
            <label className={loginStyles.rotuloAcessivel} htmlFor="login-senha">Senha</label>
            <span className={loginStyles.iconeCampo}><IconeInterface nome="cadeado" /></span>
            <input id="login-senha" name="senha" type={senhaVisivel ? 'text' : 'password'} placeholder="Senha" autoComplete="current-password" required />
            <button
              type="button"
              aria-controls="login-senha"
              aria-label={senhaVisivel ? 'Ocultar senha' : 'Mostrar senha'}
              onClick={() => definirSenhaVisivel(visivel => !visivel)}
            >
              <IconeInterface nome="olho" riscado={senhaVisivel} />
            </button>
          </div>
          <Erro mensagem={fetcher.data?.erro} />
          <button className={loginStyles.enviar} type="submit" disabled={entrando}>
            <IconeInterface nome="entrar" />{entrando ? 'Entrando…' : 'Entrar'}
          </button>
        </fetcher.Form>
      </section>
    </div>
  </main>;
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

export function Sair({ className = styles.saida, mostrarIcone = false }: { className?: string; mostrarIcone?: boolean }) {
  const fetcher = useFetcher<{ erro: string }>();
  return <fetcher.Form method="post" action="/sair" className={className}>
    <button type="submit" disabled={fetcher.state !== 'idle'}>{mostrarIcone && <IconeInterface nome="sair" />}Sair</button>
    <Erro mensagem={fetcher.data?.erro} />
  </fetcher.Form>;
}
