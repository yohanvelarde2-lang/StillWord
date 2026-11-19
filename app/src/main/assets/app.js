'use strict';
let bible=null, state={}, current='home', pendingRoute='home', book=0, chapter=0, verse=0, font=21, holding=false;
const $=id=>document.getElementById(id), main=$('main');
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
function native(method,...args){if(window.Abide&&typeof Abide[method]==='function')return Abide[method](...args);}
function refresh(){try{state=JSON.parse(native('state')||'{}');}catch(e){state={};}return state;}
function clock(ms){let s=Math.max(0,Math.ceil(ms/1000));return Math.floor(s/60)+':'+String(s%60).padStart(2,'0');}
function hours(ms){let m=Math.ceil(Math.max(0,ms)/60000);return Math.floor(m/60)+'h '+m%60+'m';}
function goal(){return (state.goalMinutes||5)*60000;}
function verseText(id,c,n){const b=bible.books.find(b=>b.id===id);return b.chapters[c-1].find(v=>v.n===String(n)).t;}
const arch='<div class="arch" aria-hidden="true"><svg viewBox="0 0 70 95"><path d="M35 91V37M35 76C14 77 10 62 12 54C27 53 36 61 35 76ZM35 60C55 60 63 45 59 36C44 37 34 45 35 60ZM35 43C18 43 13 31 15 24C28 24 36 32 35 43Z"/></svg></div>';
function tip(){return '<div class="card tip"><span class="icon" aria-hidden="true">✎</span><p>Grab your devotional notebook and pen now and start reading the bible</p></div>';}
function verseCard(id,ch,n,ref){return `<p class="verse">“${esc(verseText(id,ch,n))}”</p><p class="reference">${ref} · ${esc(bible.abbreviation)}</p>`;}
function progress(){return `<div class="row"><div><p class="eyebrow" style="margin:0 0 5px">YOUR QUIET MOMENT</p><span class="time" id="read-time">${clock(state.readMs||0)}</span><span class="mini muted"> / ${state.goalMinutes||5} min</span></div><span class="badge" id="status-badge">${state.unlocked?'SOCIAL APPS OPEN':state.ready?'READY TO UNLOCK':'WORD FIRST'}</span></div><div class="progress"><span id="progress-fill" style="width:${Math.min(100,(state.readMs||0)/goal()*100)}%"></span></div><div class="row tiny"><span>New reading window in</span><span id="reset-time">${hours(state.resetMs||0)}</span></div>`;}
function bookActions(){return '<div class="stack section"><button class="secondary" data-action="kindle">Read from Kindle</button><button class="secondary" data-action="book-import">Upload a book</button><button class="secondary" data-action="book-library">Download Library</button></div>';}
function renderHome(){
 let unlocked=state.unlocked,ready=state.ready;
 main.innerHTML=`<p class="eyebrow">A DAILY RHYTHM OF ABIDING</p><section class="hero">${arch}<h1>A little Word.<br>A quieter world.</h1><p class="intro">Make room for what matters<br>before the world gets loud.</p><div class="clear"></div></section>
 <div class="card white">${progress()}</div>
 ${!state.guardEnabled?'<div class="notice"><strong>Social Guard is not enabled.</strong><br>Read freely now, or enable blocking in Rhythm.</div>':''}
 ${unlocked?`<section class="section"><h2>${ready?'Carry the Word with you.':'Grace for this moment.'}</h2><p class="muted">Facebook and Instagram are available until the next six-hour reset.</p><div class="stack"><button class="primary" data-action="release">Open social apps</button><button class="secondary" data-action="reader">Read the Bible</button></div>${bookActions()}</section>`:ready?`<section class="section"><h2>Your quiet moment is complete.</h2><p class="muted">Take the truth you have read into the rest of your day.</p><button class="primary" data-action="release">View John 8:32 & unlock</button><div class="stack section"><button class="secondary" data-action="reader">Read the Bible</button></div>${bookActions()}</section>`:`${verseCard('PHP',4,8,'Philippians 4:8')}${tip()}<button class="primary" data-action="reader">Read the Bible <span aria-hidden="true">→</span></button>${bookActions()}<div class="apps"><span>Facebook paused</span><span>Instagram paused</span><span>Messenger available</span></div><p class="tiny">${state.goalMinutes||5} minutes of attentive reading · resets every 6 hours</p><button class="text-button" data-action="override">Need a grace pass?</button>`}
 <div class="section"><p class="tiny">${esc(bible.translation)} · complete offline edition<br>${bible.abbreviation==='WEB'?'NIV is not bundled. An authorized offline edition can be imported in Rhythm.':''}</p></div>`;
}
function renderRelease(){
 if(!state.ready&&!state.unlocked){show('home');toast('A new reading window has begun. Read your next quiet moment first.');return;}
 main.innerHTML=`<p class="eyebrow">${state.ready?'YOUR QUIET MOMENT IS COMPLETE':'GRACE PASS ACTIVE'}</p><section class="hero">${arch}<h1>Walk in<br>the truth.</h1><div class="clear"></div></section>${verseCard('JHN',8,32,'John 8:32')}<div class="divider"></div><p class="intro">Let His Word guide what you see, share, and carry into the world.</p><div class="stack" id="release-actions">${state.unlocked?socialButtons():'<button class="primary" data-action="unlock">Unlock social apps</button><button class="secondary" data-action="reader">Keep reading</button>'}</div><p class="tiny section">${state.unlocked?'Access is open':'Unlock grants access'} until the next six-hour reset.<br>Next reset in <span id="reset-time">${hours(state.resetMs)}</span>. Messenger is always available.</p>`;
}
function socialButtons(){return '<button class="primary" data-action="facebook">Open Facebook</button><button class="secondary" data-action="instagram">Open Instagram</button><button class="text-button" data-action="reader">Return to the Word</button>';}
function renderReader(){
 const b=bible.books[book], vs=b.chapters[chapter];
 main.innerHTML=`<div class="reader-controls"><div class="row"><div><p class="eyebrow">${esc(bible.abbreviation)} · THE HOLY BIBLE</p><h2>${esc(b.name)} ${chapter+1}</h2></div><div class="font-controls"><button data-action="smaller" aria-label="Smaller text">A−</button><button data-action="larger" aria-label="Larger text">A+</button><button data-action="browse" aria-label="Choose book and chapter">☷</button></div></div><div class="timerline"><span id="reading-caption">Reading time · ${clock(state.readMs||0)} / ${state.goalMinutes||5}:00</span><span id="reader-status">${state.unlocked?'Access open':'Read & reflect'}</span></div><div class="progress"><span id="progress-fill" style="width:${Math.min(100,(state.readMs||0)/goal()*100)}%"></span></div></div><article class="scripture">${vs.map((v,i)=>`<p id="v${i}" data-verse="${i}"><sup>${esc(v.n)}</sup>${esc(v.t)}</p>`).join('')}</article><div class="reader-footer"><div class="row"><button class="secondary" data-action="previous">← Previous</button><button class="primary" data-action="next">Next chapter →</button></div><p class="tiny section">Your place is saved automatically. Timer pauses outside this reader, while the screen is off, or after 90 seconds without a tap or scroll.</p><p class="tiny section">${esc(bible.copyright)}</p></div>`;
 document.documentElement.style.setProperty('--reading',font+'px');
 requestAnimationFrame(()=>{if(verse>0){const el=$('v'+verse);if(el)window.scrollTo(0,el.getBoundingClientRect().top+window.scrollY-137);}native('page','reader');});
}
function renderBrowse(){
 main.innerHTML=`<p class="eyebrow">THE HOLY BIBLE · ${esc(bible.abbreviation)}</p><h1>Find your place.</h1><p class="intro">Begin at Genesis, or return to a passage on your heart.</p><label class="setting-title" for="book-select">Book</label><div class="chapter-select"><select id="book-select">${bible.books.map((b,i)=>`<option value="${i}" ${i===book?'selected':''}>${esc(b.name)}</option>`).join('')}</select><select id="chapter-select" aria-label="Chapter"></select></div><button class="primary section" data-action="go-chapter">Read this chapter</button><button class="text-button" data-action="reader">Back to reading</button>`;
 $('book-select').addEventListener('change',()=>fillChapters(0));fillChapters(chapter);
}
function fillChapters(selected){const b=bible.books[+$('book-select').value];$('chapter-select').innerHTML=b.chapters.map((_,i)=>`<option value="${i}" ${i===selected?'selected':''}>Chapter ${i+1}</option>`).join('');}
function renderSettings(){
 const next=state.nextGoal||state.goalMinutes||5;
 main.innerHTML=`<p class="eyebrow">MAKE ROOM FOR THE WORD</p><h1>Your rhythm.</h1><p class="intro">A gentle boundary. A deliberate beginning.<br>Four reading windows in every 24 hours.</p>
 <div class="card white"><div class="row"><h3>Social Guard</h3><span class="badge" id="guard-status">${state.guardConnected?'CONNECTED':state.guardEnabled?'ENABLED':'OFF'}</span></div><p class="mini muted" style="margin:13px 0">Blocks the installed Facebook, Facebook Lite, Instagram and Instagram Lite apps. Messenger remains available. Browser websites and other apps are outside this guard.</p><button class="secondary" data-action="guard">${state.guardEnabled?'Manage Android service':'Enable Social Guard'}</button><p class="tiny" style="margin-top:12px">Android may ask you to allow restricted settings for this installed APK. See the setup guide. Blocking requires this service to remain enabled.</p></div>
 <section class="section"><p class="setting-title">Time in the Word</p><div class="select-goal"><button class="${next===5?'selected':''}" data-action="goal5">5 minutes</button><button class="${next===10?'selected':''}" data-action="goal10">10 minutes</button></div><p class="tiny">Changes apply to an untouched window, or at the next reset. The six-hour schedule starts from your first launch. Your Bible place continues across resets.</p></section>
 <div class="divider"></div><section><h3>Reading milestones</h3><p class="mini muted" style="margin:12px 0">Receive “You can now open Facebook or Instagram” when your goal is complete. Tap it for John 8:32 and the Unlock button.</p><button class="secondary" data-action="notifications">Allow notifications</button></section>
 <div class="divider"></div><section><h3>Your offline Bible</h3><p class="mini muted" style="margin:12px 0"><strong>${esc(bible.translation)} (${esc(bible.abbreviation)})</strong><br>66 books · 1,189 chapters · no connection needed</p><p class="tiny rights">${esc(bible.copyright)}</p><div class="notice">NIV is not included. Public distribution of the full NIV requires permission from Biblica, including offline rights. Import only a complete, authorized legacy ABIDE Bible pack. Importing a file does not grant a license.</div><div class="stack"><button class="secondary" data-action="import">Import an authorized Bible pack</button>${bible.abbreviation!=='WEB'?'<button class="text-button" data-action="restore">Return to World English Bible</button>':''}</div></section>
 <div class="divider"></div><section><h3>A little grace.</h3><p class="mini muted" style="margin:12px 0">One grace pass per rolling 24 hours. Hold continuously for 45 seconds to open social apps until the next six-hour reset.</p><button class="secondary" data-action="override">${state.overrideAvailable?'Open grace pass':'Grace pass used'}</button></section>
 <div class="divider"></div><h3>Quiet by design.</h3><p class="tiny section">StillWord 0.2.2 · Personal testing build<br>No account. No ads. No analytics. No internet permission. Reading progress, settings and imported text stay on your device. Android Accessibility observes only foreground app package names; screen content is not accessed. Uninstalling removes local data.<br><br>This is a voluntary focus aid. Android settings, force stop, uninstalling, other profiles and browser versions can bypass it. You remain in control of your device.<br><br>StillWord — Word Before the World<br>Created for Johnny sensei and the community he serves.<br></p>`;
}
function renderOverride(){
 main.innerHTML=`<p class="eyebrow">ONE MOMENT OF GRACE</p><section class="hero">${arch}<h1>Pause.<br>Choose with care.</h1><div class="clear"></div></section><p class="intro">A grace pass opens Facebook and Instagram for the rest of this reading window. It is available once in any 24 hours.</p><div class="card white"><p class="mini muted">Next reading reset in <strong id="reset-time">${hours(state.resetMs)}</strong>. Reading progress is kept.</p></div><button id="hold" class="hold" ${state.overrideAvailable?'':'disabled'}><span class="fill" id="hold-fill"></span><b id="hold-label">${state.overrideAvailable?'Touch and hold for 45 seconds':'Grace pass already used'}</b><small id="hold-sub">${state.overrideAvailable?'Keep your finger here continuously':'Available again in '+hours(state.overrideWaitMs)}</small></button><p class="tiny section">Lifting your finger, leaving the app, losing focus, or turning off the screen cancels the hold. A new reading cycle also cancels it.</p><button class="text-button section" data-action="home">Return to my quiet place</button>`;
 const el=$('hold');
 el.addEventListener('pointerdown',e=>{if(!state.overrideAvailable)return;e.preventDefault();holding=true;el.setPointerCapture(e.pointerId);native('beginHold');});
 const cancel=()=>{holding=false;native('cancelHold');};
 el.addEventListener('pointerup',cancel);el.addEventListener('pointercancel',cancel);el.addEventListener('lostpointercapture',cancel);
 el.addEventListener('pointermove',e=>{const r=el.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)cancel();});
 el.addEventListener('contextmenu',e=>e.preventDefault());
}
function show(page){
 if(!bible){pendingRoute=page;return;}refresh();holding=false;native('cancelHold');native('page','');current=page;
 document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('active',b.id==='nav-'+(page==='settings'?'settings':['reader','browse'].includes(page)?'reader':'home')));
 main.className=page==='reader'?'reader-main':'';window.scrollTo(0,0);
 const render={home:renderHome,reader:renderReader,release:renderRelease,settings:renderSettings,override:renderOverride,browse:renderBrowse}[page]||renderHome;render();
}
function nativeRoute(p){pendingRoute=p;if(bible)show(p);}
function readingComplete(){refresh();show('release');toast('Your reading goal is complete. You can now unlock Facebook or Instagram.');}
function unlockedNow(){refresh();if(current==='release')renderRelease();else show('release');toast('Social apps are open until the next reading reset.');}
function overrideComplete(){holding=false;refresh();show('release');toast('Grace pass active until the next six-hour reset.');}
function cycleReset(){refresh();if(current!=='reader'&&current!=='browse')show('home');else toast('A new six-hour reading window has begun. Your reading place is kept.');}
function backPage(){if(current==='home')toast('Use your Android Home gesture to leave StillWord.');else show('home');}
function updateNative(){
 if(!bible)return;const old=state;refresh();
 if(old.unlocked&&!state.unlocked&&current==='release'){show('home');return;}
 if($('read-time'))$('read-time').textContent=clock(state.readMs||0);
 if($('progress-fill'))$('progress-fill').style.width=Math.min(100,(state.readMs||0)/goal()*100)+'%';
 if($('reset-time'))$('reset-time').textContent=hours(state.resetMs||0);
 if($('reading-caption'))$('reading-caption').textContent='Reading time · '+clock(state.readMs||0)+' / '+(state.goalMinutes||5)+':00';
 if($('reader-status'))$('reader-status').textContent=state.unlocked?'Access open':state.ready?'Goal complete':state.activeTimer?'Read & reflect':'Timer paused · tap or scroll';
 if($('hold-fill'))$('hold-fill').style.width=Math.min(100,(state.holdMs||0)/45000*100)+'%';
 if($('hold-label')&&state.overrideAvailable)$('hold-label').textContent=state.holdMs>0?'Keep holding · '+Math.ceil((45000-state.holdMs)/1000)+' seconds':'Touch and hold for 45 seconds';
 if($('guard-status'))$('guard-status').textContent=state.guardConnected?'CONNECTED':state.guardEnabled?'ENABLED':'OFF';
}
let toastTimer;function toast(s){$('toast').textContent=s;$('toast').style.display='block';clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').style.display='none',5000);}
function remember(){native('position',book,chapter,verse);}
function changeChapter(direction){
 let b=book,c=chapter+direction;if(c<0){if(b===0)return;b--;c=bible.books[b].chapters.length-1;}
 if(c>=bible.books[b].chapters.length){if(b===65){toast('You have reached the end of Revelation. Return to Genesis when you are ready.');return;}b++;c=0;}
 book=b;chapter=c;verse=0;remember();show('reader');
}
document.addEventListener('click',e=>{
 const el=e.target.closest('[data-action]');if(!el)return;const a=el.dataset.action;
 if(['home','reader','settings','release','override','browse'].includes(a)){show(a);return;}
 if(a==='unlock')native('unlock');if(a==='facebook')native('openSocial','com.facebook.katana');if(a==='instagram')native('openSocial','com.instagram.android');
 if(a==='kindle'||a==='book-import'||a==='book-library')native('readingMode',a);
 if(a==='guard')native('enableGuard');if(a==='notifications')native('notifications');if(a==='import')native('importBible');if(a==='restore')native('restoreBible');
 if(a==='goal5'||a==='goal10'){native('goal',a==='goal5'?5:10);setTimeout(()=>show('settings'),100);}
 if(a==='previous')changeChapter(-1);if(a==='next')changeChapter(1);
 if(a==='go-chapter'){book=+$('book-select').value;chapter=+$('chapter-select').value;verse=0;remember();show('reader');}
 if(a==='smaller'||a==='larger'){font=Math.min(29,Math.max(17,font+(a==='smaller'?-2:2)));document.documentElement.style.setProperty('--reading',font+'px');native('font',font);}
});
let scrollTimer;window.addEventListener('scroll',()=>{if(current!=='reader')return;clearTimeout(scrollTimer);scrollTimer=setTimeout(()=>{const nodes=document.querySelectorAll('[data-verse]');let best=0;for(const n of nodes){if(n.getBoundingClientRect().top<190)best=+n.dataset.verse;else break;}verse=best;remember();},200);},{passive:true});
window.addEventListener('blur',()=>{holding=false;native('cancelHold');});
document.addEventListener('visibilitychange',()=>{if(document.hidden){holding=false;native('cancelHold');}});
fetch('bible.json').then(r=>{if(!r.ok)throw Error('Bible unavailable');return r.json();}).then(data=>{
 bible=data;refresh();book=Math.max(0,Math.min(65,state.book||0));chapter=Math.max(0,Math.min(bible.books[book].chapters.length-1,state.chapter||0));verse=state.verse||0;font=state.font||21;show(pendingRoute);setInterval(updateNative,1000);
}).catch(()=>{main.innerHTML='<h1>Your Bible could not be opened.</h1><p class="intro">Close and reopen StillWord. Social blocking can be disabled in Android Accessibility settings if needed.</p>';});
