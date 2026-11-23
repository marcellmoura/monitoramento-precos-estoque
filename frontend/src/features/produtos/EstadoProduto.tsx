import { IconeInterface } from '../../shared/IconeInterface';
import styles from './EstadoProduto.module.css';

export function EstadoProduto({ ativo }: { ativo: boolean }) {
  return <span className={styles.estado} data-ativo={ativo}>
    <IconeInterface nome={ativo ? 'confirmado' : 'bloqueado'} />{ativo ? 'Ativo' : 'Inativo'}
  </span>;
}
