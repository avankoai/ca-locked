package com.vannaka.calocked;

import com.google.gson.Gson;
import java.util.Objects;

/** Uses RuneLite's synced RuneScape-profile storage, never a character display name. */
final class ChallengeStore {
 interface Backend {
  String currentProfile();
  String read(String profile);
  void write(String profile,String json);
 }
 private final Backend backend;
 private final Gson gson;
 private volatile String profile,json;
 private volatile boolean writable;

 ChallengeStore(Backend backend,Gson gson){this.backend=backend;this.gson=gson;}

 Challenge load(String key){
  writable=false;profile=key;json=null;
  if(key==null||!key.equals(backend.currentProfile()))throw new IllegalStateException("Account changed");
  String saved=backend.read(key);
  Challenge result=saved==null?new Challenge():gson.fromJson(saved,Challenge.class);
  validate(result);
  json=saved;writable=true;
  return result;
 }

 boolean current(){
  String key=profile;
  return writable&&key!=null&&key.equals(backend.currentProfile())&&Objects.equals(json,backend.read(key));
 }

 boolean save(Challenge state){
  if(!current())return false;
  validate(state);
  String next=gson.toJson(state);
  if(next.equals(json))return true;
  String previous=json;
  // ConfigManager posts ConfigChanged synchronously. Recognize our own write there.
  json=next;
  try{backend.write(profile,next);return true;}
  catch(RuntimeException ex){json=previous;throw ex;}
 }

 boolean matches(String key,String saved){return writable&&Objects.equals(profile,key)&&Objects.equals(json,saved);}

 private static void validate(Challenge state){
  if(state==null||state.stateVersion!=1||state.mode<0||state.mode>=Challenge.MODES.length
    ||state.rulePack<0||state.rulePack>Challenge.FREE_PLAY||state.rerollLimit<0||state.rerollLimit>999)
   throw new IllegalArgumentException("Unsupported challenge data");
  for(Integer[] array:new Integer[][]{state.active,state.stored,state.excluded})
   if(array==null||array.length!=6)throw new IllegalArgumentException("Invalid assignment data");
  for(boolean[] array:new boolean[][]{state.tierActivated,state.tierReadyNotified})
   if(array==null||array.length!=6)throw new IllegalArgumentException("Invalid activation data");
  for(int[] array:new int[][]{state.tierSkipsRemaining,state.rerollsSpent,state.tierStreak,state.tierBest,
   state.assignedCompletions,state.bonusCompletions,state.skips,state.mistakes,state.blocksUsed,state.swaps,
   state.eventTierStreak,state.eventTierBest})
   if(array==null||array.length!=6)throw new IllegalArgumentException("Invalid tier data");
  for(long[] array:new long[][]{state.fees,state.skippedOwed,state.offTaskOwed})
   if(array==null||array.length!=6)throw new IllegalArgumentException("Invalid ledger data");
  if(state.killCounts==null||state.manualIncomplete==null||state.manualCompletions==null
    ||state.taskSuccesses==null||state.completionEvents==null||state.history==null
    ||state.observed==null||state.statsResetBaseline==null)
   throw new IllegalArgumentException("Incomplete challenge data");
  for(int count:state.tierSkipsRemaining)if(count<0)throw new IllegalArgumentException("Invalid reroll balance");
  for(int count:state.rerollsSpent)if(count<0)throw new IllegalArgumentException("Invalid reroll spending");
 }
}
