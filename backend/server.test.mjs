import test from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,rmSync,writeFileSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import net from 'node:net';
import {DailyBudget,createServer,gemini,validateResult} from './server.mjs';

const result={group:'Copper',confidence:'low',observations:['Reddish surface'],checks:['Ask the receiving yard'],caution:'Cannot determine the alloy from a photo.'};
const jpeg=Buffer.from([0xff,0xd8,0xff,0xd9]);
function budget(t,limit=100,clock=()=>Date.now()) {
  const directory=mkdtempSync(join(tmpdir(),'scrappy-budget-'));t.after(()=>rmSync(directory,{recursive:true,force:true}));
  return new DailyBudget(join(directory,'budget.json'),limit,clock);
}
async function service(t,options={}) {
  const server=createServer({analyze:async()=>result,budget:budget(t),...options});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>{server.closeAllConnections();server.close(resolve);}));
  return `http://127.0.0.1:${server.address().port}`;
}
const post=(url,body=jpeg)=>fetch(url+'/v1/identify',{method:'POST',headers:{'Content-Type':'image/jpeg'},body});
test('budget persists across restarts and resets by UTC day',t=> {
  let time=Date.UTC(2026,9,3);const b=budget(t,1,()=>time);
  assert.equal(b.consume(),true);assert.equal(new DailyBudget(b.file,1,()=>time).consume(),false);
  time+=86_400_000;assert.equal(b.consume(),true);
});
test('corrupt budget refuses startup',t=> {
  const b=budget(t);writeFileSync(b.file,'broken');assert.throws(()=>new DailyBudget(b.file,100));
});
test('result rejects unsupported groups and oversized fields',()=> {
  assert.throws(()=>validateResult({...result,group:'Gold'}));
  assert.throws(()=>validateResult({...result,checks:[]}));
  assert.throws(()=>validateResult({...result,caution:'x'.repeat(501)}));
  assert.throws(()=>validateResult({...result,secret:'extra'}));
});
test('JPEG POST returns only validated result and consumes one budget',async t=> {
  const b=budget(t);const response=await post(await service(t,{budget:b}));
  assert.equal(response.status,200);assert.deepEqual(await response.json(),result);assert.equal(b.state.count,1);
  assert.equal(response.headers.get('cache-control'),'no-store');
});
test('invalid JPEG does not use provider budget',async t=> {
  const b=budget(t);const response=await post(await service(t,{budget:b}),Buffer.from('nope'));
  assert.equal(response.status,400);assert.equal(b.state.count,0);
});
test('oversized photo rejected without provider call',async t=> {
  let called=false;const response=await post(await service(t,{analyze:async()=>{called=true;return result;}}),Buffer.alloc(1_048_577));
  assert.equal(response.status,413);assert.equal(called,false);
});
test('wrong content type rejected',async t=> {
  const response=await fetch((await service(t))+'/v1/identify',{method:'POST',headers:{'Content-Type':'text/plain'},body:'nope'});
  assert.equal(response.status,415);
});
test('daily limit enforced for valid requests',async t=> {
  const url=await service(t,{budget:budget(t,1)});assert.equal((await post(url)).status,200);assert.equal((await post(url)).status,429);
});
test('peer limit enforced and does not trust spoofed forwarding',async t=> {
  const url=await service(t,{perMinute:1});assert.equal((await post(url)).status,200);
  const response=await fetch(url+'/v1/identify',{method:'POST',headers:{'Content-Type':'image/jpeg','X-Forwarded-For':'1.2.3.4'},body:jpeg});
  assert.equal(response.status,429);
});
test('provider errors do not expose API keys or image contents',async t=> {
  const response=await post(await service(t,{analyze:async()=>{throw Error('private-api-key');}}));
  assert.equal(response.status,503);assert.equal((await response.text()).includes('private-api-key'),false);
});
test('provider request uses image bytes, constrained schema and server-side key',async()=> {
  let request;
  const value=await gemini(jpeg,{key:'test-only',model:'configured-model',fetchImpl:async(url,options)=>{
    request={url,options};return new Response(JSON.stringify({candidates:[{finishReason:'STOP',content:{parts:[{text:JSON.stringify(result)}]}}]}));
  }});
  assert.deepEqual(value,result);assert.equal(request.options.headers['x-goog-api-key'],'test-only');
  const body=JSON.parse(request.options.body);assert.equal(body.contents[0].parts[0].inlineData.data,jpeg.toString('base64'));
  assert.equal(body.generationConfig.responseJsonSchema.additionalProperties,false);
});
test('blocked or truncated provider response rejected',async()=> {
  await assert.rejects(()=>gemini(jpeg,{key:'test-only',model:'configured-model',fetchImpl:async()=>new Response(JSON.stringify({candidates:[{finishReason:'SAFETY'}]}))}));
  await assert.rejects(()=>gemini(jpeg,{key:'test-only',model:'bad/model'}));
});

