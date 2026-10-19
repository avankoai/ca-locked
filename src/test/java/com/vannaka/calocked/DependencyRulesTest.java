package com.vannaka.calocked;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.google.gson.Gson;
final class DependencyRulesTest {
 static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 static List<Challenge.Task> catalogue(){
  DependencyRules.Entry[] entries=new Gson().fromJson(new InputStreamReader(DependencyRules.class.getResourceAsStream("dependencies.json"),StandardCharsets.UTF_8),DependencyRules.Entry[].class);
  List<Challenge.Task> tasks=new ArrayList<>();
  for(DependencyRules.Entry e:entries)tasks.add(new Challenge.Task(e.id,Arrays.asList(Challenge.TIERS).indexOf(e.tier),e.name,e.description,e.boss,false));
  return tasks;
 }
 static Challenge.Task find(Challenge c,String name){return c.tasks.stream().filter(t->t.name.equals(name)).findFirst().orElseThrow();}
 static void done(Challenge c,String name){
  Challenge.Task old=find(c,name);c.tasks.set(c.tasks.indexOf(old),new Challenge.Task(old.id,old.tier,old.name,old.description,old.boss,true));
 }
 static boolean eligible(Challenge c,String name){Challenge.Task task=find(c,name);return c.pool(task.tier).contains(task);}
 static Challenge strict(){Challenge c=new Challenge();c.relaxedRules=false;return c;}
 public static void main(String[] args){
  DependencyRules.initialize(new com.google.gson.Gson());
  KillCountRisk.initialize(new com.google.gson.Gson());
  Challenge fresh=strict();fresh.sync(catalogue());
  check(DependencyRules.size()==655&&fresh.tasks.size()==655,"Every catalogue task has an audit entry");
  for(Challenge.Task t:fresh.tasks)check(DependencyRules.matches(t),"Reviewed metadata matches task "+t.id);
  check(!eligible(fresh,"Sit Rat")&&eligible(fresh,"Scurrius Novice"),"First kill dependency within Easy");
  done(fresh,"Scurrius Novice");check(eligible(fresh,"Sit Rat"),"Completion releases dependent task");
  Challenge creatures=strict();creatures.tasks=catalogue();
  String[][] creaturePairs={
   {"Into the Den of Giants","The Walking Volcano"},
   {"Not So Great After All","A Greater Foe"},
   {"Hitting Them Where It Hurts","If Gorillas Could Fly"},
   {"Shayzien Protector","A Scaley Encounter"},
   {"Shayzien Specialist","A Scaley Encounter"},
   {"Through Fire and Flames","Unending Torment"},
   {"Two Times the Torment","Unending Torment"},
   {"Three Times the Thrashing","Unending Torment"}
  };
  for(String[] pair:creaturePairs){
   Challenge.Task harder=find(creatures,pair[0]),basic=find(creatures,pair[1]);
   check(creatures.prerequisites(harder).stream().anyMatch(t->t.id==basic.id),"Exact creature kill locks "+pair[0]);
   check(creatures.warnings(basic).stream().anyMatch(t->t.id==harder.id),"Reverse optional overlap warns on "+pair[1]);
  }
  check(!eligible(creatures,"Into the Den of Giants"),"Giant cave task cannot roll before Fire Giant achievement");
  done(creatures,"The Walking Volcano");
  check(eligible(creatures,"Into the Den of Giants"),"Fire Giant completion releases Giant Cave task");
  check(creatures.prerequisites(find(creatures,"Rapid Reload")).stream().noneMatch(t->t.name.equals("Unending Torment")),"Hitting demons does not imply killing them");
  Challenge cross=strict();cross.tasks=catalogue();
  // Unlock Medium through unrelated Easy completions while leaving the KBD counter task unfinished.
  int needed=(cross.total(0)+1)/2;
  for(Challenge.Task task:new ArrayList<>(cross.tasks))if(task.tier==0&&!task.name.equals("King Black Dragon Novice")&&needed-->0)done(cross,task.name);
  check(cross.unlocked(1)&&!eligible(cross,"King Black Dragon Champion"),"Prerequisites checked across tiers");
  Challenge.Task kbd=find(cross,"King Black Dragon Novice");cross.stored[0]=kbd.id;
  check(!eligible(cross,"King Black Dragon Champion"),"Blocked slot does not satisfy a prerequisite");
  cross.stored[0]=null;cross.active[0]=kbd.id;
  check(!eligible(cross,"King Black Dragon Champion"),"Other tier assignment does not satisfy a prerequisite");
  done(cross,"King Black Dragon Novice");check(eligible(cross,"King Black Dragon Champion"),"Other tier completion releases task");
  check(!fresh.prerequisites(find(fresh,"Bryophyta Novice")).stream().anyMatch(t->t.name.equals("A Slow Death")),"Chest achievement is distinct from kill mechanics");
  check(eligible(fresh,"A Slow Death"),"Avoidable Bryophyta overlap remains rollable");
  Challenge all=strict();all.tasks=catalogue();for(Challenge.Task t:new ArrayList<>(all.tasks))done(all,t.name);
  Challenge.Task fifty=find(all,"Shellbane Veteran"),twentyfive=find(all,"Shellbane Adept");
  for(Challenge.Task t:Arrays.asList(fifty,twentyfive))all.tasks.set(all.tasks.indexOf(t),new Challenge.Task(t.id,t.tier,t.name,t.description,t.boss,false));
  check(all.pool(1).isEmpty()&&all.blocked(1).size()==1,"Last task remains visible but cannot be rolled");
  all.stored[1]=fifty.id;check(!all.unblock(1)&&Objects.equals(all.stored[1],fifty.id)&&!all.pool(1).contains(fifty),"Prerequisites prevent resuming but keep the blocked task intact");
  check(fresh.prerequisites(find(fresh,"Fancy feet")).stream().noneMatch(t->t.name.equals("Insanity")),"Invocation names are not task dependencies");
  check(fresh.prerequisites(find(fresh,"Perfect Grotesque Guardians")).size()==6,"All six named GG prerequisites retained, including apostrophes");
  check(fresh.prerequisites(find(fresh,"Zulrah Speed-Chaser")).stream().anyMatch(t->t.name.equals("Zulrah Speed-Trialist")),"Minutes plus seconds parsed correctly");
  Challenge advisory=strict();advisory.tasks=catalogue();for(Challenge.Task t:new ArrayList<>(advisory.tasks))if(t.tier<2)done(advisory,t.name);
  Challenge.Task rex=find(advisory,"Nex Survivors");check(advisory.prerequisites(rex).isEmpty()&&!advisory.warnings(rex).isEmpty(),"Unverified group kill-credit relation is advisory");
  // Simulate per-profile serialization. Old completions establish a clean baseline in each profile.
  Gson gson=new Gson();Challenge accountA=strict();accountA.sync(catalogue());accountA.owed=123;accountA.active[0]=find(accountA,"Brutus Novice").id;
  Challenge accountB=strict();accountB.sync(catalogue());
  Challenge restoredA=gson.fromJson(gson.toJson(accountA),Challenge.class);restoredA.sync(catalogue());
  Challenge restoredB=gson.fromJson(gson.toJson(accountB),Challenge.class);restoredB.sync(catalogue());
  check(restoredA.owed==123&&restoredA.active[0]!=null&&restoredB.owed==0&&restoredB.active[0]==null,"Profile states do not share assignments or debts");
  Challenge huey=strict();huey.tasks=catalogue();
  for(Challenge.Task task:new ArrayList<>(huey.tasks))if(task.tier==0)done(huey,task.name);
  Challenge.Task wizard=find(huey,"You're a wizard"),champion=find(huey,"Hueycoatl Champion");
  check(huey.prerequisites(wizard).stream().anyMatch(t->t.id==champion.id),"Earth-only Huey kill requires the unrestricted first kill");
  check(huey.warnings(champion).stream().anyMatch(t->t.id==wizard.id),"First Huey kill warns about avoidable earth-only completion");
  check(!huey.pool(wizard.tier).contains(wizard),"Wizard cannot be rolled while first kill remains open");
  done(huey,"Hueycoatl Champion");
  check(huey.prerequisites(wizard).isEmpty()&&huey.pool(wizard.tier).contains(wizard),"First Huey kill releases earth-only task into the medium pool");
  Challenge sessions=strict();sessions.tasks=catalogue();
  check(sessions.prerequisites(find(sessions,"Krakan't Hurt Me")).stream().anyMatch(t->t.name.equals("Kraken Adept")),"25-kill instance necessarily crosses cumulative 20 kills");
  check(sessions.prerequisites(find(sessions,"Ten-tacles")).stream().anyMatch(t->t.name.equals("Krakan't Hurt Me")),"50-kill instance necessarily completes the same-mode 25-kill instance");
  Challenge browser=strict();browser.tasks=catalogue();
  check(TaskBrowser.filter(browser,1,"All",0).size()==64,"Browse all tasks within selected tier");
  for(Challenge.Task t:new ArrayList<>(browser.tasks))if(t.tier==1||t.tier==2)done(browser,t.name);
  List<Challenge.Task> nex=TaskBrowser.filter(browser,3,"Nex",4);
  check(nex.size()==1&&nex.get(0).name.equals("Nex Survivors"),"Category and conflict filters combine");
  check(TaskBrowser.filter(browser,0,"Scurrius",3).stream().anyMatch(t->t.name.equals("Sit Rat")),"Locked filter includes dependency locks");
  done(browser,"Scurrius Novice");
  check(TaskBrowser.filter(browser,0,"Scurrius",3).isEmpty(),"Locked filter updates after prerequisite completion");
  check(TaskBrowser.filter(browser,0,"Scurrius",2).size()==1,"Completed filter");
  check(TaskBrowser.filter(browser,0,"Scurrius",1).stream().noneMatch(t->t.done),"Incomplete filter");
  Challenge.Task duo=find(browser,"Nex Duo");
  check(!browser.prerequisites(duo).isEmpty()&&TaskBrowser.warning(browser,duo)&&!TaskBrowser.filter(browser,5,"Nex",4).contains(duo),"Nex Duo has both statuses but is excluded from the rollable conflict filter while its tier is closed");
  for(Challenge.Task t:new ArrayList<>(browser.tasks))if(t.tier==3||t.tier==4&&!t.name.equals("Nex Trio"))done(browser,t.name);
  duo=find(browser,"Nex Duo");
  check(browser.unlocked(5)&&TaskBrowser.locked(browser,duo),"Opening Grandmaster preserves Nex Duo's separate prerequisite lock");
  done(browser,browser.prerequisites(duo).get(0).name);duo=find(browser,"Nex Duo");
  check(!TaskBrowser.locked(browser,duo)&&!TaskBrowser.warning(browser,duo),"Completing both prerequisite and advisory tasks clears both statuses");
  check(TaskBrowser.filter(browser,5,"All",4).stream().allMatch(t->browser.prerequisites(t).isEmpty()),"Possible conflict excludes tasks with unmet mandatory prerequisites");
  check(TaskBrowser.categories(browser,0).contains("All")&&!TaskBrowser.categories(browser,0).contains("Nex"),"Category dropdown uses selected tier");
  int thumb=TaskBrowser.thumbHeight(112,4400,140),end=TaskBrowser.thumbTop(4260,4400,140,112,thumb);
  check(TaskBrowser.fromPointer(end,4400,140,112,thumb)==4260,"Dragging reaches the final task");
  check(TaskBrowser.clamp(-10,4400,140)==0&&TaskBrowser.clamp(99999,4400,140)==4260,"Scroll limits");
  check(TaskBrowser.fromPointer(100,0,140,112,112)==0,"Empty list scroll is stable");
  check(TaskBrowser.dragTo(0,203,4400,140,112,16)==203,"Grabbing at any point preserves the exact starting scroll offset");
  check(TaskBrowser.dragTo(1,203,4400,140,112,16)==247&&TaskBrowser.dragTo(-1,203,4400,140,112,16)==159,"Drag follows pointer distance in either direction without snapping to task rows");
  check(TaskBrowser.dragTo(99999,203,4400,140,112,16)==4260&&TaskBrowser.dragTo(-99999,203,4400,140,112,16)==0,"Dragging clamps only at the list ends");
  check(TaskBrowser.dragTo(1,5,100,20,100,20)==6,"Small lists can move by a single content pixel");
  check(TaskBrowser.dragTo(100,0,100,140,112,112)==0,"A list without overflow does not jump when dragged");
  Challenge relaxed=new Challenge();relaxed.tasks=catalogue();
  check(relaxed.prerequisites(find(relaxed,"Into the Den of Giants")).isEmpty()&&eligible(relaxed,"Sit Rat"),"Creature and first-kill overlap no longer blocks assignments");
  check(relaxed.warnings(find(relaxed,"The Walking Volcano")).isEmpty()&&!TaskBrowser.warning(relaxed,find(relaxed,"Hueycoatl Champion")),"Old conflict warnings are suppressed in relaxed rules");
  check(relaxed.prerequisites(find(relaxed,"King Black Dragon Champion")).stream().anyMatch(t->t.name.equals("King Black Dragon Novice")),"Cumulative killcount ordering remains across tiers");
  check(relaxed.prerequisites(find(relaxed,"Zulrah Speed-Chaser")).stream().anyMatch(t->t.name.equals("Zulrah Speed-Trialist")),"Identical-condition speed ordering remains");
  check(relaxed.prerequisites(find(relaxed,"Nex Duo")).isEmpty(),"Party-size implication is not kept as a killcount or speed lock");
  check(relaxed.prerequisites(find(relaxed,"Who Is the King Now?")).isEmpty(),"Additional killcount completion during an assigned activity no longer creates a dynamic lock");
  Challenge search=new Challenge();search.tasks=catalogue();
  check(TaskBrowser.filter(search,0,"All",0,"  FIRE   GIANT ").stream().anyMatch(t->t.name.equals("The Walking Volcano")),"Search matches description terms independent of case and spacing");
  check(TaskBrowser.filter(search,0,"All",0,"walking volcano").size()==1,"Search matches specific task name");
  check(TaskBrowser.filter(search,0,"All",2,"walking").isEmpty(),"Search respects completion status");
  check(TaskBrowser.filter(search,0,"Scurrius",0,"walking").isEmpty(),"Search respects chosen category");
  check(TaskBrowser.filter(search,5,"All",0,"walking").isEmpty(),"Search respects chosen tier");
  check(TaskBrowser.filter(search,0,"All",0,"").size()==TaskBrowser.filter(search,0,"All",0).size(),"Clearing search restores full filtered list");
  Challenge tierLocks=new Challenge();tierLocks.tasks=catalogue();
  check(TaskBrowser.filter(tierLocks,1,"All",3).size()==64,"Every incomplete task in closed Medium appears in Locked");
  check(TaskBrowser.locked(tierLocks,find(tierLocks,"Sit Back and Relax"))&&tierLocks.prerequisites(find(tierLocks,"Sit Back and Relax")).isEmpty(),"Tier-only lock does not invent task prerequisites");
  int halfEasy=(tierLocks.total(0)+1)/2;
  for(Challenge.Task t:new ArrayList<>(tierLocks.tasks))if(t.tier==0&&!t.name.equals("King Black Dragon Novice")&&halfEasy-->0)done(tierLocks,t.name);
  check(tierLocks.unlocked(1)&&!TaskBrowser.locked(tierLocks,find(tierLocks,"Sit Back and Relax")),"Tier unlock releases tasks without prerequisites");
  check(TaskBrowser.locked(tierLocks,find(tierLocks,"King Black Dragon Champion")),"Specific prerequisites retain lock after tier opens");
  done(tierLocks,"King Black Dragon Novice");
  check(!TaskBrowser.locked(tierLocks,find(tierLocks,"King Black Dragon Champion")),"Completing remaining prerequisite releases specific lock");
  Challenge.Task finished=find(tierLocks,"Nex Duo");done(tierLocks,finished.name);
  check(!TaskBrowser.locked(tierLocks,find(tierLocks,finished.name)),"Completed task retains Done instead of becoming locked in a closed tier");
  System.out.println("Dependency checks passed: 655 entries, cross-tier locks, automatic release, block slots, last-task case, avoidable/advisory overlaps profile state, category/status filters and scroll limits.");
 }
}
