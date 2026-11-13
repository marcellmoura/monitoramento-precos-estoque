import styles from './App.module.css';

export function App() {
  return (
    <main className={styles.container}>
      <p className={styles.eyebrow}>Monitoramento</p>
      <h1>Preços, estoque e compras</h1>
      <p className={styles.description}>
        Organize seu catálogo e acompanhe os preços dos fornecedores.
      </p>
      <p className={styles.notice}>Sistema em preparação.</p>
    </main>
  );
}
