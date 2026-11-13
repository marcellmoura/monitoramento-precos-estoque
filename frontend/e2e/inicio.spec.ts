import { expect, test } from '@playwright/test';

test('abre a tela inicial sem erros no navegador', async ({ page }) => {
  const errors: string[] = [];
  page.on('pageerror', (error) => errors.push(error.message));
  await page.goto('/');
  await expect(page).toHaveTitle('Monitoramento de Preços, Estoque e Compras');
  await expect(page.getByRole('heading', { name: 'Preços, estoque e compras' }))
    .toBeVisible();
  await expect(page.getByText('Sistema em preparação.')).toBeVisible();
  expect(errors).toEqual([]);
});
