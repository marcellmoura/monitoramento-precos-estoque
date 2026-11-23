import { Link, useFetcher, useLoaderData, useOutletContext, useSearchParams } from 'react-router';
import { useState, type ReactNode } from 'react';
import { PaginaInterna } from '../../app/PaginaInterna';
import { IconeInterface, type NomeIcone } from '../../shared/IconeInterface';
import type { Identidade } from '../acesso/api';
import { destinoImportacao, podeCadastrar, quantidade, unidades } from './api';
import type { ContextoPrecoCotado, FalhaProduto, Produto } from './api';
import styles from './FormularioProduto.module.css';

const moedaCotada = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

function Falha({ mensagem }: { mensagem: string }) {
  return <p role="alert" className={styles.erro}>{mensagem}</p>;
}

function Voltar({ destino, children }: { destino: string; children: ReactNode }) {
  return <Link to={destino} className={styles.voltar}><IconeInterface nome="voltar" />{children}</Link>;
}

function BlocoCampos({ titulo, icone, children, larguraTotal }: {
  titulo: string; icone: NomeIcone; children: ReactNode; larguraTotal?: boolean;
}) {
  return <section className={larguraTotal ? `${styles.painel} ${styles.larguraTotal}` : styles.painel}>
    <h2 className={styles.titulo}><span><IconeInterface nome={icone} /></span>{titulo}</h2>
    {children}
  </section>;
}

function CampoUnidade({ valor = '' }: { valor?: Produto['unidade'] | '' }) {
  return <div className={styles.campo}>
    <label htmlFor="unidade-produto">Unidade</label>
    <select id="unidade-produto" name="unidade" defaultValue={valor} required>
      <option value="" disabled>Selecione</option>
      {Object.entries(unidades).map(([codigo, rotulo]) => <option key={codigo} value={codigo}>{rotulo}</option>)}
    </select>
  </div>;
}

function CampoVenda({ valor }: { valor?: string }) {
  return <label>Preço de venda
    <input name="precoVenda" type="text" defaultValue={valor?.replace('.', ',')} inputMode="decimal"
      maxLength={20} pattern="[0-9]{1,17}([.,][0-9]{1,2})?" aria-describedby="ajuda-precos" required />
  </label>;
}

function AjudaPrecos() {
  return <p id="ajuda-precos" className={styles.ajuda}>Use vírgula ou ponto para os centavos, sem separador de milhar.</p>;
}

function Rodape({ erro, salvando, acao, children }: { erro?: string; salvando: boolean; acao: string; children?: ReactNode }) {
  return <div className={styles.rodape}>
    {erro && <Falha mensagem={erro} />}
    <div className={styles.acoes}>
      {children}
      <button className={styles.enviar} type="submit" disabled={salvando}>
        <IconeInterface nome="confirmado" />{salvando ? 'Salvando…' : acao}
      </button>
    </div>
  </div>;
}

export function NovoProduto() {
  const [params] = useSearchParams();
  const retorno = destinoImportacao(params);
  const cotacao = useLoaderData<ContextoPrecoCotado | null>();
  const usuario = useOutletContext<Identidade>();
  const fetcher = useFetcher<FalhaProduto>();

  return <PaginaInterna titulo="Cadastrar produto" usuario={usuario}>
    <Voltar destino={retorno ?? '/produtos'}>{retorno ? 'Voltar à importação' : 'Voltar aos produtos'}</Voltar>

    {podeCadastrar(usuario) ? <>
      {cotacao?.preco != null && <aside className={styles.cotacao} aria-label="Preço na cotação">
        <IconeInterface nome="documento" />
        <div>
          <p><strong>{cotacao.conferido ? 'Preço conferido na cotação' : 'Preço cotado no PDF'}</strong></p>
          <p>{moedaCotada.format(cotacao.preco)}{cotacao.embalagem && ` · ${cotacao.embalagem}`}</p>
        </div>
      </aside>}
      <fetcher.Form method="post" className={styles.formulario}>
        <div className={styles.blocos}>
          <BlocoCampos titulo="Dados do produto" icone="caixa">
            <div className={styles.campos}>
              <label className={styles.campoInteiro}>Nome<input name="nome" maxLength={255} required /></label>
              <label>Categoria (opcional)<input name="categoria" maxLength={100} /></label>
              <CampoUnidade />
            </div>
          </BlocoCampos>
          <BlocoCampos titulo="Preços do catálogo" icone="grafico">
            <AjudaPrecos />
            <div className={styles.campos}>
              <CampoVenda />
              <label>Preço de compra de referência (opcional)<input name="precoCompra" type="text" inputMode="decimal" maxLength={20}
                pattern="[0-9]{1,17}([.,][0-9]{1,2})?" aria-describedby="ajuda-precos ajuda-referencia" /></label>
            </div>
            <p id="ajuda-referencia" className={styles.ajuda}>O preço de compra é uma referência interna do catálogo.
              {retorno && ' Pode ficar em branco. O valor da cotação vai para o histórico quando você confirmar os preços na importação.'}</p>
          </BlocoCampos>
        </div>
        <Rodape erro={fetcher.data?.erro} salvando={fetcher.state !== 'idle'} acao="Cadastrar produto">
          <p className={styles.ajuda}>O produto começa ativo, com estoque atual e mínimo em zero.</p>
        </Rodape>
      </fetcher.Form>
    </> : <Falha mensagem="Cadastro disponível para Administrador e Compras." />}
  </PaginaInterna>;
}

