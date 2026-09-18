import { Button, Code, Container, Stack, Table, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { invitesApi } from '../api';
import type { InviteResponse } from '../api/generated/models';
import { SurfaceCard } from '../components/SurfaceCard';

/**
 * Invite management for organizers.
 *
 * @returns invites page
 */
export function InvitesPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<InviteResponse[]>([]);
  const form = useForm({ initialValues: { email: '' } });

  /**
   * Reloads invite list.
   *
   * @returns void
   */
  async function reload() {
    setItems(await invitesApi.listInvites());
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, []);

  /**
   * Creates a new invite and reloads the list.
   *
   * @param values optional email
   * @returns void
   */
  async function createInvite(values: typeof form.values) {
    try {
      await invitesApi.createInvite({
        email: values.email || null,
        daysValid: 7,
      });
      form.reset();
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Renders one invite's fields for mobile card or table cells.
   *
   * @param invite invite row
   * @returns stacked field block
   */
  function inviteFields(invite: InviteResponse) {
    return (
      <Stack gap={4}>
        <Code style={{ wordBreak: 'break-all' }}>
          {`${window.location.origin}/register?invite=${invite.token}`}
        </Code>
        <Text size="sm">{new Date(invite.expiresAt).toLocaleString()}</Text>
        <Text size="sm">{invite.usedAt ? 'yes' : 'no'}</Text>
      </Stack>
    );
  }

  return (
    <Container size="md" py="md">
      <Stack>
        <Title order={2}>{t('nav.invites')}</Title>
        <form onSubmit={form.onSubmit(createInvite)}>
          <Stack>
            <TextInput label={t('auth.email')} {...form.getInputProps('email')} />
            <Button type="submit" w="fit-content">
              Create invite
            </Button>
          </Stack>
        </form>

        <Stack gap="sm" hiddenFrom="sm" data-testid="mobile-card-list">
          {items.map((invite) => (
            <SurfaceCard key={invite.id} p="md" accent="ink">
              {inviteFields(invite)}
            </SurfaceCard>
          ))}
        </Stack>

        <Table visibleFrom="sm" data-testid="desktop-table">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Token / link</Table.Th>
              <Table.Th>Expires</Table.Th>
              <Table.Th>Used</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((invite) => (
              <Table.Tr key={invite.id}>
                <Table.Td>
                  <Code>{`${window.location.origin}/register?invite=${invite.token}`}</Code>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{new Date(invite.expiresAt).toLocaleString()}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{invite.usedAt ? 'yes' : 'no'}</Text>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>
    </Container>
  );
}
