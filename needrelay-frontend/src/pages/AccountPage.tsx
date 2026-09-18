import {
  Button,
  Container,
  Divider,
  Group,
  Modal,
  PasswordInput,
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
import { authApi, contactsApi } from '../api';
import type { OrganizerContactResponse, OrganizerResponse } from '../api/generated/models';
import { SurfaceCard } from '../components/SurfaceCard';

/**
 * Account settings: org profile, contacts, and password change.
 *
 * @returns account page
 */
export function AccountPage() {
  const { t } = useTranslation();
  const [me, setMe] = useState<OrganizerResponse | null>(null);
  const [contacts, setContacts] = useState<OrganizerContactResponse[]>([]);
  const [contactOpen, setContactOpen] = useState(false);
  const [editingContact, setEditingContact] = useState<OrganizerContactResponse | null>(null);
  const profileForm = useForm({
    initialValues: { displayName: '', description: '' },
  });
  const passwordForm = useForm({
    initialValues: { currentPassword: '', newPassword: '' },
  });
  const contactForm = useForm({
    initialValues: { name: '', role: '', phone: '', email: '', note: '' },
  });

  /**
   * Reloads profile and contacts.
   */
  async function reload() {
    const [profile, contactList] = await Promise.all([
      authApi.me(),
      contactsApi.listContacts(),
    ]);
    setMe(profile);
    setContacts(contactList);
    profileForm.setValues({
      displayName: profile.displayName,
      description: profile.description ?? '',
    });
  }

  useEffect(() => {
    void reload().catch(() => {
      notifications.show({ color: 'red', message: t('common.error') });
    });
  }, []);

  /**
   * Saves organization display name and description.
   *
   * @param values profile form
   */
  async function saveProfile(values: typeof profileForm.values) {
    try {
      const updated = await authApi.updateProfile({
        displayName: values.displayName,
        description: values.description.trim() || null,
      });
      setMe(updated);
      notifications.show({ color: 'green', message: t('account.profileSaved') });
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
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
   * Deletes a contact.
   *
   * @param contactId contact id
   */
  async function removeContact(contactId: string) {
    try {
      await contactsApi.deleteContact(contactId);
      await reload();
    } catch {
      notifications.show({ color: 'red', message: t('common.error') });
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
                  label={t('auth.displayName')}
                  {...profileForm.getInputProps('displayName')}
                />
                <Textarea
                  label={t('account.description')}
                  description={t('account.descriptionHint')}
                  minRows={4}
                  {...profileForm.getInputProps('description')}
                />
                <Button type="submit" w="fit-content" color="ink">
                  {t('account.saveProfile')}
                </Button>
              </Stack>
            </form>
          </Stack>
        </SurfaceCard>

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
                          {contact.phone} · {contact.email}
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
                          onClick={() => void removeContact(contact.id)}
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
    </Container>
  );
}
