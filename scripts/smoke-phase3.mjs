import { readFileSync } from 'fs';
import { dirname, join } from 'path';
import { fileURLToPath } from 'url';

const BASE = process.env.BASE || 'http://localhost:8080';
const D = join(dirname(fileURLToPath(import.meta.url)), 'smoke-assets');
const TS = Date.now().toString().slice(-9);
const CRED = `cred_${TS}`, DEBT = `debt_${TS}`, PW = '123456';

let pass = 0, fail = 0;
const ok = (m) => { pass++; console.log('  [PASS] ' + m); };
const bad = (m) => { fail++; console.log('  [FAIL] ' + m); };

async function login(u, p) {
  const r = await fetch(`${BASE}/api/auth/login`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: u, password: p }),
  });
  const j = await r.json();
  if (!j.data?.accessToken) throw new Error(`login failed ${u}: ${JSON.stringify(j)}`);
  return j.data.accessToken;
}
const H = (tk) => ({ Authorization: 'Bearer ' + tk });
const jget = async (p, tk) => { const r = await fetch(BASE + p, { headers: H(tk) }); return { s: r.status, j: await r.json().catch(() => null) }; };
const jpost = async (p, tk, b) => { const r = await fetch(BASE + p, { method: 'POST', headers: { ...H(tk), 'Content-Type': 'application/json' }, body: JSON.stringify(b) }); return { s: r.status, j: await r.json().catch(() => null) }; };
const jdel = async (p, tk) => { const r = await fetch(BASE + p, { method: 'DELETE', headers: H(tk) }); return { s: r.status, j: await r.json().catch(() => null) }; };
function upload(path, tk, fields, fileField, filename, mime) {
  const fd = new FormData();
  for (const [k, v] of Object.entries(fields)) fd.append(k, v);
  fd.append(fileField, new Blob([readFileSync(join(D, filename))], { type: mime }), filename);
  return fetch(BASE + path, { method: 'POST', headers: H(tk), body: fd }).then(async (r) => ({ s: r.status, j: await r.json().catch(() => null) }));
}
async function fetchBin(url) {
  const r = await fetch(url);
  const buf = Buffer.from(await r.arrayBuffer());
  return { s: r.status, ct: r.headers.get('content-type'), len: buf.length, buf };
}

console.log(`\n=== PHASE 3 SMOKE TEST  creditor=${CRED} debtor=${DEBT} ===\n`);

const admin = await login('admin', PW);
console.log('[1] admin login ok');

let r = await jpost('/api/creditors', admin, { username: CRED, password: PW, name: 'Smoke Creditor' });
r.s === 201 ? ok(`create creditor (${r.s})`) : bad(`create creditor -> ${r.s} ${JSON.stringify(r.j)}`);

const cred = await login(CRED, PW);
console.log('[2] creditor login ok');

r = await jpost('/api/debtors', cred, { username: DEBT, password: PW, name: 'Smoke Debtor' });
r.s === 201 ? ok(`create debtor (${r.s})`) : bad(`create debtor -> ${r.s} ${JSON.stringify(r.j)}`);

const debt = await login(DEBT, PW);
console.log('[3] debtor login ok');

console.log('\n[4] slip upload x4 (rolling window = 3)');
const slipIds = [];
const slipUrls = [];
for (let i = 1; i <= 4; i++) {
  const u = await upload('/api/slips', debt, { creditorUsername: CRED }, 'file', `img${i}.png`, 'image/png');
  if (u.s === 201) { ok(`upload slip ${i} (id=${u.j.data.id})`); slipIds.push(u.j.data.id); slipUrls.push(u.j.data.url); }
  else bad(`upload slip ${i} -> ${u.s} ${JSON.stringify(u.j)}`);
}
let list = await jget(`/api/slips?debtorUsername=${DEBT}&creditorUsername=${CRED}`, cred);
const remaining = list.j?.data || [];
remaining.length === 3 ? ok('rolling window: 3 slips remain (uploaded 4)') : bad(`expected 3 slips, got ${remaining.length}`);
const remIds = remaining.map((x) => x.id).sort((a, b) => a - b);
(!remIds.includes(slipIds[0]) && remIds.includes(slipIds[3]))
  ? ok(`oldest slip id=${slipIds[0]} evicted, newest id=${slipIds[3]} kept`)
  : bad(`eviction wrong: remIds=${remIds} slipIds=${slipIds}`);
await new Promise((res) => setTimeout(res, 1500));
const evicted = await fetchBin(slipUrls[0]);
(evicted.s === 404 || evicted.s === 403) ? ok(`evicted slip file removed from Cloudinary (delivery -> ${evicted.s})`) : bad(`evicted slip file STILL on Cloudinary (delivery -> ${evicted.s})`);

console.log('\n[5] Cloudinary signed URL delivery');
const firstSlip = remaining[0];
let b = await fetchBin(firstSlip.url);
(b.s === 200 && /image/.test(b.ct || '')) ? ok(`slip signed url -> ${b.s} ${b.ct} ${b.len}b`) : bad(`slip url -> ${b.s} ${b.ct}`);
b = await fetchBin(firstSlip.thumbnailUrl);
(b.s === 200) ? ok(`slip thumbnail url -> ${b.s} ${b.ct} ${b.len}b`) : bad(`thumbnail url -> ${b.s}`);

