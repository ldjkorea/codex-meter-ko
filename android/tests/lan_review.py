"""Exact additive LAN hooks; original historical source fixtures stay immutable."""
from pathlib import Path
import json
def strip_lan(name,text):
    patches=json.loads(Path(__file__).with_name('lan-reviewed-patches.json').read_text(encoding='utf-8'))
    for before,after in reversed(patches.get(Path(name).name,[])):
        assert text.count(after)==1,(name,'Unexpected LAN hook')
        text=text.replace(after,before)
    return text

def strip_lan_bytes(name,raw):
    if Path(name).name in ("UsageApi.java","UsageLedgerDatabase.java"):
        return strip_lan(name,raw.decode("utf-8").replace("\r\n","\n")).encode()
    return raw
