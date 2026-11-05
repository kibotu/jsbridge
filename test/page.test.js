'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const html = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8');

test('the demo page loads nothing from the network', () => {
  assert.deepEqual(html.match(/<script[^>]+src=/g), null);
  assert.deepEqual(html.match(/<link[^>]+href=["']https?:/g), null);
});

test('highlight.js is inlined with a version marker', () => {
  assert.match(html, /<!-- hljs:begin -->[\s\S]*highlight\.js \d+\.\d+\.\d+ \(BSD-3-Clause\)[\s\S]*<!-- hljs:end -->/);
});

test('every code sample uses a language the page ships', () => {
  const modes = [...html.matchAll(/class="code-src" data-mode="([a-z]+)"/g)].map((m) => m[1]);

  assert.ok(modes.length > 0);
  for (const mode of modes) assert.ok(['javascript', 'css'].includes(mode), mode);
});

test('the page has the sections the README promises', () => {
  for (const heading of ['Feature Detection', 'Automated Tests', 'Secure Storage']) {
    assert.ok(html.includes(`<h2>${heading}</h2>`), heading);
  }
});

test('the vendored library is the minified build', () => {
  const vendored = html.slice(html.indexOf('<!-- hljs:begin -->'), html.indexOf('<!-- hljs:end -->'));
  const longest = Math.max(...vendored.split('\n').map((line) => line.length));

  assert.ok(vendored.length > 20000, 'library is inlined');
  assert.ok(longest > 5000, `expected minified lines, longest line was ${longest}`);
});
