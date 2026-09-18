import { getAdmin } from './generated/admin/admin';
import { getAuth } from './generated/auth/auth';
import { getComments } from './generated/comments/comments';
import { getContacts } from './generated/contacts/contacts';
import { getInvites } from './generated/invites/invites';
import { getNeeds } from './generated/needs/needs';
import { getPublic } from './generated/public/public';
import { getReliefRequests } from './generated/relief-requests/relief-requests';

/** Typed auth API from OpenAPI. */
export const authApi = getAuth();
/** Typed admin API from OpenAPI. */
export const adminApi = getAdmin();
/** Typed comments API from OpenAPI. */
export const commentsApi = getComments();
/** Typed organization contacts API from OpenAPI. */
export const contactsApi = getContacts();
/** Typed invites API from OpenAPI. */
export const invitesApi = getInvites();
/** Typed needs API from OpenAPI. */
export const needsApi = getNeeds();
/** Typed public API from OpenAPI. */
export const publicApi = getPublic();
/** Typed relief-request API from OpenAPI. */
export const reliefRequestsApi = getReliefRequests();
