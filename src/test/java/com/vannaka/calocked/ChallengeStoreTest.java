package com.vannaka.calocked;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

/** Storage-contract tests; these do not contact RuneLite's cloud service. */
public final class ChallengeStoreTest {
 static void check(boolean value,String description){if(!value)throw new AssertionError(description);}
 static final class Memory implements ChallengeStore.Backend {
  String current="rsprofile.character-a";
  final Map<String,String> values=new HashMap<>();
  int writes;
  public String currentProfile(){return current;}
  public String read(String key){return values.get(key);}
  public void write(String key,String value){writes++;values.put(key,value);}
 }
 public static void main(String[] args){
  DependencyRules.initialize(new com.google.gson.Gson());
  KillCountRisk.initialize(new com.google.gson.Gson());
  Gson gson=new Gson();Memory data=new Memory();ChallengeStore first=new ChallengeStore(data,gson);
  Challenge a=first.load(data.current);a.challengeActive=true;a.active[0]=42;a.stored[0]=43;
  a.rerollSettingsInitialized=true;a.rulePack=Challenge.CA_RULES;a.rerollLimit=3;
  a.tierSkipsRemaining[0]=1;a.rerollsSpent[0]=2;a.allowSwapping=false;a.tierActivated[0]=true;
  check(first.save(a),"First account can be saved");String savedA=data.read(data.current);
  int writes=data.writes;check(first.save(a)&&data.writes==writes,"Unchanged data does not create configuration traffic");
  data.current="rsprofile.character-b";
  check(!first.save(a)&&data.values.size()==1,"An old account cannot write into the newly logged-in account");
  Challenge b=first.load(data.current);check(!b.challengeActive&&b.active[0]==null,"New account starts separately");
  b.challengeActive=true;b.active[0]=84;check(first.save(b),"Second account has its own challenge");
  data.current="rsprofile.character-a";a=first.load(data.current);
  check(a.active[0]==42&&a.stored[0]==43&&a.tierSkipsRemaining[0]==1&&!a.allowSwapping,"Switching back restores assignments, storage, allowance and rules");
  ChallengeStore otherInstallation=new ChallengeStore(data,new Gson());
  Challenge restored=otherInstallation.load(data.current);
  check(restored.challengeActive&&restored.active[0]==42&&restored.tierActivated[0]&&restored.rerollsSpent[0]==2,"A separate store restores portable JSON without computer-specific paths");
  restored.active[0]=99;check(otherInstallation.save(restored),"A synchronized incoming value can be represented by another installation");
  a.active[0]=100;check(!first.current()&&!first.save(a),"Stale in-memory data cannot overwrite a newer configuration value");
  check(first.load(data.current).active[0]==99,"Reload adopts the new account state");
  check(!first.matches("rsprofile.character-b",data.read(data.current)),"A matching JSON string from another account is not a self-write");
  for(String bad:new String[]{"{", "null", "[]", "{\"active\":[1]}", "{\"manualCompletions\":null}", "{\"stateVersion\":2}", "{\"mode\":77}"}){
   data.values.put(data.current,bad);writes=data.writes;
   boolean failed=false;try{first.load(data.current);}catch(RuntimeException expected){failed=true;}
   check(failed&&!first.save(new Challenge())&&data.writes==writes&&bad.equals(data.read(data.current)),"Unreadable or future data stays intact: "+bad);
  }
  data.values.put(data.current,savedA.replace("\"stateVersion\":1,",""));
  check(first.load(data.current).active[0]==42,"Existing unversioned test saves load without losing progress");
  data.current=null;check(!first.save(new Challenge()),"Logged-out clients cannot save account data");
  System.out.println("Account storage checks passed: isolated characters, portable saves, stale-write protection, legacy saves and corrupt/future data preservation.");
 }
}
