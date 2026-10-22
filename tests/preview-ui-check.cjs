// Expanded preview gestures, split pieces, slider scroll guard, colour suggestions, new layouts and finishes.
const fs=require('fs'),http=require('http'),path=require('path'),assert=require('assert');
const {chromium}=require((process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES?process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES+'/':'')+'playwright');
const ROOT=path.resolve(__dirname,'..'),ASSETS=path.join(ROOT,'assets');
(async()=>{
 const server=http.createServer((req,res)=>{const url=new URL(req.url,'http://localhost'),file=url.pathname.startsWith('/fonts/')?path.join(ASSETS,url.pathname):path.join(ASSETS,'studio.html');res.setHeader('Content-Type',file.endsWith('.ttf')?'font/ttf':file.endsWith('.json')?'application/json':'text/html');res.end(fs.readFileSync(file));});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.STUDIO_CHROME,headless:true,args:['--no-sandbox']});
 for(const [width,height]of [[393,852],[360,640],[800,400]]){
  const page=await (await browser.newContext({viewport:{width,height},isMobile:true,hasTouch:true})).newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.actions=[];window.StudioNative={action:(a,raw)=>actions.push({action:a,payload:JSON.parse(raw)})};});
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.evaluate(()=>receive('state',{design:{height:2400,v2:JSON.stringify({...DEFAULTS,bg:'gradient',hpos:0,vpos:30,rotate:0})},shadeChoices:SHADE_DEFAULTS,presets:[],connected:true,busy:false,revision:0,wallColors:['#336699','#CC8844','bad']}));
  assert.equal(await page.evaluate(()=>S.caption),'','No personal default caption');
  // Pointer helpers dispatch real PointerEvents with distinct pointer ids.
  const ptr=(type,id,x,y,sel)=>page.evaluate(([type,id,x,y,sel])=>{const el=sel?document.querySelector(sel):document.elementFromPoint(x,y);el.dispatchEvent(new PointerEvent(type,{pointerId:id,clientX:x,clientY:y,bubbles:true,pointerType:'touch',isPrimary:id===1}))},[type,id,x,y,sel]);
  await page.locator('[data-act="full"]').first().click();await page.waitForTimeout(80);
  const box=async sel=>page.locator(sel).boundingBox();
  let screen=await box('#fscreen'),clock=await box('#fscreen .clockpos[data-piece="h"]');
  // Relative drag: grabbing the clock off-centre must not snap its centre to the finger.
  const gx=clock.x+clock.width*.2,gy=clock.y+clock.height*.2;
  await ptr('pointerdown',1,gx,gy);await ptr('pointermove',1,gx+30,gy+40,'#fscreen');await ptr('pointerup',1,gx+30,gy+40,'#fscreen');
  let st=await page.evaluate(()=>({h:S.hpos,v:S.vpos}));
  assert(Math.abs(st.h-Math.round(30/screen.width*100/.6))<=1&&Math.abs(st.v-(30+Math.round(40/screen.height*100/1.1)))<=1,'Drag moves by the finger offset '+JSON.stringify(st));
  // Pinch and twist with two fingers.
  await page.waitForTimeout(80);clock=await box('#fscreen .clockpos[data-piece="h"]');const cx=clock.x+clock.width/2,cy=clock.y+clock.height/2;
  await ptr('pointerdown',1,cx-30,cy);await ptr('pointerdown',2,cx+30,cy,'#fscreen');
  await ptr('pointermove',2,cx+90*Math.cos(Math.PI/6),cy+90*Math.sin(Math.PI/6),'#fscreen');
  await ptr('pointerup',2,0,0,'#fscreen');await ptr('pointerup',1,0,0,'#fscreen');
  st=await page.evaluate(()=>({size:S.size,rot:S.rotate}));assert(st.size>266*1.2,'Pinch enlarges '+JSON.stringify(st));assert(st.rot!==0,'Twist rotates');
  assert.match(await page.locator('#freadout').textContent(),/Size \d+ · -?\d+°/);
  // Handle: one-finger resize to the maximum, clamped.
  await page.waitForTimeout(80);const h=await box('#fscreen .hdl');await ptr('pointerdown',1,h.x+h.width/2,h.y+h.height/2);await ptr('pointermove',1,h.x+2000,h.y+2000,'#fscreen');await ptr('pointerup',1,0,0,'#fscreen');
  assert.equal(await page.evaluate(()=>S.size),600,'Size clamps at 600');
  await page.evaluate(()=>{S.size=266;S.rotate=0;changed()});await page.waitForTimeout(80);
  // Split into three pieces and move only the minutes.
  await page.locator('[data-act="splitToggle"]').click();await page.waitForTimeout(80);assert.equal(await page.locator('#fscreen .clockpos[data-piece]').count(),3,'Hours, minutes and date pieces');
  const before=await page.evaluate(()=>({h:[S.hpos,S.vpos],m:[S.mins.x,S.mins.y],d:[S.meta.x,S.meta.y]}));
  await page.waitForTimeout(80);const m=await box('#fscreen .clockpos[data-piece="m"]');await ptr('pointerdown',1,m.x+m.width/2,m.y+m.height/2);await ptr('pointermove',1,m.x+m.width/2-40,m.y+m.height/2+30,'#fscreen');await ptr('pointerup',1,0,0,'#fscreen');
  const after=await page.evaluate(()=>({h:[S.hpos,S.vpos],m:[S.mins.x,S.mins.y],d:[S.meta.x,S.meta.y],sel:UI.sel}));
  assert.deepEqual(after.h,before.h);assert.deepEqual(after.d,before.d);assert.notDeepEqual(after.m,before.m);assert.equal(after.sel,'m','Touching a piece selects it');
  await page.locator('[data-act="pieceSel"][data-v="d"]').click();assert.match(await page.locator('#freadout').textContent(),/^Date/);
  await page.waitForTimeout(500);const sent=await page.evaluate(()=>JSON.parse(actions.filter(a=>a.action==='change').at(-1).payload.design.v2));
  assert(sent.split===true&&Number.isFinite(sent.mins.x)&&Number.isFinite(sent.meta.y),'Split pieces reach the native design');
  await page.locator('[data-act="splitToggle"]').click();await page.waitForTimeout(80);assert.equal(await page.locator('#fscreen .clockpos[data-piece]').count(),1,'Join pieces');
  await page.evaluate(()=>{S.layout='grid';changed()});await page.waitForTimeout(50);assert.equal(await page.locator('[data-act="splitToggle"]').count(),0,'Grid cannot split');
  await page.evaluate(()=>{S.layout='stacked';changed()});await page.locator('[data-act="fullClose"]').click();
  // Sliders: a vertical swipe never changes the value; a sideways drag does, relative to the start.
  await page.evaluate(()=>navigate('clock'));const r=page.locator('input[data-key="size"]');await r.scrollIntoViewIfNeeded();const rb=await r.boundingBox(),v0=await page.evaluate(()=>S.size);
  await ptr('pointerdown',7,rb.x+rb.width*.9,rb.y+rb.height/2);await ptr('pointermove',7,rb.x+rb.width*.9+3,rb.y+rb.height/2+40,'body');await ptr('pointerup',7,0,0,'body');
  assert.equal(await page.evaluate(()=>S.size),v0,'Vertical swipe over a slider leaves it unchanged');
  await ptr('pointerdown',8,rb.x+rb.width*.9,rb.y+rb.height/2);await ptr('pointerup',8,rb.x+rb.width*.9,rb.y+rb.height/2,'body');assert.equal(await page.evaluate(()=>S.size),v0,'Tapping the track does not jump');
  await ptr('pointerdown',9,rb.x+rb.width*.5,rb.y+rb.height/2);await ptr('pointermove',9,rb.x+rb.width*.5+rb.width*.1,rb.y+rb.height/2+2,'body');await ptr('pointerup',9,0,0,'body');
  const v1=await page.evaluate(()=>S.size);assert(Math.abs(v1-(v0+52))<=2,'Sideways drag moves by the finger offset '+v0+'→'+v1);
  // Colour suggestions for both colours; Material You is gone.
  await page.evaluate(()=>{UI.palette=['#d4a373','#6b705c'];S.twoTone=true;changed();navigate('style')});
  assert.equal(await page.locator('[data-set="color2"][data-val="#d4a373"]').count(),1,'Photo colours for the minutes');assert.equal(await page.locator('[data-set="color2"][data-val="#336699"]').count(),1,'Wallpaper colours for the minutes');
  assert.equal(await page.locator('[data-set="color"][data-val="#cc8844"]').count(),1,'Wallpaper colours normalised');assert.equal(await page.locator('[data-act="materialColor"]').count(),0);
  // Every layout and finish renders inside the screen without errors.
  for(const l of await page.evaluate(()=>LAYOUTS.map(x=>x[0])))for(const f of ['solid','retro','chrome','stencil']){
   await page.evaluate(([l,f])=>{S.layout=l;S.finish=f;changed()},[l,f]);await page.waitForTimeout(20);
   const ok=await page.evaluate(()=>{const c=document.querySelector('#screen .clockpos .digits');return !!c&&c.getBoundingClientRect().width>0});assert(ok,'Renders '+l+'/'+f);
  }
  assert.equal(await page.locator('#screen .L-analog svg').count(),1,'Analog dial');
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),'No horizontal overflow');
  assert.deepEqual(errors,[]);console.log('Preview UI passed '+width+'x'+height);await page.close();
 }
 await browser.close();server.close();console.log('Relative drag, pinch/twist, handle, split pieces, native payload, slider guard, colour suggestions, 16 layouts and finishes passed.');
})().catch(e=>{console.error(e);process.exit(1)});
