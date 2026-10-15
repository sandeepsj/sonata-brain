#!/usr/bin/env node
// Runs the Postman collections against a backend on a fresh database, with a clean slate for every case.
//
//   node run.js                       build, start the backend on a new database, run every case,
//                                     and rewrite docs/api/openapi.json from the running backend
//   node run.js Payments              only the Payments collection
//   node run.js "PAY-004"             only cases whose name starts with PAY-004
//   node run.js --no-build            skip the Maven build
//   node run.js --keep-server         leave the backend running afterwards (for the Postman app)
//   node run.js --base-url URL        use a backend you started yourself (no build, no database reset)
//
// Database settings come from PGHOST, PGPORT, PGUSER, PGPASSWORD and TEST_DB (defaults below); the backend's port
// from TEST_PORT (default 18080).
'use strict';
const { execFileSync, spawn } = require('child_process');
const fs = require('fs');
const path = require('path');
const newman = require('newman');

const HERE = __dirname;
const REPO = path.resolve(HERE, '..');
const OUT = path.join(HERE, '.out');
const COLLECTIONS = path.join(HERE, 'collections');
const ENVIRONMENT = path.join(HERE, 'local.postman_environment.json');

const db = {
  host: process.env.PGHOST || 'localhost',
  port: process.env.PGPORT || '5432',
  user: process.env.PGUSER || 'sandeepsj',
  password: process.env.PGPASSWORD || 'sonata123',
  name: process.env.TEST_DB || 'sonata-postman',
};
const PORT = Number(process.env.TEST_PORT || 18080);
// Kept between cases: migration history and access control, so the bootstrap admin survives.
const PRESERVED = ['flyway_schema_history', 'account', 'role', 'role_route', 'user_roles'];

const args = process.argv.slice(2);
const flag = (name) => args.includes(name);
const option = (name) => { const i = args.indexOf(name); return i >= 0 ? args[i + 1] : undefined; };
const externalUrl = option('--base-url');
const filters = args.filter((a, i) => !a.startsWith('--') && args[i - 1] !== '--base-url');

function psql(database, sql) {
  return execFileSync('psql', ['-h', db.host, '-p', db.port, '-U', db.user, '-d', database, '-v', 'ON_ERROR_STOP=1', '-Atc', sql],
    { env: { ...process.env, PGPASSWORD: db.password }, encoding: 'utf8' });
}

async function startBackend() {
  if (!flag('--no-build')) {
    console.log('Building the backend...');
    execFileSync('./mvnw', ['-q', 'clean', 'package', '-DskipTests'], { cwd: REPO, stdio: 'inherit' });
  }
  console.log(`Creating a fresh database ${db.name}...`);
  psql('postgres', `DROP DATABASE IF EXISTS "${db.name}" WITH (FORCE)`);
  psql('postgres', `CREATE DATABASE "${db.name}"`);

  const jar = fs.readdirSync(path.join(REPO, 'target')).find((f) => /^sonata-brain-.*\.jar$/.test(f));
  const log = fs.openSync(path.join(OUT, 'server.log'), 'w');
  const server = spawn('java', ['-jar', path.join('target', jar)], {
    cwd: REPO,
    stdio: ['ignore', log, log],
    env: {
      ...process.env,
      SERVER_PORT: String(PORT),
      DB_URL: `jdbc:postgresql://${db.host}:${db.port}/${db.name}`,
      DB_USERNAME: db.user,
      DB_PASSWORD: db.password,
      SPRING_JPA_SHOW_SQL: 'false',
      LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB: 'INFO',
      SONATA_BOOTSTRAP_ADMIN_EMAIL: 'root@sonata.test',
      SONATA_BOOTSTRAP_ADMIN_PASSWORD: 'root-password-1',
    },
  });
  const url = `http://localhost:${PORT}`;
  const deadline = Date.now() + 120000;
  while (Date.now() < deadline) {
    if (server.exitCode !== null) throw new Error(`Backend exited during startup; see ${path.join(OUT, 'server.log')}`);
    try {
      if ((await fetch(`${url}/heartbeat`)).ok) return { url, server };
    } catch (e) { /* not up yet */ }
    await new Promise((r) => setTimeout(r, 500));
  }
  server.kill();
  throw new Error('Backend did not answer /heartbeat within 120s');
}

