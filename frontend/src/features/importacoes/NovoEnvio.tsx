import { useRef, useState, type DragEvent } from 'react';
import { Link, useFetcher, useLoaderData, useOutletContext } from 'react-router';
import { BannerInterno, PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import type { Fornecedor } from '../fornecedores/api';
import type { FalhaImportacao } from './api';
import styles from './NovoEnvio.module.css';

export function NovoEnvio() {
  const usuario = useOutletContext<Identidade>();
  const opcoes = useLoaderData<Fornecedor[] | FalhaImportacao>();
  const disponivel = !('erro' in opcoes);
  return <PaginaInterna titulo="Enviar PDF" usuario={usuario}>
    <BannerInterno compacto titulo={<>Importar uma <span>cotação em PDF</span></>}>
      Vincule o PDF ao fornecedor que emitiu a cotação para acompanhar os preços no sistema.
    </BannerInterno>
    <div className={styles.layout}>
      <section className={styles.painel} aria-labelledby="titulo-formulario-envio">
        <header className={styles.titulo}>
          <span className={styles.icone}><IconeInterface nome="enviar" /></span>
          <h2 id="titulo-formulario-envio">Dados do envio</h2>
        </header>
        {disponivel ? <FormularioEnvio fornecedores={opcoes} /> : <p className={styles.erro} role="alert">{opcoes.erro}</p>}
        <Link className={styles.voltar} to="/importacoes"><IconeInterface nome="voltar" />Voltar às importações</Link>
      </section>
      {disponivel && <aside className={`${styles.painel} ${styles.orientacoes}`} aria-labelledby="titulo-orientacoes">
        <header className={styles.titulo}>
          <span className={styles.icone}><IconeInterface nome="informacao" /></span>
          <h2 id="titulo-orientacoes">Orientações de envio</h2>
        </header>
        <ul>
          <li>
            <span className={styles.icone}><IconeInterface nome="documento" /></span>
            <div><strong>Use um PDF com texto selecionável</strong><p>Arquivos digitalizados apenas como imagem podem não ser lidos.</p></div>
          </li>
          <li>
            <span className={styles.icone}><IconeInterface nome="relogio" /></span>
            <div><strong>Confira o resultado após o envio</strong><p>O sistema abre os detalhes da tentativa, onde você acompanha a leitura do PDF e vê as pendências.</p></div>
          </li>
        </ul>
      </aside>}
    </div>
  </PaginaInterna>;
}

function FormularioEnvio({ fornecedores }: { fornecedores: Fornecedor[] }) {
  const fetcher = useFetcher<FalhaImportacao>();
  const input = useRef<HTMLInputElement>(null);
  const [arquivo, setArquivo] = useState<File>();
  const [erroArquivo, setErroArquivo] = useState<string>();
  const enviando = fetcher.state !== 'idle';

  function conferirArquivos(arquivos: FileList | null) {
    const escolhido = arquivos?.[0];
    if (!escolhido) {
      setArquivo(undefined);
      setErroArquivo(undefined);
      return undefined;
    }
    let erro: string | undefined;
    if (arquivos.length > 1) erro = 'Selecione apenas um PDF por envio.';
    else if (!escolhido.name.toLowerCase().endsWith('.pdf')) erro = 'Selecione um arquivo com extensão .pdf.';
    else if (escolhido.size === 0) erro = 'Selecione um PDF que não esteja vazio.';
    setErroArquivo(erro);
    setArquivo(erro ? undefined : escolhido);
    if (erro && input.current) input.current.value = '';
    return erro ? undefined : escolhido;
  }

  function receberArquivo(evento: DragEvent<HTMLDivElement>) {
    evento.preventDefault();
    if (enviando || !input.current || evento.dataTransfer.files.length === 0) return;
    const escolhido = conferirArquivos(evento.dataTransfer.files);
    if (escolhido) {
      const transferencia = new DataTransfer();
      transferencia.items.add(escolhido);
      input.current.files = transferencia.files;
    }
  }

  if (fornecedores.length === 0) {
    return <p className={styles.aviso}>Cadastre um fornecedor antes de enviar um PDF. <Link to="/fornecedores/novo">Cadastrar fornecedor</Link></p>;
  }

  return <fetcher.Form method="post" encType="multipart/form-data" className={styles.formulario}>
    <div className={styles.campo}>
      <label htmlFor="fornecedor-envio" className={styles.rotulo}><span aria-hidden="true" data-passo="1" />Fornecedor</label>
      <select id="fornecedor-envio" name="fornecedorId" defaultValue="" disabled={enviando} required>
        <option value="" disabled>Selecione um fornecedor</option>
        {fornecedores.map(fornecedor => <option key={fornecedor.id} value={fornecedor.id}>{fornecedor.codigo} · {fornecedor.nome}</option>)}
      </select>
    </div>

    <div className={styles.campo}>
      <label htmlFor="arquivo-envio" className={styles.rotulo}><span aria-hidden="true" data-passo="2" />Arquivo PDF</label>
      <div className={styles.seletor} data-selecionado={Boolean(arquivo)} onDragOver={evento => evento.preventDefault()} onDrop={receberArquivo}>
        <input ref={input} id="arquivo-envio" type="file" name="arquivo" accept=".pdf,application/pdf" disabled={enviando}
          aria-describedby={erroArquivo ? 'limite-pdf erro-arquivo' : 'limite-pdf'}
          aria-invalid={erroArquivo ? true : undefined}
          onChange={evento => conferirArquivos(evento.currentTarget.files)} required />
        <span className={styles.icone}><IconeInterface nome={arquivo ? 'confirmado' : 'documento'} /></span>
        <div className={styles.selecao} aria-live="polite">
          <strong>{arquivo?.name ?? 'Selecione ou arraste um PDF'}</strong>
          {arquivo && <span>Trocar arquivo</span>}
        </div>
        <small id="limite-pdf">Apenas PDF. Limite padrão de 10 MiB.</small>
      </div>
      {erroArquivo && <p id="erro-arquivo" className={styles.erro} role="alert">{erroArquivo}</p>}
    </div>

    {!enviando && fetcher.data?.erro && <p className={styles.erro} role="alert">{fetcher.data.erro}</p>}
    <div>
      <button className={styles.enviar} type="submit" disabled={enviando}>
        {enviando ? <span className={styles.carregando} aria-hidden="true" /> : <IconeInterface nome="enviar" />}
        {enviando ? 'Enviando PDF…' : 'Enviar PDF'}
      </button>
      <div role="status">{enviando && <p className={styles.aviso}>Envio em andamento. Aguarde a resposta do sistema.</p>}</div>
    </div>
  </fetcher.Form>;
}
