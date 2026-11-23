import { NavLink } from 'react-router';
import { Sair } from '../features/acesso/TelasAcesso';
import { IconeInterface, type NomeIcone } from '../shared/IconeInterface';
import styles from './MenuPrincipal.module.css';

export type ItemMenu = { caminho: string; nome: string; icone: NomeIcone };

export function MenuPrincipal({ itens }: { itens: ItemMenu[] }) {
  return <aside className={styles.menu}>
    <a className={styles.pular} href="#conteudo-principal">Pular para o conteúdo</a>
    <div className={styles.marca}>
      <span><IconeInterface nome="caixa" /></span>Monitoramento
    </div>
    <nav className={styles.navegacao} aria-label="Navegação principal">
      <NavLink to="/" end><IconeInterface nome="casa" />Início</NavLink>
      {itens.map(item => <NavLink key={item.caminho} to={item.caminho}>
        <IconeInterface nome={item.icone} />{item.nome}
      </NavLink>)}
    </nav>
    <Sair className={styles.saida} mostrarIcone />
  </aside>;
}
