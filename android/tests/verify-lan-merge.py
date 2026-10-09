from pathlib import Path
import os,subprocess,tempfile
root=Path(__file__).resolve().parents[1];jdk=Path(os.environ['JAVA_HOME'])/'bin'
out=root/'build/sqlite-verification';classes=out/'classes';jar=root/'build/test-libs/json-20250517.jar';cp=str(classes)+os.pathsep+str(jar)
files=[Path(__file__).with_name(n+'.java') for n in ['LedgerLanMergeSelfTest','LanSyncSelfTest']]+[root/'shared/src/main/java/dev/bennett/codexmeter/LanSyncWire.java']
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',cp,'-d',str(classes),*map(str,files)],check=True)
directory=tempfile.mkdtemp(prefix='lan-merge-',dir=str(out))
import sys
subprocess.run([str(jdk/'java'),'-ea','-Dledger.python='+sys.executable,'-Dledger.bridge='+str(out/'sqlite_bridge.py'),'-Dledger.dbdir='+directory,'-cp',cp,'dev.bennett.codexmeter.LedgerLanMergeSelfTest'],check=True)
subprocess.run([str(jdk/'java'),'-ea','-cp',cp,'dev.bennett.codexmeter.LanSyncSelfTest'],check=True)
