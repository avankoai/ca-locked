package com.vannaka.calocked;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
/** Observed killcounts are lower bounds, never invented zeroes or loot-drop totals. */
final class KillCountRisk {
 static final class Rule {int id,total,minimum;String name,description,key;String[] aliases;}
 static final Map<Integer,Rule> RULES=new HashMap<>();
 static {
  try(Reader reader=new InputStreamReader(KillCountRisk.class.getResourceAsStream("kill-count-rules.json"),StandardCharsets.UTF_8)){
   for(Rule rule:new Gson().fromJson(reader,Rule[].class))RULES.put(rule.id,rule);
  }catch(IOException e){throw new IllegalStateException(e);}
 }
 static String key(String name){return name.replaceAll("<[^>]*>","").trim().toLowerCase(Locale.ROOT).replaceFirst("^the ","");}
 static Rule rule(Challenge.Task task){Rule r=RULES.get(task.id);return r!=null&&r.name.equals(task.name)&&r.description.equals(task.description)?r:null;}
 static Set<String> aliases(){Set<String> aliases=new HashSet<>();for(Rule r:RULES.values())Collections.addAll(aliases,r.aliases);return aliases;}
 static boolean observe(Challenge state,String name,int count){
  if(count<0)return false;String normalized=key(name);
  if(RULES.values().stream().noneMatch(r->r.key.equals(normalized)))return false;
  Integer previous=state.killCounts.get(normalized);
  if(previous!=null&&previous>=count)return false;
  state.killCounts.put(normalized,count);return true;
 }
 static Integer known(Challenge state,String key){
  Integer count=state.killCounts.get(key);
  if(count==null&&state.zeroKillcountBaseline)count=0;
  for(Challenge.Task task:state.tasks){Rule r=rule(task);if(task.done&&r!=null&&r.total>0&&r.key.equals(key))count=Math.max(count==null?0:count,r.total);}
  return count;
 }
 static boolean startAtZero(Challenge state){
  if(state.tasks.isEmpty()||state.tasks.stream().anyMatch(t->t.done)||state.killCounts.values().stream().anyMatch(n->n>0))return false;
  state.zeroKillcountBaseline=true;return true;
 }
 static String enterCount(Challenge state,String input){
  int split=input.lastIndexOf('=');
  if(split<1)throw new IllegalArgumentException("Use Boss name = count, for example King Black Dragon = 21.");
  String boss=key(input.substring(0,split));
  Rule match=RULES.values().stream().filter(r->r.key.equals(boss)||Arrays.stream(r.aliases).anyMatch(a->key(a).equals(boss))).findFirst().orElse(null);
  if(match==null)throw new IllegalArgumentException("Boss not recognised. Use the full category name shown in Tasks.");
  int count;
  try{count=Integer.parseInt(input.substring(split+1).trim().replace(",",""));}catch(NumberFormatException e){throw new IllegalArgumentException("Enter a whole killcount, such as 0 or 21.");}
  if(count<0)throw new IllegalArgumentException("Killcount cannot be negative.");
  Integer previous=known(state,match.key);
  if(previous!=null&&count<previous)throw new IllegalArgumentException("Already recorded at least "+previous+" kills for this boss. This entry would lower that count.");
  observe(state,match.key,count);
  return match.key+": "+count+" kills recorded.";
 }
 static final class Risk {
  final Challenge.Task target;final int minimum;final Integer count;final boolean forced,creditReviewed;
  Risk(Challenge.Task target,int minimum,Integer count,int threshold,boolean creditReviewed){this.target=target;this.minimum=minimum;this.count=count;this.creditReviewed=creditReviewed;
   // Count >= threshold with an uncompleted achievement signals a mapping/sync mismatch, not proof.
   forced=creditReviewed&&(count==null?0:count)<threshold&&(long)(count==null?0:count)+minimum>=threshold;
  }
 }
 static List<Risk> risks(Challenge state,Challenge.Task source){
  List<Risk> out=new ArrayList<>();Rule own=rule(source);
  if(source.done||own==null||own.total>0)return out;
  Integer count=known(state,own.key);
  for(Challenge.Task target:state.tasks){Rule other=rule(target);
   if(target.done||target.id==source.id||other==null||other.total<=0||!other.key.equals(own.key))continue;
   out.add(new Risk(target,own.minimum,count,other.total,!own.key.equals("nex")&&!own.key.equals("nightmare")));
  }
  return out;
 }
 static boolean assigned(Challenge state,Challenge.Task task){for(Integer id:state.active)if(Objects.equals(id,task.id))return true;return false;}
 static List<Challenge.Task> related(Challenge state,Challenge.Task task,boolean advisory){
  List<Challenge.Task> result=new ArrayList<>();boolean active=assigned(state,task);
  for(Risk risk:risks(state,task))if(advisory?((risk.forced&&active)||(!risk.forced&&nearThreshold(risk))):(risk.forced&&!active))result.add(risk.target);
  return result;
 }
 // Highlight a concrete nearby retry risk. General uncertainty remains in task details.
 static boolean nearThreshold(Risk risk){
  return risk.creditReviewed&&risk.count!=null&&(long)risk.count+2L*risk.minimum>=rule(risk.target).total;
 }
 static String explanation(Challenge state,Challenge.Task source,Challenge.Task target){
  for(Risk r:risks(state,source))if(r.target.id==target.id){
   int threshold=rule(target).total;
   if(!r.creditReviewed)return "Attempts can advance this killcount. Individual/group kill credit needs review before this relation can be used as a lock.";
   if(r.count!=null&&r.count>=threshold)return "Known KC is already "+r.count+", but this achievement is still open. Refresh game completion data; this counter relation needs checking.";
   String prefix=r.count==null?"Current KC unknown. ":"Known KC: at least "+r.count+". ";
   if(r.forced){
    String calculation=r.count==null?"At least "+r.minimum+" kills are needed even from zero.":r.count+" + "+r.minimum+" = "+((long)r.count+r.minimum)+" kills.";
    return prefix+"Fresh attempt: "+calculation+" This reaches the open "+threshold+"-kill achievement. "+(assigned(state,source)?"Check before restarting your assigned task.":"Complete that achievement first.");
   }
   return prefix+"Fresh attempt needs at least "+r.minimum+" kills. Extra kills from failed attempts can reach the open "+threshold+"-kill achievement first. "+(r.count==null?"Read the boss kill log or a killcount message to update KC.":"Update the boss kill log if this count is behind.");
  }
  return null;
 }
 private static final Pattern CHAT=Pattern.compile("^Your (?:(?:completion count for|subdued|completed) )?(.+?) (?:(?:kill|harvest|completion|success) )?(?:count )?is: ?([0-9,]+)\\.?$",Pattern.CASE_INSENSITIVE);
 static boolean chat(Challenge state,String message){
  Matcher m=CHAT.matcher(message.replaceAll("<[^>]*>",""));
  if(!m.matches())return false;
  try{return observe(state,m.group(1),Integer.parseInt(m.group(2).replace(",","")));}catch(NumberFormatException e){return false;}
 }
}
