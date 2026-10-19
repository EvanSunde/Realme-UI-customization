#!/usr/bin/env python3
"""Reproducible Linux x86_64 build. Requires Python 3, Java 17+ and keytool."""
import concurrent.futures,os,pathlib,subprocess,zipfile,urllib.request,sys
ROOT=pathlib.Path(__file__).resolve().parent
TOOLS=pathlib.Path(os.environ.get('EVAN_ANDROID_TOOLS',str(ROOT/'.tools'))).resolve()
BUILD=ROOT/'build';DIST=ROOT/'dist';TOOLS.mkdir(parents=True,exist_ok=True);DIST.mkdir(exist_ok=True)
GOOGLE='https://dl.google.com/dl/android/maven2/'
URLS={
 'android.jar':'https://raw.githubusercontent.com/Sable/android-platforms/master/android-35/android.jar',
 'ecj.jar':'https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.37.0/ecj-3.37.0.jar',
 'aapt2.jar':GOOGLE+'com/android/tools/build/aapt2/8.5.2-11315950/aapt2-8.5.2-11315950-linux.jar',
 'r8.jar':GOOGLE+'com/android/tools/r8/8.3.37/r8-8.3.37.jar',
 'apksig.jar':GOOGLE+'com/android/tools/build/apksig/8.5.2/apksig-8.5.2.jar'}
for part in ['api','provider','aidl','shared']:
 URLS['shizuku-'+part+'.aar']='https://repo.maven.apache.org/maven2/dev/rikka/shizuku/'+part+'/13.1.5/'+part+'-13.1.5.aar'
def fetch(item):
 name,url=item;file=TOOLS/name
 if not file.is_file():
  print('Downloading',name,flush=True)
  with urllib.request.urlopen(url,timeout=90) as r:file.write_bytes(r.read())
with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:list(pool.map(fetch,URLS.items()))
for part in ['api','provider','aidl','shared']:
 with zipfile.ZipFile(TOOLS/('shizuku-'+part+'.aar')) as z:(TOOLS/('shizuku-'+part+'.jar')).write_bytes(z.read('classes.jar'))
with zipfile.ZipFile(TOOLS/'aapt2.jar') as z:(TOOLS/'aapt2').write_bytes(z.read('aapt2'))
os.chmod(TOOLS/'aapt2',0o755)
def run(*args):subprocess.run([str(a) for a in args],cwd=ROOT,check=True)
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
for old in classes.rglob('*.class'):old.unlink()
libs=[TOOLS/('shizuku-'+part+'.jar') for part in ['api','provider','aidl','shared']]
if not (ROOT/'assets/fonts/catalog.json').exists():run(sys.executable,ROOT/'fetch-fonts.py')
mlkit=ROOT/'.tools/mlkit/classpath.txt'
if not mlkit.exists():run(sys.executable,ROOT/'mlkit-deps.py')
mlkit_jars=[pathlib.Path(s) for s in mlkit.read_text().splitlines() if s.strip()]
cp=os.pathsep.join(str(p) for p in [TOOLS/'android.jar']+libs+mlkit_jars)
run('java','-jar',TOOLS/'ecj.jar','-8','-nowarn','-classpath',cp,'-d',classes,*sorted((ROOT/'src').rglob('*.java')))
(BUILD/'tests').mkdir(exist_ok=True)
run('java','-jar',TOOLS/'ecj.jar','-8','-nowarn','-classpath',str(classes)+os.pathsep+str(TOOLS/'android.jar'),'-d',BUILD/'tests',*sorted((ROOT/'tests').glob('*.java')))
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'LayoutCheck')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'ApplyCheck')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'StudioCheck')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'V2Check')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'AodCheck')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'ShadeCheck')
run('java','-cp',os.pathsep.join(str(p) for p in [BUILD/'tests',classes,TOOLS/'android.jar']),'AodDisplayCheck')
jar=BUILD/'app-classes.jar'
with zipfile.ZipFile(jar,'w') as z:
 for file in sorted(classes.rglob('*.class')):z.write(file,file.relative_to(classes))
(BUILD/'dex').mkdir(exist_ok=True)
run('java','-Xmx3g','-cp',TOOLS/'r8.jar','com.android.tools.r8.D8','--min-api','29','--lib',TOOLS/'android.jar','--output',BUILD/'dex',jar,*libs,*mlkit_jars)
run(TOOLS/'aapt2','compile','--dir',ROOT/'res','-o',BUILD/'resources.zip')
run(TOOLS/'aapt2','link','-I',TOOLS/'android.jar','--manifest',ROOT/'AndroidManifest.xml','-o',BUILD/'unsigned.apk',BUILD/'resources.zip')
with zipfile.ZipFile(BUILD/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:
 for dex in sorted((BUILD/'dex').glob('classes*.dex')):z.write(dex,dex.name)
 for file in sorted((ROOT/'assets').rglob('*')):
  if file.is_file():z.write(file,'assets/'+str(file.relative_to(ROOT/'assets')))
key=ROOT/'signing/debug.keystore';key.parent.mkdir(exist_ok=True)
if not key.is_file():run('keytool','-genkeypair','-alias','evan-debug','-keyalg','RSA','-keysize','2048','-validity','10000','-keystore',key,'-storetype','JKS','-storepass','android','-keypass','android','-dname','CN=Evan Lockscreen Development','-noprompt')
run('java','-jar',TOOLS/'ecj.jar','-17','-nowarn','-classpath',TOOLS/'apksig.jar','-d',BUILD/'sign',ROOT/'SignApk.java')
out=DIST/'Evan-Lockscreen-0.7.0.apk'
run('java','-cp',str(BUILD/'sign')+os.pathsep+str(TOOLS/'apksig.jar'),'SignApk',BUILD/'unsigned.apk',out,key)
run('java','-cp',str(BUILD/'sign')+os.pathsep+str(TOOLS/'apksig.jar'),'SignApk','verify',out)
print('Built:',out)