console.log('\n[6] delete slip (as creditor) + audit + Cloudinary file removed');
const toDelete = remaining.find((s) => s.id === remIds[remIds.length - 1]);
const deletedUrl = toDelete.url;
r = await jdel(`/api/slips/${toDelete.id}`, cred);
r.s === 200 ? ok(`delete slip id=${toDelete.id} (${r.s})`) : bad(`delete slip -> ${r.s} ${JSON.stringify(r.j)}`);
list = await jget(`/api/slips?debtorUsername=${DEBT}&creditorUsername=${CRED}`, cred);
(list.j?.data?.length === 2) ? ok('2 slips remain after delete') : bad(`expected 2, got ${list.j?.data?.length}`);
await new Promise((res) => setTimeout(res, 1500));
const gone = await fetchBin(deletedUrl);
(gone.s === 404 || gone.s === 403) ? ok(`deleted slip file removed from Cloudinary (delivery -> ${gone.s})`) : bad(`deleted slip file STILL on Cloudinary (delivery -> ${gone.s}) — authenticated destroy no-op?`);

console.log('\n[7] document upload/list/download/delete');
const du = await upload('/api/documents', cred, { title: 'Smoke Contract', debtorUsername: DEBT }, 'file', 'contract.pdf', 'application/pdf');
let docId;
if (du.s === 201) { ok(`upload document (id=${du.j.data.id})`); docId = du.j.data.id; }
else bad(`upload document -> ${du.s} ${JSON.stringify(du.j)}`);
const dl = await jget('/api/documents', cred);
(dl.j?.data || []).some((d) => d.id === docId) ? ok('document appears in list') : bad('document not in list');
if (docId) {
  const info = await jget(`/api/documents/${docId}/download`, cred);
  const durl = info.j?.data?.url;
  if (durl) { const bb = await fetchBin(durl); (bb.s === 200) ? ok(`document signed url -> ${bb.s} ${bb.ct} ${bb.len}b`) : bad(`document url -> ${bb.s} ${bb.ct}`); }
  else bad('no document url');
}

console.log('\n[8] avatar upload (as debtor)');
const au = await upload('/api/profile/me/avatar', debt, {}, 'file', 'avatar.png', 'image/png');
au.s === 200 ? ok(`upload avatar (${au.s})`) : bad(`upload avatar -> ${au.s} ${JSON.stringify(au.j)}`);
const prof = await jget('/api/profile/me', debt);
const avurl = prof.j?.data?.avatarUrl;
if (avurl) { const bb = await fetchBin(avurl); (bb.s === 200 && /image/.test(bb.ct || '')) ? ok(`avatar url (public) -> ${bb.s} ${bb.ct} ${bb.len}b`) : bad(`avatar url -> ${bb.s} ${bb.ct}`); }
else bad('no avatarUrl in profile');

console.log('\n[9] due report + PDF (Thai font)');
const rep = await jget('/api/reports/due', cred);
rep.s === 200 ? ok(`GET /api/reports/due (${rep.s})`) : bad(`due report -> ${rep.s} ${JSON.stringify(rep.j)}`);
const pr = await fetch(`${BASE}/api/reports/due/pdf`, { headers: H(cred) });
const pbuf = Buffer.from(await pr.arrayBuffer());
const magic = pbuf.slice(0, 5).toString('latin1');
(pr.status === 200 && magic === '%PDF-') ? ok(`due PDF -> ${pr.status} ${pr.headers.get('content-type')} ${pbuf.length}b`) : bad(`due PDF -> ${pr.status} magic=${magic} len=${pbuf.length}`);
const cd = pr.headers.get('content-disposition') || '';
/Share_money_\d{8}\.pdf/.test(cd) ? ok(`PDF filename ok: ${cd}`) : bad(`PDF filename: ${cd}`);

console.log('\n[10] audit log (admin)');
const al = await jget('/api/admin/login-logs?limit=100', admin);
const acts = (al.j?.data || []).map((x) => x.action);
const want = [`UPLOAD_SLIP -> ${CRED}`, `DELETE_SLIP -> ${DEBT}`, 'UPLOAD_DOCUMENT', 'UPLOAD_AVATAR', 'LOGIN'];
for (const w of want) acts.includes(w) ? ok(`audit has "${w}"`) : bad(`audit MISSING "${w}" (sample: ${[...new Set(acts)].slice(0, 8).join(', ')})`);
const slipAct = acts.find((a) => a.startsWith('UPLOAD_SLIP'));
if (slipAct) (slipAct.length <= 80 ? ok(`slip action fits VARCHAR(80): "${slipAct}" (${slipAct.length})`) : bad(`slip action too long: ${slipAct.length}`));

console.log('\n[11] cleanup');
for (const s of (list.j?.data || [])) await jdel(`/api/slips/${s.id}`, cred);
if (docId) await jdel(`/api/documents/${docId}`, cred);
await jdel('/api/profile/me/avatar', debt);
const dd = await jdel(`/api/debtors/${DEBT}`, cred);
dd.s === 200 ? ok('deleted debtor (cascade)') : bad(`delete debtor -> ${dd.s} ${JSON.stringify(dd.j)}`);
console.log(`  (creditor ${CRED} left in DB - no delete-creditor endpoint)`);

console.log(`\n=== RESULT: ${pass} passed, ${fail} failed ===`);
process.exit(fail ? 1 : 0);
