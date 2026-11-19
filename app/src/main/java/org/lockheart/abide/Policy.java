package org.lockheart.abide;

/** Pure rules: six-hour periods anchored to first launch; rolling 24h override. */
public final class Policy {
  public static final long CYCLE=6*60*60*1000L, DAY=24*60*60*1000L;
  public long cycleStart, readMs, lastOverride=-1;
  public int goalMinutes=5;
  public boolean unlocked=false;
  public Policy(long now){cycleStart=now;}
  public boolean refresh(long now){
    if(now<cycleStart || now-cycleStart>=CYCLE){
      cycleStart=now<cycleStart?now:cycleStart+((now-cycleStart)/CYCLE)*CYCLE;
      readMs=0;unlocked=false;return true;
    }return false;
  }
  public long goal(){return goalMinutes*60000L;}
  public boolean ready(){return readMs>=goal();}
  public boolean credit(long ms,long now){
    refresh(now);boolean before=ready();
    readMs=Math.min(goal(),readMs+Math.max(0,ms));return !before&&ready();
  }
  public boolean unlock(long now){refresh(now);if(ready()){unlocked=true;return true;}return false;}
  public boolean canOverride(long now){return lastOverride<0 || (now>=lastOverride && now-lastOverride>=DAY);}
  public boolean override(long now){refresh(now);if(!canOverride(now))return false;lastOverride=now;unlocked=true;return true;}
  public long remaining(long now){refresh(now);return Math.max(0,cycleStart+CYCLE-now);}
}
