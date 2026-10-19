package com.vannaka.calocked;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Reviewed description rules. Potential interactions never remove a task from the pool. */
final class DependencyRules {
 static final class Entry {
  int id;String name,description,tier,boss,review;
  int[] required,warnings;Evidence[] evidence;
 }
 static final class Evidence {int prerequisite;String kind;
 }
 private static final Map<Integer,Entry> ENTRIES=new HashMap<>();
 static void initialize(Gson gson){
  ENTRIES.clear();
  InputStream stream=DependencyRules.class.getResourceAsStream("dependencies.json");
  if(stream==null)throw new IllegalStateException("Dependency catalogue is missing");
  try(Reader reader=new InputStreamReader(stream,StandardCharsets.UTF_8)){
   for(Entry entry:gson.fromJson(reader,Entry[].class)){
    if(ENTRIES.put(entry.id,entry)!=null)throw new IllegalStateException("Duplicate dependency task");
   }
  }catch(IOException e){throw new IllegalStateException(e);}
 }
 static int size(){return ENTRIES.size();}
 static boolean matches(Challenge.Task task){
  Entry entry=ENTRIES.get(task.id);
  return entry!=null&&entry.name.equals(task.name)&&entry.description.equals(task.description);
 }
 static List<Challenge.Task> missing(Challenge.Task task,Challenge state,boolean advisory){
  List<Challenge.Task> result=new ArrayList<>();
  if(!matches(task))return result;
  Entry entry=ENTRIES.get(task.id);
  if(state.relaxedRules){
   if(advisory)return result;
   if(entry.evidence!=null)for(Evidence e:entry.evidence)if("cumulative_count".equals(e.kind)||"nested_speed".equals(e.kind)){
    Challenge.Task reference=state.task(e.prerequisite);
    if(reference!=null&&matches(reference)&&!reference.done)result.add(reference);
   }
   return result;
  }
  List<Integer> ids=new ArrayList<>();for(int id:advisory?entry.warnings:entry.required)ids.add(id);
  if(advisory)for(int id:entry.required){Challenge.Task reference=state.task(id);if(reference==null||!matches(reference))ids.add(id);}
  for(int id:ids){
   Challenge.Task prerequisite=state.task(id);
   if(prerequisite!=null&&prerequisite.done)continue;
   if(!advisory&&(prerequisite==null||!matches(prerequisite)))continue;
   if(prerequisite==null){Entry reference=ENTRIES.get(id);prerequisite=new Challenge.Task(id,Arrays.asList(Challenge.TIERS).indexOf(reference.tier),reference.name,reference.description,reference.boss,false);}
   result.add(prerequisite);
  }
  return result;
 }
}
