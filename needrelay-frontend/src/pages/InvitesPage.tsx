import { Button, Code, Container, Select, Stack, Table, Text, TextInput, Title } from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { adminApi, authApi, invitesApi } from '../api';
import {
  OrganizationRole,
  OrganizerResponseRole,
  type InviteResponse,
  type OrganizationResponse,
} from '../api/generated/models';
import { ConfirmModal } from '../components/ConfirmModal';
import { SurfaceCard } from '../components/SurfaceCard';

/**
 * Invite management. Organization admins invite into their own organization as USER;
 * platform admins pick a target organization and role. Unused invites can be revoked.
 *
 * @returns invites page
 */
export function InvitesPage() {
  const { t } = useTranslation();
  const [items, setItems] = useState<InviteResponse[]>([]);
  const [isPlatformAdmin, setIsPlatformAdmin] = useState(false);
  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [revokeId, setRevokeId] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const form = useForm({
    initialValues: {
      email: '',
      organizationId: '',
      organizationRole: OrganizationRole.USER as OrganizationRole,
    },
  });

  /**
   * Reloads invite list, and organizations for a platform admin.
   *
   * @returns void
   */
  async function reload() {
    const me = await authApi.me();
    const admin = me.role === OrganizerResponseRole.ADMIN;
    setIsPlatformAdmin(admin);
    const [inviteList, orgList] = await Promise.all([
      invitesApi.listInvites(),
      admin ? adminApi.listOrganizations() : Promise.resolve([]),
    ]);
    setItems(inviteList);
    setOrganizations(orgList);
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, []);

  /**
   * Creates a new invite and reloads the list.
   *
   * @param values email plus, for a platform admin, target org/role
   * @returns void
   */
  async function createInvite(values: typeof form.values) {
    try {
      await invitesApi.createInvite({
        email: values.email || null,
        daysValid: 7,
        organizationId: isPlatformAdmin ? values.organizationId || null : null,
        organizationRole: isPlatformAdmin ? values.organizationRole : null,
      });
      form.reset();
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Revokes an unused invite after confirmation.
   *
   * @param inviteId invite id
   * @returns void
   */
  async function revokeInvite(inviteId: string) {
    setConfirming(true);
    try {
      await invitesApi.revokeInvite(inviteId);
      setRevokeId(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    } finally {
      setConfirming(false);
    }
  }

  /**
   * Revoke button for an unused invite, or nothing once it's been used.
   *
   * @param invite invite row
   * @returns action control or null
   */
  function inviteActions(invite: InviteResponse) {
    if (invite.usedAt) {
      return null;
    }
    return (
      <Button
        size="xs"
        color="red"
        variant="outline"
        onClick={() => setRevokeId(invite.id)}
      >
        {t('invites.revoke')}
      </Button>
    );
  }

  /**
   * Describes an invite's email delivery outcome for the list.
   *
   * @param invite invite row
   * @returns translated status label, or null when no address was given
   */
  function emailStatusLabel(invite: InviteResponse): string | null {
    if (!invite.email) {
      return null;
    }
    if (invite.emailSentAt) {
      return t('invites.emailSent');
    }
    if (invite.emailError) {
      return t('invites.emailFailed');
    }
    return t('invites.emailPending');
  }

  /**
   * Renders one invite's fields for mobile card or table cells.
   *
   * @param invite invite row
   * @returns stacked field block
   */
  function inviteFields(invite: InviteResponse) {
    const status = emailStatusLabel(invite);
    return (
      <Stack gap={4}>
        <Code style={{ wordBreak: 'break-all' }}>
          {`${window.location.origin}/register?invite=${invite.token}`}
        </Code>
        <Text size="sm">
          {invite.email ?? t('invites.noEmail')}
          {status ? ` · ${status}` : ''}
        </Text>
        {isPlatformAdmin ? (
          <Text size="sm" c="dimmed">
            {invite.organizationName} · {t(`organization.roleValues.${invite.organizationRole}`)}
          </Text>
        ) : null}
        <Text size="sm">{new Date(invite.expiresAt).toLocaleString()}</Text>
        <Text size="sm">{invite.usedAt ? t('invites.used') : t('invites.unused')}</Text>
        {inviteActions(invite)}
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
            {isPlatformAdmin ? (
              <>
                <Select
                  label={t('invites.organization')}
                  required
                  data={organizations.map((org) => ({ value: org.id, label: org.name }))}
                  {...form.getInputProps('organizationId')}
                />
                <Select
                  label={t('invites.role')}
                  allowDeselect={false}
                  data={[
                    { value: OrganizationRole.ADMIN, label: t('organization.roleValues.ADMIN') },
                    { value: OrganizationRole.USER, label: t('organization.roleValues.USER') },
                  ]}
                  {...form.getInputProps('organizationRole')}
                />
              </>
            ) : null}
            <Button type="submit" w="fit-content">
              {t('invites.create')}
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
              <Table.Th>{t('invites.tokenLink')}</Table.Th>
              <Table.Th>{t('invites.email')}</Table.Th>
              <Table.Th>{t('invites.emailStatus')}</Table.Th>
              {isPlatformAdmin ? <Table.Th>{t('invites.organization')}</Table.Th> : null}
              <Table.Th>{t('invites.expires')}</Table.Th>
              <Table.Th>{t('invites.usedColumn')}</Table.Th>
              <Table.Th />
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {items.map((invite) => (
              <Table.Tr key={invite.id}>
                <Table.Td>
                  <Code>{`${window.location.origin}/register?invite=${invite.token}`}</Code>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{invite.email ?? t('invites.noEmail')}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{emailStatusLabel(invite) ?? '—'}</Text>
                </Table.Td>
                {isPlatformAdmin ? (
                  <Table.Td>
                    <Text size="sm">
                      {invite.organizationName} · {t(`organization.roleValues.${invite.organizationRole}`)}
                    </Text>
                  </Table.Td>
                ) : null}
                <Table.Td>
                  <Text size="sm">{new Date(invite.expiresAt).toLocaleString()}</Text>
                </Table.Td>
                <Table.Td>
                  <Text size="sm">{invite.usedAt ? t('invites.used') : t('invites.unused')}</Text>
                </Table.Td>
                <Table.Td>{inviteActions(invite)}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Stack>

      <ConfirmModal
        opened={Boolean(revokeId)}
        onClose={() => setRevokeId(null)}
        title={t('common.confirmTitle')}
        body={t('invites.confirmRevoke')}
        confirming={confirming}
        onConfirm={() => {
          if (revokeId) {
            void revokeInvite(revokeId);
          }
        }}
      />
    </Container>
  );
}
