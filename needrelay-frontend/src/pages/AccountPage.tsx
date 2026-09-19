import {
  Badge,
  Button,
  Container,
  Divider,
  Group,
  Modal,
  PasswordInput,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { authApi, contactsApi, organizationApi } from '../api';
import {
  OrganizationRole,
  type OrganizationMemberResponse,
  type OrganizationResponse,
  type OrganizerContactResponse,
  type OrganizerResponse,
} from '../api/generated/models';
import { SurfaceCard } from '../components/SurfaceCard';
import { ConfirmModal } from '../components/ConfirmModal';

/**
 * Account settings: personal profile, organization profile, members, contacts, and password.
 *
 * @returns account page
 */
export function AccountPage() {
  const { t } = useTranslation();
  const [me, setMe] = useState<OrganizerResponse | null>(null);
  const [organization, setOrganization] = useState<OrganizationResponse | null>(null);
  const [members, setMembers] = useState<OrganizationMemberResponse[]>([]);
  const [contacts, setContacts] = useState<OrganizerContactResponse[]>([]);
  const [contactOpen, setContactOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<OrganizerContactResponse | null>(null);
  const [deleteContactId, setDeleteContactId] = useState<string | null>(null);
  const [removeMemberId, setRemoveMemberId] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);
  const profileForm = useForm({
    initialValues: { displayName: '' },
  });
  const orgForm = useForm({
    initialValues: { name: '', description: '' },
  });
  const passwordForm = useForm({
    initialValues: { currentPassword: '', newPassword: '' },
  });
  const contactForm = useForm({
    initialValues: { name: '', role: '', phone: '', email: '', note: '' },
  });

  const isOrgAdmin = me?.organizationRole === OrganizationRole.ADMIN;

  /**
   * Reloads profile, organization, members, and contacts.
   */
  async function reload() {
    const profile = await authApi.me();
    setMe(profile);
    profileForm.setValues({ displayName: profile.displayName });
    if (profile.organizationId) {
      const [org, memberList, contactList] = await Promise.all([
        organizationApi.getMyOrganization(),
        organizationApi.listOrganizationMembers(),
        contactsApi.listContacts(),
      ]);
      setOrganization(org);
      orgForm.setValues({ name: org.name, description: org.description ?? '' });
      setMembers(memberList);
      setContacts(contactList);
    }
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, []);

  /**
   * Saves the person's own display name.
   *
   * @param values profile form
   */
  async function saveProfile(values: typeof profileForm.values) {
    try {
      const updated = await authApi.updateProfile({ displayName: values.displayName });
      setMe(updated);
      notifications.show({ color: 'green', message: t('account.profileSaved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Saves the organization's name/description. Organization admin only.
   *
   * @param values organization form
   */
  async function saveOrganization(values: typeof orgForm.values) {
    try {
      const updated = await organizationApi.updateMyOrganization({
        name: values.name,
        description: values.description.trim() || null,
      });
      setOrganization(updated);
      notifications.show({ color: 'green', message: t('organization.saved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Changes a member's role.
   *
   * @param memberId member id
   * @param role new role
   * @returns void
   */
  async function changeMemberRole(memberId: string, role: OrganizationRole) {
    try {
      await organizationApi.updateMemberRole(memberId, { organizationRole: role });
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Removes a member after confirmation.
   *
   * @param memberId member id
   * @returns void
   */
  async function removeMember(memberId: string) {
    setConfirming(true);
    try {
      await organizationApi.removeMember(memberId);
      setRemoveMemberId(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    } finally {
      setConfirming(false);
    }
  }

  /**
   * Changes the account password.
   *
   * @param values password form
   */
  async function savePassword(values: typeof passwordForm.values) {
    try {
      await authApi.changePassword(values);
      passwordForm.reset();
      notifications.show({ color: 'green', message: t('account.passwordChanged') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Opens create-contact modal.
   */
  function openCreateContact() {
    setEditingContact(null);
    contactForm.reset();
    setContactOpen(true);
  }

  /**
   * Opens edit-contact modal.
   *
   * @param contact contact to edit
   */
  function openEditContact(contact: OrganizerContactResponse) {
    setEditingContact(contact);
    contactForm.setValues({
      name: contact.name,
      role: contact.role,
      phone: contact.phone,
      email: contact.email,
      note: contact.note ?? '',
    });
    setContactOpen(true);
  }

  /**
   * Creates or updates a contact.
   *
   * @param values contact form
   */
  async function saveContact(values: typeof contactForm.values) {
    const payload = {
      name: values.name,
      role: values.role,
      phone: values.phone,
      email: values.email,
      note: values.note.trim() || null,
    };
    try {
      if (editingContact) {
        await contactsApi.updateContact(editingContact.id, payload);
      } else {
        await contactsApi.createContact(payload);
      }
      setContactOpen(false);
      contactForm.reset();
      setEditingContact(null);
      await reload();
      notifications.show({ color: 'green', message: t('account.contactSaved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    }
  }

  /**
   * Deletes a contact after confirmation.
   *
   * @param contactId contact id
   * @returns void
   */
  async function removeContact(contactId: string) {
    setConfirming(true);
    try {
      await contactsApi.deleteContact(contactId);
      setDeleteContactId(null);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
    } finally {
      setConfirming(false);
    }
  }

  return (
    <Container size="sm" py="md">
      <Stack gap="lg">
        <SurfaceCard accent="signal">
          <Stack gap="md">
            <Title order={2}>{t('account.title')}</Title>
            {me ? <TextInput label={t('auth.email')} value={me.email} disabled /> : null}
            <form onSubmit={profileForm.onSubmit(saveProfile)}>
              <Stack>
                <TextInput
                  label={t('account.myName')}
                  {...profileForm.getInputProps('displayName')}
                />
                <Button type="submit" w="fit-content" color="ink">
                  {t('account.saveProfile')}
                </Button>
              </Stack>
            </form>
          </Stack>
        </SurfaceCard>

        {organization ? (
          <SurfaceCard accent="ink">
            <Stack gap="md">
              <Title order={3}>{t('organization.title')}</Title>
              {isOrgAdmin ? (
                <form onSubmit={orgForm.onSubmit(saveOrganization)}>
                  <Stack>
                    <TextInput label={t('organization.name')} required {...orgForm.getInputProps('name')} />
                    <Textarea
                      label={t('organization.description')}
                      minRows={4}
                      {...orgForm.getInputProps('description')}
                    />
                    <Button type="submit" w="fit-content" color="ink">
                      {t('organization.save')}
                    </Button>
                  </Stack>
                </form>
              ) : (
                <Stack gap={4}>
                  <Text fw={700}>{organization.name}</Text>
                  {organization.description ? <Text size="sm">{organization.description}</Text> : null}
                </Stack>
              )}
            </Stack>
          </SurfaceCard>
        ) : null}

        {organization ? (
          <SurfaceCard accent="none">
            <Stack gap="md">
              <Group justify="space-between">
                <Title order={3}>{t('organization.membersTitle')}</Title>
                {isOrgAdmin ? (
                  <Button component={Link} to="/invites">
                    {t('organization.invite')}
                  </Button>
                ) : null}
              </Group>
              <Stack gap="sm">
                {members.map((member) => (
                  <SurfaceCard key={member.id} p="md" accent="none">
                    <Group justify="space-between" align="center" wrap="wrap">
                      <div>
                        <Group gap="xs" align="center">
                          <Text fw={700}>{member.displayName}</Text>
                          {!member.active ? (
                            <Badge color="gray" size="sm">
                              {t('admin.banned')}
                            </Badge>
                          ) : null}
                        </Group>
                        <Text size="sm">{member.email}</Text>
                      </div>
                      {isOrgAdmin && member.id !== me?.id ? (
                        <Group gap="xs">
                          <Select
                            size="xs"
                            w={140}
                            value={member.organizationRole}
                            data={[
                              { value: OrganizationRole.ADMIN, label: t('organization.roleValues.ADMIN') },
                              { value: OrganizationRole.USER, label: t('organization.roleValues.USER') },
                            ]}
                            allowDeselect={false}
                            onChange={(value) => {
                              if (value) {
                                void changeMemberRole(member.id, value as OrganizationRole);
                              }
                            }}
                          />
                          <Button
                            size="xs"
                            color="red"
                            variant="outline"
                            onClick={() => setRemoveMemberId(member.id)}
                          >
                            {t('organization.remove')}
                          </Button>
                        </Group>
                      ) : (
                        <Badge variant="light">{t(`organization.roleValues.${member.organizationRole}`)}</Badge>
                      )}
                    </Group>
                  </SurfaceCard>
                ))}
              </Stack>
            </Stack>
          </SurfaceCard>
        ) : null}

        <SurfaceCard accent="ink">
          <Stack gap="md">
            <Group justify="space-between">
              <Title order={3}>{t('account.contactsTitle')}</Title>
              <Button onClick={openCreateContact}>{t('account.addContact')}</Button>
            </Group>
            <Text size="sm" c="dimmed">
              {t('account.contactsHint')}
            </Text>
            {contacts.length === 0 ? (
              <Text c="dimmed">{t('account.contactsEmpty')}</Text>
            ) : (
              <Stack gap="sm">
                {contacts.map((contact) => (
                  <SurfaceCard key={contact.id} p="md" accent="none">
                    <Group justify="space-between" align="flex-start">
                      <div>
                        <Text fw={700}>{contact.name}</Text>
                        <Text size="sm">{contact.role}</Text>
                        <Text size="sm">
                          <bdi dir="ltr">
                            {contact.phone} · {contact.email}
                          </bdi>
                        </Text>
                        {contact.note ? (
                          <Text size="xs" c="dimmed" mt={4}>
                            {contact.note}
                          </Text>
                        ) : null}
                      </div>
                      <Group gap="xs">
                        <Button size="xs" variant="light" onClick={() => openEditContact(contact)}>
                          {t('account.editContact')}
                        </Button>
                        <Button
                          size="xs"
                          color="red"
                          variant="outline"
                          onClick={() => setDeleteContactId(contact.id)}
                        >
                          {t('common.delete')}
                        </Button>
                      </Group>
                    </Group>
                  </SurfaceCard>
                ))}
              </Stack>
            )}
          </Stack>
        </SurfaceCard>

        <SurfaceCard accent="none">
          <form onSubmit={passwordForm.onSubmit(savePassword)}>
            <Stack>
              <Title order={4}>{t('account.changePassword')}</Title>
              <Divider />
              <PasswordInput
                label={t('auth.currentPassword')}
                {...passwordForm.getInputProps('currentPassword')}
              />
              <PasswordInput
                label={t('auth.newPassword')}
                {...passwordForm.getInputProps('newPassword')}
              />
              <Button type="submit" w="fit-content" color="ink">
                {t('account.savePassword')}
              </Button>
            </Stack>
          </form>
        </SurfaceCard>
      </Stack>

      <Modal
        opened={contactOpen}
        onClose={() => setContactOpen(false)}
        title={editingContact ? t('account.editContact') : t('account.addContact')}
      >
        <form onSubmit={contactForm.onSubmit(saveContact)}>
          <Stack>
            <TextInput label={t('account.contactName')} required {...contactForm.getInputProps('name')} />
            <TextInput label={t('account.contactRole')} required {...contactForm.getInputProps('role')} />
            <TextInput label={t('account.contactPhone')} required {...contactForm.getInputProps('phone')} />
            <TextInput label={t('account.contactEmail')} required {...contactForm.getInputProps('email')} />
            <Textarea label={t('account.contactNote')} {...contactForm.getInputProps('note')} />
            <Button type="submit">{t('request.save')}</Button>
          </Stack>
        </form>
      </Modal>

      <ConfirmModal
        opened={Boolean(deleteContactId)}
        onClose={() => setDeleteContactId(null)}
        title={t('common.confirmTitle')}
        body={t('account.confirmDeleteContact')}
        confirming={confirming}
        onConfirm={() => {
          if (deleteContactId) {
            void removeContact(deleteContactId);
          }
        }}
      />

      <ConfirmModal
        opened={Boolean(removeMemberId)}
        onClose={() => setRemoveMemberId(null)}
        title={t('common.confirmTitle')}
        body={t('organization.confirmRemoveMember')}
        confirming={confirming}
        onConfirm={() => {
          if (removeMemberId) {
            void removeMember(removeMemberId);
          }
        }}
      />
    </Container>
  );
}
