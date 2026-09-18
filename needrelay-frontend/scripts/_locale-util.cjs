const fs = require('fs');
const path = require('path');
const outDir = path.join(__dirname, '../src/locales');
const en = JSON.parse(fs.readFileSync(path.join(outDir, 'en.json'), 'utf8'));

function unflat(flat) {
  const root = {};
  for (const [key, value] of Object.entries(flat)) {
    const parts = key.split('.');
    let cur = root;
    for (let i = 0; i < parts.length - 1; i++) {
      cur[parts[i]] ??= {};
      cur = cur[parts[i]];
    }
    cur[parts[parts.length - 1]] = value;
  }
  return root;
}

function writeLocale(code, flat) {
  // Ensure every EN key exists
  function collect(obj, pref = '', out = {}) {
    for (const [k, v] of Object.entries(obj)) {
      const key = pref ? `${pref}.${k}` : k;
      if (v && typeof v === 'object' && !Array.isArray(v)) collect(v, key, out);
      else out[key] = v;
    }
    return out;
  }
  const enFlat = collect(en);
  const merged = { ...enFlat, ...flat };
  const missing = Object.keys(enFlat).filter((k) => !(k in flat));
  if (missing.length) {
    console.warn(code, 'missing', missing.length, 'keys — filled from EN');
  }
  const json = unflat(merged);
  fs.writeFileSync(path.join(outDir, `${code}.json`), JSON.stringify(json, null, 2) + '\n');
  console.log('wrote', code);
}

module.exports = { writeLocale, unflat };
