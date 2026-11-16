const inteiros = new Intl.NumberFormat('pt-BR');

export function formatarMoeda(valor: string): string {
  const [inteiro, centavos] = valor.split('.');
  return `R$ ${inteiros.format(BigInt(inteiro))},${centavos}`;
}

export function precoParaApi(valor: string, opcional = false): string | null {
  const texto = valor.trim();
  if (!texto && opcional) return null;
  if (!/^\d{1,17}([.,]\d{1,2})?$/.test(texto)) {
    throw new Error('Use um valor não negativo, com até 17 dígitos inteiros e duas casas decimais.');
  }
  return texto.replace(',', '.');
}
