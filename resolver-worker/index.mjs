/**
 * Pooyan TV's optional public-media resolver (Cloudflare Worker).
 * DeepSeek Search is used only to locate additional FarsiPlex PUBLIC pages.
 * It never produces a media link: every link must occur in real HTML5 media markup
 * and pass an allowlisted CDN + HTTP check. No cookies, iframes, DRM or tokens.
 */
const SITE = 'farsiplex.com';
const PATH = /^\/(movie|tvshow|episode)\/[^/]+\/?$/i;
const MAX_HTML = 600_000;

function json(data, status=200) {
  return new Response(JSON.stringify(data), {
    status, headers: {'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store'}
  });
}
function secureEquals(a,b) {
  if(typeof a!=='string'||typeof b!=='string'||!a||!b)return false;
  let result=a.length^b.length; const max=Math.max(a.length,b.length);
  for(let i=0;i<max;i++)result|=(a.charCodeAt(i)||0)^(b.charCodeAt(i)||0);
  return result===0;
}
export function publicPage(candidate) {
  try {
    const u=new URL(candidate);
    if(u.protocol!=='https:'||!['farsiplex.com','www.farsiplex.com'].includes(u.hostname)||u.port||u.username||u.password||!PATH.test(u.pathname))return '';
    return 'https://farsiplex.com'+u.pathname.replace(/\/$/,'')+'/';
  } catch {return '';}
}
function mediaHostAllowed(url,env) {
  let u;
  try {u=new URL(url);} catch {return false;}
  if(u.protocol!=='https:'||u.username||u.password||u.port)return false;
  const hosts=String(env.MEDIA_ALLOWED_HOSTS||'farsiplex.com,www.farsiplex.com,pl.farsiplex.buzz')
    .split(',').map(s=>s.trim().toLowerCase()).filter(Boolean);
  return hosts.includes(u.hostname.toLowerCase());
}
export function directMedia(candidate) {
  try { const u=new URL(candidate);return u.protocol==='https:'&&/\.(mp4|m4v|webm|m3u8|mpd)$/i.test(u.pathname); }
  catch {return false;}
}
function decodeEntities(s){
  return s.replace(/&amp;/gi,'&').replace(/&quot;/gi,'"').replace(/&#39;|&apos;/gi,"'")
    .replace(/&#(\d+);/g,(_,n)=>String.fromCharCode(Number(n)))
    .replace(/&#x([0-9a-f]+);/gi,(_,n)=>String.fromCharCode(parseInt(n,16)));
}
export function videoCandidates(html,base,env){
  const candidates=[];const seen=new Set();
  function add(raw){
    try {
      const absolute=new URL(decodeEntities(raw.trim()),base).toString();
      if(directMedia(absolute)&&mediaHostAllowed(absolute,env)&&!seen.has(absolute)){
        seen.add(absolute);candidates.push(absolute);
      }
    }catch{}
  }
  // Only explicit HTML5 <video src> and nested <source src>; never parse JS or iframe embeds.
  for(const block of html.match(/<video\b[^>]*>(?:[\s\S]*?<\/video\s*>)?/gi)||[]){
    const open=block.match(/^<video\b[^>]*>/i)?.[0]||'';
    const src=open.match(/\bsrc\s*=\s*["']([^"']+)["']/i);
    if(src)add(src[1]);
    for(const source of block.match(/<source\b[^>]*>/gi)||[]){
      const match=source.match(/\bsrc\s*=\s*["']([^"']+)["']/i);if(match)add(match[1]);
    }
  }
  return candidates.slice(0,8);
}
async function fetchPublicHtml(url,fetcher){
  // `redirect:manual` prevents unexpected requests outside the approved source.
  const response=await fetcher(url,{method:'GET',redirect:'manual',headers:{'Accept':'text/html','User-Agent':'PooyanTV-Resolver/0.4.2'}});
  if(response.status!==200||!(response.headers.get('content-type')||'').toLowerCase().includes('text/html'))return null;
  if(Number(response.headers.get('content-length')||0)>MAX_HTML)return null;
  const html=await response.text();return html.length<=MAX_HTML?html:null;
}
async function checkMedia(url,env,fetcher){
  if(!directMedia(url)||!mediaHostAllowed(url,env))return false;
  const okType=(type)=>/^(video\/|application\/(vnd\.apple\.mpegurl|x-mpegurl|dash\+xml|octet-stream))/i.test(type);
  for(const method of ['HEAD','GET']){
    try {
      const options={method,redirect:'manual',headers: method==='GET'?{'Range':'bytes=0-0'}:{}};
      const response=await fetcher(url,options);
      const type=(response.headers.get('content-type')||'').split(';')[0].trim();
      if(response.body)await response.body.cancel();
      if([200,206].includes(response.status) && okType(type))return true;
      if(response.status===401||response.status===403||response.status===402)return false;
    }catch { /* Try an alternate method. */ }
  }
  return false;
}
async function searchOfficialPages(page,title,env,fetcher){
  if(!env.DEEPSEEK_API_KEY)return {pages:[],attempted:false,warning:'DeepSeek key is not configured'};
  const trimmed=String(title||'').replace(/[\r\n"\\]/g,' ').trim().slice(0,80);
  if(!trimmed)return {pages:[],attempted:false,warning:'Missing movie title'};
  const response=await fetcher('https://api.deepseek.com/v1/tools/search',{
    method:'POST',headers:{'Authorization':'Bearer '+env.DEEPSEEK_API_KEY,'Content-Type':'application/json'},
    body:JSON.stringify({query:`site:farsiplex.com "${trimmed}"`,count:6,need_content:false}),redirect:'manual'
  });
  if(!response.ok)return {pages:[],attempted:true,warning:'DeepSeek Search HTTP '+response.status};
  const data=await response.json();
  const originalType=new URL(page).pathname.split('/')[1];
  const originalSlug=new URL(page).pathname.split('/')[2]?.toLowerCase();
  const pages=[];
  for(const item of (Array.isArray(data.results)?data.results:[])){
    const normalized=publicPage(item.url);
    if(!normalized || normalized===page)continue;
    const parts=new URL(normalized).pathname.split('/');
    // Only the identical entry, including known canonical variants, may be resolved.
    // No cross-title substitution, even if DeepSeek suggests a plausible match.
    if(parts[1]===originalType && parts[2]?.toLowerCase()===originalSlug && !pages.includes(normalized))pages.push(normalized);
  }
  return {pages:pages.slice(0,2),attempted:true,warning:''};
}
export function jwCandidates(html,env){
 const match=html.match(/var\s+jw\s*=\s*(\{[^;]*\})\s*;/s);
 if(!match)return [];
 try{const c=JSON.parse(match[1]);return [c.file].filter(u=>typeof u==='string'&&directMedia(u)&&mediaHostAllowed(u,env));}catch{return [];}
}
async function publicWatch(html,env,fetcher){
 const form=(html.match(/<form\b[^>]*id=['"]watch-\d+['"][^>]*>[\s\S]*?<\/form>/i)||[])[0];
 if(!form||!form.includes("https://farsiplex.com/play/"))return [];
 const id=(form.match(/name=['"]id['"]\s+value=['"](\d{1,9})['"]/i)||[])[1];
 if(!id)return [];
 let cookies='';
 const takeCookies=r=>{const values=r.headers.getSetCookie?.()||[r.headers.get('set-cookie')||''];const pairs=values.map(v=>v.split(';')[0]).filter(Boolean);if(pairs.length)cookies=pairs.join('; ');};
 let response=await fetcher('https://farsiplex.com/play/',{method:'POST',redirect:'manual',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'id='+id});takeCookies(response);
 for(let i=0;i<3&&response.status>=300&&response.status<400;i++){
  const next=new URL(response.headers.get('location')||'', 'https://farsiplex.com');
  if(next.protocol!=='https:'||next.hostname!=='farsiplex.com'||next.port||next.username||!/^\/(play|player|play-[a-z-]+)\/$/.test(next.pathname))return [];
  if(response.body)await response.body.cancel();
  response=await fetcher(next.href,{redirect:'manual',headers:cookies?{'Cookie':cookies}:{}});takeCookies(response);
 }
 if(response.status!==200)return [];
 const page=await response.text();if(page.length>1000000)return [];
 for(const frame of page.matchAll(/<iframe\b[^>]*src=['"]([^'"]+)['"]/gi)){
  let url;try{url=new URL(decodeEntities(frame[1]),'https://farsiplex.com');}catch{continue;}
  if(url.protocol!=='https:'||url.hostname!=='farsiplex.com'||url.port||url.username||url.pathname!=='/jwplayer/')continue;
  const doc=await fetchPublicHtml(url.href,(u,o)=>fetcher(u,{...o,headers:{...o.headers,...(cookies?{'Cookie':cookies}:{})}}));
  if(doc){const candidates=jwCandidates(doc,env);if(candidates.length)return candidates;}
 }
 return [];
}
export async function handle(request,env,fetcher=fetch){
  const requestUrl=new URL(request.url);
  if(request.method==='GET'&&requestUrl.pathname==='/health')return json({service:'Pooyan TV Resolver',ready:!!(env.TV_ACCESS_TOKEN&&env.DEEPSEEK_API_KEY)});
  if(requestUrl.pathname!=='/resolve'||request.method!=='POST')return json({error:'Not found'},404);
  if(!env.TV_ACCESS_TOKEN||!secureEquals(request.headers.get('X-Pooyan-Token'),env.TV_ACCESS_TOKEN))return json({error:'Unauthorized'},401);
  if(Number(request.headers.get('content-length')||0)>4096)return json({error:'Request too large'},413);
  let input;
  try {input=await request.json();} catch {return json({error:'Invalid JSON'},400);}
  const page=publicPage(input.pageUrl);
  if(!page||typeof input.title!=='string'||input.title.length>128)return json({error:'Invalid page/title'},400);
  const controller=new AbortController();
  const deadline=setTimeout(()=>controller.abort(),16000);
  const upstream=fetcher;fetcher=(url,options={})=>upstream(url,{...options,signal:controller.signal});
  try {
    const initialHtml=await fetchPublicHtml(page,fetcher);
    if(initialHtml===null)return json({status:'unavailable',reason:'Official public page is inaccessible or not HTML',searchUsed:false});
    for(const candidate of videoCandidates(initialHtml,page,env)){
      if(await checkMedia(candidate,env,fetcher))return json({status:'playable',mediaUrl:candidate,verified:true,searchUsed:false});
    }
    for(const candidate of await publicWatch(initialHtml,env,fetcher)){
      if(await checkMedia(candidate,env,fetcher))return json({status:"playable",mediaUrl:candidate,verified:true,searchUsed:false});
    }
    const search=await searchOfficialPages(page,input.title,env,fetcher);
    for(const alternate of search.pages){
      const html=await fetchPublicHtml(alternate,fetcher);
      if(!html)continue;
      for(const candidate of videoCandidates(html,alternate,env)){
        if(await checkMedia(candidate,env,fetcher))return json({status:'playable',mediaUrl:candidate,verified:true,searchUsed:true});
      }
    }
    return json({status:'unavailable',searchUsed:search.attempted,
      reason:search.warning||'No verified public direct media URL; an official playback integration may be required'});
  }catch(e){return json({status:'error',reason:String(e?.message||e).slice(0,180)},502);}finally{clearTimeout(deadline);}
}
export default {async fetch(request,env){return handle(request,env);}};


