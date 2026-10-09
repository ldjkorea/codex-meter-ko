// Render the exact SVG path assets for visual review. Requires the maintainer's local sharp runtime.
const fs=require('fs'),path=require('path'),sharp=require('sharp');
const dir=path.resolve(__dirname,'../../docs/tier-previews');
const files=fs.readdirSync(dir).filter(f=>f.endsWith('.svg')).sort();
let svg='<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="640"><rect width="1200" height="640" fill="#0d0e10"/>';
for(let i=0;i<files.length;i++){
 const x=16+(i%5)*238,y=16+Math.floor(i/5)*312;
 const body=fs.readFileSync(path.join(dir,files[i]),'utf8').replace(/<svg[^>]+>/,'').replace('</svg>','');
 svg+=`<rect x="${x}" y="${y}" width="222" height="296" rx="20" fill="#17191c" stroke="#454b55"/><g transform="translate(${x+15} ${y+20}) scale(1.5)">${body}</g><text x="${x+111}" y="${y+260}" fill="#f5f5f5" font-size="19" font-family="sans-serif" text-anchor="middle">${files[i].slice(3,-4)}</text>`;
}
svg+='</svg>';sharp(Buffer.from(svg)).png().toFile(path.join(dir,'tier-gallery.png')).then(()=>console.log('Exact vector artwork gallery rendered.'));
