// Test-only loopback service: real gateway, fake provider, no key or saved photo.
import assert from 'node:assert/strict';
import {createServer} from '../../backend/server.mjs';
const mode=process.argv[2];
const result={group:'Copper',confidence:'low',observations:['Reddish surface'],
  checks:['Confirm with yard'],caution:'Photo cannot prove alloy.'};
let calls=0;
const server=createServer({budget:{consume:()=>true},maxConcurrent:1,perMinute:100,
  analyze:(image,{signal})=> {
    assert.deepEqual(image,Buffer.from([0xff,0xd8,0xff,0xd9]));
    calls++;
    if(calls===1 && mode==='invalid')return {...result,group:'Gold'};
    if(calls===1 && mode==='stall') {
      console.log('START');
      return new Promise((_,reject)=>signal.addEventListener('abort',()=> {
        console.log('ABORT');reject(Error('test provider aborted'));
      },{once:true}));
    }
    return result;
  }
});
server.listen(0,'127.0.0.1',()=>console.log(server.address().port));
