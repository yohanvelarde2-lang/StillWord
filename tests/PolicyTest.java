package org.lockheart.abide;

public class PolicyTest {
 static int checks;
 static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args){
  long start=1000000000L;Policy p=new Policy(start);
  check(!p.unlocked&&!p.ready(),"new install is locked");
  check(!p.credit(299999,start+299999),"cannot finish before five minutes");
  check(!p.unlock(start+299999),"unlock denied before threshold");
  check(p.credit(1,start+300000),"milestone at exactly five minutes");
  check(p.ready()&&!p.unlocked,"milestone requires explicit unlock");
  check(p.unlock(start+300001),"explicit unlock succeeds");
  p.refresh(start+Policy.CYCLE-1);check(p.unlocked,"open before exact cycle end");
  p.refresh(start+Policy.CYCLE);check(!p.unlocked&&p.readMs==0,"reset at exact six hours");
  check(!p.unlock(start+Policy.CYCLE),"stale notification cannot unlock new cycle");
  p.goalMinutes=10;check(!p.credit(300000,start+Policy.CYCLE+300000),"ten-minute option stays locked at five");
  check(p.credit(300000,start+Policy.CYCLE+600000),"ten-minute milestone");
  p.refresh(start+3*Policy.CYCLE+100);check(p.cycleStart==start+3*Policy.CYCLE,"skipped windows retain anchor");
  check(p.override(start+3*Policy.CYCLE+100),"first grace pass allowed");
  long used=p.lastOverride;check(p.unlocked&&!p.ready(),"grace does not fake reading");
  check(!p.override(used+1),"second pass blocked");
  p.refresh(start+4*Policy.CYCLE);check(!p.unlocked,"grace expires next window");
  check(!p.canOverride(used+Policy.DAY-1),"rolling 24h boundary not calendar day");
  check(p.canOverride(used+Policy.DAY),"pass returns exactly after 24h");
  check(!p.canOverride(used-1),"clock rollback cannot grant pass");
  p.unlock(used+Policy.DAY);p.refresh(start-1);check(!p.unlocked&&p.readMs==0,"rollback relocks instead of extending cycle");
  p.credit(-500,start);check(p.readMs==0,"negative time ignored");
  System.out.println("PASS: "+checks+" policy checks");
 }
}
