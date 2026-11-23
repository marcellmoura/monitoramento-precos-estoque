import { Link, useOutletContext } from 'react-router';
import { BannerInterno, PaginaInterna } from '../../app/PaginaInterna';
import { modulosVisiveis, perfis } from '../../app/modulos';
import { IconeInterface } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import styles from './Inicio.module.css';

export function Inicio() {
  const usuario = useOutletContext<Identidade>();
  const perfil = perfis[usuario.perfil];
  const modulos = modulosVisiveis(usuario.perfil);
  const atalhos = modulos.filter(modulo => modulo.atalho);

  return <PaginaInterna titulo="Início" usuario={usuario}>
    <BannerInterno titulo={<>Preços, estoque<br /> e <span>compras</span></>}>
      Organize seu catálogo e acompanhe os preços dos fornecedores.
    </BannerInterno>

    <div className={styles.resumo}>
      <section className={styles.boasVindas} aria-labelledby="titulo-boas-vindas">
        <h2 id="titulo-boas-vindas">Olá, {usuario.nome}</h2>
        <p className={styles.perfil}><IconeInterface nome="escudo" />{perfil}</p>
        <p className={styles.orientacao}>Use os atalhos para consultar informações e acessar as ferramentas disponíveis para o seu perfil.</p>
        <span className={styles.marcaBoasVindas}><IconeInterface nome="caixa" /></span>
      </section>

      <nav className={styles.atalhos} aria-labelledby="titulo-atalhos">
        <h2 id="titulo-atalhos">Acesso rápido</h2>
        <ul>
          {atalhos.map(modulo => <li key={modulo.caminho}>
            <Link to={modulo.caminho}>
              <span className={styles.icone}><IconeInterface nome={modulo.icone} /></span>
              {modulo.atalho}
              <IconeInterface nome="seta" />
            </Link>
          </li>)}
        </ul>
      </nav>
    </div>
  </PaginaInterna>;
}
