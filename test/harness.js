'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(path.join(__dirname, '..', 'bridge.js'), 'utf8');

/**
 * Loads bridge.js into a fresh window, the way native injects it.
 * `posted` collects every message the page sends to the fake iOS handler.
 */
function load({ features = [], name = 'jsbridge', ios = true } = {}) {
  const posted = [];
  const events = [];
  const window = {
    performance: { now: () => 0 },
    console: { log() {}, warn() {}, error() {} },
    CustomEvent: class { constructor(type, init) { this.type = type; this.detail = init && init.detail; } },
    dispatchEvent(e) { events.push(e); },
    setTimeout, clearTimeout, Promise, JSON, Object, Array, Boolean, Error, String, Date, Math,
  };
  window.window = window;
  window.console = window.console;
  if (ios) {
    window.webkit = { messageHandlers: { [name]: { postMessage: (m) => posted.push(JSON.parse(m)) } } };
  }

  const source = SOURCE
    .replace(/__BRIDGE_NAME__/g, name)
    .replace(/__FEATURES__/g, JSON.stringify(features));
  vm.runInNewContext(source, window);

  return { window, bridge: window[name], posted, events };
}

module.exports = { load };
