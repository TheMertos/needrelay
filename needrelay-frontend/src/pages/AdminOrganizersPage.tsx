import { Badge, Button, Container, Group, Stack, Table, Text, Title } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi, authApi } from '../api';
import type { OrganizerResponse } from '../api/generated/models';
import { OrganizerResponseRole } from '../api/generated/models';
import { SurfaceCard } from '../components/SurfaceCard';

/**
 * Admin page to list organizers and ban/unban accounts.
 *
 * @returns admin organizers page
 */
export function AdminOrganizersPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<OrganizerResponse[]>([]);
  const [meId, setMeId] = useState<string | null>(null);

  /**
   * Reloads organizer list and current user id.
   *
   * @returns void
   */
  async function reload() {
    const [list, me] = await Promise.all([adminApi.listOrganizers(), authApi.me()]);
    setItems(list);
    setMeId(me.id);
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, []);

  /**
   * Bans an organizer.
   *
   * @param id organizer id
   * @returns void
   */
  async function ban(id: string) {
    try {
      await adminApi.banOrganizer(id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Unbans an organizer.
   *
   * @param id organizer id
   * @returns void
   */
  async function unban(id: string) {
    try {
      await adminApi.unbanOrganizer(id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Ban/unban controls for one organizer.
   *
   * @param org organizer row
   * @returns action controls
   */
  function orgActions(org: OrganizerResponse) {
    if (org.id === meId || org.role === OrganizerResponseRole.ADMIN) {
      return (
        <Text size="sm" c="dimmed">
          —
        </Text>
      );
    }
    if (org.active) {
      return (
        <Button size="sm" color="red" variant="light" onClick={() => void ban(org.id)}>
          {t('admin.ban')}
        </Button>
      );
    }
    return (
      <Button size="sm" variant="light" onClick={() => void unban(org.id)}>
        {t('admin.unban')}
      </Button>
    );
  }

  return (
    <Container size="lg" py="md">
      <Stack>
        <Title order={2}>{t('admin.title')}</Title>

        <Stack gap="sm" hiddenFrom="sm" data-testid="mobile-card-list">
          {items.map((org) => (
            <SurfaceCard key={org.id} p="md" accent="ink">
              <Stack gap="xs">
                <Text fw={700}>{org.displayName}</Text>
                <Text size="sm">{org.email}</Text>
                <Text size="sm">{org.role}</Text>
                <Badge color={org.active ? 'teal' : 'red'} variant="light" w="fit-content">
                  {org.active ? t('admin.active') : t('admin.banned')}
                </Badge>
                {orgActions(org)}
              </Stack>
            </SurfaceCard>
          ))}
        </Stack>

        <Table visibleFrom="sm" data-testid="desktop-table">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('auth.displayName')}</Table.Th>
              <Table.Th>{t('auth.email')}</Table.Th>
              <Table.Th>Role</Table.Th>
              <Table.Th>Status</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((org) => (
              <Table.Tr key={org.id}>
                <Table.Td>{org.displayName}</Table.Td>
                <Table.Td>{org.email}</Table.Td>
                <Table.Td>{org.role}</Table.Td>
                <Table.Td>
                  <Badge color={org.active ? 'teal' : 'red'} variant="light">
                    {org.active ? t('admin.active') : t('admin.banned')}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" justify="flex-end">
                    {orgActions(org)}
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>
    </Container>
  );
}
