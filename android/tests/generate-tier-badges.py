"""Original polygon emblems, identical geometry in VectorDrawable/SVG/contact sheet.
No game artwork, downloaded images, font glyphs or external runtime dependencies.
Run with bundled Pillow for the authoring contact sheet, not an APK screen capture.
"""
from pathlib import Path
import math,json
from PIL import Image,ImageDraw,ImageFont
repo=Path(__file__).resolve().parents[2]
assets=repo/'docs/badges-2.8.4';assets.mkdir(parents=True,exist_ok=True)
res=repo/'android/app/src/main/res/drawable'
names=['iron','bronze','silver','gold','platinum','emerald','diamond','master','grandmaster','challenger']
colors=['#8293A0','#BD895D','#C6D9E4','#E8BE54','#90DFE0','#4AE2AD','#88BEFF','#D5A1FF','#FF9172','#F9DC8D']
all_shapes=[]
for rank,(name,color) in enumerate(zip(names,colors)):
    shapes=[]
    def poly(points,fill):shapes.append((points,fill))
    def diamond(x,y,w,h,fill):poly([(x,y-h),(x+w,y),(x,y+h),(x-w,y)],fill)
    # Distinct silhouettes evolve from compact shields to winged crown/halo crests.
    if rank>=3:
        for side in [-1,1]:
            for feather in range(min(2+rank//2,6)):
                x=80+side*(28+feather*6);y=83-feather*5
                poly([(x,y),(80+side*(43+feather*5),y-20-rank),(80+side*(40+feather*5),y+13),(80+side*25,108-feather*2)],'#17243B')
                poly([(x,y),(80+side*(40+feather*5),y-15-rank),(80+side*(36+feather*5),y+9),(80+side*27,99-feather*2)],color)
    if rank>=7:
        # Segmented halo rather than a color swap; brighter, wider for final tiers.
        for step in range(12+rank):
            a=step*2*math.pi/(12+rank);x=80+math.cos(a)*63;y=78+math.sin(a)*63
            diamond(x,y,2 if rank==7 else 3,4 if rank==9 else 3,color)
    if rank<2:outer=[(80,37),(112,55),(108,98),(80,126),(52,98),(48,55)]
    elif rank<4:outer=[(80,25),(118,47),(117,96),(99,119),(80,134),(61,119),(43,96),(42,47)]
    elif rank<7:outer=[(80,20),(124,54),(112,109),(80,139),(48,109),(36,54)]
    else:outer=[(80,19),(107,33),(124,61),(115,109),(80,141),(45,109),(36,61),(53,33)]
    poly(outer,'#111E32')
    inner=[(80+(x-80)*.86,80+(y-80)*.86) for x,y in outer];poly(inner,color)
    cavity=[(80+(x-80)*.71,80+(y-80)*.71) for x,y in outer];poly(cavity,'#28374F')
    # Asymmetric metal facets give structure in both dark and light backgrounds.
    poly([inner[0],inner[1],cavity[1],cavity[0]],'#F1F7FF')
    poly([(80,132),(53,108),(53,101),(80,120),(107,101),(107,108)],color)
    if rank>=3:
        tips=3 if rank<8 else 5
        crown=[(49,47),(49,23),(65,30),(80,12),(95,30),(111,23),(111,47)] if rank<8 else [(47,47),(47,22),(58,31),(64,15),(74,29),(80,9),(86,29),(96,15),(102,31),(113,22),(113,47)]
        poly(crown,'#152136')
        poly([(55,44),(54,29),(67,35),(80,20),(93,35),(106,29),(105,44)],color)
    if rank<2:
        poly([(69,62),(91,62),(99,77),(80,106),(61,77)],color)
        poly([(69,64),(80,78),(80,101),(64,77)],'#E8F1F9' if rank==0 else '#F8CAA0')
        poly([(76,73),(84,73),(88,78),(80,88),(72,78)],'#293B50')
        if rank==1:poly([(64,108),(80,118),(96,108),(96,113),(80,125),(64,113)],'#F8CAA0')
    elif rank<5:
        for y in [67,83] if rank==2 else [63,78,93]:
            poly([(60,y),(80,y+13),(100,y),(100,y+8),(80,y+21),(60,y+8)],color)
        if rank>=4:diamond(80,74,12,20,'#F4FFFF')
    else:
        diamond(80,81,23 if rank<8 else 27,32,color)
        poly([(80,49),(57,81),(80,74)],'#F6FBFF')
        poly([(80,49),(103,81),(80,74)],color)
        poly([(57,81),(80,74),(80,113)],'#254C65' if rank<7 else '#604C86')
        poly([(103,81),(80,74),(80,113)],color)
        diamond(80,78,7,11,'#F5FFFF')
        if rank==6:
            poly([(80,46),(70,63),(80,58),(90,63)],'#EAF7FF')
            poly([(59,84),(70,96),(65,102),(53,88)],'#B1D8FF')
            poly([(101,84),(90,96),(95,102),(107,88)],'#EAF7FF')
        if rank>=8:
            for x in [42,118]:diamond(x,84,6,11,'#FFF1CB')
    if rank>=1:
        for x in [54,106]:diamond(x,61,2.5,3,'#F5F4D9')
    if rank==9:
        poly([(71,135),(80,124),(89,135),(80,149)],color)
        diamond(80,12,4,6,'#FFF7D9')
    def path(points):return 'M'+ ' L'.join(f'{x:.2f},{y:.2f}' for x,y in points)+' Z'
    vector=['<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="128dp" android:height="128dp" android:viewportWidth="160" android:viewportHeight="160">']
    svg=['<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 160">']
    for points,fill in shapes:
        vector.append(f'<path android:fillColor="{fill}" android:pathData="{path(points)}"/>')
        svg.append(f'<path fill="{fill}" d="{path(points)}"/>')
    vector.append('</vector>');svg.append('</svg>')
    (res/f'badge_{name}.xml').write_text('\n'.join(vector),encoding='utf-8')
    (assets/f'{name}.svg').write_text('\n'.join(svg),encoding='utf-8');all_shapes.append(shapes)
sheet=Image.new('RGB',(1200,540),'#F5F7FC');draw=ImageDraw.Draw(sheet)
font=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',18)
small=ImageFont.truetype('C:/Windows/Fonts/segoeui.ttf',13)
for i,name in enumerate(names):
    for theme in [0,1]:
        x=(i%5)*240;y=(i//5)*260
        if theme==1:continue
        draw.rounded_rectangle((x+8,y+8,x+232,y+252),16,fill='#FFFFFF')
        # Large preview on neutral light with thumbnail on dark for each tier.
        for points,color in all_shapes[i]:draw.polygon([(x+40+px,y+10+py) for px,py in points],fill=color)
        draw.rounded_rectangle((x+94,y+173,x+146,y+225),8,fill='#101A2A')
        for points,color in all_shapes[i]:draw.polygon([(x+96+px*.3,y+175+py*.3) for px,py in points],fill=color)
        draw.text((x+14,y+229),f'{i+1:02d}  {name.title()}',font=font,fill='#243148')
draw.text((12,522),'Original VectorDrawable / SVG asset review. Not an Android APK screenshot.',font=small,fill='#556376')
sheet.save(assets/'tier-contact-sheet.png')
(assets/'README.md').write_text('# Badge asset review\n\nTen original polygon emblems. Geometry is identical in the Android VectorDrawable and SVG files. This contact sheet is a Pillow rendering of asset geometry, not a native APK screenshot. Each badge is shown on light and dark backgrounds; names remain real text in the application.\n',encoding='utf-8')
print('Created ten original badges, SVG originals and contact sheet; no native UI claim.')
