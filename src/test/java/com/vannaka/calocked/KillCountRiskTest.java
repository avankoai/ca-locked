package com.vannaka.calocked;
import java.util.*;
import com.google.gson.Gson;
public class KillCountRiskTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static Challenge setup(){
  Challenge c=strict();c.tasks=DependencyRulesTest.catalogue();
  // Open Hard, while leaving the medium cumulative 25-KBD task uncompleted.
  for(Challenge.Task task:new ArrayList<>(c.tasks))if(task.tier<=1&&!task.name.equals("King Black Dragon Champion"))DependencyRulesTest.done(c,task.name);
  return c;
 }
 static boolean related(List<Challenge.Task> list,String name){return list.stream().anyMatch(t->t.name.equals(name));}
 static Challenge strict(){Challenge c=new Challenge();c.relaxedRules=false;return c;}
 public static void main(String[] args){
  DependencyRules.initialize(new com.google.gson.Gson());
  KillCountRisk.initialize(new com.google.gson.Gson());
  Challenge unknown=strict();unknown.tasks=DependencyRulesTest.catalogue();
  check(KillCountRisk.known(unknown,"king black dragon")==null,"Absent count is unknown, not zero");
  Challenge zero=strict();zero.tasks=DependencyRulesTest.catalogue();
  check(KillCountRisk.startAtZero(zero)&&KillCountRisk.known(zero,"king black dragon")==0,"Explicit new-account setup needs no item or house log");
  check(KillCountRisk.chat(zero,"Your King Black Dragon kill count is: 1.")&&KillCountRisk.known(zero,"king black dragon")==1,"Observed kills replace zero baseline");
  Challenge zeroSaved=new Gson().fromJson(new Gson().toJson(zero),Challenge.class);zeroSaved.tasks=zero.tasks;
  check(zeroSaved.zeroKillcountBaseline&&KillCountRisk.known(zeroSaved,"araxxor")==0&&KillCountRisk.known(zeroSaved,"king black dragon")==1,"Zero baseline persists separately from observed counters");
  check(!KillCountRisk.startAtZero(zero),"Existing kills prevent mistaken new-account initialization");
  Challenge manual=strict();manual.tasks=DependencyRulesTest.catalogue();
  KillCountRisk.enterCount(manual,"The King Black Dragon = 21");
  check(KillCountRisk.known(manual,"king black dragon")==21&&KillCountRisk.known(manual,"araxxor")==null,"Manual entry affects only its boss and account");
  KillCountRisk.enterCount(manual,"Araxxor = 0");
  check(KillCountRisk.known(manual,"araxxor")==0,"Existing accounts can explicitly record a zero counter");
  try{KillCountRisk.enterCount(manual,"King Black Dragon = 20");throw new AssertionError("Lower count accepted");}catch(IllegalArgumentException expected){}
  try{KillCountRisk.enterCount(manual,"Unknown boss = 20");throw new AssertionError("Unknown boss accepted");}catch(IllegalArgumentException expected){}
  try{KillCountRisk.enterCount(manual,"Araxxor = -1");throw new AssertionError("Negative count accepted");}catch(IllegalArgumentException expected){}
  java.awt.image.BufferedImage highlight=StatusSprites.cardHighlight(90,71,0xc5832e);
  check((highlight.getRGB(2,0)>>>24)==255&&(highlight.getRGB(0,2)>>>24)==255&&(highlight.getRGB(1,0)>>>24)==0,"Highlight uses native two-pixel corners without cutting away the tile interior");
  check((highlight.getRGB(0,0)>>>24)==0&&(highlight.getRGB(89,0)>>>24)==0&&(highlight.getRGB(0,70)>>>24)==0&&(highlight.getRGB(89,70)>>>24)==0&&(highlight.getRGB(45,35)>>>24)==255,"Highlight leaves all beveled corners clear");
  Map<net.runelite.client.hiscore.HiscoreSkill,net.runelite.client.hiscore.Skill> scores=new EnumMap<>(net.runelite.client.hiscore.HiscoreSkill.class);
  scores.put(net.runelite.client.hiscore.HiscoreSkill.KING_BLACK_DRAGON,new net.runelite.client.hiscore.Skill(100,21,-1));
  scores.put(net.runelite.client.hiscore.HiscoreSkill.ARAXXOR,new net.runelite.client.hiscore.Skill(-1,-1,-1));
  scores.put(net.runelite.client.hiscore.HiscoreSkill.NEX,new net.runelite.client.hiscore.Skill(-1,50,-1));
  scores.put(net.runelite.client.hiscore.HiscoreSkill.ATTACK,new net.runelite.client.hiscore.Skill(100,99,13034431));
  net.runelite.client.hiscore.HiscoreResult result=new net.runelite.client.hiscore.HiscoreResult("Test Player",scores);
  check(HiscoreKillCounts.samePlayer("test_player",result)&&!HiscoreKillCounts.samePlayer("Other Player",result),"Highscore result must belong to the requested character");
  Challenge imported=strict();imported.tasks=DependencyRulesTest.catalogue();
  check(HiscoreKillCounts.importCounts(imported,result)==1&&KillCountRisk.known(imported,"king black dragon")==21,"Published boss counts import from scores, not experience or skills");
  check(KillCountRisk.known(imported,"araxxor")==null&&KillCountRisk.known(imported,"nex")==null,"Unranked, missing or malformed entries stay unknown, never zero");
  check(HiscoreKillCounts.missing(imported).contains("araxxor")&&!HiscoreKillCounts.missing(imported).contains("king black dragon"),"Only still-missing relevant counters are prompted");
  KillCountRisk.observe(imported,"King Black Dragon",24);
  check(HiscoreKillCounts.importCounts(imported,result)==0&&KillCountRisk.known(imported,"king black dragon")==24,"Old highscores never lower a count observed in the current session");
  check(HiscoreKillCounts.missing(zero).isEmpty(),"Explicit new-account zero baseline needs no gap questionnaire");
  Set<String> mapped=new HashSet<>();
  for(net.runelite.client.hiscore.HiscoreSkill boss:net.runelite.client.hiscore.HiscoreSkill.values())if(boss.getType()==net.runelite.client.hiscore.HiscoreSkillType.BOSS)mapped.add(KillCountRisk.key(boss.getName()));
  for(KillCountRisk.Rule rule:KillCountRisk.RULES.values())check(mapped.contains(rule.key),"Highscores use the exact matching boss counter: "+rule.key);
  Challenge.Task attempt=DependencyRulesTest.find(unknown,"Who Is the King Now?");
  check(!related(unknown.warnings(attempt),"King Black Dragon Champion"),"Missing count alone does not mark a possible conflict");
  check(!KillCountRisk.risks(unknown,attempt).isEmpty(),"Missing-count guidance remains available in task details");
  KillCountRisk.observe(unknown,"King Black Dragon",0);
  check(!related(unknown.warnings(attempt),"King Black Dragon Champion"),"Distant threshold does not mark every task as a conflict");
  Challenge c=setup();attempt=DependencyRulesTest.find(c,"Who Is the King Now?");
  check(KillCountRisk.known(c,"king black dragon")==10,"Completed achievement gives a valid lower bound");
  KillCountRisk.observe(c,"King Black Dragon",14);
  check(c.pool(attempt.tier).contains(attempt)&&related(c.warnings(attempt),"King Black Dragon Champion"),"14+10 below 25: rollable with retry warning");
  KillCountRisk.observe(c,"King Black Dragon",15);
  check(!c.pool(attempt.tier).contains(attempt)&&related(c.prerequisites(attempt),"King Black Dragon Champion"),"Equality at 15+10=25 locks the task");
  check(KillCountRisk.chat(c,"Your King Black Dragon kill count is: <col=ff0000>21</col>."),"Read a real-style tagged game killcount message");
  check(!c.pool(attempt.tier).contains(attempt)&&KillCountRisk.explanation(c,attempt,DependencyRulesTest.find(c,"King Black Dragon Champion")).contains("21"),"21+10 reaches the open 25-kill task, with visible calculation context");
  check(!KillCountRisk.observe(c,"King Black Dragon",13)&&KillCountRisk.known(c,"king black dragon")==21,"Stale cache cannot lower observed count");
  check(!KillCountRisk.chat(c,"Someone said: Your King Black Dragon kill count is: 100."),"Reject non-system-message format");
  check(c.pool(1).contains(DependencyRulesTest.find(c,"King Black Dragon Champion")),"Cumulative 25-count task does not mistakenly need 25 additional kills");
  c.active[attempt.tier]=attempt.id;
  check(c.prerequisites(attempt).isEmpty()&&related(c.warnings(attempt),"King Black Dragon Champion")&&Objects.equals(c.active[attempt.tier],attempt.id),"Existing assignment is retained and warns before another attempt");
  check(c.blockTask(attempt.tier)&&!c.unblock(attempt.tier)&&Objects.equals(c.stored[attempt.tier],attempt.id)&&!c.pool(attempt.tier).contains(attempt),"Unblocking a dangerous task does not bypass its KC prerequisites");
  DependencyRulesTest.done(c,"King Black Dragon Champion");
  check(c.prerequisites(attempt).isEmpty()&&c.unblock(attempt.tier)&&Objects.equals(c.active[attempt.tier],attempt.id),"Completing target count allows resuming the blocked task");
  Challenge fresh=setup();Gson gson=new Gson();Challenge saved=gson.fromJson(gson.toJson(c),Challenge.class);saved.tasks=c.tasks;
  check(KillCountRisk.known(saved,"king black dragon")==25&&KillCountRisk.known(fresh,"king black dragon")==10,"Profile save/load retains counts without sharing them");
  Challenge speed=strict();speed.tasks=DependencyRulesTest.catalogue();
  Challenge.Task batch=DependencyRulesTest.find(speed,"Araxxor Speed-Runner");
  check(KillCountRisk.rule(batch).minimum==6,"Timed six-kill batch uses six kills, not one");
  Challenge.Task altered=new Challenge.Task(batch.id,batch.tier,batch.name,batch.description+" changed",batch.boss,false);
  check(KillCountRisk.risks(speed,altered).isEmpty(),"Changed task definitions do not reuse a stale minimum");
  check(!KillCountRisk.observe(speed,"Unknown boss",21),"Unmapped counters do not contaminate another boss");
  System.out.println("Killcount checks passed: unknowns, 21+10, exact boundary, retries, assigned/block transitions, game messages, profile persistence and timed batches.");
 }
}