test('invalid service limits refuse startup',t=> {
  const b=budget(t);
  for(const options of [{perMinute:0},{perMinute:1.5},{maxConcurrent:0},{maxConcurrent:NaN}]) {
    assert.throws(()=>createServer({analyze:async()=>result,budget:b,...options}),/positive integers/);
  }
  assert.throws(()=>createServer({analyze:true,budget:b}),/configuration/);
  assert.throws(()=>createServer({analyze:async()=>result,budget:{}}),/configuration/);
});

test('busy service rejects excess work without charging budget and recovers after provider failure',async t=> {
  let release,started;let calls=0;
  const waiting=new Promise(resolve=>{started=resolve;});
  const b=budget(t);
  const url=await service(t,{budget:b,maxConcurrent:1,analyze:async()=>{
    calls++;
    if(calls===1) {
      started();await new Promise(resolve=>{release=resolve;});throw Error('provider failed');
    }
    return result;
  }});
  t.after(()=>release?.());
  const first=post(url);
  await waiting;
  const second=await post(url);
  assert.equal(second.status,429);assert.equal(b.state.count,1);assert.equal(calls,1);
  release();assert.equal((await first).status,503);
  assert.equal((await post(url)).status,200);assert.equal(b.state.count,2);assert.equal(calls,2);
});

test('oversized and malformed provider replies are rejected',async()=> {
  for(const body of ['x'.repeat(65_537),'not json',JSON.stringify({candidates:[{finishReason:'STOP',content:{parts:[{text:JSON.stringify({...result,group:'Gold'})}]}}]})]) {
    await assert.rejects(()=>gemini(jpeg,{key:'test-only',model:'configured-model',fetchImpl:async()=>new Response(body)}));
  }
});

test('analysis deadline aborts stalled work and frees the service for a retry',async t=> {
  let calls=0,firstSignal;
  const b=budget(t);
  const url=await service(t,{budget:b,maxConcurrent:1,analysisTimeoutMs:40,analyze:async(_image,{signal})=> {
    calls++;
    if(calls===1) {
      firstSignal=signal;
      return new Promise((_,reject)=>signal.addEventListener('abort',()=>reject(Error('private timeout details')),{once:true}));
    }
    return result;
  }});
  const response=await post(url);
  assert.equal(response.status,503);
  assert.deepEqual(await response.json(),{error:'Analysis unavailable'});
  assert.equal(firstSignal.aborted,true);
  assert.equal((await post(url)).status,200);
  assert.equal(b.state.count,2);
});

test('analysis deadline validation rejects invalid values',t=> {
  for(const analysisTimeoutMs of [0,-1,1.5,NaN,30_001]) {
    assert.throws(()=>createServer({budget:budget(t),analyze:async()=>result,analysisTimeoutMs}),/Analysis timeout/);
  }
});

test('a late provider result cannot replace the timeout response',async t=> {
  let finish;
  const url=await service(t,{analysisTimeoutMs:40,analyze:()=>new Promise(resolve=>{finish=resolve;})});
  const response=await post(url);
  assert.equal(response.status,503);
  finish(result);
  assert.deepEqual(await response.json(),{error:'Analysis unavailable'});
  assert.equal((await fetch(url+'/health')).status,200);
});

test('provider receives the gateway cancellation signal',async()=> {
  const controller=new AbortController();let providerSignal;
  const pending=gemini(jpeg,{key:'test-only',model:'configured-model',signal:controller.signal,fetchImpl:async(_url,{signal})=> {
    providerSignal=signal;
    return new Promise((_,reject)=>signal.addEventListener('abort',()=>reject(Error('aborted')),{once:true}));
  }});
  controller.abort();
  await assert.rejects(pending,/aborted/);
  assert.equal(providerSignal.aborted,true);
});

