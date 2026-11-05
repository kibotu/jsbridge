#!/usr/bin/env node
// Inlines highlight.js (core + javascript + css grammars + light/dark themes) into index.html.
//
//   node scripts/update-highlightjs.mjs [version]     # default: latest
//
// The demo page is the only file the sample apps symlink, so the library is inlined instead of loaded
// from a CDN. That keeps the page working offline and behind proxies, and updating is one command.

import { execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const page = join(root, 'index.html');
const version = process.argv[2] ?? 'latest';
const languages = ['javascript', 'css'];

const work = mkdtempSync(join(tmpdir(), 'hljs-'));
execFileSync('npm', ['pack', `@highlightjs/cdn-assets@${version}`, '--silent'], { cwd: work, stdio: 'pipe' });
execFileSync('sh', ['-c', 'tar -xzf *.tgz'], { cwd: work });
const pkg = join(work, 'package');
const read = (file) => readFileSync(join(pkg, file), 'utf8');
const resolved = JSON.parse(read('package.json')).version;

// The ES build ends with `export{x as default};`. Turn it into a classic script that sets window.hljs.
const core = read('es/core.min.js');
const exported = core.match(/export\{(\w+) as default\};?\s*$/);
if (!exported) throw new Error('Unexpected core.min.js format: no default export found');
const coreScript = `(function(){${core.slice(0, exported.index)}window.hljs=${exported[1]};})();`;

const grammars = languages.map((name) => read(`languages/${name}.min.js`)).join('\n');

// Scope each theme to the page's data-theme attribute so the existing toggle switches it.
const scope = (css, theme) => css
  .replace(/\/\*[\s\S]*?\*\//g, '')
  .replace(/([^{}]+)\{([^{}]*)\}/g, (_, selectors, body) =>
    selectors.split(',').map((s) => `[data-theme="${theme}"] ${s.trim()}`).join(',') + `{${body}}`);

const block = [
  '<!-- hljs:begin -->',
  `    <style>/* highlight.js ${resolved} (BSD-3-Clause), GitHub themes */`,
  scope(read('styles/github.min.css'), 'light'),
  scope(read('styles/github-dark.min.css'), 'dark'),
  '    </style>',
  `    <script>/* highlight.js ${resolved} (BSD-3-Clause) */`,
  coreScript,
  grammars,
  '    </script>',
  '    <!-- hljs:end -->',
].join('\n');

const html = readFileSync(page, 'utf8');
const updated = html.replace(/<!-- hljs:begin -->[\s\S]*?<!-- hljs:end -->/, () => block);
if (updated === html && !html.includes(block)) throw new Error('hljs markers not found in index.html');
writeFileSync(page, updated);
console.log(`index.html now inlines highlight.js ${resolved} (${languages.join(', ')})`);
