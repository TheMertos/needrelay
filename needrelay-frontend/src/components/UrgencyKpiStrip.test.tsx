import { MantineProvider } from '@mantine/core';
import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import i18n from '../i18n';
import { theme } from '../theme';
import { UrgencyKpiStrip } from './UrgencyKpiStrip';

describe('UrgencyKpiStrip', () => {
  it('shows critical open covered values', async () => {
    await i18n.changeLanguage('en');
    render(
      <MantineProvider theme={theme}>
        <UrgencyKpiStrip critical={5} open={14} covered={8} />
      </MantineProvider>,
    );
    expect(screen.getByTestId('kpi-critical')).toHaveTextContent('5');
    expect(screen.getByTestId('kpi-open')).toHaveTextContent('14');
    expect(screen.getByTestId('kpi-covered')).toHaveTextContent('8');
  });
});
