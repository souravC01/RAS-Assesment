import test from 'node:test';
import assert from 'node:assert/strict';
import { today } from './checklist.js';
test('today follows Vancouver across the UTC date boundary',()=>{
  assert.equal(today(new Date('2026-10-04T06:30:00Z')),'2026-10-03');
  assert.equal(today(new Date('2026-10-04T08:30:00Z')),'2026-10-04');
});
