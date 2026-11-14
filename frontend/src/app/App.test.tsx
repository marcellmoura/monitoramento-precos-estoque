import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { App } from './App';

describe('Tela inicial', () => {
  it('identifica o sistema e informa seu estado inicial', () => {
    render(<App />);
    expect(screen.getByRole('heading', { name: 'Preços, estoque e compras' }))
      .toBeVisible();
    expect(screen.getByText('Sistema em preparação.')).toBeVisible();
  });
});
