import bcrypt from 'bcryptjs';
import pg from 'pg';

const { Client } = pg;

const IDS = {
  admin: '11111111-1111-1111-1111-111111111101',
  org: '11111111-1111-1111-1111-111111111102',
  contact1: '22222222-2222-2222-2222-222222222201',
  contact2: '22222222-2222-2222-2222-222222222202',
  activeA: '33333333-3333-3333-3333-333333333301',
  activeB: '33333333-3333-3333-3333-333333333302',
  archived: '33333333-3333-3333-3333-333333333303',
  needOpenA: '44444444-4444-4444-4444-444444444401',
  needPartialA: '44444444-4444-4444-4444-444444444402',
  needCoveredA: '44444444-4444-4444-4444-444444444403',
  needClosedA: '44444444-4444-4444-4444-444444444404',
  needOpenB: '44444444-4444-4444-4444-444444444405',
  needArchived: '44444444-4444-4444-4444-444444444406',
  offer: '55555555-5555-5555-5555-555555555501',
};

/**
 * Builds a Postgres client from env overrides or defaults.
 *
 * @returns {import('pg').Client} database client
 */
function createClient() {
  return new Client({
    host: process.env.DATABASE_HOST ?? 'localhost',
    port: Number(process.env.DATABASE_PORT ?? 5432),
    database: process.env.DATABASE_NAME ?? 'needrelay',
    user: process.env.DATABASE_USER ?? 'needrelay',
    password: process.env.DATABASE_PASSWORD ?? 'needrelay',
  });
}

/**
 * Deletes all application rows in FK-safe order.
 *
 * @param {import('pg').Client} client open client
 * @returns {Promise<void>}
 */
async function wipe(client) {
  const tables = [
    'offers',
    'relief_request_comments',
    'needs',
    'relief_requests',
    'organizer_contacts',
    'refresh_tokens',
    'password_reset_tokens',
    'invites',
    'organizers',
  ];
  for (const table of tables) {
    await client.query(`DELETE FROM ${table}`);
  }
}

/**
 * Inserts demo organizers, contacts, requests, needs, and one offer.
 *
 * @param {import('pg').Client} client open client
 * @returns {Promise<void>}
 */
async function seed(client) {
  const now = new Date().toISOString();
  const adminHash = bcrypt.hashSync('Admin123!', 10);
  const orgHash = bcrypt.hashSync('Org1234!', 10);

  await client.query(
    `INSERT INTO organizers (id, email, password_hash, display_name, description, role, active, created_at)
     VALUES ($1,$2,$3,$4,$5,'ADMIN',true,$6)`,
    [
      IDS.admin,
      'admin@needrelay.local',
      adminHash,
      'NeedRelay Admin',
      'Seeded admin account for local development.',
      now,
    ],
  );

  await client.query(
    `INSERT INTO organizers (id, email, password_hash, display_name, description, role, active, created_at)
     VALUES ($1,$2,$3,$4,$5,'ORGANIZER',true,$6)`,
    [
      IDS.org,
      'org@needrelay.local',
      orgHash,
      'Aleppo Relief Org',
      'Community organizers coordinating water and shelter needs.',
      now,
    ],
  );

  await client.query(
    `INSERT INTO organizer_contacts
      (id, organizer_id, name, role, phone, email, note, sort_order, created_at)
     VALUES
      ($1,$2,'Sara Khalil','Coordinator','+963-11-0000001','sara@example.local','Primary contact',0,$3),
      ($4,$2,'Omar Haddad','Logistics','+963-11-0000002','omar@example.local',NULL,1,$3)`,
    [IDS.contact1, IDS.org, now, IDS.contact2],
  );

  await client.query(
    `INSERT INTO relief_requests
      (id, organizer_id, title, description, location_label, latitude, longitude, public_slug, status, created_at, updated_at)
     VALUES
      ($1,$2,'Aleppo North Camp','Urgent water and shelter.','Aleppo North',36.2021,37.1343,'aleppo-north','ACTIVE',$3,$3),
      ($4,$2,'Damascus Hub','Blankets and food staging.','Damascus Center',33.5138,36.2765,'damascus-hub','ACTIVE',$3,$3),
      ($5,$2,'Old Site (archived)','No longer active.','Homs',34.7268,36.7234,'homs-archived','ARCHIVED',$3,$3)`,
    [IDS.activeA, IDS.org, now, IDS.activeB, IDS.archived],
  );

  await client.query(
    `INSERT INTO needs
      (id, relief_request_id, title, description, category, quantity_required, quantity_offered, unit, priority, status, version, created_at, updated_at)
     VALUES
      ($1,$2,'Water tanks',NULL,'WATER',100,0,'L','CRITICAL','OPEN',0,$8,$8),
      ($3,$2,'Tarps',NULL,'SHELTER',50,20,'pcs','HIGH','PARTIALLY_COVERED',0,$8,$8),
      ($4,$2,'Blankets (covered)',NULL,'CLOTHING',30,30,'pcs','NORMAL','COVERED',0,$8,$8),
      ($5,$2,'Old request (closed)',NULL,'OTHER',1,0,'x','LOW','CLOSED',0,$8,$8),
      ($6,$7,'Rice bags',NULL,'FOOD',200,0,'kg','HIGH','OPEN',0,$8,$8),
      ($9,$10,'Should hide from discovery',NULL,'FOOD',5,0,'kg','NORMAL','OPEN',0,$8,$8)`,
    [
      IDS.needOpenA,
      IDS.activeA,
      IDS.needPartialA,
      IDS.needCoveredA,
      IDS.needClosedA,
      IDS.needOpenB,
      IDS.activeB,
      now,
      IDS.needArchived,
      IDS.archived,
    ],
  );

  await client.query(
    `INSERT INTO offers (id, need_id, provider_name, quantity, quantity_received, status, first_name, last_name, phone, email, note, created_at)
     VALUES ($1,$2,'Local Donor',10,NULL,'PENDING','Local','Donor','+963-11-9999999','donor@example.local','Partial water delivery',$3)`,
    [IDS.offer, IDS.needOpenA, now],
  );
}

/**
 * Entry point: wipe then seed, print summary.
 *
 * @returns {Promise<void>}
 */
async function main() {
  const client = createClient();
  await client.connect();
  try {
    await client.query('BEGIN');
    await wipe(client);
    await seed(client);
    await client.query('COMMIT');
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    await client.end();
  }

  console.log('Seed complete (wipe + insert).');
  console.log('Admin:     admin@needrelay.local / Admin123!');
  console.log('Organizer: org@needrelay.local / Org1234!');
  console.log('Public:    /r/aleppo-north  /r/damascus-hub');
  console.log('Archived (hidden from discovery): /r/homs-archived');
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
