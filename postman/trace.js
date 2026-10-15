#!/usr/bin/env node
// Requirement coverage (REQ-003.AC1): every acceptance criterion in docs/requirements should be proved by a case,
// and every case should cite criteria that exist. A case cites criteria in its folder description: "Proves: REQ-020.AC1".
//
//   node trace.js            list uncovered criteria and unknown citations; exit 1 on unknown citations
//   node trace.js --strict   also exit 1 when a testable criterion has no case
'use strict';
const fs = require('fs');
const path = require('path');

const REQ_DIR = path.resolve(__dirname, '..', 'docs', 'requirements');
const COLLECTIONS = path.join(__dirname, 'collections');
const CRITERION = /\*\*(REQ-\d{3}\.AC\d+)\*\* \[([a-z-]+)\]/g;
const UNTESTED = new Set(['process', 'retired', 'manual']); // not checked through the API

const criteria = new Map();
for (const file of fs.readdirSync(REQ_DIR).filter((f) => /^\d.*\.md$/.test(f))) {
  for (const m of fs.readFileSync(path.join(REQ_DIR, file), 'utf8').matchAll(CRITERION)) criteria.set(m[1], m[2]);
}

const cited = new Map(); // criterion -> case names
for (const file of fs.readdirSync(COLLECTIONS).filter((f) => f.endsWith('.postman_collection.json'))) {
  const collection = JSON.parse(fs.readFileSync(path.join(COLLECTIONS, file), 'utf8'));
  for (const item of collection.item) {
    for (const id of (item.description || '').match(/REQ-\d{3}\.AC\d+/g) || []) {
      cited.set(id, [...(cited.get(id) || []), item.name]);
    }
  }
}

const unknown = [...cited.keys()].filter((id) => !criteria.has(id)).sort();
const uncovered = [...criteria].filter(([id, tag]) => !UNTESTED.has(tag) && !cited.has(id)).map(([id, tag]) => `${id} [${tag}]`);

console.log(`${criteria.size} criteria, ${cited.size} cited by cases.`);
if (uncovered.length) console.log(`\nNo case yet (${uncovered.length}):\n  ${uncovered.join('\n  ')}`);
if (unknown.length) console.log(`\nCited but not defined (${unknown.length}):\n  ${unknown.map((id) => `${id} by ${cited.get(id).join(', ')}`).join('\n  ')}`);
process.exitCode = unknown.length || (process.argv.includes('--strict') && uncovered.length) ? 1 : 0;
