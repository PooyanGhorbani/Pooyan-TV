import test from 'node:test';
import assert from 'node:assert/strict';
import {publicPage,videoCandidates,directMedia,handle} from './index.mjs';
const env={TV_ACCESS_TOKEN:'secret-access-code',DEEPSEEK_API_KEY:'dummy',MEDIA_ALLOWED_HOSTS:'cdn.example.com,farsiplex.com'};
const page='https://farsiplex.com/movie/sample/';
const request=(url=page,title='Sample',token='secret-access-code')=>new Request('https://myworker.workers.dev/resolve',{
 method:'POST',headers:{'X-Pooyan-Token':token,'Content-Type':'application/json'},body:JSON.stringify({pageUrl:url,title})});
const resp=(status,contentType,body='')=>new Response(body,{status,headers:{'Content-Type':contentType}});
test('page allowlist rejects external host, redirect tricks, userinfo, IP',()=>{
 assert.equal(publicPage(page),page);
 assert.equal(publicPage('https://farsiplex.com.evil.test/movie/sample/'),'');
 assert.equal(publicPage('https://farsiplex.com@evil.test/movie/sample/'),'');
 assert.equal(publicPage('http://farsiplex.com/movie/sample/'),'');
 assert.equal(publicPage('https://farsiplex.com/wp-admin/'),'');
});
test('only explicit HTML5 video tags and allowlisted hosts',()=>{
 const html='<iframe src="https://cdn.example.com/private.mp4"></iframe><script>url="https://cdn.example.com/script.mp4"</script><video><source src="https://cdn.example.com/good.m3u8?token=public"></video><video src="https://bad.example.com/fake.mp4"></video>';
 assert.deepEqual(videoCandidates(html,page,env),['https://cdn.example.com/good.m3u8?token=public']);
 assert.equal(directMedia('https://cdn.example.com/a.m3u8?x=1'),true);
 assert.equal(directMedia('https://cdn.example.com/watch/episode'),false);
});
test('token mandatory and forbidden alternate domain',async()=>{
 const bad=await handle(request(page,'Sample','wrong'),env,()=>{throw Error('must not call upstream')});
 assert.equal(bad.status,401);
 const bad2=await handle(request('https://evil.test/movie/a/'),env,()=>{throw Error('must not call upstream')});
 assert.equal(bad2.status,400);
});
test('verified HTML5 media produces playback candidate and does not invoke DeepSeek',async()=>{
 let search=false;
 const mock=async(url,opt)=>{
  if(url===page)return resp(200,'text/html','<html><video><source src="https://cdn.example.com/film.mp4"></video></html>');
  if(url==='https://cdn.example.com/film.mp4')return resp(206,'video/mp4','x');
  if(url.includes('api.deepseek.com'))search=true;
  throw Error('unexpected URL: '+url);
 };
 const response=await handle(request(),env,mock);
 assert.equal(response.status,200);const data=await response.json();
 assert.equal(data.status,'playable');assert.equal(data.verified,true);assert.equal(search,false);
});
test('DeepSeek cannot invent a link absent from public HTML',async()=>{
 let searched=false;
 const mock=async(url,opt)=>{
  if(url===page)return resp(200,'text/html','<html><iframe src="https://cdn.example.com/video.mp4"></iframe></html>');
  if(url==='https://api.deepseek.com/v1/tools/search'){
    searched=true;
    return resp(200,'application/json',JSON.stringify({results:[{url:'https://cdn.example.com/imaginary.mp4'},{url:'https://farsiplex.com/movie/not-the-movie/'}]}));
  }
  throw Error('unexpected '+url);
 };
 const response=await handle(request(),env,mock);const data=await response.json();
 assert.equal(data.status,'unavailable');assert.equal(data.searchUsed,true);assert.equal(searched,true);
});
test('unverified remote video is not called playable',async()=>{
 const mock=async(url,opt)=>{
  if(url===page)return resp(200,'text/html','<video src="https://cdn.example.com/movie.mp4"></video>');
  if(url==='https://cdn.example.com/movie.mp4')return resp(403,'text/html','denied');
  if(url==='https://api.deepseek.com/v1/tools/search')return resp(200,'application/json','{"results":[]}');
  throw Error('unexpected '+url);
 };
 const data=await (await handle(request(),env,mock)).json();
 assert.equal(data.status,'unavailable');
});

test('public watch form follows only same-site player to exact verified CDN',async()=>{
 const mock=async(url,opt)=>{
  if(url===page)return resp(200,'text/html',`<form id='watch-123' action='https://farsiplex.com/play/' method='post'><input name='id' value='123'/></form>`);
  if(url==='https://farsiplex.com/play/'){assert.equal(opt.method,'POST');assert.equal(opt.body,'id=123');return new Response(null,{status:302,headers:{location:'/play-online-video/'}});}
  if(url==='https://farsiplex.com/play-online-video/')return resp(200,'text/html',`<iframe src='https://farsiplex.com/jwplayer/?id=123'></iframe>`);
  if(url==='https://farsiplex.com/jwplayer/?id=123')return resp(200,'text/html','var jw = {"file":"https://pl.farsiplex.buzz/test.mp4?md5=fixture"};');
  if(url==='https://pl.farsiplex.buzz/test.mp4?md5=fixture')return resp(206,'video/mp4','x');
  throw Error('unexpected route');
 };
 const data=await (await handle(request(),{TV_ACCESS_TOKEN:env.TV_ACCESS_TOKEN},mock)).json();
 assert.equal(data.status,'playable');assert.equal(data.searchUsed,false);
});
