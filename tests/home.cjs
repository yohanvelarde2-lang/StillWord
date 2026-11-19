const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const assets = path.resolve(__dirname, '../app/src/main/assets');
const handlers = {};
const main = {};
const modeCalls = [];
const context = vm.createContext({
  document: {
    getElementById: id => id === 'main' ? main : null,
    querySelectorAll: () => [],
    addEventListener: (event, listener) => { handlers[event] = listener; }
  },
  window: {scrollTo() {}, addEventListener() {}},
  Abide: {state: () => '{"goalMinutes":5}', readingMode: mode => modeCalls.push(mode)},
  fetch: () => new Promise(() => {})
});
context.window.Abide = context.Abide;
context.testBible = JSON.parse(fs.readFileSync(path.join(assets, 'bible.json')));
vm.runInContext(fs.readFileSync(path.join(assets, 'app.js'), 'utf8'), context);
vm.runInContext('bible = testBible', context);

const actions = ['reader', 'kindle', 'book-import', 'book-library'];
for (const state of ['{}', '{ready:true}', '{unlocked:true}']) {
  vm.runInContext('state=' + state + '; renderHome()', context);
  const positions = actions.map(action => main.innerHTML.indexOf('data-action="' + action + '"'));
  assert(positions[0] >= 0);
  for (let index = 1; index < positions.length; index++) assert(positions[index] > positions[index - 1]);
}
for (const action of actions.slice(1)) {
  handlers.click({target: {closest: () => ({dataset: {action}})}});
}
assert.deepEqual(modeCalls, actions.slice(1));
console.log('PASS: reading button order in all three home states and the three Android bridge routes');