function cleanDatabase() {
  const tables = psql(db.name, "SELECT tablename FROM pg_tables WHERE schemaname = 'public'")
    .split('\n').filter((t) => t && !PRESERVED.includes(t));
  if (tables.length) psql(db.name, `TRUNCATE ${tables.map((t) => `"${t}"`).join(', ')} CASCADE`);
}

function runCase(collectionFile, folders, baseUrl) {
  return new Promise((resolve, reject) => {
    newman.run({
      collection: collectionFile,
      environment: ENVIRONMENT,
      envVar: [{ key: 'baseUrl', value: baseUrl }],
      folder: folders,
      reporters: [],
    }, (err, summary) => (err ? reject(err) : resolve(summary)));
  });
}

// REQ-009.AC3: keep the committed copy of the generated API spec in step with the code.
async function exportSpec(baseUrl) {
  const res = await fetch(`${baseUrl}/v3/api-docs`);
  if (!res.ok) throw new Error(`GET /v3/api-docs answered ${res.status}`);
  const spec = await res.json();
  spec.servers = [{ url: 'http://localhost:8080', description: 'local backend' }];
  const file = path.join(REPO, 'docs', 'api', 'openapi.json');
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(spec, null, 2) + '\n');
  console.log(`Wrote ${path.relative(REPO, file)}`);
}

function describeFailure(f) {
  const where = f.source && f.source.name ? f.source.name : '(collection)';
  return `      ${where}\n        ${f.error.test ? f.error.test + ': ' : ''}${f.error.message}`;
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  let backend;
  const baseUrl = externalUrl || (backend = await startBackend()).url;
  if (!externalUrl && !filters.length) await exportSpec(baseUrl);
  const files = fs.readdirSync(COLLECTIONS).filter((f) => f.endsWith('.postman_collection.json')).sort();

  let passed = 0;
  const failed = [];
  const started = Date.now();
  try {
    for (const file of files) {
      const collection = JSON.parse(fs.readFileSync(path.join(COLLECTIONS, file), 'utf8'));
      const title = collection.info.name.replace(/^Sonata API - /, '');
      const fixed = collection.item.filter((i) => i.name.startsWith('00 ')).map((i) => i.name);
      let cases = collection.item.filter((i) => !i.name.startsWith('00 '));
      if (filters.length) {
        cases = filters.some((f) => f.toLowerCase() === title.toLowerCase())
          ? cases
          : cases.filter((c) => filters.some((f) => c.name.startsWith(f)));
      }
      if (!cases.length) continue;
      console.log(`\n${title}`);
      for (const c of cases) {
        if (!externalUrl) cleanDatabase();
        const summary = await runCase(path.join(COLLECTIONS, file), [...fixed, c.name], baseUrl);
        const failures = summary.run.failures;
        if (failures.length === 0) {
          passed++;
          console.log(`  ✓ ${c.name}`);
        } else {
          failed.push(c.name);
          console.log(`  ✗ ${c.name}`);
          failures.slice(0, 5).forEach((f) => console.log(describeFailure(f)));
        }
      }
    }
  } finally {
    if (backend && !flag('--keep-server')) backend.server.kill();
    if (backend && flag('--keep-server')) console.log(`\nBackend left running at ${baseUrl} (pid ${backend.server.pid}).`);
  }
  const secs = ((Date.now() - started) / 1000).toFixed(1);
  console.log(`\n${passed} passed, ${failed.length} failed in ${secs}s`);
  if (failed.length) console.log('Failed:\n' + failed.map((n) => `  ${n}`).join('\n'));
  process.exitCode = failed.length ? 1 : 0;
}

main().catch((e) => { console.error(e.message || e); process.exitCode = 2; });
