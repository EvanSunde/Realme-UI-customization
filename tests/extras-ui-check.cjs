// Stretch handles, per-part styles, detached date, day counters, daily quote, charging preview, Rings and the seconds hand.
const fs=require('fs'),http=require('http'),path=require('path'),assert=require('assert');
const {chromium}=require((process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES?process.env.CODEX_PRIMARY_RUNTIME_NODE_MODULES+'/':'')+'playwright');
const ROOT=path.resolve(__dirname,'..'),ASSETS=path.join(ROOT,'assets');
(async()=>{
 const server=http.createServer((req,res)=>{const url=new URL(req.url,'http://localhost'),file=url.pathname.startsWith('/fonts/')?path.join(ASSETS,url.pathname):path.join(ASSETS,'studio.html');res.setHeader('Content-Type',file.endsWith('.ttf')?'font/ttf':file.endsWith('.json')?'application/json':'text/html');res.end(fs.readFileSync(file));});await new Promise(r=>server.listen(0,'127.0.0.1',r));
 const browser=await chromium.launch({executablePath:process.env.STUDIO_CHROME,headless:true,args:['--no-sandbox']});
 for(const [width,height]of [[393,852],[360,640]]){
  const page=await (await browser.newContext({viewport:{width,height},isMobile:true,hasTouch:true})).newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{window.actions=[];window.StudioNative={action:(a,raw)=>actions.push({action:a,payload:JSON.parse(raw)})};});
  await page.goto('http://127.0.0.1:'+server.address().port);
  await page.evaluate(()=>receive('state',{design:{height:2400,v2:JSON.stringify({...DEFAULTS,bg:'gradient',layout:'stacked',hpos:0,vpos:30,rotate:0,size:240})},shadeChoices:SHADE_DEFAULTS,presets:[],connected:true,busy:false,revision:0,wallColors:['#336699']}));
  const ptr=(type,id,x,y,sel)=>page.evaluate(([type,id,x,y,sel])=>{const el=sel?document.querySelector(sel):document.elementFromPoint(x,y);el.dispatchEvent(new PointerEvent(type,{pointerId:id,clientX:x,clientY:y,bubbles:true,pointerType:'touch',isPrimary:id===1}))},[type,id,x,y,sel]);
  const box=sel=>page.locator(sel).boundingBox(),drag=async(sel,dx,dy)=>{const b=await box(sel);const x=b.x+b.width/2,y=b.y+b.height/2;await ptr('pointerdown',1,x,y);await ptr('pointermove',1,x+dx,y+dy,'#fscreen');await ptr('pointerup',1,x+dx,y+dy,'#fscreen');await page.waitForTimeout(60)};
  const sent=async()=>{await page.waitForTimeout(400);return JSON.parse((await page.evaluate(()=>actions.filter(a=>a.action==='change').at(-1))).payload.design.v2)};

  // Stretch: the side handle changes only the width, the bottom handle only the height.
  await page.locator('[data-act="full"]').first().click();await page.waitForTimeout(80);
  const w0=(await box('#fscreen .clockpos[data-piece="h"] .digits')).width;
  await drag('#fscreen .hdl-x',40,0);let st=await page.evaluate(()=>({sx:S.sx,sy:S.sy,size:S.size}));
  assert(st.sx>110&&st.sy===100&&st.size===240,'Side handle stretches width only '+JSON.stringify(st));
  assert((await box('#fscreen .clockpos[data-piece="h"] .str')).width>w0*1.08,'Preview gets wider');
  await drag('#fscreen .hdl-y',0,50);st=await page.evaluate(()=>({sx:S.sx,sy:S.sy}));assert(st.sy>110,'Bottom handle stretches height '+JSON.stringify(st));
  assert.match(await page.locator('#freadout').textContent(),/W \d+% H \d+%/);
  await page.locator('#fbar [data-act="stretchReset"]').click();assert.deepEqual(await page.evaluate(()=>[S.sx,S.sy]),[100,100],'Reset stretch');
  // On a clock turned 90°, the width axis points down the screen.
  await page.evaluate(()=>{S.rotate=90;changed()});await page.waitForTimeout(80);await drag('#fscreen .hdl-x',0,40);
  st=await page.evaluate(()=>({sx:S.sx,sy:S.sy}));assert(st.sx>110&&st.sy===100,'Stretch follows the rotated axis '+JSON.stringify(st));
  assert.equal((await sent()).sx,st.sx,'Stretch reaches the native design');
  await page.evaluate(()=>{S.rotate=0;S.sx=100;changed()});
  await page.evaluate(()=>{S.layout='analog';changed()});await page.waitForTimeout(60);assert.equal(await page.locator('#fscreen .hdl-x').count(),0,'Dials are not stretched');
  // Detached date in a layout that cannot split.
  await page.evaluate(()=>{S.layout='grid';changed()});await page.locator('[data-act="fullClose"]').click();
  await page.evaluate(()=>navigate('clock'));await page.locator('label.row.tog:has(input[data-key="detachDate"])').click();await page.waitForTimeout(80);
  assert.equal(await page.evaluate(()=>S.detachDate),true);await page.locator('[data-act="full"]').first().click();await page.waitForTimeout(80);
  assert.equal(await page.locator('#fscreen .clockpos[data-piece="d"]').count(),1,'Date is its own piece in Grid');
  let before=await page.evaluate(()=>({h:[S.hpos,S.vpos],d:[S.meta.x,S.meta.y]}));await drag('#fscreen .clockpos[data-piece="d"]',-30,40);
  let after=await page.evaluate(()=>({h:[S.hpos,S.vpos],d:[S.meta.x,S.meta.y],sel:UI.sel}));assert.deepEqual(after.h,before.h);assert.notDeepEqual(after.d,before.d);assert.equal(after.sel,'d');
  assert.equal((await sent()).detachDate,true);
  await page.locator('[data-act="fullClose"]').click();await page.evaluate(()=>{S.layout='stacked';changed()});

  // Per-part styling works without splitting: only the hours change colour and finish.
  await page.evaluate(()=>navigate('style'));await page.locator('[data-act="scope"][data-v="h"]').click();
  await page.locator('[data-set="styleH.color"][data-val="#ff8c7a"]').click();await page.locator('[data-set="styleH.finish"][data-val="neon"]').click();await page.waitForTimeout(60);
  await page.locator('select[data-key="styleM.font"]').count().then(n=>assert.equal(n,0,'Hours scope shows hours controls only'));
  let look=await page.evaluate(()=>{const h=document.querySelector('#screen [data-t="hh"]'),m=document.querySelector('#screen [data-t="mm"]');return{h:h.className,hc:getComputedStyle(h).getPropertyValue('--c').trim(),m:m.className,mc:getComputedStyle(m).getPropertyValue('--c').trim()}});
  assert(look.h.includes('f-neon')&&look.hc==='#ff8c7a','Hours carry their own finish and colour '+JSON.stringify(look));assert(!look.m.includes('f-neon')&&look.mc!=='#ff8c7a','Minutes keep the shared look');
  await page.locator('[data-act="scope"][data-v="m"]').click();await page.locator('select[data-key="styleM.font"]').selectOption('bebas');await page.waitForTimeout(60);
  assert.equal(await page.evaluate(()=>S.styleM.font),'bebas');assert.match(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen [data-t="mm"]')).fontFamily),/Bebas/);
  await page.locator('[data-act="scope"][data-v="d"]').click();await page.locator('[data-set="styleD.color"][data-val="#a8e6cf"]').click();await page.waitForTimeout(60);
  assert.equal(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .meta')).getPropertyValue('--c').trim()),'#a8e6cf','Date colour');
  let doc=await sent();assert.deepEqual([doc.styleH,doc.styleM.font,doc.styleD.color],[{color:'#ff8c7a',finish:'neon'},'bebas','#a8e6cf'],'Per-part styles reach the native design');
  await page.locator('[data-act="scope"][data-v="m"]').click();await page.locator('[data-act="scopeReset"]').click();assert.deepEqual(await page.evaluate(()=>S.styleM),{},'Match the rest again');
  await page.evaluate(()=>{S.styleH={};S.styleD={};UI.scope='all';changed()});

  // Day counters: number in a circle, optional label, at most three, hidden once the date has passed.
  await page.evaluate(()=>navigate('extras'));
  for(let i=0;i<3;i++)await page.locator('[data-act="cntAdd"]').click();assert.equal(await page.locator('[data-act="cntAdd"]').count(),0,'Three counters at most');
  const t=new Date(),iso=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`,add=n=>{const d=new Date(t);d.setDate(d.getDate()+n);return d};
  await page.locator('input[data-key="counters.0.date"]').fill(iso(add(12)));await page.locator('input[data-key="counters.0.label"]').fill('Trip');await page.locator('input[data-key="counters.0.label"]').blur();
  await page.locator('input[data-key="counters.1.date"]').fill(iso(add(-3)));await page.locator('input[data-key="counters.1.date"]').blur();
  const past=add(-3);await page.locator('input[data-key="counters.2.date"]').fill(`2000-${String(past.getMonth()+1).padStart(2,'0')}-${String(past.getDate()).padStart(2,'0')}`);await page.locator('input[data-key="counters.2.date"]').blur();
  await page.locator('label.row.tog:has(input[data-key="counters.2.yearly"])').click();await page.waitForTimeout(80);
  const nums=await page.evaluate(()=>[...document.querySelectorAll('#screen .clockpos[data-piece^="c"]')].map(e=>[e.dataset.piece,e.querySelector('b').textContent,(e.querySelector('small')||{}).textContent||'']));
  const yearly=await page.evaluate(()=>daysTo(S.counters[2],new Date()));assert(yearly>=360&&yearly<=366,'Yearly counter rolls to next year '+yearly);
  assert.deepEqual(nums,[['c0','12','Trip'],['c2',String(yearly),'']],'Counters show days left; a passed one-off date is hidden');
  assert.match(await page.locator('.card:has(input[data-key="counters.1.date"])').textContent(),/passed/);
  await page.locator('[data-act="full"]').first().click();await page.waitForTimeout(80);
  before=await page.evaluate(()=>[S.counters[0].x,S.counters[0].y]);await drag('#fscreen .clockpos[data-piece="c0"]',50,-20);after=await page.evaluate(()=>[S.counters[0].x,S.counters[0].y]);assert.notDeepEqual(after,before,'Counters move');
  assert.match(await page.locator('#freadout').textContent(),/^Counter 1/);
  await page.locator('[data-act="fullClose"]').click();
  doc=await sent();assert(doc.counters.length===3&&doc.counters[0].label==='Trip'&&doc.counters[2].yearly===true,'Counters reach the native design');

  // Daily quote: one line per day from the list, movable.
  await page.locator('label.row.tog:has(input[data-key="quote.on"])').click();await page.waitForTimeout(60);
  await page.locator('textarea[data-key="quote.lines"]').fill('First line\n\nSecond line\nThird line');await page.waitForTimeout(60);
  const q=await page.evaluate(()=>({text:document.querySelector('#screen .clockpos[data-piece="q"] .quote').textContent,want:['First line','Second line','Third line'][dayNo(new Date())%3]}));
  assert.equal(q.text,q.want,'Quote rotates by day and skips blank lines');doc=await sent();assert.deepEqual(doc.quote.lines,['First line','Second line','Third line'],'Native gets the clean list');

  // Charging styles in the preview.
  for(const [c,sel] of [['glow','#screen .clockpos.chg-glow'],['ring','#screen .chg-ring'],['fill','#screen .clockpos.chg-fill'],['bar','#screen .chg-bar']]){
   await page.locator(`[data-set="charge"][data-val="${c}"]`).click();if(!(await page.evaluate(()=>!!UI.charging)))await page.locator('#panel [data-act="chargePreview"]').click();
   await page.waitForTimeout(40);assert.equal(await page.locator(sel).count(),1,'Charging preview '+c);
   if(c==='ring'){const ring=await box('#screen .chg-ring'),clk=await box('#screen .clockpos[data-piece="h"]');assert(ring.width>=Math.max(clk.width,clk.height)*1.1,'Ring wraps the clock');}
  }
  await page.locator('[data-set="charge"][data-val="color"]').click();await page.waitForTimeout(40);
  assert.equal(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .digits')).getPropertyValue('--c').trim()),await page.evaluate(()=>S.chargeColor),'Colour shift uses the charging colour');
  await page.evaluate(()=>{S.layout='rings';changed();renderPanel(true)});assert.match(await page.locator('#panel').textContent(),/no digits to recolour/);
  await page.locator('#panel [data-act="chargePreview"]').click();assert.equal(await page.evaluate(()=>UI.charging),false);
  assert.equal((await sent()).charge,'color');

  // Rings layout and the analog seconds hand.
  assert.equal(await page.locator('#screen .L-rings circle').count(),4,'Two tracks and two rings');
  assert.match(await page.locator('#screen .L-rings text').textContent(),/^\d\d:\d\d$/);
  await page.evaluate(()=>{S.layout='analog';changed();navigate('clock')});await page.locator('label.row.tog:has(input[data-key="secHand"])').click();await page.waitForTimeout(60);assert.equal(await page.evaluate(()=>S.secHand),true,'Seconds hand on');
  const a1=await page.evaluate(()=>document.querySelector('#screen .L-analog g[style*="--c2"] line').getAttribute('transform'));
  await page.waitForFunction(a=>document.querySelector('#screen .L-analog g[style*="--c2"] line').getAttribute('transform')!==a,a1,{timeout:4000});

  // Validation of stored designs.
  const fixed=await page.evaluate(()=>fixState({...DEFAULTS,sx:999,mins:{sx:10},styleH:{finish:'bogus',color:'red',font:'nope',weight:5000},counters:[{on:true,date:'soon',label:'x'.repeat(50)},{},{},{}],quote:{on:true,lines:['a','',7],size:500},charge:'zap',chargeColor:'nope'}));
  assert.deepEqual([fixed.sx,fixed.mins.sx,fixed.styleH,fixed.counters.length,fixed.counters[0].date,fixed.counters[0].label.length,fixed.quote.lines,fixed.quote.size,fixed.charge,fixed.chargeColor],[200,50,{weight:900},3,'',24,['a','7'],90,'none','#7be0b0']);
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),'No horizontal overflow');
  assert.deepEqual(errors,[]);console.log('Extras UI passed '+width+'x'+height);await page.close();
 }
 await browser.close();server.close();console.log('Stretch handles, per-part styles, detached date, counters, quote, charging preview, Rings and seconds hand passed.');
})().catch(e=>{console.error(e);process.exit(1)});
