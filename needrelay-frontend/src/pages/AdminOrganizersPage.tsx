import {
  Badge,
  Button,
  Container,
  Group,
  Modal,
  Select,
  Stack,
  Table,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi, authApi, settingsApi } from '../api';
import type { OrganizationResponse, OrganizerResponse } from '../api/generated/models';
import { OrganizationRole, OrganizerResponseRole } from '../api/generated/models';
import { ConfirmModal } from '../components/ConfirmModal';
import { SurfaceCard } from '../components/SurfaceCard';
import { UI_LANGUAGES } from '../locales/languages';

type PendingAdminConfirm =
  | { kind: 'ban'; id: string }
  | { kind: 'unban'; id: string }
  | { kind: 'deactivateOrg'; id: string }
  | { kind: 'activateOrg'; id: string };

/**
 * Admin page to list organizers (ban/unban) and organizations (create/edit/deactivate).
 *
 * @returns admin organizers page
 */
export function AdminOrganizersPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<OrganizerResponse[]>([]);
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [meId, setMeId] = useState<string | null>(null);
  const [pendingConfirm, setPendingConfirm] = useState<PendingAdminConfirm | null>(null);
  const [confirming, setConfirming] = useState(false);
  const [orgModalOpen, setOrgModalOpen] = useState(false);
  const [editingOrganization, setEditingOrganization] = useState<OrganizationResponse | null>(null);
  const orgForm = useForm({ initialValues: { name: '', description: '' } });
  const [defaultLanguage, setDefaultLanguage] = useState('en');
  const [savingDefaultLanguage, setSavingDefaultLanguage] = useState(false);

  /**
   * Reloads organizer list, organization list, platform settings, and current user id.
   *
   * @returns void
   */
  async function reload() {
    const [list, orgList, settings, me] = await Promise.all([
      adminApi.listOrganizers(),
      adminApi.listOrganizations(),
      settingsApi.getPublicSettings(),
      authApi.me(),
    ]);
    setItems(list);
    setOrganizations(orgList);
    setDefaultLanguage(settings.defaultLanguage);
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
   * Saves the platform's default UI language.
   *
   * @param language new default language code
   * @returns void
   */
  async function saveDefaultLanguage(language: string) {
    setSavingDefaultLanguage(true);
    try {
      const updated = await adminApi.updateSystemSettings({ defaultLanguage: language });
      setDefaultLanguage(updated.defaultLanguage);
      notifications.show({ color: 'green', message: t('admin.settingsSaved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    } finally {
      setSavingDefaultLanguage(false);
    }
  }

  /**
   * Changes an organization member's role.
   *
   * @param id organizer id
   * @param role new organization role
   * @returns void
   */
  async function changeOrganizationRole(id: string, role: OrganizationRole) {
    try {
      await adminApi.adminUpdateOrganizerRole(id, { organizationRole: role });
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Deactivates an organization (its members can no longer sign in).
   *
   * @param id organization id
   * @returns void
   */
  async function deactivateOrganization(id: string) {
    try {
      await adminApi.deactivateOrganization(id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Reactivates an organization.
   *
   * @param id organization id
   * @returns void
   */
  async function activateOrganization(id: string) {
    try {
      await adminApi.activateOrganization(id);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Runs the pending confirm action after confirmation.
   *
   * @returns void
   */
  async function runPendingConfirm() {
    if (!pendingConfirm) {
      return;
    }
    setConfirming(true);
    try {
      if (pendingConfirm.kind === 'ban') {
        await ban(pendingConfirm.id);
      } else if (pendingConfirm.kind === 'unban') {
        await unban(pendingConfirm.id);
      } else if (pendingConfirm.kind === 'deactivateOrg') {
        await deactivateOrganization(pendingConfirm.id);
      } else {
        await activateOrganization(pendingConfirm.id);
      }
      setPendingConfirm(null);
    } finally {
      setConfirming(false);
    }
  }

  /**
   * Opens the create-organization modal.
   *
   * @returns void
   */
  function openCreateOrganization() {
    setEditingOrganization(null);
    orgForm.reset();
    setOrgModalOpen(true);
  }

  /**
   * Opens the edit-organization modal seeded with its current values.
   *
   * @param organization organization to edit
   * @returns void
   */
  function openEditOrganization(organization: OrganizationResponse) {
    setEditingOrganization(organization);
    orgForm.setValues({ name: organization.name, description: organization.description ?? '' });
    setOrgModalOpen(true);
  }

  /**
   * Creates or updates an organization.
   *
   * @param values name/description
   * @returns void
   */
  async function saveOrganization(values: typeof orgForm.values) {
    try {
      const payload = { name: values.name, description: values.description.trim() || null };
      if (editingOrganization) {
        await adminApi.updateOrganization(editingOrganization.id, payload);
      } else {
        await adminApi.createOrganization(payload);
      }
      setOrgModalOpen(false);
      orgForm.reset();
      setEditingOrganization(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Editable organization-role control for one organizer; a static badge (or dash for
   * platform admins, who have no organization) otherwise.
   *
   * @param org organizer row
   * @returns role control
   */
  function orgRoleControl(org: OrganizerResponse) {
    if (!org.organizationId || !org.organizationRole) {
      return (
        <Text size="sm" c="dimmed">
          —
        </Text>
      );
    }
    return (
      <Select
        size="xs"
        w={140}
        value={org.organizationRole}
        data={[
          { value: OrganizationRole.ADMIN, label: t('organization.roleValues.ADMIN') },
          { value: OrganizationRole.USER, label: t('organization.roleValues.USER') },
        ]}
        allowDeselect={false}
        onChange={(value) => {
          if (value) {
            void changeOrganizationRole(org.id, value as OrganizationRole);
          }
        }}
      />
    );
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
        <Button
          size="sm"
          color="red"
          variant="light"
          onClick={() => setPendingConfirm({ kind: 'ban', id: org.id })}
        >
          {t('admin.ban')}
        </Button>
      );
    }
    return (
      <Button
        size="sm"
        variant="light"
        onClick={() => setPendingConfirm({ kind: 'unban', id: org.id })}
      >
        {t('admin.unban')}
      </Button>
    );
  }

  return (
    <Container size="lg" py="md">
      <Stack gap="xl">
        <Stack>
          <Title order={2}>{t('admin.title')}</Title>

          <Stack gap="sm" hiddenFrom="sm" data-testid="mobile-card-list">
            {items.map((org) => (
              <SurfaceCard key={org.id} p="md" accent="ink">
                <Stack gap="xs">
                  <Text fw={700}>{org.displayName}</Text>
                  <Text size="sm">{org.email}</Text>
                  <Text size="sm">{t(`admin.roleValues.${org.role}`)}</Text>
                  <Text size="sm" c="dimmed">
                    {org.organizationName ?? '—'}
                  </Text>
                  <Badge color={org.active ? 'teal' : 'red'} variant="light" w="fit-content">
                    {org.active ? t('admin.active') : t('admin.banned')}
                  </Badge>
                  {orgRoleControl(org)}
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
                <Table.Th>{t('admin.role')}</Table.Th>
                <Table.Th>{t('admin.organization')}</Table.Th>
                <Table.Th>{t('admin.orgRole')}</Table.Th>
                <Table.Th>{t('admin.status')}</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {items.map((org) => (
                <Table.Tr key={org.id}>
                  <Table.Td>{org.displayName}</Table.Td>
                  <Table.Td>{org.email}</Table.Td>
                  <Table.Td>{t(`admin.roleValues.${org.role}`)}</Table.Td>
                  <Table.Td>{org.organizationName ?? '—'}</Table.Td>
                  <Table.Td>{orgRoleControl(org)}</Table.Td>
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

        <Stack>
          <Group justify="space-between">
            <Title order={3}>{t('admin.organizationsTitle')}</Title>
            <Button onClick={openCreateOrganization}>{t('admin.createOrganization')}</Button>
          </Group>
          <Stack gap="sm">
            {organizations.map((org) => (
              <SurfaceCard key={org.id} p="md" accent="none">
                <Group justify="space-between" align="flex-start" wrap="wrap">
                  <div>
                    <Group gap="xs" align="center">
                      <Text fw={700}>{org.name}</Text>
                      <Badge color={org.active ? 'teal' : 'red'} variant="light" size="sm">
                        {org.active ? t('admin.active') : t('admin.banned')}
                      </Badge>
                    </Group>
                    {org.description ? (
                      <Text size="sm" c="dimmed">
                        {org.description}
                      </Text>
                    ) : null}
                  </div>
                  <Group gap="xs">
                    <Button size="xs" variant="light" onClick={() => openEditOrganization(org)}>
                      {t('admin.editOrganization')}
                    </Button>
                    {org.active ? (
                      <Button
                        size="xs"
                        color="red"
                        variant="outline"
                        onClick={() => setPendingConfirm({ kind: 'deactivateOrg', id: org.id })}
                      >
                        {t('admin.deactivateOrganization')}
                      </Button>
                    ) : (
                      <Button
                        size="xs"
                        variant="outline"
                        onClick={() => setPendingConfirm({ kind: 'activateOrg', id: org.id })}
                      >
                        {t('admin.activateOrganization')}
                      </Button>
                    )}
                  </Group>
                </Group>
              </SurfaceCard>
            ))}
          </Stack>
        </Stack>

        <Stack>
          <Title order={3}>{t('admin.settingsTitle')}</Title>
          <SurfaceCard p="md" accent="none">
            <Group justify="space-between" align="flex-end" wrap="wrap" gap="sm">
              <Select
                label={t('admin.defaultLanguage')}
                description={t('admin.defaultLanguageHint')}
                data={[...UI_LANGUAGES]}
                value={defaultLanguage}
                onChange={(value) => value && void saveDefaultLanguage(value)}
                disabled={savingDefaultLanguage}
                searchable
                allowDeselect={false}
                w={220}
              />
            </Group>
          </SurfaceCard>
        </Stack>
      </Stack>

      <Modal
        opened={orgModalOpen}
        onClose={() => setOrgModalOpen(false)}
        title={editingOrganization ? t('admin.editOrganization') : t('admin.createOrganization')}
      >
        <form onSubmit={orgForm.onSubmit(saveOrganization)}>
          <Stack>
            <TextInput
              label={t('admin.organizationName')}
              required
              {...orgForm.getInputProps('name')}
            />
            <Textarea
              label={t('admin.organizationDescription')}
              {...orgForm.getInputProps('description')}
            />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <ConfirmModal
        opened={Boolean(pendingConfirm)}
        onClose={() => setPendingConfirm(null)}
        title={t('common.confirmTitle')}
        body={
          pendingConfirm?.kind === 'unban'
            ? t('admin.confirmUnban')
            : pendingConfirm?.kind === 'ban'
              ? t('admin.confirmBan')
              : pendingConfirm?.kind === 'activateOrg'
                ? t('admin.confirmActivateOrganization')
                : t('admin.confirmDeactivateOrganization')
        }
        confirming={confirming}
        onConfirm={() => void runPendingConfirm()}
      />
    </Container>
  );
}
