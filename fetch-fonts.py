#!/usr/bin/env python3
"""Bundle the supplied design's Google Fonts with their licenses for offline use."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
from urllib.request import urlopen
from urllib.parse import quote
import re,json
ROOT=Path(__file__).resolve().parent;DEST=ROOT/'assets/fonts';DEST.mkdir(parents=True,exist_ok=True)
families={'inter':'inter','outfit':'outfit','manrope':'manrope','sora':'sora','lexend':'lexend','poppins':'poppins','spacegrotesk':'spacegrotesk','nunito':'nunito','bebas':'bebasneue','anton':'anton','archivoblack':'archivoblack','bungee':'bungee','righteous':'righteous','unbounded':'unbounded','bigshoulders':'bigshoulders','syne':'syne','fraunces':'fraunces','playfair':'playfairdisplay','dmserif':'dmserifdisplay','cormorant':'cormorantgaramond','robotoslab':'robotoslab','abril':'abrilfatface','jetbrains':'jetbrainsmono','spacemono':'spacemono','major':'majormonodisplay','orbitron':'orbitron','audiowide':'audiowide','chakra':'chakrapetch','rubikmono':'rubikmonoone','barlow':'barlowcondensed'}
def get(url):
 with urlopen(url,timeout=45) as r:return r.read()
def fetch(item):
 key,folder=item;base='https://raw.githubusercontent.com/google/fonts/main/'+('apache/' if key=='robotoslab' else 'ofl/')+folder+'/'
 meta=get(base+'METADATA.pb').decode();blocks=re.findall(r'fonts\s*\{(.*?)\n\}',meta,re.S);faces=[]
 for block in blocks:
  fn=re.search(r'filename:\s*"(.*?)"',block);style=re.search(r'style:\s*"(.*?)"',block);weight=re.search(r'weight:\s*(\d+)',block)
  if not fn or style and style.group(1)!='normal':continue
  source=fn.group(1);w=int(weight.group(1)) if weight else 400
  name=key+('-var' if '[' in source else '-'+str(w))+'.ttf';target=DEST/name
  if not target.exists():target.write_bytes(get(base+quote(source)))
  faces.append({'file':name,'weight':w,'variable':'[' in source})
 if not faces:raise RuntimeError('No upright font for '+key)
 license=DEST/(key+('-LICENSE.txt' if key=='robotoslab' else '-OFL.txt'))
 if not license.exists():license.write_bytes(get(base+('LICENSE.txt' if key=='robotoslab' else 'OFL.txt')))
 print(key,len(faces),flush=True);return key,faces
result={}
with ThreadPoolExecutor(max_workers=8) as pool:
 for future in as_completed([pool.submit(fetch,item) for item in families.items()]):
  key,value=future.result();result[key]=value
(DEST/'catalog.json').write_text(json.dumps(result,indent=2))
print('Bundled',len(result),'families',sum(p.stat().st_size for p in DEST.glob('*.ttf'))//1024,'KiB')
