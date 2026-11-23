import type { ReactNode } from 'react';
import type { Identidade } from '../features/acesso/api';
import { IconeInterface } from '../shared/IconeInterface';
import { MenuPrincipal } from './MenuPrincipal';
import { modulosVisiveis, perfis } from './modulos';
import styles from './PaginaInterna.module.css';

export function PaginaInterna({ titulo, usuario, children }: { titulo: string; usuario: Identidade; children: ReactNode }) {
  return <div className={styles.pagina}>
    <MenuPrincipal itens={modulosVisiveis(usuario.perfil)} />
    <main id="conteudo-principal" tabIndex={-1} className={styles.conteudo}>
      <header className={styles.cabecalho}>
        <h1>{titulo}</h1>
        <div className={styles.conta}>
          <span className={styles.icone}><IconeInterface nome="pessoa" /></span>
          <div><strong>{usuario.nome}</strong><small>{perfis[usuario.perfil]}</small></div>
        </div>
      </header>
      {children}
    </main>
  </div>;
}

export function BannerInterno({ titulo, children, compacto = false }: { titulo: ReactNode; children: ReactNode; compacto?: boolean }) {
  return <section className={compacto ? `${styles.banner} ${styles.compacto}` : styles.banner} aria-labelledby="titulo-banner">
    <h2 id="titulo-banner">{titulo}</h2>
    <p>{children}</p>
  </section>;
}
