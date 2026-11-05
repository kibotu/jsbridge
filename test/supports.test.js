'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { load } = require('./harness');

test('supports() is true only for features the host declared', () => {
  const { bridge } = load({ features: ['haptic', 'haptic.pattern'] });

  assert.equal(bridge.supports('haptic'), true);
  assert.equal(bridge.supports('haptic.pattern'), true);
  assert.equal(bridge.supports('keyboard'), false);
});

test('supports() rejects non-strings and prototype names', () => {
  const { bridge } = load({ features: ['haptic'] });

  for (const value of [undefined, null, 1, {}, [], 'toString', 'constructor', '__proto__']) {
    assert.equal(bridge.supports(value), false, String(value));
  }
});

test('features lists what the host declared and cannot be changed', () => {
  const { bridge } = load({ features: ['a', 'b'] });

  assert.deepEqual([...bridge.features], ['a', 'b']);
  assert.throws(() => { 'use strict'; bridge.features.push('c'); }, { name: 'TypeError' });
  assert.equal(bridge.supports('c'), false);
});

test('a host without features supports nothing', () => {
  const { bridge } = load({ features: [] });

  assert.equal(bridge.supports('haptic'), false);
  assert.deepEqual([...bridge.features], []);
});

test('mock handler declares its own features and restores the host list when removed', () => {
  const { bridge } = load({ features: ['haptic'], ios: false });

  bridge.setMockHandler(() => ({}), { features: ['toast'] });
  assert.equal(bridge.supports('toast'), true);
  assert.equal(bridge.supports('haptic'), false);

  bridge.setMockHandler(null);
  assert.equal(bridge.supports('haptic'), true);
  assert.equal(bridge.supports('toast'), false);
});

test('a mock handler without features supports nothing', () => {
  const { bridge } = load({ features: ['haptic'], ios: false });

  bridge.setMockHandler(() => ({}));

  assert.deepEqual([...bridge.features], []);
});

test('schemaVersion is gone and messages carry no version', async () => {
  const { bridge, posted } = load({ features: ['ping'] });

  assert.equal('schemaVersion' in bridge, false);
  bridge.call('ping', { a: 1 }, { timeout: 10 }).catch(() => {});

  assert.equal(posted.length, 1);
  assert.equal('version' in posted[0], false);
  assert.deepEqual(posted[0].data, { action: 'ping', content: { a: 1 } });
});

test('getStats() reports features, not a schema version', () => {
  const { bridge } = load({ features: ['ping'] });
  const stats = bridge.getStats();

  assert.deepEqual([...stats.features], ['ping']);
  assert.equal('schemaVersion' in stats, false);
});

test('bridgeReady event carries platform and features', async () => {
  const { events } = load({ features: ['ping'] });
  await new Promise((r) => setTimeout(r, 5));

  assert.equal(events.length, 1);
  assert.equal(events[0].type, 'bridgeReady');
  assert.equal(events[0].detail.platform, 'ios');
  assert.deepEqual([...events[0].detail.features], ['ping']);
});

test('injecting twice keeps the first bridge', () => {
  const { window, bridge } = load({ features: ['ping'] });
  const src = fs.readFileSync(path.join(__dirname, '..', 'bridge.js'), 'utf8')
    .replace(/__BRIDGE_NAME__/g, 'jsbridge').replace(/__FEATURES__/g, '["other"]');

  vm.runInNewContext(src, window);

  assert.equal(window.jsbridge, bridge);
  assert.equal(window.jsbridge.supports('ping'), true);
});