export function EditarProduto() {
  const usuario = useOutletContext<Identidade>();
  const produto = useLoaderData<Produto | FalhaProduto>();
  const dadosDisponiveis = !('erro' in produto);
  const podeEditar = usuario.perfil !== 'CONSULTA';

  return <PaginaInterna titulo="Editar produto" usuario={usuario}>
    <Voltar destino={dadosDisponiveis && podeEditar ? `/produtos/${produto.id}` : '/produtos'}>
      {dadosDisponiveis && podeEditar ? 'Voltar aos detalhes' : 'Voltar aos produtos'}
    </Voltar>
    {podeEditar ? (dadosDisponiveis
      ? <FormularioEdicao key={`${produto.id}-${usuario.perfil}`} produto={produto} usuario={usuario} />
      : <Falha mensagem={produto.erro} />)
      : <Falha mensagem="Seu perfil não permite editar produtos." />}
  </PaginaInterna>;
}

function FormularioEdicao({ produto, usuario }: { produto: Produto; usuario: Identidade }) {
  const fetcher = useFetcher<FalhaProduto>();
  const [original] = useState(produto);
  const administrador = usuario.perfil === 'ADMINISTRADOR';
  const editaDados = podeCadastrar(usuario);
  const editaEstoque = administrador || usuario.perfil === 'ESTOQUE';

  return <>
    <div className={styles.produto}>
      <span className={styles.iconeProduto}><IconeInterface nome="caixa" /></span>
      <div><p>{original.codigo}</p><h2>{original.nome}</h2></div>
    </div>
    <fetcher.Form method="post" className={styles.formulario}>
      <input type="hidden" name="versao" value={original.versao} readOnly />
      <div className={styles.blocos}>
        {editaDados && <BlocoCampos titulo="Dados do produto" icone="caixa" larguraTotal={administrador}>
          <div className={styles.campos}>
            <label>Nome<input name="nome" defaultValue={original.nome} maxLength={255} required /></label>
            <label>Categoria (opcional)<input name="categoria" defaultValue={original.categoria ?? ''} maxLength={100} /></label>
            {administrador && <>
              {original.unidadeEditavel ? <CampoUnidade valor={original.unidade} /> : <div className={styles.somenteLeitura}>
                <span>Unidade</span><strong>{unidades[original.unidade]}</strong>
              </div>}
              <div className={styles.campo}>
                <label htmlFor="estado-produto">Estado</label>
                <select id="estado-produto" name="ativo" defaultValue={String(original.ativo)}>
                  <option value="true">Ativo</option><option value="false">Inativo</option>
                </select>
              </div>
            </>}
          </div>
          {administrador && !original.unidadeEditavel && <p className={styles.ajuda}>
            A troca direta exige estoques zerados e ausência de histórico ou vínculos.
          </p>}
        </BlocoCampos>}

        {editaEstoque && <BlocoCampos titulo="Estoque" icone="camadas">
          <p id="ajuda-estoques" className={styles.ajuda}>Use número inteiro para UN e até três casas decimais para as demais unidades.</p>
          <div className={styles.campos}>
            <label>Estoque atual<input name="estoqueAtual" defaultValue={quantidade(original.estoqueAtual, original.unidade)}
              inputMode="decimal" maxLength={20} pattern="[0-9]{1,16}([.,][0-9]{1,3})?" aria-describedby="ajuda-estoques" required /></label>
            <label>Estoque mínimo<input name="estoqueMinimo" defaultValue={quantidade(original.estoqueMinimo, original.unidade)}
              inputMode="decimal" maxLength={20} pattern="[0-9]{1,16}([.,][0-9]{1,3})?" aria-describedby="ajuda-estoques" required /></label>
          </div>
        </BlocoCampos>}

        {administrador && <BlocoCampos titulo="Venda" icone="grafico">
          <AjudaPrecos />
          <div className={styles.campos}><CampoVenda valor={original.precoVenda} /></div>
        </BlocoCampos>}
      </div>
      <Rodape erro={fetcher.data?.erro} salvando={fetcher.state !== 'idle'} acao="Salvar alterações" />
    </fetcher.Form>
  </>;
}
