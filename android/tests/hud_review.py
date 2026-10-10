"""Reverse only the exact reviewed precision/display deltas; keep old baselines immutable."""
from pathlib import Path
import json
def strip_hud(name,text):
    if Path(name).name=='AndroidManifest.xml' and 'wear' in Path(name).parts:return text
    patches=json.loads(Path(__file__).with_name('hud-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected precision/brand hook')
        text=text.replace(after,before)
    return text
