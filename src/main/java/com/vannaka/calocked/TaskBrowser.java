package com.vannaka.calocked;
import java.util.*;
final class TaskBrowser {
 static boolean locked(Challenge state,Challenge.Task task){return !task.done&&(!state.unlocked(task.tier)||!state.prerequisites(task).isEmpty());}
 static boolean warning(Challenge state,Challenge.Task task){return !state.relaxedRules&&!task.done&&(!state.warnings(task).isEmpty()||!DependencyRules.matches(task));}
 static List<Challenge.Task> filter(Challenge state,int tier,String boss,int status){return filter(state,tier,boss,status,"");}
 static List<Challenge.Task> filter(Challenge state,int tier,String boss,int status,String search){
  String query=search==null?"":search.trim().toLowerCase(Locale.ROOT);
  String[] words=query.isEmpty()?new String[0]:query.split("\\s+");
  List<Challenge.Task> result=new ArrayList<>();
  for(Challenge.Task task:state.tasks){
   if(task.tier!=tier||!boss.equals("All")&&!boss.equals(task.boss))continue;
   boolean match=status==0||status==1&&!task.done||status==2&&task.done||status==3&&locked(state,task)||status==4&&warning(state,task)&&!locked(state,task);
   String haystack=(task.name+" "+task.description+" "+task.boss).replaceAll("<[^>]*>","").toLowerCase(Locale.ROOT);
   if(match&&Arrays.stream(words).allMatch(haystack::contains))result.add(task);
  }
  return result;
 }
 static List<String> categories(Challenge state,int tier){
  Set<String> names=new TreeSet<>();for(Challenge.Task task:state.tasks)if(task.tier==tier)names.add(task.boss);
  List<String> result=new ArrayList<>();result.add("All");result.addAll(names);return result;
 }
 static int clamp(int value,int total,int height){return Math.max(0,Math.min(value,Math.max(0,total-height)));}
 static int thumbHeight(int track,int total,int height){return Math.min(track,Math.max(16,total<=0?track:(int)((long)track*height/total)));}
 static int thumbTop(int offset,int total,int height,int track,int thumb){return total<=height?0:(int)((long)clamp(offset,total,height)*(track-thumb)/(total-height));}
 static int dragTo(int delta,int start,int total,int height,int track,int thumb){
  if(track<=thumb)return clamp(start,total,height);
  long offset=(long)start+Math.round((double)delta*(total-height)/(track-thumb));
  return (int)Math.max(0,Math.min(offset,Math.max(0,total-height)));
 }
 static int fromPointer(int pointer,int total,int height,int track,int thumb){return track<=thumb?0:clamp((int)((long)pointer*(total-height)/(track-thumb)),total,height);}
}
