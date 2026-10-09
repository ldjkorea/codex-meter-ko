"""Original Meter Scout vector family; Android and SVG share exact path geometry.
Run only to deliberately regenerate checked-in artwork, never during app execution.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
colors=['#A9B3BD','#DFA778','#D5E1EB','#E8CE91','#AEE4EE','#68E1B1','#9BDCFA','#C7A7F4','#F1A0AC','#FFDFA1']
names=['Iron','Bronze','Silver','Gold','Platinum','Emerald','Diamond','Master','Grandmaster','Challenger']
out=root.parent/'docs/tier-previews';out.mkdir(parents=True,exist_ok=True)
for i,color in enumerate(colors):
    paths=[]
    def path(d,fill,stroke='none',width=1):paths.append((d,fill,stroke,width))
    # One coherent survey drone: instrument core, visor, two feet, central status diamond.
    path('M36 80 L31 101 L48 101 L51 84 Z M77 84 L80 101 L97 101 L91 80 Z','#343C46',color,2)
    if i<3:path('M30 43 L40 32 L88 32 L98 43 L98 80 L86 92 L42 92 L30 80 Z','#29313A',color,2)
    else:path('M26 44 L40 29 L88 29 L102 44 L96 81 L81 96 L47 96 L32 81 Z','#29313A',color,2)
    path('M38 48 L48 40 L80 40 L90 48 L88 67 L77 75 L51 75 L40 67 Z','#0D1117',color,1.5)
    path('M47 53 L58 53 L58 59 L47 59 Z M70 53 L81 53 L81 59 L70 59 Z',color)
    path('M58 64 L64 67 L70 64','none',color,1.5)
    path('M60 81 L64 77 L68 81 L64 87 Z',color)
    if i==0:path('M23 53 L30 53 L30 69 L23 69 Z M98 53 L105 53 L105 69 L98 69 Z','#4D5865',color)
    if i>=1:
        path('M24 45 L34 40 L38 50 L34 77 L24 72 Z M104 45 L94 40 L90 50 L94 77 L104 72 Z','#39414B',color,1.5)
        path('M43 81 L51 87 M85 81 L77 87','none',color,1)
    if i>=2:path('M42 29 L51 22 L77 22 L86 29 L80 32 L48 32 Z','#424C58',color,1)
    if i>=3:
        path('M36 78 L47 90 L81 90 L92 78','none',color,2)
        path('M60 21 L64 15 L68 21 L64 27 Z',color)
    if i>=4:
        path('M16 54 L23 50 L23 70 L16 76 Z M112 54 L105 50 L105 70 L112 76 Z','#233B45',color,1.5)
        path('M45 37 L51 34 M83 37 L77 34 M48 94 L45 108 M80 94 L83 108','none',color,2)
    if i>=5:
        path('M22 42 L19 25 L34 36 L29 47 Z M106 42 L109 25 L94 36 L99 47 Z',color,'#ECF7FF',1)
        path('M19 25 L29 47 M109 25 L99 47','none','#557B8A',1)
    if i>=6:
        path('M9 61 L15 40 L19 58 L13 77 Z M119 61 L113 40 L109 58 L115 77 Z',color,'#ECF7FF',1)
        path('M36 108 L41 101 L46 108 L41 116 Z M82 108 L87 101 L92 108 L87 116 Z',color)
    if i>=7:
        path('M10 88 L20 81 L28 88 L20 98 Z M100 88 L108 81 L118 88 L108 98 Z',color,'#F6EFFC',1)
        path('M17 87 L23 87 M105 87 L111 87','none','#17191C',2)
        path('M46 15 L54 10 L64 13 L74 10 L82 15','none',color,1.5)
    if i>=8:
        path('M43 22 L40 9 L52 16 L64 4 L76 16 L88 9 L85 22 Z','#5A4245',color,2)
        path('M60 17 L64 10 L68 17 L64 22 Z',color)
    if i>=9:
        path('M6 34 L14 22 L30 19 L23 27 L14 30 L12 43 Z M122 34 L114 22 L98 19 L105 27 L114 30 L116 43 Z','#4E4B40',color,1.5)
        path('M30 100 L24 111 L47 119 L64 113 L81 119 L104 111 L98 100','none',color,1.5)
        path('M62 118 L64 115 L66 118 L64 123 Z',color)
    vector='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="128dp" android:height="128dp" android:viewportWidth="128" android:viewportHeight="128">\n'
    svg='<svg xmlns="http://www.w3.org/2000/svg" width="256" height="256" viewBox="0 0 128 128">\n'
    for d,f,s,w in paths:
        vector+=f'<path android:pathData="{d}" android:fillColor="{f if f!="none" else "#00000000"}" android:strokeColor="{s if s!="none" else "#00000000"}" android:strokeWidth="{w}"/>\n'
        svg+=f'<path d="{d}" fill="{f}" stroke="{s}" stroke-width="{w}"/>\n'
    (root/f'app/src/main/res/drawable/evo_{i}.xml').write_text(vector+'</vector>\n',encoding='utf-8')
    (out/f'{i:02}-{names[i]}.svg').write_text(svg+'</svg>\n',encoding='utf-8')
html='<!doctype html><meta charset="utf-8"><title>Meter Scout — Tier Evolution</title><style>body{background:#0d0e10;color:#f5f5f5;font:16px system-ui;padding:32px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:18px}article{background:#17191c;border:1px solid #40454d;border-radius:20px;padding:20px;text-align:center}img{width:180px;height:180px}p{color:#a8adb5}</style><h1>Meter Scout · 10 Evolutions</h1><p>Original local vector artwork. Preview fixtures do not change any account or tier record.</p><main>'
for i,n in enumerate(names):html+=f'<article style="border-color:{colors[i]}"><img src="{i:02}-{n}.svg" alt="{n} Meter Scout"><h2>{n}</h2></article>'
(out/'index.html').write_text(html+'</main>',encoding='utf-8')
print('Generated 10 original VectorDrawable/SVG geometry pairs and isolated visual gallery.')
