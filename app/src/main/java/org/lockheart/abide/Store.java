package org.lockheart.abide;

import android.content.*;
import org.json.*;

final class Store {
  private static Store instance;
  final SharedPreferences prefs;
  final Policy policy;
  private Store(Context c){
    prefs=c.getSharedPreferences("abide",0);
    policy=new Policy(prefs.getLong("cycle",System.currentTimeMillis()));
    policy.readMs=prefs.getLong("read",0);policy.lastOverride=prefs.getLong("override",-1);
    policy.unlocked=prefs.getBoolean("unlocked",false);policy.goalMinutes=prefs.getInt("goal",5);
    refresh();save();
  }
  static synchronized Store get(Context c){if(instance==null)instance=new Store(c.getApplicationContext());return instance;}
  boolean refresh(){boolean changed=policy.refresh(System.currentTimeMillis());if(changed){policy.goalMinutes=prefs.getInt("nextGoal",policy.goalMinutes);save();}return changed;}
  // Last values written to storage, so unchanged state is never rewritten.
  private long savedCycle=Long.MIN_VALUE,savedRead=-1,savedOverride=Long.MIN_VALUE;private boolean savedUnlocked;private int savedGoal=-1;
  void save(){
    if(policy.cycleStart==savedCycle&&policy.readMs==savedRead&&policy.lastOverride==savedOverride
        &&policy.unlocked==savedUnlocked&&policy.goalMinutes==savedGoal)return;
    prefs.edit().putLong("cycle",policy.cycleStart).putLong("read",policy.readMs)
      .putLong("override",policy.lastOverride).putBoolean("unlocked",policy.unlocked).putInt("goal",policy.goalMinutes).commit();
    savedCycle=policy.cycleStart;savedRead=policy.readMs;savedOverride=policy.lastOverride;savedUnlocked=policy.unlocked;savedGoal=policy.goalMinutes;
  }
  boolean allowed(){refresh();return policy.unlocked;}
  JSONObject json(Context c){
    refresh();JSONObject j=new JSONObject();
    try{
      j.put("readMs",policy.readMs);j.put("goalMinutes",policy.goalMinutes);j.put("ready",policy.ready());
      j.put("unlocked",policy.unlocked);j.put("resetMs",policy.remaining(System.currentTimeMillis()));
      j.put("overrideAvailable",policy.canOverride(System.currentTimeMillis()));
      j.put("overrideWaitMs",Math.max(0,policy.lastOverride+Policy.DAY-System.currentTimeMillis()));
      j.put("guardEnabled",GuardService.isEnabled(c));j.put("guardConnected",GuardService.instance!=null);
      j.put("book",prefs.getInt("book",0));j.put("chapter",prefs.getInt("chapter",0));
      j.put("verse",prefs.getInt("verse",0));j.put("font",prefs.getInt("font",21));
      j.put("target",prefs.getString("target","com.facebook.katana"));
    }catch(JSONException ignored){}return j;
  }
}
