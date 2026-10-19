const fs=require('fs'),http=require('http'),path=require('path'),assert=require('assert');
const {chromium}=require(process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES+'/playwright');
const ROOT=path.resolve(__dirname,'..'),ASSETS=path.join(ROOT,'assets');
(async()=>{
 const server=http.createServer((req,res)=>{const url=new URL(req.url,'http://localhost'),file=url.pathname.startsWith('/fonts/')?path.join(ASSETS,url.pathname):path.join(ASSETS,'studio.html');res.end(fs.readFileSync(file));});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.STUDIO_CHROME,headless:true,args:['--no-sandbox']});
 for(const [width,height,font]of [[320,568,1],[393,852,1],[360,640,2]]){
  const page=await (await browser.newContext({viewport:{width,height},isMobile:true,hasTouch:true})).newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.actions=[];window.StudioNative={action:(a,raw)=>actions.push({action:a,payload:JSON.parse(raw)})};});
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.evaluate(font=>{receive('uiMetrics',{fontScale:font});receive('state',{design:{height:2400,v2:JSON.stringify(DEFAULTS)},shadeChoices:SHADE_DEFAULTS,shadeDirty:false,shadePresets:[],presets:[],connected:true,busy:false,revision:0});},font);
  await page.evaluate(()=>navigate('aod'));
  assert(await page.evaluate(()=>actions.some(a=>a.action==='aodRead')),'Opening AOD reads the music state');assert(!(await page.evaluate(()=>actions.some(a=>a.action==='aodDisplayRead'))),'Display read waits for the helper');
  await page.evaluate(()=>receive('aod',{active:false,enabled:false,styles:[],note:'ok',report:'r'}));await page.waitForFunction(()=>actions.some(a=>a.action==='aodDisplayRead'));
  assert(await page.locator('[data-act="aodDisplayApply"]').isDisabled(),'Apply waits for a readback and a change');
  await page.evaluate(()=>receive('aodDisplay',{on:true,mode:'saving',beginHour:6,beginMin:0,endHour:0,endMin:0,backup:false,note:'Read'}));
  assert.equal(await page.locator('[data-aodd-mode="saving"]').getAttribute('aria-checked'),'true','Current mode is selected');assert(await page.locator('.aodd-times').isHidden(),'Times hidden unless scheduled');
  await page.locator('[data-aodd-mode="scheduled"]').click();assert(await page.locator('.aodd-times').isVisible(),'Schedule shows times');
  for(const b of await page.locator('[data-aodd-mode]').evaluateAll(es=>es.map(e=>e.getBoundingClientRect().height)))assert(b>=48,'Mode targets are touch-sized');
  await page.locator('[data-aodd="begin"]').fill('22:30');await page.locator('[data-aodd="begin"]').dispatchEvent('change');await page.locator('[data-aodd="end"]').fill('07:05');await page.locator('[data-aodd="end"]').dispatchEvent('change');
  await page.locator('[data-act="aodDisplayApply"]').click();const a=await page.evaluate(()=>actions.filter(x=>x.action==='aodDisplayApply').at(-1));
  assert.deepEqual(a.payload,{on:true,mode:'scheduled',beginHour:22,beginMin:30,endHour:7,endMin:5});
  await page.evaluate(()=>receive('aodDisplay',{on:true,mode:'scheduled',beginHour:22,beginMin:30,endHour:7,endMin:5,backup:true,note:'Scheduled'}));assert(await page.locator('[data-act="aodDisplayRestore"]').isEnabled(),'Restore available once a backup exists');
  await page.locator('[data-aodd="on"]').uncheck();assert(await page.locator('.aodd-sub').isHidden());await page.locator('[data-act="aodDisplayApply"]').click();assert.equal((await page.evaluate(()=>actions.filter(x=>x.action==='aodDisplayApply').at(-1))).payload.on,false);
  await page.locator('[data-act="aodDisplayRestore"]').click();assert(await page.evaluate(()=>actions.some(x=>x.action==='aodDisplayRestore')));
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),width+' overflow');assert.deepEqual(errors,[]);console.log('AOD display UI passed '+width+'x'+height+' font '+font);
 }
 await browser.close();server.close();console.log('AOD display read chaining, modes, schedule, off switch, restore and layout passed.');
})().catch(e=>{console.error(e);process.exit(1)});
