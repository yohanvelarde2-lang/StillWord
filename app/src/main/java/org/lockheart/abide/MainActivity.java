package org.lockheart.abide;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;
import android.widget.FrameLayout;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class MainActivity extends Activity {
  WebView web;Store store;Bible bible;Handler h=new Handler(Looper.getMainLooper());
  boolean resumed=false,focused=false,reader=false,loaded=false;String route="home";
  long lastTick,lastInput=0,holdStart=-1,holdCycle=-1,lastSave=0,lastUi=0;
  // Battery: the tick runs only while this screen is in front, and progress is written at most every 5 seconds.
  static final long SAVE_EVERY=5000,UI_EVERY=1000;
  @Override public void onCreate(Bundle b){super.onCreate(b);store=Store.get(this);bible=Bible.get(this);
    route=getIntent().getStringExtra("page");if(route==null)route="home";
    getWindow().setStatusBarColor(Color.parseColor("#F6F4EC"));getWindow().setNavigationBarColor(Color.parseColor("#F6F4EC"));
    web=new WebView(this);web.setBackgroundColor(Color.parseColor("#F6F4EC"));
    web.getSettings().setJavaScriptEnabled(true);web.getSettings().setDomStorageEnabled(false);
    web.getSettings().setAllowFileAccess(false);web.getSettings().setAllowContentAccess(false);web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
    web.addJavascriptInterface(new Bridge(),"Abide");
    web.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest req){
        Uri u=req.getUrl();if(!"https".equals(u.getScheme())||!"abide.local".equals(u.getHost()))return denied();
        try{
          String p=u.getPath();if("/bible.json".equals(p))return new WebResourceResponse("application/json","UTF-8",new ByteArrayInputStream(bible.active.toString().getBytes(StandardCharsets.UTF_8)));
          if(!"/index.html".equals(p)&&!"/app.js".equals(p)&&!"/style.css".equals(p))return denied();
          String mime=p.endsWith("js")?"application/javascript":p.endsWith("css")?"text/css":"text/html";
          return new WebResourceResponse(mime,"UTF-8",getAssets().open(p.substring(1)));
        }catch(Exception e){return denied();}
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"abide.local".equals(r.getUrl().getHost());}
      @Override public void onPageFinished(WebView v,String url){loaded=true;send("nativeRoute("+JSONObject.quote(route)+")");}
    });
    FrameLayout container=new FrameLayout(this);container.setBackgroundColor(Color.parseColor("#F6F4EC"));container.addView(web,new FrameLayout.LayoutParams(-1,-1));setContentView(container);
    if(Build.VERSION.SDK_INT>=35)container.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets a=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(a.left,a.top,a.right,a.bottom);return insets;});
    web.loadUrl("https://abide.local/index.html");
    NotificationManager nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("reading","Reading milestones",NotificationManager.IMPORTANCE_DEFAULT));
  }
  WebResourceResponse denied(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
  void send(String js){if(web!=null&&loaded)web.evaluateJavascript(js,null);}
  @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);route=i.getStringExtra("page");if(route==null)route="home";reader=false;holdStart=-1;send("nativeRoute("+JSONObject.quote(route)+")");}
  @Override protected void onResume(){super.onResume();resumed=true;lastTick=SystemClock.elapsedRealtime();lastInput=lastTick;lastUi=0;
    if(web!=null){web.onResume();web.resumeTimers();}
    h.removeCallbacks(tick);h.post(tick);
    if(GuardService.instance!=null)GuardService.instance.requestCheck();}
  @Override protected void onPause(){resumed=false;holdStart=-1;h.removeCallbacks(tick);store.save();
    // Stop the page's 1-second refresh while StillWord is in the background.
    if(web!=null){web.onPause();web.pauseTimers();}
    super.onPause();}
  @Override public void onWindowFocusChanged(boolean f){super.onWindowFocusChanged(f);focused=f;lastTick=SystemClock.elapsedRealtime();if(!f)holdStart=-1;}
  @Override public boolean dispatchTouchEvent(MotionEvent e){lastInput=SystemClock.elapsedRealtime();return super.dispatchTouchEvent(e);}
  @Override protected void onDestroy(){h.removeCallbacksAndMessages(null);if(web!=null){web.removeJavascriptInterface("Abide");web.destroy();web=null;}super.onDestroy();}
  @Override public void onBackPressed(){reader=false;holdStart=-1;send("backPage()");}
  final Runnable tick=new Runnable(){public void run(){
    long uptime=SystemClock.elapsedRealtime(),now=System.currentTimeMillis(),delta=Math.min(1500,Math.max(0,uptime-lastTick));lastTick=uptime;
    boolean reset=store.refresh();if(reset){holdStart=-1;getSystemService(NotificationManager.class).cancel(8);send("cycleReset()");}
    boolean interactive=((PowerManager)getSystemService(POWER_SERVICE)).isInteractive()&&!((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked();
    if(reader&&resumed&&focused&&interactive&&uptime-lastInput<90000&&!reset){
      if(store.policy.credit(delta,now)){store.save();notifyReady(MainActivity.this);reader=false;send("readingComplete()");}
    }
    if(holdStart>=0){
      if(!resumed||!focused||!interactive||holdCycle!=store.policy.cycleStart)holdStart=-1;
      else if(uptime-holdStart>=45000){store.policy.override(now);store.save();holdStart=-1;send("overrideComplete()");}
    }
    if(uptime-lastSave>=SAVE_EVERY){store.save();lastSave=uptime;}
    if(uptime-lastUi>=UI_EVERY){send("updateNative()");lastUi=uptime;}
    h.postDelayed(this,250);
  }};
  static void notifyReady(Context context){
    if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
    Intent i=new Intent(context,MainActivity.class).putExtra("page","release");
    PendingIntent pi=PendingIntent.getActivity(context,8,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification n=new Notification.Builder(context,"reading").setSmallIcon(org.lockheart.abide.R.drawable.ic_notification).setContentTitle("You can now open Facebook or Instagram")
      .setContentText("Your reading goal is complete. Tap for John 8:32 and Unlock.").setContentIntent(pi).setAutoCancel(true).build();
    context.getSystemService(NotificationManager.class).notify(8,n);
  }
  void launch(String pkg){
    if(!GuardService.target(pkg)||!store.allowed())return;
    Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
    if(i==null){String alt=pkg.contains("instagram")?"com.instagram.lite":"com.facebook.lite";i=getPackageManager().getLaunchIntentForPackage(alt);}
    if(i!=null){reader=false;startActivity(i);}else Toast.makeText(this,"This social app is not installed",Toast.LENGTH_LONG).show();
  }
  void disclosure(){reader=false;holdStart=-1;
    new AlertDialog.Builder(this).setTitle("Enable StillWord Social Guard?")
      .setMessage("Android Accessibility grants StillWord access to app windows. It uses that access only to identify the foreground app by package name. It places a Bible reminder over Facebook and Instagram until your reading goal is complete. Messenger is not blocked.\n\nIt does not read screen content, messages, passwords or keystrokes, and does not collect or share this activity. Everything stays on your phone.\n\nYou can turn it off at any time in Android Accessibility settings. This is a voluntary focus aid, not an unbreakable lock.")
      .setNegativeButton("Not now",(d,w)->{}).setPositiveButton("I agree · open settings",(d,w)->{store.prefs.edit().putBoolean("consent",true).commit();startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}).show();
  }
  final class Bridge {
    @JavascriptInterface public String state(){
      JSONObject s=store.json(MainActivity.this);try{s.put("route",route);s.put("activeTimer",reader&&resumed&&focused&&SystemClock.elapsedRealtime()-lastInput<90000);
        s.put("holdMs",holdStart<0?0:SystemClock.elapsedRealtime()-holdStart);s.put("nextGoal",store.prefs.getInt("nextGoal",store.policy.goalMinutes));}catch(Exception ignored){}return s.toString();
    }
    @JavascriptInterface public void page(String p){runOnUiThread(()->{reader="reader".equals(p);holdStart=-1;lastInput=SystemClock.elapsedRealtime();lastTick=lastInput;});}
    @JavascriptInterface public void position(int book,int chapter,int verse){runOnUiThread(()->{
      try{JSONArray bs=bible.active.getJSONArray("books");if(book<0||book>=bs.length())return;JSONArray cs=bs.getJSONObject(book).getJSONArray("chapters");if(chapter<0||chapter>=cs.length()||verse<0||verse>=cs.getJSONArray(chapter).length())return;
        store.prefs.edit().putInt("book",book).putInt("chapter",chapter).putInt("verse",verse).apply();}catch(Exception ignored){}
    });}
    @JavascriptInterface public void font(int size){if(size>=17&&size<=29)store.prefs.edit().putInt("font",size).apply();}
    @JavascriptInterface public void goal(int minutes){if(minutes==5||minutes==10)runOnUiThread(()->{store.prefs.edit().putInt("nextGoal",minutes).commit();
      if(store.policy.readMs==0&&!store.policy.unlocked){store.policy.goalMinutes=minutes;store.save();}send("updateNative()");});}
    @JavascriptInterface public void enableGuard(){runOnUiThread(()->disclosure());}
    @JavascriptInterface public void notifications(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},7);else Toast.makeText(MainActivity.this,"Notifications are available in Android app settings",Toast.LENGTH_SHORT).show();});}
    @JavascriptInterface public void unlock(){runOnUiThread(()->{if(store.policy.unlock(System.currentTimeMillis())){store.save();send("unlockedNow()");}else send("cycleReset()");});}
    @JavascriptInterface public void openSocial(String pkg){runOnUiThread(()->launch(pkg));}
    @JavascriptInterface public void beginHold(){runOnUiThread(()->{if(resumed&&focused&&store.policy.canOverride(System.currentTimeMillis())){holdStart=SystemClock.elapsedRealtime();holdCycle=store.policy.cycleStart;}});}
    @JavascriptInterface public void cancelHold(){runOnUiThread(()->holdStart=-1);}
    @JavascriptInterface public void readingMode(String mode){runOnUiThread(()->{
      if(ReadingActivity.KINDLE.equals(mode)||ReadingActivity.IMPORT.equals(mode)||ReadingActivity.LIBRARY.equals(mode)){
        reader=false;startActivity(ReadingActivity.intent(MainActivity.this,mode));
      }
    });}
    @JavascriptInterface public void importBible(){runOnUiThread(()->{
      reader=false;new AlertDialog.Builder(MainActivity.this).setTitle("Import an authorized Bible")
       .setMessage("Import a complete ABIDE Bible JSON pack only if its license permits offline use in this app. Purchasing another Bible app does not grant redistribution rights. NIV is not included in this build.\n\nPublic NIV distribution needs permission from Biblica. Imported text stays on this device.")
       .setNegativeButton("Cancel",null).setPositiveButton("I have permission · choose file",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,21);}).show();
    });}
    @JavascriptInterface public void restoreBible(){runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Return to the World English Bible?").setMessage("This removes the imported Bible pack. Your reading place is kept.").setNegativeButton("Cancel",null).setPositiveButton("Restore WEB",(d,w)->{bible.restore();web.reload();}).show());}
  }
  @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(req==21&&res==RESULT_OK&&data!=null){
    Uri uri=data.getData();new Thread(()->{try{String raw=Bible.read(getContentResolver().openInputStream(uri));bible.install(raw);runOnUiThread(()->{Toast.makeText(this,"Authorized Bible saved for offline reading",Toast.LENGTH_LONG).show();web.reload();});}
      catch(Exception e){runOnUiThread(()->new AlertDialog.Builder(this).setTitle("Bible pack could not be imported").setMessage("Your current Bible is unchanged. "+e.getMessage()).setPositiveButton("OK",null).show());}}).start();
  }}
}
