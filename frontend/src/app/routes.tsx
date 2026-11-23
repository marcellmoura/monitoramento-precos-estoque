import type { RouteObject } from 'react-router';
import { App, FalhaDeConexao } from './App';
import { carregarIdentidade } from '../features/acesso/api';
import { entrar, sair, trocarSenha } from '../features/acesso/actions';
import { AlterarSenha, Login } from '../features/acesso/TelasAcesso';
import { Inicio } from '../features/inicio/Inicio';

import { Usuarios, NovoUsuario, AdministrarUsuario } from '../features/usuarios/Usuarios';
import { carregarUsuarios, carregarUsuario, cadastrarUsuario, administrarUsuario } from '../features/usuarios/api';

import { ListaProdutos } from '../features/produtos/ListaProdutos';
import { DetalheProduto } from '../features/produtos/DetalheProduto';
import { NovoProduto, EditarProduto } from '../features/produtos/Produtos';
import { carregarContextoCadastro, carregarProdutos, carregarProduto, cadastrarProduto, editarProduto } from '../features/produtos/api';

import { Fornecedores, NovoFornecedor, DetalheFornecedor } from '../features/fornecedores/Fornecedores';
import { carregarFornecedores, carregarFornecedor, cadastrarFornecedor } from '../features/fornecedores/api';

import { HistoricoPrecos } from '../features/importacoes/HistoricoPrecos';
import { ListaImportacoes } from '../features/importacoes/ListaImportacoes';
import { NovoEnvio } from '../features/importacoes/NovoEnvio';
import { DetalheImportacao } from '../features/importacoes/Importacoes';
import { carregarFornecedoresDoEnvio, carregarHistorico, carregarImportacoes, carregarCatalogo, carregarTentativa, atualizarTentativa, enviarPdf } from '../features/importacoes/api';

export const routes: RouteObject[] = [{
  path: '/', element: <App />, loader: carregarIdentidade, errorElement: <FalhaDeConexao />,
  children: [
    { index: true, element: <Inicio /> },
    { id: 'lista-produtos', path: 'produtos', element: <ListaProdutos />, loader: carregarProdutos },
    { id: 'novo-produto', path: 'produtos/novo', element: <NovoProduto />, loader: carregarContextoCadastro, action: cadastrarProduto },
    { id: 'detalhe-produto', path: 'produtos/:id', element: <DetalheProduto />, loader: carregarProduto },
    { id: 'editar-produto', path: 'produtos/:id/editar', element: <EditarProduto />, loader: carregarProduto, action: editarProduto },
    { path: 'fornecedores', element: <Fornecedores />, loader: carregarFornecedores },
    { path: 'fornecedores/novo', element: <NovoFornecedor />, action: cadastrarFornecedor },
    { path: 'fornecedores/:id', element: <DetalheFornecedor />, loader: carregarFornecedor },
    { id: 'historico-precos', path: 'historico-precos', element: <HistoricoPrecos />, loader: carregarHistorico },
    { id: 'lista-importacoes', path: 'importacoes', element: <ListaImportacoes />, loader: carregarImportacoes },
    { id: 'enviar-pdf', path: 'importacoes/nova', element: <NovoEnvio />, loader: carregarFornecedoresDoEnvio, action: enviarPdf },
    { path: 'importacoes/catalogo', loader: carregarCatalogo },
    { id: 'detalhe-importacao', path: 'importacoes/:id', element: <DetalheImportacao />, loader: carregarTentativa, action: atualizarTentativa },
    { path: 'login', element: <Login />, action: entrar },
    { path: 'alterar-senha', element: <AlterarSenha />, action: trocarSenha },
    { path: 'sair', action: sair },
    { path: 'usuarios', element: <Usuarios />, loader: carregarUsuarios },
    { path: 'usuarios/novo', element: <NovoUsuario />, action: cadastrarUsuario },
    { path: 'usuarios/:id', element: <AdministrarUsuario />, loader: carregarUsuario, action: administrarUsuario },
  ],
}];
