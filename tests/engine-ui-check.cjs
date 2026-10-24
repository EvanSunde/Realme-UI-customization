// Lock-screen shortcuts, alert badges, light-mode colours, water ripple and drag parallax.
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
  await page.evaluate(()=>receive('state',{design:{height:2400,v2:JSON.stringify({...DEFAULTS,bg:'gradient',layout:'stacked',vpos:20})},shadeChoices:SHADE_DEFAULTS,presets:[],connected:true,busy:false,revision:0,wallColors:[]}));
  const sent=async()=>{await page.waitForTimeout(400);return JSON.parse((await page.evaluate(()=>actions.filter(a=>a.action==='change').at(-1))).payload.design.v2)};
  const tog=async k=>{await page.locator(`label.row.tog:has(input[data-key="${k}"])`).click();await page.waitForTimeout(60)};

  // Shortcuts: camera and ringer by default; apps come from the phone's launcher list.
  await page.evaluate(()=>navigate('extras'));await tog('shortcuts.on');
  assert.equal(await page.locator('#screen .clockpos[data-piece="sc"] .sci').count(),2,'Camera and ringer shortcuts');
  await page.locator('[data-act="scAdd"]').click();await page.waitForTimeout(60);
  assert(await page.evaluate(()=>actions.some(a=>a.action==='listApps')),'Asks the phone for its apps');
  const icon=await page.evaluate(()=>{const c=document.createElement('canvas');c.width=c.height=32;const x=c.getContext('2d');x.fillStyle='#34a853';x.fillRect(4,4,24,24);return c.toDataURL()});
  await page.evaluate(icon=>receive('apps',{apps:[{label:'Maps',pkg:'com.google.android.apps.maps',cls:'com.google.android.maps.MapsActivity',icon},{label:'Evil',pkg:'',cls:'x',icon:'javascript:alert(1)'},{label:'Notes',pkg:'com.example.notes',cls:'com.example.notes.Main',icon:'http://bad/icon.png'}]}),icon);
  assert.equal(await page.evaluate(()=>UI.apps.length),2,'Entries without a package are dropped');assert.equal(await page.evaluate(()=>UI.apps[1].icon),'','Only PNG data icons are kept');
  await page.locator('select[data-key="shortcuts.items.2.app"]').selectOption('com.google.android.apps.maps/com.google.android.maps.MapsActivity');await page.waitForTimeout(60);
  assert.equal(await page.locator('#screen .clockpos[data-piece="sc"] .sci img').count(),1,'App icon in the preview');
  await tog('shortcuts.tint');assert.equal(await page.locator('#screen .sci .tinted').count(),1,'Tinted app icon');
  await page.locator('[data-act="scAdd"]').click();await page.waitForTimeout(60);assert.equal(await page.locator('[data-act="scAdd"]').count(),0,'Four shortcuts at most');
  await page.locator('select[data-key="shortcuts.items.3.type"]').selectOption('wifi');await page.waitForTimeout(60);
  assert.equal(await page.locator('#screen .sci').count(),4);assert.equal(await page.locator('#screen .sci.on').count(),1,'Wi-Fi shows its on state');
  let doc=await sent();assert.deepEqual(doc.shortcuts.items.map(i=>i.type),['camera','ring','app','wifi']);assert.equal(doc.shortcuts.items[2].pkg,'com.google.android.apps.maps');
  await page.locator('[data-act="scDel"][data-v="0"]').click();await page.waitForTimeout(60);assert.deepEqual(await page.evaluate(()=>S.shortcuts.items.map(i=>i.type)),['ring','app','wifi']);
  // Shortcuts move and resize but stay upright.
  await page.locator('[data-act="full"]').first().click();await page.waitForTimeout(80);
  const b=await page.locator('#fscreen .clockpos[data-piece="sc"]').boundingBox();
  await page.evaluate(([x,y])=>{const t=document.querySelector('#fscreen .clockpos[data-piece="sc"]');for(const [type,px,py] of [['pointerdown',x,y],['pointermove',x+40,y-30],['pointerup',x+40,y-30]])(type==='pointerdown'?t:document.querySelector('#fscreen')).dispatchEvent(new PointerEvent(type,{pointerId:1,clientX:px,clientY:py,bubbles:true,pointerType:'touch',isPrimary:true}))},[b.x+b.width/2,b.y+b.height/2]);
  assert.notDeepEqual(await page.evaluate(()=>[S.shortcuts.x,S.shortcuts.y]),[0,70],'Shortcuts drag');assert.match(await page.locator('#freadout').textContent(),/^Shortcuts/);
  await page.locator('[data-act="fullClose"]').click();

  // Alerts: sample badges, each optional.
  await tog('alerts.on');assert.equal(await page.locator('#screen .clockpos[data-piece="al"] .alp').count(),2,'Missed calls and messages');
  await tog('alerts.sms');assert.equal(await page.locator('#screen .alp').count(),1);
  doc=await sent();assert.deepEqual([doc.alerts.on,doc.alerts.calls,doc.alerts.sms],[true,true,false]);

  // Experimental music widget: a movable card; the phone fills it through the bridge app.
  await tog('music.on');assert.equal(await page.locator('#screen .clockpos[data-piece="mu"] .mucard').count(),1,'Music widget card');
  assert.match(await page.locator('#panel').textContent(),/Studio Music Bridge/);doc=await sent();assert.deepEqual(doc.music,{on:true,x:0,y:58,size:110});
  assert.deepEqual(await page.evaluate(()=>fixState({...DEFAULTS,music:{on:1,size:9999,x:-500}}).music),{on:true,x:-80,y:58,size:240});
  await tog('music.on');
  // Light mode colours, previewed from the Style tab or the preview tools.
  await page.evaluate(()=>navigate('style'));await tog('lightMode.on');
  await page.locator('[data-set="lightMode.color"][data-val="#9ec5ff"]').click();await page.locator('#panel [data-act="lightPreview"]').click();await page.waitForTimeout(60);
  assert.equal(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .digits')).getPropertyValue('--c').trim()),'#9ec5ff','Light-mode colour in the preview');
  assert.equal(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .scrow')).getPropertyValue('--c').trim()),'#9ec5ff','Shortcuts follow it');
  await page.locator('#panel [data-act="lightPreview"]').click();await page.waitForTimeout(60);
  assert.notEqual(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .digits')).getPropertyValue('--c').trim()),'#9ec5ff');
  assert.deepEqual((await sent()).lightMode,{on:true,color:'#9ec5ff',color2:'#4b4b60'});

  // Water ripple on photo or gradient backgrounds.
  await page.evaluate(()=>navigate('wallpaper'));await tog('water.on');assert(await page.evaluate(()=>document.querySelector('#screen').classList.contains('water')));
  const sb=await page.locator('#screen').boundingBox();
  await page.evaluate(([x,y])=>document.querySelector('#screen .bg').dispatchEvent(new PointerEvent('pointerdown',{pointerId:5,clientX:x,clientY:y,bubbles:true,pointerType:'touch'})),[sb.x+sb.width*.2,sb.y+sb.height*.85]);
  assert.equal(await page.locator('#screen .ripple').count(),1,'Touch makes a ripple');
  await page.evaluate(()=>{S.bg='atmos';changed();renderPanel(true)});await page.waitForTimeout(60);assert.equal(await page.locator('input[data-key="water.on"]').count(),0,'No ripple over a live wallpaper');
  await page.evaluate(()=>{S.bg='gradient';changed()});assert.equal((await sent()).water.on,true);

  // Parallax can follow the finger instead of (or as well as) tilt.
  await page.evaluate(()=>navigate('motion'));await page.locator('[data-set="movement"][data-val="parallax"]').click();await page.waitForTimeout(60);
  await page.locator('[data-set="parallaxSrc"][data-val="drag"]').click();assert.equal((await sent()).parallaxSrc,'drag');

  const fixed=await page.evaluate(()=>fixState({...DEFAULTS,parallaxSrc:'mind',water:{on:1,strength:99},lightMode:{on:1,color:'blue'},shortcuts:{on:1,size:5,items:[{type:'app',pkg:'a b',cls:'c'},{type:'nuke'},{type:'app',pkg:'com.a',cls:'com.a.Main',label:'A'},{type:'camera'},{type:'ring'},{type:'wifi'}]},alerts:{on:1,size:9999,sms:false}}));
  assert.deepEqual([fixed.parallaxSrc,fixed.water,fixed.lightMode.color,fixed.shortcuts.size,fixed.shortcuts.items,fixed.alerts.size,fixed.alerts.sms,fixed.alerts.calls],
   ['gyro',{on:true,strength:5},'#1d1d24',60,[{type:'app'},{type:'app',pkg:'com.a',cls:'com.a.Main',label:'A'},{type:'camera'},{type:'ring'}],140,false,true]);
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),'No horizontal overflow');
  assert.deepEqual(errors,[]);console.log('Engine UI passed '+width+'x'+height);await page.close();
 }
 await browser.close();server.close();console.log('Shortcuts, alert badges, light-mode colours, water ripple and drag parallax passed.');
})().catch(e=>{console.error(e);process.exit(1)});
