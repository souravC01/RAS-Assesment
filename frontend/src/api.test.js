import test from 'node:test';
import assert from 'node:assert/strict';
import { api } from './api.js';

test('interrupted successful write response offers history without repeating the write', async t => {
  let writes=0;
  t.mock.method(globalThis,'fetch',async path=>{
    if(path.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'token'});
    writes++;
    return new Response('{"id":',{status:201,headers:{'content-type':'application/json'}});
  });
  await assert.rejects(api('/submissions',{method:'POST',body:new FormData()}),e=>e.status===0&&e.message.includes('history'));
  assert.equal(writes,1);
});

test('malformed error bodies preserve authentication status', async t => {
  t.mock.method(globalThis,'fetch',async()=>new Response('{',{status:401,headers:{'content-type':'application/json'}}));
  await assert.rejects(api('/sites'),e=>e.status===401&&e.message==='Please sign in to continue.');
});

test('expired session fails without retrying a write', async t => {
  let writes = 0;
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    if (path.endsWith('/csrf')) return Response.json({ headerName: 'X-CSRF-TOKEN', token: 'token' });
    if (options.method === 'POST') writes++;
    return Response.json({ message: 'Please sign in.' }, { status: 401 });
  });
  await assert.rejects(api('/submissions', { method: 'POST', body: new FormData() }), e => e.status === 401);
  assert.equal(writes, 1);
});

test('expired CSRF session is recognized without repeating the write', async t => {
  let writes = 0;
  t.mock.method(globalThis, 'fetch', async (path, options) => {
    if (path.endsWith('/csrf')) return Response.json({ headerName: 'X-CSRF-TOKEN', token: 'token' });
    if (path.endsWith('/auth/me')) return Response.json({}, { status: 401 });
    if (options.method === 'POST') writes++;
    return Response.json({ message: 'Forbidden' }, { status: 403 });
  });
  await assert.rejects(api('/submissions', { method: 'POST', body: new FormData() }), e => e.status === 401);
  assert.equal(writes, 1);
});

test('valid session keeps genuine forbidden response', async t => {
  t.mock.method(globalThis, 'fetch', async path => {
    if (path.endsWith('/csrf')) return Response.json({ headerName: 'X-CSRF-TOKEN', token: 'token' });
    if (path.endsWith('/auth/me')) return Response.json({ id: 1 });
    return Response.json({ message: 'Forbidden' }, { status: 403 });
  });
  await assert.rejects(api('/submissions', { method: 'POST', body: new FormData() }), e => e.status === 403);
});

test('proxy HTML errors become readable service errors', async t => {
  t.mock.method(globalThis, 'fetch', async () => new Response('<h1>Bad gateway</h1>', { status: 502 }));
  await assert.rejects(api('/auth/me'), e => e.status === 502 && e.message.includes('service'));
});

test('failed session diagnosis preserves the original forbidden response', async t => {
  t.mock.method(globalThis,'fetch',async path=>{
    if(path.endsWith('/csrf')) return Response.json({headerName:'X-CSRF-TOKEN',token:'token'});
    if(path.endsWith('/auth/me')) throw new TypeError('fetch failed');
    return Response.json({message:'Request forbidden.'},{status:403});
  });
  await assert.rejects(api('/submissions',{method:'POST',body:new FormData()}),e=>e.status===403&&e.message==='Request forbidden.');
});
