'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');
const read = (p) => fs.readFileSync(path.join(root, p), 'utf8');

test('bridge.js is identical on Android and iOS', () => {
  const source = read('bridge.js');

  assert.equal(read('android/jsbridge/src/main/res/raw/bridge.js'), source);
  assert.equal(read('ios/JSBridge/Sources/JSBridge/Resources/bridge.js'), source);
});

test('no template placeholder other than the documented ones', () => {
  const placeholders = new Set(read('bridge.js').match(/__[A-Z_]+__/g));

  assert.deepEqual([...placeholders].sort(), ['__BRIDGE_NAME__', '__FEATURES__']);
});
