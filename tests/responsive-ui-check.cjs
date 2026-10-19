const fs=require('fs'),http=require('http'),path=require('path'),assert=require('assert');
const {chromium}=require((process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES?process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES+'/':'')+'playwright');
const ROOT=path.resolve(__dirname,'..'),ASSETS=path.join(ROOT,'assets'),QA=path.join(ROOT,'build','ui-responsive');fs.mkdirSync(QA,{recursive:true});
(async()=>{
 const server=http.createServer((req,res)=>{const p=new URL(req.url,'http://localhost').pathname;let f=p.startsWith('/fonts/')?path.join(ASSETS,p):path.join(ASSETS,'studio.html');res.setHeader('Content-Type',f.endsWith('.ttf')?'font/ttf':f.endsWith('.json')?'application/json':'text/html');res.end(fs.readFileSync(f));});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.STUDIO_CHROME,headless:true,args:['--no-sandbox']});
 const matrix=[{w:320,h:568,d:1,f:1},{w:360,h:640,d:2,f:1},{w:393,h:800,d:2.75,f:1},{w:412,h:892,d:3.5,f:1.3},{w:360,h:640,d:3,f:2},{w:600,h:960,d:2,f:1.3},{w:800,h:360,d:2,f:1},{w:1024,h:768,d:1.5,f:1},{w:1280,h:800,d:1,f:2}];
 for(const c of matrix){
  const context=await browser.newContext({viewport:{width:c.w,height:c.h},deviceScaleFactor:c.d,isMobile:true,hasTouch:true});const page=await context.newPage();page.setDefaultTimeout(6000);const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.actions=[];window.StudioNative={action:(action,raw)=>window.actions.push({action,payload:JSON.parse(raw)})};});
  await page.goto('http://127.0.0.1:'+server.address().port);await page.evaluate(f=>{receive('uiMetrics',{fontScale:f});receive('state',{design:{height:2400,v2:JSON.stringify({...DEFAULTS,bg:'gradient',gradient:'dusk'})},presets:[],photoReady:false,subjectReady:false,revision:1,connected:true,busy:false});},c.f);
  await page.waitForTimeout(100);const name=`${c.w}x${c.h}-density${c.d}-font${c.f}`;
  if(c.w===393)await page.screenshot({path:path.join(QA,'phone-preview.png')});
  for(const tab of ['clock','depth','style','scenes','setup']){
   await page.locator(`#tabs [data-tab="${tab}"]`).click();
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),name+' document overflow '+tab);
   assert(await page.locator('#panel').evaluate(el=>el.scrollWidth<=el.clientWidth+1),name+' panel overflow '+tab);
   const nav=await page.locator('#tabs').boundingBox(),w=await page.locator('#workspace').boundingBox(),foot=await page.locator('.foot').boundingBox();assert(nav.height<=100,name+' oversized navigation');assert(foot.height<160,name+' oversized footer');assert(nav.y>=w.y-1&&nav.y+nav.height<=foot.y+1,name+' navigation inaccessible '+tab);
   for(const box of await page.locator('#tabs button').evaluateAll(es=>es.map(e=>{const r=e.getBoundingClientRect();return{x:r.x,y:r.y,w:r.width,h:r.height};})))assert(box.w>=48&&box.h>=48&&box.x>=0&&box.x+box.w<=c.w+1,name+' small/clipped tab '+tab+' '+JSON.stringify(box));
  }
  await page.locator('[data-tab="aod"]').click();await page.evaluate(()=>receive('aod',{active:false,enabled:false,until:0,style:'vertical',smart:false,styles:[{id:'vertical',name:'Vertical digital',tested:true},{id:'horizontal',name:'Horizontal digital',tested:false}],note:'Ready to enable',report:'Readback report'}));
  assert(await page.locator('#panel').evaluate(el=>el.scrollWidth<=el.clientWidth+1),name+' native AOD panel overflow');assert(await page.locator('#apply').isHidden(),name+' misleading AOD apply button');
  for(const el of await page.locator('#panel button').elementHandles()){const r=await el.boundingBox();if(r)assert(r.height>=44,name+' native AOD touch target too short');}
  if(c.w===393)await page.screenshot({path:path.join(QA,'native-aod-controls.png')});
  await page.locator('#tabs [data-tab="clock"]').click();
  if(c.w===393||c.w===1024||c.f===2)await page.screenshot({path:path.join(QA,name+'-editor.png')});
  const tapTab=async(tab)=>{const b=await page.locator('#tabs [data-tab="'+tab+'"]').boundingBox();await page.touchscreen.tap(b.x+b.width/2,b.y+b.height/2);};
  await page.locator('[data-disclosure="type"] > summary').click();await page.locator('[data-key="slant"]').scrollIntoViewIfNeeded();await page.waitForTimeout(80);const stored=await page.locator('#workspace').evaluate(e=>e.scrollTop);await tapTab('style');await tapTab('clock');assert(await page.locator('[data-disclosure="type"]').evaluate(e=>e.open),name+' disclosure lost');assert(Math.abs(await page.locator('#workspace').evaluate(e=>e.scrollTop)-stored)<3,name+' section scroll lost '+JSON.stringify({stored,actual:await page.locator('#workspace').evaluate(e=>e.scrollTop),debug:await page.evaluate(()=>({paneScroll,editor:document.querySelector('.editor-pane').offsetTop,workspace:document.querySelector('#workspace').offsetTop}))}));
  // Scroll position and stage geometry must not feed back into each other.
  await page.locator('[data-key="size"]').scrollIntoViewIfNeeded();const result=await page.evaluate(async()=>{const workspace=document.querySelector('#workspace'),screen=document.querySelector('#screen'),clock=screen.querySelector('.clockpos'),bg=screen.querySelector('.bg'),stage=document.querySelector('.stage'),before=workspace.scrollTop,height=stage.offsetHeight,input=document.querySelector('[data-key="size"]');let renders=0;const original=renderPreview;renderPreview=function(r){renders++;return original(r);};for(let i=0;i<60;i++){input.value=160+i;input.dispatchEvent(new Event('input',{bubbles:true}));}await new Promise(requestAnimationFrame);renderPreview=original;return{delta:workspace.scrollTop-before,sameClock:clock===screen.querySelector('.clockpos'),sameBg:bg===screen.querySelector('.bg'),height:height===stage.offsetHeight,renders};});
  assert(Math.abs(result.delta)<2&&result.height,name+' scroll resize/jump');assert(result.sameBg&&result.sameClock&&result.renders<=2,name+' unnecessary preview rebuild');
  await page.locator('[data-act="fontPicker"]').click();await page.locator('#fontSearch').fill('orbit');assert.equal(await page.locator('[data-pick-font]').count(),1);await page.locator('[data-pick-font="orbitron"]').click();assert.equal(await page.evaluate(()=>S.font),'orbitron');
  // Caption entry must survive keyboard-sized viewports and remain focused while rendering.
  await page.locator('[data-key="caption"]').fill('Focused caption');await page.setViewportSize({width:c.w,height:Math.max(320,Math.round(c.h*.6))});await page.locator('[data-key="caption"]').fill('Still focused');assert(await page.locator('[data-key="caption"]').evaluate(e=>document.activeElement===e));assert.equal(await page.locator('[data-key="caption"]').inputValue(),'Still focused');assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),name+' keyboard overflow');
  await page.locator('[data-key="caption"]').blur();await page.setViewportSize({width:c.w,height:c.h});
  assert.deepEqual(errors,[],name+' JS errors');await context.close();console.log('Passed '+name);
 }
 await browser.close();server.close();console.log('Responsive checks passed: 9 viewport/density/font-scale cases, five visible destinations, scroll stability, retained preview nodes, coalesced frames, font search and keyboard entry.');
})().catch(e=>{console.error(e);process.exit(1)});