test('disconnect aborts provider work and releases capacity even if adapter ignores cancellation', {timeout:3000}, async t=> {
  let started,aborted,finish,signal,calls=0;
  const waiting=new Promise(resolve=>{started=resolve;});
  const cancelled=new Promise(resolve=>{aborted=resolve;});
  const b=budget(t);
  const url=await service(t,{budget:b,maxConcurrent:1,analyze:(_image,context)=> {
    calls++;
    if(calls>1)return result;
    signal=context.signal;
    signal.addEventListener('abort',aborted,{once:true});
    started();
    return new Promise(resolve=>{finish=resolve;});
  }});
  const controller=new AbortController();
  const first=fetch(url+'/v1/identify',{method:'POST',headers:{'Content-Type':'image/jpeg'},body:jpeg,signal:controller.signal});
  const rejected=assert.rejects(first,error=>error.name==='AbortError');
  await waiting;controller.abort();await rejected;await cancelled;
  assert.equal(signal.aborted,true);
  // The first adapter is still pending, but its abandoned request must not
  // block another photo for the whole 30-second provider deadline.
  const retry=await post(url);
  assert.equal(retry.status,200);assert.deepEqual(await retry.json(),result);
  assert.equal(b.state.count,2);
  finish(result);
  assert.equal((await fetch(url+'/health')).status,200);
});

test('finishing a normal response does not cancel a successful provider call',async t=> {
  let signal;
  const url=await service(t,{analyze:(_image,context)=>{signal=context.signal;return result;}});
  const response=await post(url);
  assert.equal(response.status,200);assert.deepEqual(await response.json(),result);
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(signal.aborted,false);
});

async function partialUpload(t,url,length=1000) {
  const socket=net.createConnection({host:'127.0.0.1',port:Number(new URL(url).port)});
  t.after(()=>socket.destroy());
  await new Promise((resolve,reject)=>{socket.once('connect',resolve);socket.once('error',reject);});
  let response='';
  socket.on('data',chunk=>{response+=chunk.toString();});
  const closed=new Promise((resolve,reject)=>{
    socket.once('close',()=>resolve(response));
    socket.once('error',error=>{
      // Closing a timed-out upload while the peer is still writing may reset
      // TCP. Accept that only after a complete chunked HTTP reply arrived;
      // a reset with a missing/truncated response must still fail the test.
      if(error.code==='ECONNRESET' && response.endsWith('\r\n0\r\n\r\n'))resolve(response);
      else reject(error);
    });
  });
  socket.write(`POST /v1/identify HTTP/1.1\r\nHost: localhost\r\nContent-Type: image/jpeg\r\nContent-Length: ${length}\r\n\r\n`);
  socket.write(Buffer.from([0xff,0xd8]));
  return {socket,closed};
}

test('stalled upload times out without provider charge and releases capacity', {timeout:3000}, async t=> {
  const b=budget(t);let calls=0;
  const url=await service(t,{budget:b,maxConcurrent:1,uploadTimeoutMs:150,analyze:()=>{calls++;return result;}});
  const {closed}=await partialUpload(t,url);
  const response=await closed;
  assert.match(response,/HTTP\/1\.1 408/);
  assert.match(response,/Photo upload timed out/);
  assert.match(response,/Connection: close/i);
  assert.equal(calls,0);assert.equal(b.state.count,0);
  assert.equal((await post(url)).status,200);
  assert.equal(calls,1);assert.equal(b.state.count,1);
});

test('trickling bytes cannot extend the upload deadline', {timeout:3000}, async t=> {
  const b=budget(t);
  const url=await service(t,{budget:b,maxConcurrent:1,uploadTimeoutMs:150});
  const {socket,closed}=await partialUpload(t,url);
  const timer=setInterval(()=>{if(!socket.destroyed)socket.write(Buffer.from([0]));},15);
  t.after(()=>clearInterval(timer));
  const response=await closed;clearInterval(timer);
  assert.match(response,/HTTP\/1\.1 408/);
  assert.equal(b.state.count,0);
  assert.equal((await post(url)).status,200);
});

