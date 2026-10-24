#!/usr/bin/env python3
"""Builds the Studio Music Bridge test APK with the tools the main build downloads (run ../build.py once first)."""
import os,pathlib,subprocess,zipfile
ROOT=pathlib.Path(__file__).resolve().parent;MAIN=ROOT.parent
TOOLS=pathlib.Path(os.environ.get('EVAN_ANDROID_TOOLS',str(MAIN/'.tools'))).resolve();BUILD=ROOT/'build';DIST=MAIN/'dist'
def run(*a):subprocess.run([str(x) for x in a],cwd=ROOT,check=True)
for need in ['android.jar','ecj.jar','aapt2','r8.jar','apksig.jar']:
 if not (TOOLS/need).exists():raise SystemExit('Missing '+need+'; run ../build.py once to download the build tools.')
classes=BUILD/'classes';classes.mkdir(parents=True,exist_ok=True)
for old in classes.rglob('*.class'):old.unlink()
run('java','-jar',TOOLS/'ecj.jar','-8','-nowarn','-classpath',TOOLS/'android.jar','-d',classes,*sorted((ROOT/'src').rglob('*.java')))
jar=BUILD/'classes.jar'
with zipfile.ZipFile(jar,'w') as z:
 for f in sorted(classes.rglob('*.class')):z.write(f,f.relative_to(classes))
(BUILD/'dex').mkdir(exist_ok=True)
run('java','-cp',TOOLS/'r8.jar','com.android.tools.r8.D8','--min-api','26','--lib',TOOLS/'android.jar','--output',BUILD/'dex',jar)
run(TOOLS/'aapt2','link','-I',TOOLS/'android.jar','--manifest',ROOT/'AndroidManifest.xml','-o',BUILD/'unsigned.apk')
with zipfile.ZipFile(BUILD/'unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write(BUILD/'dex/classes.dex','classes.dex')
key=MAIN/'.signing/dev.keystore'
if not key.is_file():raise SystemExit('Missing the development key; run ../build.py once.')
run('java','-jar',TOOLS/'ecj.jar','-17','-nowarn','-classpath',TOOLS/'apksig.jar','-d',BUILD/'sign',MAIN/'SignApk.java')
out=DIST/'Studio-Music-Bridge-test.apk'
run('java','-cp',str(BUILD/'sign')+os.pathsep+str(TOOLS/'apksig.jar'),'SignApk',BUILD/'unsigned.apk',out,key)
run('java','-cp',str(BUILD/'sign')+os.pathsep+str(TOOLS/'apksig.jar'),'SignApk','verify',out)
print('Built:',out)
