#!/usr/bin/env python3
"""Compile and test pure Kotlin sources. Requires Java 17, Node 22+ and Maven Central access.
Does not compile Android activities or SharedPreferences adapters.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import tempfile
import urllib.request

root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--cache-dir',type=Path,default=Path(tempfile.gettempdir())/'scrappy-jvm-test-dependencies')
args=parser.parse_args()
args.cache_dir.mkdir(parents=True,exist_ok=True)

def matches(path, expected):
    return path.exists() and hashlib.sha256(path.read_bytes()).hexdigest()==expected

spec=json.loads((root/'scripts/jvm-dependencies.json').read_text())
jars=[]
for item in spec:
    path=args.cache_dir/item['path'].split('/')[-1]
    if not matches(path,item['sha256']):
        print('Downloading',path.name,flush=True)
        url='https://repo.maven.apache.org/maven2/'+item['path']
        partial=path.with_suffix('.partial')
        for attempt in range(8):
            offset=partial.stat().st_size if partial.exists() else 0
            request=urllib.request.Request(url,headers={'Range':f'bytes={offset}-'} if offset else {})
            try:
                with urllib.request.urlopen(request,timeout=45) as response:
                    mode='ab' if offset and response.status==206 else 'wb'
                    with partial.open(mode) as output:
                        shutil.copyfileobj(response,output)
                if matches(partial,item['sha256']):
                    partial.replace(path);break
            except Exception as error:
                print(f'Download attempt {attempt+1}: {error}',flush=True)
        if not matches(path,item['sha256']):
            raise SystemExit('Dependency checksum verification failed: '+path.name)
    jars.append(str(path.resolve()))

cp=os.pathsep.join(jars)
sources=[str(p) for p in sorted((root/'app/src/main/java/com/scrappy/app/data').glob('*.kt'))
         if p.name not in ('Prefs.kt','LedgerStore.kt')]
tests=sorted((root/'app/src/test/java/com/scrappy/app').glob('*Test.kt'))
tests += sorted((root/'transport-tests/src/test/kotlin/com/scrappy/app').glob('*Test.kt'))
sources += [str(p) for p in tests]
with tempfile.TemporaryDirectory(prefix='scrappy-core-tests-') as work:
    classes=Path(work)/'classes'
    compile_cmd=['java','-cp',cp,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect',
                 '-jvm-target','17','-classpath',cp,'-d',str(classes)] + sources
    subprocess.run(compile_cmd,check=True)
    command=['java','-Dscrappy.projectRoot='+str(root),'-cp',str(classes)+os.pathsep+cp,'org.junit.runner.JUnitCore']
    command += ['com.scrappy.app.'+p.stem for p in tests]
    subprocess.run(command,check=True)
