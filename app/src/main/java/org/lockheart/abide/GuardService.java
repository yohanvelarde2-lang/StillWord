package org.lockheart.abide;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.*;

public final class GuardService extends AccessibilityService {
  static GuardService instance;
  static final String[] TARGETS={"com.facebook.katana","com.facebook.lite","com.instagram.android","com.instagram.lite"};
  final Handler handler=new Handler(Looper.getMainLooper());
  WindowManager wm;View overlay;String foreground="";Button holdButton;long holdStart=-1,holdCycle=-1;
  // Battery: no constant polling. Each Android window event (or screen on/off/unlock) starts a short
  // burst of checks, because Recents can resume a task before the window list settles.
  // Timed checks also cover a six-hour reset, or a grace pass becoming available, while an app stays open.
  static final long BURST_MS=3000,BURST_STEP=250,MAX_WAIT=60*60*1000L;
  long burstUntil;
  final Runnable burst=new Runnable(){public void run(){
    reconcile();
    if(SystemClock.uptimeMillis()<burstUntil)handler.postDelayed(this,BURST_STEP);
  }};
  final Runnable deadline=this::requestCheck;
  final BroadcastReceiver screen=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){requestCheck();}};
  boolean screenRegistered;
  static boolean target(String p){for(String t:TARGETS)if(t.equals(p))return true;return false;}
  static boolean isEnabled(Context c){String e=Settings.Secure.getString(c.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
    return e!=null&&e.toLowerCase(java.util.Locale.ROOT).contains(new ComponentName(c,GuardService.class).flattenToString().toLowerCase(java.util.Locale.ROOT));}
  @Override protected void onServiceConnected(){
    instance=this;wm=(WindowManager)getSystemService(WINDOW_SERVICE);
    if(!screenRegistered){
      IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_USER_PRESENT);
      registerReceiver(screen,f);screenRegistered=true;
    }
    requestCheck();
  }
  @Override public void onAccessibilityEvent(AccessibilityEvent e){requestCheck();}
  void requestCheck(){
    burstUntil=SystemClock.uptimeMillis()+BURST_MS;
    handler.removeCallbacks(burst);handler.post(burst);
  }
  /** While a guarded app stays in front, wake up only when its access state can change. */
  private void scheduleDeadline(Store state){
    handler.removeCallbacks(deadline);
    if(!target(foreground))return;
    long now=System.currentTimeMillis(),wait;
    if(state.allowed())wait=state.policy.remaining(now);
    else if(!state.policy.canOverride(now))wait=Math.max(0,state.policy.lastOverride+Policy.DAY-now);
    else return;
    handler.postDelayed(deadline,Math.min(MAX_WAIT,wait+500));
  }

  @SuppressWarnings("deprecation")
  private String currentAppPackage(){
    AccessibilityWindowInfo app=ForegroundWindow.select(getWindows(),overlay!=null&&overlay.hasWindowFocus());
    if(app==null)return null;
    AccessibilityNodeInfo root=Build.VERSION.SDK_INT>=33?app.getRoot(0):app.getRoot();
    if(root==null)return null;
    try{
      CharSequence name=root.getPackageName();
      return name==null?null:name.toString();
    }finally{if(Build.VERSION.SDK_INT<33)root.recycle();}
  }

  private void reconcile(){
    if(wm==null)return;
    Store state=Store.get(this);
    if(!state.prefs.getBoolean("consent",false)
        ||!getSystemService(PowerManager.class).isInteractive()
        ||getSystemService(KeyguardManager.class).isKeyguardLocked()){
      foreground="";handler.removeCallbacks(deadline);hide();return;
    }
    String current=currentAppPackage();
    // A system window or an incomplete task animation may briefly hide the app window.
    // Keep the previous decision until Android identifies the next foreground app.
    if(current!=null&&!current.equals(foreground)){
      foreground=current;
      if(target(current))state.prefs.edit().putString("target",current).apply();
    }
    if(target(foreground)&&!state.allowed())show();else hide();
    scheduleDeadline(state);
  }

  @Override public void onInterrupt(){hide();}
  @Override public void onDestroy(){handler.removeCallbacksAndMessages(null);hide();
    if(screenRegistered){try{unregisterReceiver(screen);}catch(Exception ignored){}screenRegistered=false;}
    instance=null;super.onDestroy();}
  void openReading(String mode){
    startActivity(ReadingActivity.intent(this,mode).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    requestCheck();
  }
  int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
  GradientDrawable bg(String c,int radius){GradientDrawable d=new GradientDrawable();d.setColor(Color.parseColor(c));d.setCornerRadius(dp(radius));return d;}
  TextView label(String s,int size,boolean serif){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(Color.parseColor("#203C33"));v.setLineSpacing(dp(6),1);if(serif)v.setTypeface(Typeface.create("serif",0));v.setPadding(0,dp(8),0,dp(8));return v;}
  Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextSize(16);b.setTextColor(Color.WHITE);b.setBackground(bg("#345D50",18));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(58));lp.topMargin=dp(12);b.setLayoutParams(lp);return b;}
  void show(){
    if(((KeyguardManager)getSystemService(KEYGUARD_SERVICE)).isKeyguardLocked())return;
    if(!Store.get(this).prefs.getBoolean("consent",false))return;
    if(overlay!=null){if(holdButton!=null&&!holdButton.isEnabled()&&Store.get(this).policy.canOverride(System.currentTimeMillis())){holdButton.setEnabled(true);holdButton.setText("Hold 45 seconds · grace pass");}return;}holdStart=-1;
    Store state=Store.get(this);Bible bible=Bible.get(this);
    ScrollView scroll=new ScrollView(this){
      @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);
        if(!focused&&holdStart>=0){
          holdStart=-1;handler.removeCallbacks(holdTick);
          holdButton.setText("Hold 45 seconds · grace pass");
        }
        requestCheck();
      }
    };
    scroll.setFillViewport(true);scroll.setBackgroundColor(Color.parseColor("#F6F4EC"));
    LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(28),dp(48),dp(28),dp(36));scroll.addView(box);
    box.addView(label("STILLWORD   /   SOCIAL GUARD",13,false));
    box.addView(label("A little Word.\nA quieter world.",32,true));
    box.addView(label(bible.verse("PHP",4,"8"),20,true));
    box.addView(label("PHILIPPIANS 4:8 · "+bible.active.optString("abbreviation"),12,false));
    TextView tip=label("Grab your devotional notebook and pen now and start reading the bible",16,false);tip.setPadding(dp(18),dp(16),dp(18),dp(16));tip.setBackground(bg("#E3E9DD",18));box.addView(tip);
    Button read=button(state.policy.ready()?"View John 8:32 & unlock":"Read the Bible");box.addView(read);
    read.setOnClickListener(v->{Intent i=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("page",state.policy.ready()?"release":"reader");startActivity(i);requestCheck();});
    Button kindle=button("Read from Kindle");box.addView(kindle);
    kindle.setOnClickListener(v->openReading(ReadingActivity.KINDLE));
    Button upload=button("Upload a book");box.addView(upload);
    upload.setOnClickListener(v->openReading(ReadingActivity.IMPORT));
    Button library=button("Download Library");box.addView(library);
    library.setOnClickListener(v->openReading(ReadingActivity.LIBRARY));
    box.addView(label("Facebook and Instagram are paused. Messenger remains available.",13,false));
    holdButton=button(state.policy.canOverride(System.currentTimeMillis())?"Hold 45 seconds · grace pass":"Grace pass used · available after 24 hours");box.addView(holdButton);
    holdButton.setTextColor(Color.parseColor("#345D50"));holdButton.setBackground(bg("#E7EBE2",18));
    holdButton.setEnabled(state.policy.canOverride(System.currentTimeMillis()));
    holdButton.setOnTouchListener((v,e)->{
      if(e.getActionMasked()==MotionEvent.ACTION_DOWN){if(!state.policy.canOverride(System.currentTimeMillis()))return true;holdStart=SystemClock.elapsedRealtime();holdCycle=state.policy.cycleStart;handler.post(holdTick);return true;}
      if(e.getActionMasked()==MotionEvent.ACTION_UP||e.getActionMasked()==MotionEvent.ACTION_CANCEL||(e.getActionMasked()==MotionEvent.ACTION_MOVE&&(e.getX()<0||e.getY()<0||e.getX()>v.getWidth()||e.getY()>v.getHeight()))){holdStart=-1;holdButton.setText("Hold 45 seconds · grace pass");return true;}return true;
    });
    box.addView(label("Grace pass: this six-hour cycle only, once in any 24 hours. Keep your finger on the button continuously.",12,false));
    Button home=button("Return to home screen");box.addView(home);home.setOnClickListener(v->{performGlobalAction(GLOBAL_ACTION_HOME);requestCheck();});
    scroll.setFocusableInTouchMode(true);scroll.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){performGlobalAction(GLOBAL_ACTION_HOME);requestCheck();return true;}return false;});
    WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.OPAQUE);
    lp.setTitle("StillWord Social Guard");try{wm.addView(scroll,lp);overlay=scroll;scroll.requestFocus();}catch(Exception ignored){overlay=null;}
  }
  final Runnable holdTick=new Runnable(){public void run(){
    if(holdStart<0||overlay==null)return;Store.get(GuardService.this).refresh();if(holdCycle!=Store.get(GuardService.this).policy.cycleStart){holdStart=-1;holdButton.setText("Hold 45 seconds · grace pass");return;}long elapsed=SystemClock.elapsedRealtime()-holdStart;
    if(!overlay.hasWindowFocus()||!getSystemService(PowerManager.class).isInteractive()){holdStart=-1;return;}
    if(elapsed>=45000){Store s=Store.get(GuardService.this);s.policy.override(System.currentTimeMillis());s.save();holdStart=-1;hide();Toast.makeText(GuardService.this,"Grace pass active until the next six-hour reset",Toast.LENGTH_LONG).show();return;}
    holdButton.setText("Keep holding · "+(45-elapsed/1000)+" seconds");handler.postDelayed(this,100);
  }};
  void hide(){holdStart=-1;handler.removeCallbacks(holdTick);if(overlay!=null){try{wm.removeView(overlay);}catch(Exception ignored){}overlay=null;}}
}
