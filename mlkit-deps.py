#!/usr/bin/env python3
"""Fetch ML Kit subject segmentation's declared Android runtime artifacts from Maven.
Only SDK jars are bundled in the APK. This is a build-time dependency resolver,
not a download path inside the phone app.
"""
from concurrent.futures import ThreadPoolExecutor,as_completed
from pathlib import Path
import urllib.request,xml.etree.ElementTree as ET,zipfile,io,json,sys
ROOT=Path(__file__).resolve().parent
DEST=ROOT/'.tools/mlkit';DEST.mkdir(parents=True,exist_ok=True)
REPOS=['https://dl.google.com/dl/android/maven2/','https://repo.maven.apache.org/maven2/']
SEED=('com.google.android.gms','play-services-mlkit-subject-segmentation','16.0.0-beta1')
cache={}
def download(g,a,v,ext):
 name=f'{a}-{v}.{ext}';folder=DEST/g/a/v;folder.mkdir(parents=True,exist_ok=True);path=folder/name
 if path.exists() and path.stat().st_size>0:return path.read_bytes()
 partial=folder/(name+'.tmp')
 for repo in REPOS:
  try:
   with urllib.request.urlopen(repo+g.replace('.','/')+'/'+a+'/'+v+'/'+name,timeout=50) as response:data=response.read()
   partial.write_bytes(data);partial.replace(path);return data
  except Exception:continue
 raise RuntimeError('Cannot fetch '+':'.join((g,a,v,ext)))
def pom(coord):
 g,a,v=coord;root=ET.fromstring(download(g,a,v,'pom'))
 def pick(node,key):return node.findtext('{*}'+key)
 packaging=pick(root,'packaging') or 'jar';children=[]
 for dep in root.findall('.//{*}dependencies/{*}dependency'):
  dg,da,dv=(pick(dep,k) for k in ('groupId','artifactId','version'))
  scope=pick(dep,'scope') or 'compile';optional=pick(dep,'optional')
  if dv and dv.startswith('['):dv=dv.strip('[]()').split(',')[0]
  if dg and da and dv and scope in ('compile','runtime') and optional!='true' and '$' not in dv:children.append((dg,da,dv))
 return packaging,children
pending={SEED};seen={};rounds=0
while pending:
 rounds+=1;print('Resolving Maven dependency wave',rounds,len(pending),flush=True)
 with ThreadPoolExecutor(max_workers=12) as pool:
  futures={pool.submit(pom,c):c for c in pending}
  for f in as_completed(futures):
   coord=futures[f];pack,children=f.result();seen[coord]=(pack,children)
 pending={c for _,kids in seen.values() for c in kids if c not in seen}
 if rounds>12:raise RuntimeError('Excessive dependency depth')
print('Downloading Android artifacts',len(seen),flush=True)
selected={}
for (g,a,v),meta in seen.items():
 key=(g,a)
 if key not in selected or tuple(int(''.join(c for c in x if c.isdigit()) or '0') for x in v.replace('-','.').split('.'))>tuple(int(''.join(c for c in x if c.isdigit()) or '0') for x in selected[key][2].replace('-','.').split('.')):selected[key]=(g,a,v)
with ThreadPoolExecutor(max_workers=12) as pool:
 futures={pool.submit(download,g,a,v,seen.get((g,a,v),('aar',[]))[0]):(g,a,v) for g,a,v in selected.values()}
 for f in as_completed(futures):f.result()
jars=[];manifests=[]
for g,a,v in selected.values():
 pack,_=seen.get((g,a,v),('aar',[]));loc=DEST/g/a/v/(f'{a}-{v}.{pack}')
 if pack=='aar':
  with zipfile.ZipFile(loc) as z:
   for item in z.namelist():
    if item=='classes.jar' or item.startswith('libs/') and item.endswith('.jar'):
     name=DEST/'jars'/f'{g}.{a}-{v}-{Path(item).name}';name.parent.mkdir(exist_ok=True);name.write_bytes(z.read(item));jars.append(str(name))
   if 'AndroidManifest.xml' in z.namelist():manifests.append((g,a,v,z.read('AndroidManifest.xml').decode(errors='replace')))
 elif pack=='jar':jars.append(str(loc))
(DEST/'manifest-notes.json').write_text(json.dumps(manifests,indent=2))
(DEST/'classpath.txt').write_text('\n'.join(jars))
print('Ready:',len(jars),'jars,',(sum(Path(j).stat().st_size for j in jars)//1024//1024),'MiB',flush=True)
