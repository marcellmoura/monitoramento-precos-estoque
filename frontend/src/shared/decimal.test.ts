import { describe, expect, it } from 'vitest';
import { formatarMoeda, precoParaApi } from './decimal';

describe('Valores decimais', () => {
  it.each([
    ['0.00', 'R$ 0,00'], ['12.34', 'R$ 12,34'], ['99999999999999999.99', 'R$ 99.999.999.999.999.999,99'],
    ['9007199254740993.01', 'R$ 9.007.199.254.740.993,01'],
  ])('formata %s sem perder centavos', (input, output) => expect(formatarMoeda(input)).toBe(output));
  it('normaliza vírgula e ponto, preserva zero e distingue ausência', () => {
    expect(precoParaApi(' 12,34 ')).toBe('12.34'); expect(precoParaApi('12.34')).toBe('12.34');
    expect(precoParaApi('0', true)).toBe('0'); expect(precoParaApi('', true)).toBeNull();
  });
  it.each(['', '-1', '1.001', '1.234,56', '100000000000000000.00', 'NaN', '1e2'])('recusa entrada %s', input => {
    expect(() => precoParaApi(input)).toThrow('Use um valor não negativo');
  });
});
