import { useFetcher } from 'react-router';
import { unidades } from '../produtos/api';
import type { ConferenciaCotacao, FalhaImportacao } from './api';
import styles from '../../shared/gestao.module.css';
import importacoes from './Importacoes.module.css';

export function FormularioConferencia({ conferencia, bloqueada }: { conferencia: ConferenciaCotacao; bloqueada: boolean }) {
  const fetcher = useFetcher<FalhaImportacao>();
  const { colunas, confirmada, sugestao, baseDoPdf, dataSugerida } = conferencia;
  const base = confirmada?.base ?? sugestao ?? baseDoPdf;
  if (bloqueada && confirmada) {
    return <section className={importacoes.painelCotacao} aria-labelledby="conferencia-comercial">
      <h2 id="conferencia-comercial">Conferência comercial</h2>
      <p>{confirmada.base.coluna} · {confirmada.base.unidade} · {confirmada.base.embalagem} · {confirmada.dataComercial.split('-').reverse().join('/')}</p>
      <p>Conferência bloqueada após a confirmação de preços.</p>
    </section>;
  }
  return <section className={importacoes.painelCotacao} aria-labelledby="conferencia-comercial">
    <h2 id="conferencia-comercial" tabIndex={-1}>Dados para usar os preços</h2>
    {confirmada && <p>Conferência comercial salva. <a href="#previa-itens">Voltar à prévia dos itens</a></p>}
    {!confirmada && sugestao && <p>Configuração sugerida a partir de uma cotação deste fornecedor. Confira os campos antes de salvar.</p>}
    {!confirmada && !sugestao && baseDoPdf && <p>Preenchemos os dados que aparecem no PDF. Confira antes de continuar.</p>}
    <p>Confira qual preço usar e a apresentação de compra. Os itens com informações diferentes podem ser revisados depois.</p>
    <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="operacao" value="conferir" />
      <input type="hidden" name="revisao" value={confirmada?.id ?? 0} />
      <div className={styles.campo}>
        <label htmlFor="coluna-compra">Coluna de preço de compra</label>
        <select id="coluna-compra" name="coluna" defaultValue={base?.coluna ?? (colunas.length === 1 ? colunas[0] : '')} required>
          <option value="" disabled>Selecione</option>
          {colunas.map(coluna => <option key={coluna} value={coluna}>{coluna}</option>)}
        </select>
      </div>
      <div className={styles.campo}>
        <label htmlFor="unidade-compra">Unidade de compra</label>
        <select id="unidade-compra" name="unidade" defaultValue={base?.unidade ?? ''} required>
          <option value="" disabled>Selecione</option>
          {Object.entries(unidades).map(([codigo, nome]) => <option key={codigo} value={codigo}>{nome}</option>)}
        </select>
      </div>
      <label>Embalagem de compra<input name="embalagem" required maxLength={120} defaultValue={base?.embalagem ?? ''} /></label>
      <label>Data comercial<input type="date" name="dataComercial" required max="9999-12-31"
        defaultValue={confirmada?.dataComercial ?? dataSugerida} /></label>
      {fetcher.data?.erro && <p role="alert" className={styles.erro}>{fetcher.data.erro}</p>}
      <button type="submit" disabled={fetcher.state !== 'idle'}>
        {fetcher.state === 'idle' ? 'Salvar conferência' : 'Salvando…'}
      </button>
    </fetcher.Form>
  </section>;
}
