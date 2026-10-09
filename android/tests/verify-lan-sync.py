"""Real Java/C# pinned TLS wire using isolated synthetic data; never user auth or records."""
from pathlib import Path
import os, subprocess, tempfile, time
root=Path(__file__).resolve().parents[1];repo=root.parent
out=root/'build/lan-verification';out.mkdir(parents=True,exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin';jar=root/'build/test-libs/json-20250517.jar'
files=[root/'shared/src/main/java/dev/bennett/codexmeter/LanSyncWire.java',Path(__file__).with_name('LanSyncSelfTest.java')]
subprocess.run([str(jdk/'javac'),'-encoding','UTF-8','-cp',str(jar),'-d',str(out),*map(str,files)],check=True)
exe=repo/'windows-widget/build/WidgetTests.exe'
if not exe.exists():raise RuntimeError('Run windows-widget/build.ps1 -Test first')
directory=Path(tempfile.mkdtemp(prefix='probe-',dir=os.environ.get('CODEX_METER_TEST_ROOT')))
log=open(out/'probe.log','w',encoding='utf-8')
server=subprocess.Popen([str(exe),'--lan-probe',str(directory)],stdout=log,stderr=log)
try:
    for _ in range(100):
        if (directory/'pair.txt').exists():break
        if server.poll() is not None:raise RuntimeError('Synthetic server startup failed; see probe.log')
        time.sleep(.1)
    else:raise RuntimeError('Synthetic server startup timed out')
    subprocess.run([str(jdk/'java'),'-ea','-cp',str(out)+os.pathsep+str(jar),'dev.bennett.codexmeter.LanSyncSelfTest',str(directory/'pair.txt')],check=True,timeout=90)
finally:
    (directory/'stop').touch()
    try:server.wait(timeout=15)
    except subprocess.TimeoutExpired:server.terminate();server.wait(timeout=5)
    log.close()