test('disconnect during upload releases capacity without spending provider budget', {timeout:3000}, async t=> {
  const b=budget(t);
  const url=await service(t,{budget:b,maxConcurrent:1,uploadTimeoutMs:1000});
  const {socket,closed}=await partialUpload(t,url);
  // Wait until the request has occupied the only analysis slot.
  assert.equal((await post(url)).status,429);
  socket.destroy();await closed;
  let response;
  for(let attempt=0;attempt<20;attempt++) {
    response=await post(url);
    if(response.status!==429)break;
    await new Promise(resolve=>setTimeout(resolve,5));
  }
  assert.equal(response.status,200);assert.equal(b.state.count,1);
});

test('upload deadline validation rejects invalid values',t=> {
  for(const uploadTimeoutMs of [0,-1,1.5,NaN,15_001]) {
    assert.throws(()=>createServer({budget:budget(t),analyze:async()=>result,uploadTimeoutMs}),/Upload timeout/);
  }
});

test('provider HTTP failures cancel unread response bodies before a retry',async()=> {
  for(const status of [400,429,500,503]) {
    let cancelled=0;
    const body=new ReadableStream({
      start(controller) { controller.enqueue(new TextEncoder().encode('private provider error')); },
      cancel() { cancelled++; }
    });
    await assert.rejects(()=>gemini(jpeg,{
      key:'test-only',model:'configured-model',
      fetchImpl:async()=>new Response(body,{status})
    }),/^Error: Provider unavailable$/);
    assert.equal(cancelled,1,`HTTP ${status} must release its unread body`);
    assert.equal(body.locked,false);
  }
});

test('provider body cancellation failure does not expose private error details',async()=> {
  const body=new ReadableStream({cancel() { throw Error('private-provider-key'); }});
  await assert.rejects(()=>gemini(jpeg,{
    key:'test-only',model:'configured-model',
    fetchImpl:async()=>new Response(body,{status:503})
  }),/^Error: Provider unavailable$/);
});

// Refusing an upload must release the socket even when the peer never sends its body.
for (const scenario of [
  {name:'unsupported format',path:'/v1/identify',type:'text/plain',length:1000,status:415},
  {name:'oversized photo',path:'/v1/identify',type:'image/jpeg',length:1_048_577,status:413},
  {name:'unknown route',path:'/missing',type:'image/jpeg',length:1000,status:404},
  {name:'chunked photo',path:'/v1/identify',type:'image/jpeg',chunked:true,status:413},
  {name:'peer quota',path:'/v1/identify',type:'image/jpeg',length:1000,status:429,prime:true}
]) {
  test(`early ${scenario.name} refusal closes incomplete upload`,{timeout:3000},async t=> {
    const b=budget(t);let calls=0;
    const url=await service(t,{budget:b,perMinute:scenario.prime ? 1 : 4,analyze:()=>{calls++;return result;}});
    if(scenario.prime)assert.equal((await post(url)).status,200);
    const socket=net.createConnection({host:'127.0.0.1',port:Number(new URL(url).port)});
    t.after(()=>socket.destroy());
    await new Promise((resolve,reject)=>{socket.once('connect',resolve);socket.once('error',reject);});
    let response='';socket.on('data',chunk=>{response+=chunk.toString();});
    const closed=new Promise((resolve,reject)=>{
      socket.once('close',resolve);socket.once('error',reject);
    });
    const framing=scenario.chunked ? 'Transfer-Encoding: chunked' : `Content-Length: ${scenario.length}`;
    socket.write(`POST ${scenario.path} HTTP/1.1\r\nHost: localhost\r\nContent-Type: ${scenario.type}\r\n${framing}\r\n\r\n`);
    await closed;
    assert.match(response,new RegExp(`HTTP/1.1 ${scenario.status}`));
    assert.match(response,/Connection: close/i);
    assert.match(response,/\r\n0\r\n\r\n$/);
    assert.equal(calls,scenario.prime ? 1 : 0);
    assert.equal(b.state.count,scenario.prime ? 1 : 0);
    assert.equal((await fetch(url+'/health')).status,200);
  });
}
