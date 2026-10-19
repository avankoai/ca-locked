package com.vannaka.calocked;
import java.util.*;
import java.util.regex.*;
/** Counts actual completion messages; never inferred from completion bits or manual edits. */
final class TaskSuccesses {
 private static final Pattern MESSAGE=Pattern.compile("Congratulations, you've (?:completed|re-completed) an? (easy|medium|hard|elite|master|grandmaster) combat task: (.+)",Pattern.CASE_INSENSITIVE);
 // Game task-type enum 3969: Kill Count = 3, Speed = 6. Unknown types
 // are excluded until a fresh game catalogue supplies their classification.
 static boolean supportsRepeats(Challenge.Task task){
  return task.type==1||task.type==2||task.type==4||task.type==5;
 }
 static int[] totals(Challenge state,int tier,boolean allTiers){
  Set<Integer> completed=new HashSet<>();
  for(Challenge.CompletionEvent event:state.completionEvents)if(!event.ignoreStats)completed.add(event.id);
  int unique=0,total=0;
  for(Challenge.Task task:state.tasks){
   if(!allTiers&&task.tier!=tier)continue;
   int count=Math.max(state.successes(task.id),completed.contains(task.id)?1:0);
   if(count>0){unique++;total+=count;}
  }
  return new int[]{unique,total};
 }
 static List<Challenge.Task> ranked(Challenge state,int tier,boolean allTiers){
  List<Challenge.Task> result=new ArrayList<>();
  for(Challenge.Task task:state.tasks)
   if((allTiers||task.tier==tier)&&supportsRepeats(task)&&state.successes(task.id)>0)result.add(task);
  result.sort(Comparator.comparingInt((Challenge.Task task)->state.successes(task.id)).reversed().thenComparing(task->task.name));
  return result;
 }
 static Challenge.Task parse(String raw,List<Challenge.Task> tasks){
  if(raw==null)return null;
  String clean=raw.replaceAll("<[^>]*>", "").replaceAll("@[a-zA-Z_]+@", "");
  Matcher match=MESSAGE.matcher(clean);if(!match.find())return null;
  String name=match.group(2).trim();if(name.endsWith("."))name=name.substring(0,name.length()-1);
  name=name.replaceFirst("\\s+\\(\\d+ points?\\)$", "");
  for(Challenge.Task task:tasks)if(Challenge.TIERS[task.tier].equalsIgnoreCase(match.group(1))&&task.name.equals(name))return task;
  return null;
 }
 static boolean record(Challenge state,String message){
  Challenge.Task task=parse(message,state.tasks);if(task==null)return false;
  state.taskSuccesses.merge(task.id,1,Integer::sum);return true;
 }
}
