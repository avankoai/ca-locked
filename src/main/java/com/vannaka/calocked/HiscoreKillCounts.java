package com.vannaka.calocked;

import java.util.*;
import net.runelite.client.hiscore.*;

/** Published counts are snapshots; an unranked boss never implies zero kills. */
final class HiscoreKillCounts {
 static String playerKey(String name){return name==null?"":name.replace('\u00a0',' ').replace('_',' ').trim().toLowerCase(Locale.ROOT);}
 static boolean samePlayer(String requested,HiscoreResult result){return result!=null&&playerKey(requested).equals(playerKey(result.getPlayer()));}
 static Map<String,Integer> published(HiscoreResult result){
  Map<String,Integer> counts=new TreeMap<>();
  for(HiscoreSkill boss:HiscoreSkill.values()){
   if(boss.getType()!=HiscoreSkillType.BOSS)continue;
   Skill score=result.getSkill(boss);
   if(score!=null&&score.getRank()>0&&score.getLevel()>=0)counts.put(KillCountRisk.key(boss.getName()),score.getLevel());
  }
  return counts;
 }
 static int importCounts(Challenge state,HiscoreResult result){
  int updated=0;
  for(Map.Entry<String,Integer> count:published(result).entrySet())if(KillCountRisk.observe(state,count.getKey(),count.getValue()))updated++;
  return updated;
 }
 static List<String> missing(Challenge state){
  Set<String> keys=new TreeSet<>();
  for(Challenge.Task task:state.tasks){
   KillCountRisk.Rule rule=KillCountRisk.rule(task);
   if(!task.done&&rule!=null&&rule.total>0&&!state.zeroKillcountBaseline&&!state.killCounts.containsKey(rule.key))keys.add(rule.key);
  }
  return new ArrayList<>(keys);
 }
 static String displayName(String key){
  for(HiscoreSkill boss:HiscoreSkill.values())if(boss.getType()==HiscoreSkillType.BOSS&&KillCountRisk.key(boss.getName()).equals(key))return boss.getName();
  return key;
 }
}
