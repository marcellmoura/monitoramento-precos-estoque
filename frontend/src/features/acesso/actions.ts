import { redirect } from 'react-router';
import type { ActionFunctionArgs } from 'react-router';
import { HttpError } from '../../shared/http';
import { alterarAcesso, mensagemErro } from './api';

export async function entrar({ request }: ActionFunctionArgs) {
  const data = await request.formData();
  try {
    const usuario = await alterarAcesso('login', 'POST', {
      email: data.get('email'), senha: data.get('senha'),
    });
    return redirect(usuario?.trocaSenhaObrigatoria ? '/alterar-senha' : '/');
  } catch (error) { return { erro: mensagemErro(error) }; }
}

export async function trocarSenha({ request }: ActionFunctionArgs) {
  const data = await request.formData();
  if (data.get('novaSenha') !== data.get('confirmacao')) return { erro: 'As senhas não conferem.' };
  try {
    await alterarAcesso('senha', 'PUT', {
      senhaAtual: data.get('senhaAtual'), novaSenha: data.get('novaSenha'),
    });
    return redirect('/login?senha=alterada');
  } catch (error) {
    if (error instanceof HttpError && error.status === 401) return redirect('/login');
    return { erro: mensagemErro(error) };
  }
}

export async function sair() {
  try {
    await alterarAcesso('logout', 'POST');
    return redirect('/login');
  } catch (error) {
    if (error instanceof HttpError && error.status === 401) return redirect('/login');
    return { erro: mensagemErro(error) };
  }
}
