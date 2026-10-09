// Maintainer-only resizing/packaging of approved generated artwork. Never called by the app.
const fs=require('fs'),path=require('path'),crypto=require('crypto'),sharp=require('sharp');
const root=path.resolve(__dirname,'../..');
async function main(){
 const sources=JSON.parse(fs.readFileSync(process.argv[2],'utf8')).sort((a,b)=>a.tier-b.tier);
 if(sources.length!==10||sources.some((x,i)=>x.tier!==i))throw Error('Ten ordered production assets required');
 const res=path.join(root,'android/app/src/main/res/drawable-nodpi'),docs=path.join(root,'docs/precision-crests');
 fs.mkdirSync(res,{recursive:true});fs.mkdirSync(docs,{recursive:true});
 const manifest={artwork:'Codex Precision Crests',version:'2.8.9',origin:'Original artwork generated with the built-in image generation tool; approved Codex concept used as a visual reference. No external game or company logos.',license:'Project MIT license',assets:[]};
 const composites=[];let svg='<svg xmlns="http://www.w3.org/2000/svg" width="1400" height="850"><rect width="1400" height="850" fill="#111214"/><text x="40" y="44" fill="#eeeeee" font-size="26" font-family="sans-serif">CODEX METER · PRECISION CRESTS</text><text x="40" y="73" fill="#b9bec7" font-size="16" font-family="sans-serif">Bundled production assets · asset preview, not an Android screenshot</text>';
 for(const s of sources){
  const sourceMeta=await sharp(s.path).metadata(),sourceStats=await sharp(s.path).stats();
  if(!sourceMeta.hasAlpha||sourceStats.isOpaque)throw Error('Source lacks real transparency: '+s.name);
  const target=path.join(res,'prestige_'+s.tier+'.webp');
  await sharp(s.path).resize(512,512,{fit:'contain',background:{r:0,g:0,b:0,alpha:0}}).webp({quality:94,alphaQuality:100,effort:6}).toFile(target);
  const bytes=fs.readFileSync(target),meta=await sharp(bytes).metadata(),stats=await sharp(bytes).stats();
  if(meta.width!==512||meta.height!==512||!meta.hasAlpha||stats.isOpaque)throw Error('Invalid output');
  manifest.assets.push({tier:s.tier,name:s.name,resource:'prestige_'+s.tier,path:path.relative(root,target).replaceAll('\\','/'),sha256:crypto.createHash('sha256').update(bytes).digest('hex'),bytes:bytes.length,width:512,height:512,alpha:true,decodedArgbBytes:512*512*4,sourceSha256:crypto.createHash('sha256').update(fs.readFileSync(s.path)).digest('hex')});
  const x=30+(s.tier%5)*276,y=100+Math.floor(s.tier/5)*340;
  svg+=`<rect x="${x}" y="${y}" width="256" height="326" rx="16" fill="#191b1f" stroke="#444952"/><text x="${x+128}" y="${y+250}" text-anchor="middle" fill="#ededed" font-size="18" font-family="sans-serif">${s.name}</text><rect x="${x+40}" y="${y+265}" width="176" height="52" rx="8" fill="#f4f4f5"/>`;
  composites.push({input:await sharp(bytes).resize(220,220).png().toBuffer(),left:x+18,top:y+12});
  composites.push({input:await sharp(bytes).resize(48,48).png().toBuffer(),left:x+104,top:y+267});
 }
 svg+='</svg>';await sharp(Buffer.from(svg)).composite(composites).png().toFile(path.join(docs,'tier-gallery.png'));
 fs.writeFileSync(path.join(docs,'ASSETS.json'),JSON.stringify(manifest,null,2)+'\n');
 const html='<!doctype html><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Codex Precision Crests</title><style>body{background:#111214;color:#eee;font:16px system-ui;padding:24px}main{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:16px}article{background:#191b1f;border:1px solid #444952;border-radius:16px;text-align:center;padding:16px}img{width:180px;height:180px;object-fit:contain}small{color:#b9bec7}</style><h1>Codex Meter · Precision Crests</h1><p>Production artwork preview. This is not an Android screen capture.</p><main>'+manifest.assets.map(a=>`<article><img src="../../${a.path}" alt="${a.name} precision crest"><h2>${a.name}</h2><small>512 × 512 · transparent WebP</small></article>`).join('')+'</main>';
 fs.writeFileSync(path.join(docs,'index.html'),html);
 console.log(JSON.stringify({assets:10,totalBytes:manifest.assets.reduce((n,a)=>n+a.bytes,0),maximumDecodedArgbBytes:1048576,alpha:'verified'}));
}
main().catch(e=>{console.error(e);process.exitCode=1});
