const {chromium}=require('playwright');
const http=require('http'),fs=require('fs'),path=require('path'),assert=require('assert');
const assetRoot=path.resolve(__dirname,'../app/src/main/assets');
const server=http.createServer((req,res)=>{let p=path.join(assetRoot,req.url==='/'?'index.html':req.url.split('?')[0]);try{res.setHeader('Content-Type',p.endsWith('.js')?'application/javascript':p.endsWith('.css')?'text/css':p.endsWith('.json')?'application/json':'text/html');res.end(fs.readFileSync(p));}catch(e){res.statusCode=404;res.end();}});
server.listen(0,'127.0.0.1',async()=>{
 let browser;try{
  browser=await chromium.launch({executablePath:process.env.ABIDE_CHROME||'/tmp/abide-chromium',headless:true,args:['--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--no-zygote','--single-process']});
  const page=await browser.newPage({viewport:{width:412,height:820},deviceScaleFactor:1,isMobile:true,hasTouch:true});
  const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>{
   window.testState={readMs:0,goalMinutes:5,nextGoal:5,ready:false,unlocked:false,resetMs:21000000,overrideAvailable:true,overrideWaitMs:0,guardEnabled:false,guardConnected:false,book:0,chapter:0,verse:0,font:21,holdMs:0};
   window.lastOpened='';window.Abide={state:()=>JSON.stringify(testState),page:p=>{testState.activeTimer=p==='reader';},position:(b,c,v)=>Object.assign(testState,{book:b,chapter:c,verse:v}),font:n=>testState.font=n,goal:n=>{testState.nextGoal=n;testState.goalMinutes=n;},cancelHold:()=>testState.holdMs=0,beginHold:()=>{},unlock:()=>{if(testState.ready){testState.unlocked=true;setTimeout(()=>unlockedNow(),0)}},openSocial:p=>window.lastOpened=p,enableGuard:()=>{},notifications:()=>{},importBible:()=>{},restoreBible:()=>{}};
  });
  const base='http://127.0.0.1:'+server.address().port;
  await page.goto(base);await page.getByRole('button',{name:'Read the Bible',exact:false}).waitFor();
  assert((await page.locator('.reference').first().innerText()).includes('PHILIPPIANS 4:8'));
  assert((await page.locator('.reference').first().innerText()).includes('WEB'));
  assert(await page.getByText('NIV is not bundled.',{exact:false}).count());
  await page.screenshot({path:path.resolve(__dirname,'../build/qa-home.png'),fullPage:true});
  await page.getByRole('button',{name:'Read the Bible',exact:false}).click();
  await page.locator('.scripture').waitFor();
  assert.equal(await page.locator('.scripture p').count(),31);
  assert((await page.locator('.scripture p').first().innerText()).includes('In the beginning, God created'));
  await page.screenshot({path:path.resolve(__dirname,'../build/qa-reader.png'),fullPage:false});
  await page.getByRole('button',{name:'Larger text'}).click();
  assert.equal(await page.evaluate(()=>testState.font),23);
  await page.getByRole('button',{name:'Next chapter'}).click();
  assert((await page.locator('h2').innerText()).includes('Genesis 2'));
  await page.getByRole('button',{name:'Choose book and chapter'}).click();
  await page.locator('#book-select').selectOption('65');
  await page.locator('#chapter-select').selectOption('21');
  await page.getByRole('button',{name:'Read this chapter'}).click();
  assert.equal(await page.locator('.scripture p').count(),21);
  await page.getByRole('button',{name:'Next chapter'}).click();
  assert((await page.locator('#toast').innerText()).includes('end of Revelation'));
  await page.evaluate(()=>{show('release');});
  assert.equal(await page.evaluate(()=>current),'home');
  await page.evaluate(()=>{testState.ready=true;testState.readMs=300000;readingComplete();});
  assert((await page.locator('.reference').innerText()).includes('JOHN 8:32'));
  await page.getByRole('button',{name:'Unlock social apps',exact:true}).click();
  await page.getByRole('button',{name:'Open Facebook',exact:true}).waitFor();
  await page.getByRole('button',{name:'Open Instagram',exact:true}).click();
  assert.equal(await page.evaluate(()=>lastOpened),'com.instagram.android');
  await page.screenshot({path:path.resolve(__dirname,'../build/qa-release.png'),fullPage:true});
  await page.evaluate(()=>{testState.ready=false;testState.unlocked=false;testState.readMs=0;cycleReset();});
  assert.equal(await page.evaluate(()=>current),'home');
  await page.evaluate(()=>show('override'));
  await page.screenshot({path:path.resolve(__dirname,'../build/qa-override.png'),fullPage:true});
  assert(await page.locator('#hold').isEnabled());
  await page.evaluate(()=>{testState.overrideAvailable=false;show('override')});
  assert(await page.locator('#hold').isDisabled());
  await page.getByRole('button',{name:'Rhythm',exact:false}).click();
  await page.getByRole('button',{name:'10 minutes',exact:true}).click();
  await page.waitForTimeout(200);assert.equal(await page.evaluate(()=>testState.goalMinutes),10);
  assert(await page.getByText('NIV is not included.',{exact:false}).count());
  await page.screenshot({path:path.resolve(__dirname,'../build/qa-settings.png'),fullPage:true});
  await page.setViewportSize({width:320,height:640});
  for(const p of ['home','reader','settings','override','browse']){
   await page.evaluate(p=>show(p),p);await page.waitForTimeout(100);
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),'horizontal overflow on '+p);
  }
  assert.deepEqual(errors,[]);console.log('PASS: reader navigation, complete Bible access, saved position, text sizing, gate/release flows, stale unlock, grace state, duration choice, and 320px layout. No JS errors.');
 }catch(e){console.error(e);process.exitCode=1;}finally{if(browser)await browser.close();server.close();}
});
