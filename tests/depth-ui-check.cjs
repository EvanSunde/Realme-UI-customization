// Digit depth around the subject, subject shadow and rim light, photo-filled digits, weight through the hour and parallax.
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
  await page.evaluate(()=>receive('state',{design:{height:2400,v2:JSON.stringify({...DEFAULTS,bg:'photo',layout:'stacked',hpos:0,vpos:30,size:300})},shadeChoices:SHADE_DEFAULTS,presets:[],connected:true,busy:false,revision:0,wallColors:[]}));
  // A striped photo and a round subject cut-out, as the app would supply them.
  await page.evaluate(()=>{const c=document.createElement('canvas');c.width=540;c.height=1200;const x=c.getContext('2d');for(let i=0;i<12;i++){x.fillStyle=i%2?'#2a6f97':'#f4a261';x.fillRect(0,i*100,540,100)}assets.photo=c.toDataURL();
   const m=document.createElement('canvas');m.width=540;m.height=1200;const y=m.getContext('2d');y.fillStyle='#e9c46a';y.beginPath();y.arc(270,640,200,0,7);y.fill();assets.subject=m.toDataURL();photoReady=true;subjectReady=true;changed()});
  const sent=async()=>{await page.waitForTimeout(400);return JSON.parse((await page.evaluate(()=>actions.filter(a=>a.action==='change').at(-1))).payload.design.v2)};
  const vis=sel=>page.evaluate(sel=>[...document.querySelectorAll(sel)].map(e=>getComputedStyle(e).visibility),sel);

  // Digit depth: the chosen digit is hidden behind the subject and drawn in a copy above it.
  await page.evaluate(()=>navigate('depth'));const open=async()=>{const sum=page.locator('summary:has-text("Subject depth effect")');if(await sum.count()&&!(await sum.evaluate(e=>e.parentElement.open)))await sum.click()};await open();await page.locator('[data-act="depthDigit"][data-v="1"]').click();await page.waitForTimeout(60);
  assert.deepEqual(await page.evaluate(()=>S.depthFront),[false,true,false,false]);
  assert.deepEqual(await vis('#screen>.clockpos .dg.fr'),['hidden'],'Front digit hidden behind the subject');
  assert.deepEqual(await vis('#screen .layer-front .dg'),['hidden','visible','hidden','hidden'],'Only the chosen digit shows in front');
  const order=await page.evaluate(()=>[...document.querySelector('#screen').children].map(e=>e.classList.contains('layer-front')?'front':e.classList.contains('subject')?'subject':e.classList.contains('clockpos')?'clock':'').filter(Boolean));
  assert.deepEqual(order,['clock','subject','front'],'Clock, subject, then front digits');
  assert.equal(await page.evaluate(()=>{const f=document.querySelector('#screen .layer-front');return getComputedStyle(f).pointerEvents}),'none','Front copy never takes touches');
  await page.evaluate(()=>{S.layout='grid';changed()});await page.waitForTimeout(60);assert.deepEqual(await vis('#screen .layer-front .dg.fr'),['visible'],'Grid digits work too');
  await page.evaluate(()=>{S.layout='analog';changed();renderPanel(true)});await page.waitForTimeout(60);
  assert.equal(await page.locator('#screen .layer-front').count(),0);assert.match(await page.locator('#panel').textContent(),/can’t sit on both sides/);
  await page.evaluate(()=>{S.layout='stacked';changed();renderPanel(true)});
  // Rim light and shadow.
  await page.evaluate(()=>{S.rimLight=60;S.subjShadow=50;changed()});await page.waitForTimeout(60);
  const layers=await page.evaluate(()=>[...document.querySelectorAll('#screen .subject')].map(e=>e.className));assert.deepEqual(layers,['subject sshadow','subject','subject rim'],'Shadow under, rim over the subject');
  assert.equal(await page.evaluate(()=>getComputedStyle(document.querySelector('#screen .subject.rim')).backgroundColor),'rgb(255, 211, 165)','Rim uses the clock colour');
  let doc=await sent();assert.deepEqual([doc.depthFront,doc.rimLight,doc.subjShadow],[[false,true,false,false],60,50]);

  // Photo fill: the photo shows through the digits, aligned to the screen; no still background falls back to solid.
  await page.evaluate(()=>{S.finish='photo';S.depthFront=[false,false,false,false];changed()});await page.waitForTimeout(80);
  const fill=await page.evaluate(()=>{const d=document.querySelector('#screen .digits');const c=getComputedStyle(d);return{cls:d.className,img:c.backgroundImage.slice(0,15),size:d.style.backgroundSize,clip:c.backgroundClip||c.webkitBackgroundClip}});
  assert(fill.cls.includes('f-photo')&&fill.img.startsWith('url(')&&/px/.test(fill.size)&&fill.clip==='text','Photo fill '+JSON.stringify(fill));
  await page.evaluate(()=>{S.bg='atmos';changed()});await page.waitForTimeout(60);assert.match(await page.evaluate(()=>document.querySelector('#screen .digits').className),/f-solid/,'Live wallpaper falls back to solid');
  await page.evaluate(()=>{S.bg='photo';S.finish='glow';changed()});

  // Weight follows the hour in six steps.
  await page.evaluate(()=>navigate('clock'));await page.locator('label.row.tog:has(input[data-key="weightFlow.on"])').click();await page.waitForTimeout(60);
  const fw=await page.evaluate(()=>[getComputedStyle(document.querySelector('#screen .digits')).getPropertyValue('--fw').trim(),flowW(S,new Date())]);assert.equal(+fw[0],fw[1],'Weight for this part of the hour');
  assert.equal(await page.evaluate(()=>[0,9,10,35,50,59].map(m=>flowW(S,new Date(2026,0,1,10,m))).join()),'200,200,340,620,900,900');
  assert.equal((await sent()).weightFlow.on,true);

  // Parallax: the clock follows the tilt, the photo and subject drift the other way.
  await page.evaluate(()=>navigate('motion'));await page.locator('[data-set="movement"][data-val="parallax"]').click();await page.waitForTimeout(60);
  assert(await page.evaluate(()=>document.querySelector('#screen').classList.contains('parallax')&&!!document.querySelector('#screen .anim.tilt')),'Parallax planes');
  await page.evaluate(()=>{const r=document.querySelector('#screen');r.style.setProperty('--tx',2);r.style.setProperty('--ty',-1)});
  const tr=await page.evaluate(()=>[getComputedStyle(document.querySelector('#screen .bg')).translate,getComputedStyle(document.querySelector('#screen .subject')).translate]);
  assert(tr[0]!=='none'&&tr[0]===tr[1]&&tr[0].startsWith('-'),'Photo and subject move together, against the tilt '+tr);
  assert.equal((await sent()).movement,'parallax');

  const fixed=await page.evaluate(()=>fixState({...DEFAULTS,depthFront:'yes',weightFlow:{on:1,from:-5,to:5000},rimLight:300,subjShadow:-1,movement:'spin'}));
  assert.deepEqual([fixed.depthFront,fixed.weightFlow,fixed.rimLight,fixed.subjShadow,fixed.movement],[[false,false,false,false],{on:true,from:100,to:900},100,0,'still']);
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=document.documentElement.clientWidth),'No horizontal overflow');
  assert.deepEqual(errors,[]);console.log('Depth UI passed '+width+'x'+height);await page.close();
 }
 await browser.close();server.close();console.log('Digit depth, subject shadow and rim light, photo fill, weight through the hour and parallax passed.');
})().catch(e=>{console.error(e);process.exit(1)});
