package com.vannaka.calocked;

import java.util.*;

/** Account-specific series state. Game completions remain the authoritative source. */
final class Challenge {
    int stateVersion=1;
    static final String[] TIERS = {"Easy", "Medium", "Hard", "Elite", "Master", "Grandmaster"};
    static final class Task {
        final int id, tier, type; final String name, description, boss; final boolean done;
        Task(int id, int tier, String name, String description, String boss, boolean done) {
            this(id,tier,name,description,boss,done,0);
        }
        Task(int id, int tier, String name, String description, String boss, boolean done, int type) {
            this.id=id; this.tier=tier; this.type=type; this.name=name; this.description=description; this.boss=boss; this.done=done;
        }
    }
    static final int CA_LOCKED=0, ALL_TIERS=1, ONE_TIER=2, TIER_BY_TIER=3;
    static final String[] MODES={"CA Locked", "All Tiers", "One Tier", "Tier by Tier"};
    static final int CUSTOM_RULES=0, CA_RULES=1, FREE_PLAY=2;
    int rulePack=CUSTOM_RULES,rerollLimit=3;
    boolean rerollSettingsInitialized;
    int[] tierSkipsRemaining={3,3,3,3,3,3},rerollsSpent=new int[6];
    int remainingSkips(int tier){return rulePack==CA_RULES?tierSkipsRemaining[tier]:skipTokens;}
    boolean selectRulePack(int pack){
        if(pack<0||pack>2)return false;
        rulePack=pack;return true;
    }
    static int parseRerollLimit(String input){
        java.math.BigInteger value=new java.math.BigInteger(input.trim());
        if(value.signum()<0)throw new IllegalArgumentException("Enter a non-negative whole number.");
        return value.min(java.math.BigInteger.valueOf(999)).intValue();
    }
    void setRerollLimit(int limit){
        if(challengeActive)throw new IllegalStateException("Start a new challenge to change its rules.");
        if(limit<0||limit>999)throw new IllegalArgumentException("Choose a number from 0 to 999.");
        for(int i=0;i<6;i++)tierSkipsRemaining[i]=Math.max(0,limit-rerollsSpent[i]);
        rerollLimit=limit;rulePack=limit==0?FREE_PLAY:CA_RULES;
    }
    void resetRerolls(){Arrays.fill(tierSkipsRemaining,rerollLimit);Arrays.fill(rerollsSpent,0);history.add("Reset reroll allowance: "+rerollLimit+" per tier");}
    boolean canSwap(int tier){
        Task current=task(active[tier]),saved=task(stored[tier]);
        return allowStorage&&allowSwapping&&unlocked(tier)&&current!=null&&!current.done&&saved!=null&&!saved.done&&current.id!=saved.id&&prerequisites(saved).isEmpty();
    }
    boolean swap(int tier){
        if(!canSwap(tier))return false;
        Integer previous=active[tier];active[tier]=stored[tier];stored[tier]=previous;
        swaps[tier]++;history.add("Swapped: "+task(active[tier]).name);return true;
    }
    boolean manualTierUnlocks,tierUnlocksInitialized,easyUnlockInitialized;
    boolean[] tierReadyNotified=new boolean[6];
    void beginReadyNotificationSession(){Arrays.fill(tierReadyNotified,false);}
    boolean hasReadyTier(){for(int i=0;i<6;i++)if(total(i)>0&&readyToUnlock(i))return true;return false;}
    List<Integer> newReadyTiers(){
        List<Integer> result=new ArrayList<>();
        for(int i=0;i<6;i++)if(total(i)>0&&readyToUnlock(i)&&!tierReadyNotified[i]){tierReadyNotified[i]=true;result.add(i);}
        return result;
    }
    boolean[] tierActivated={false,false,false,false,false,false};
    boolean readyToUnlock(int tier){return tierEligible(tier)&&!unlocked(tier);}
    boolean activateTier(int tier){
        if(!readyToUnlock(tier))return false;
        tierActivated[tier]=true;history.add("Unlocked tier: "+TIERS[tier]);return true;
    }
    boolean unlocked(int tier){return tierEligible(tier)&&(!manualTierUnlocks||mode==ALL_TIERS||mode==ONE_TIER||tierActivated[tier]);}
    boolean challengeActive;
    int mode=CA_LOCKED;
    boolean allowSkipping=true,allowStorage=true,allowSwapping=true;
    boolean changeMode(int next){
        if(challengeActive)return false;
        if(next<0||next>=MODES.length||Arrays.stream(active).anyMatch(Objects::nonNull))return false;
        mode=next;history.add("Mode: "+MODES[next]);return true;
    }
    String tierRequirement(int tier){
        if(mode==TIER_BY_TIER)return "Requires 100% of every earlier tier.";
        return "Requires "+(tier>1?"100% "+TIERS[tier-2]+" and ":"")+"50% "+TIERS[Math.max(0,tier-1)];
    }
    Integer[] active = new Integer[6], stored = new Integer[6], excluded = new Integer[6];
    long[] fees = {100_000,300_000,1_000_000,2_000_000,5_000_000,10_000_000};
    long owed, deposited;
    Map<String,Integer> killCounts=new HashMap<>();
    boolean zeroKillcountBaseline;
    boolean relaxedRules=true,repairMigrationApplied;
    int[] bonusCompletions=new int[6];
    Set<Integer> manualIncomplete=new HashSet<>();
    List<CompletionEvent> completionEvents=new ArrayList<>();
    boolean eventBaseline;
    int eventStreak,eventBest;
    int[] eventTierStreak=new int[6],eventTierBest=new int[6];
    static final class CompletionEvent {
        int id,tier;boolean assigned,bonus,advanceStreak,ignoreStats;long pointDelta;
    }
    long[] skippedOwed=new long[6], offTaskOwed=new long[6];
    boolean penaltyLedgerInitialized;
    static final int SKIP_COST=50, BLOCK_COST=200;
    long points;
    int skipTokens=3, blockTokens=1;
    Map<Integer,Integer> taskSuccesses=new HashMap<>();
    int successes(int id){return taskSuccesses.getOrDefault(id,0);}
    int[] swaps=new int[6];
    Set<Integer> statsResetBaseline=new HashSet<>();
    void resetStatistics(){
        taskSuccesses.clear();streak=0;bestStreak=0;
        for(int[] counts:new int[][]{tierStreak,tierBest,assignedCompletions,bonusCompletions,skips,mistakes,blocksUsed,swaps})Arrays.fill(counts,0);
        eventBaseline=true;eventStreak=0;eventBest=0;Arrays.fill(eventTierStreak,0);Arrays.fill(eventTierBest,0);
        for(CompletionEvent event:completionEvents)event.ignoreStats=true;
        statsResetBaseline=new HashSet<>(observed);resetRerolls();history.add("Reset challenge statistics");
    }
    Challenge restarted(List<Task> fresh){
        Challenge clean=new Challenge();
        clean.manualTierUnlocks=manualTierUnlocks;clean.tierUnlocksInitialized=true;
        clean.mode=mode;clean.rulePack=rulePack;clean.rerollLimit=rerollLimit;
        clean.allowSkipping=allowSkipping;clean.allowStorage=allowStorage;clean.allowSwapping=allowSwapping;
        clean.relaxedRules=relaxedRules;clean.rerollSettingsInitialized=true;
        clean.repairMigrationApplied=true;
        clean.killCounts=new HashMap<>(killCounts);clean.zeroKillcountBaseline=zeroKillcountBaseline;
        clean.resetRerolls();clean.sync(fresh);return clean;
    }
    Challenge started(List<Task> fresh){
        Challenge clean=restarted(fresh);clean.challengeActive=true;return clean;
    }
    int totalSwaps(){return Arrays.stream(swaps).sum();}
    int streak, bestStreak;
    int[] tierStreak=new int[6], tierBest=new int[6], assignedCompletions=new int[6], skips=new int[6], mistakes=new int[6], blocksUsed=new int[6];
    boolean economyInitialized;
    List<String> history = new ArrayList<>();
    Set<Integer> observed = new HashSet<>();
    Set<Integer> manualCompletions = new HashSet<>();
    boolean baseline;
    transient List<Task> tasks = new ArrayList<>();
    int total(int tier) {return (int)tasks.stream().filter(t->t.tier==tier).count();}
    int completed(int tier) {return (int)tasks.stream().filter(t->t.tier==tier&&t.done).count();}
    boolean tierEligible(int tier) {
        if(mode==ALL_TIERS||mode==ONE_TIER)return true;
        if(mode==TIER_BY_TIER){
            for(int i=0;i<tier;i++)if(total(i)==0||completed(i)!=total(i))return false;
            return true;
        }
        if(tier==0)return true;
        return total(tier-1)>0 && completed(tier-1)*2>=total(tier-1)
            && (tier==1 || total(tier-2)>0 && completed(tier-2)==total(tier-2));
    }
    Task task(Integer id) { if(id==null)return null; return tasks.stream().filter(t->t.id==id).findFirst().orElse(null); }
    List<Task> prerequisites(Task task){return mode==CA_LOCKED?related(task,false):Collections.emptyList();}
    List<Task> warnings(Task task){return related(task,true);}
    private List<Task> related(Task task,boolean advisory){
        Map<Integer,Task> result=new LinkedHashMap<>();
        for(Task t:DependencyRules.missing(task,this,advisory))result.put(t.id,t);
        for(Task t:relaxedRules?Collections.<Task>emptyList():KillCountRisk.related(this,task,advisory))result.put(t.id,t);
        return new ArrayList<>(result.values());
    }
    List<Task> blocked(int tier){
        List<Task> result=new ArrayList<>();
        for(Task task:tasks)if(task.tier==tier&&!task.done&&!prerequisites(task).isEmpty())result.add(task);
        return result;
    }
    List<Task> pool(int tier) {
        List<Task> result=new ArrayList<>();
        if(!unlocked(tier)||active[tier]!=null||(mode==ALL_TIERS&&Arrays.stream(active).anyMatch(Objects::nonNull)))return result;
        for(Task t:tasks)if((mode==ALL_TIERS||t.tier==tier)&&!t.done&&!Objects.equals(stored[t.tier],t.id)&&!Objects.equals(excluded[t.tier],t.id)&&prerequisites(t).isEmpty())result.add(t);
        return result;
    }
    Task roll(int tier, Random random) {
        List<Task> pool=pool(tier); if(pool.isEmpty())return null;
        Task task=pool.get(random.nextInt(pool.size()));active[task.tier]=task.id;if(mode==ALL_TIERS)Arrays.fill(excluded,null);else excluded[tier]=null;
        history.add("Assigned: "+task.name); return task;
    }
    boolean blockTask(int tier) {return blockTask(tier,relaxedRules);}
    boolean blockTask(int tier,boolean freeTestBlock) {
        if(!allowStorage||(!freeTestBlock&&blockTokens<=0)||active[tier]==null||stored[tier]!=null||task(active[tier])==null||task(active[tier]).done)return false;
        if(!freeTestBlock)blockTokens--;blocksUsed[tier]++;stored[tier]=active[tier];active[tier]=null;
        history.add("Blocked: "+task(stored[tier]).name);return true;
    }
    boolean canUnblock(int tier){
        Task t=task(stored[tier]);
        return unlocked(tier)&&active[tier]==null&&(mode!=ALL_TIERS||Arrays.stream(active).noneMatch(Objects::nonNull))&&t!=null&&!t.done&&prerequisites(t).isEmpty();
    }
    boolean unblock(int tier) {
        if(!canUnblock(tier))return false;
        Task t=task(stored[tier]);active[tier]=stored[tier];stored[tier]=null;
        history.add("Resumed blocked task: "+t.name);return true;
    }
    boolean buySkip(){if(rulePack!=CUSTOM_RULES||points<SKIP_COST)return false;points-=SKIP_COST;skipTokens++;history.add("Bought skip token (-"+SKIP_COST+" points)");return true;}
    boolean buyBlock(){if(points<BLOCK_COST)return false;points-=BLOCK_COST;blockTokens++;history.add("Bought block token (-"+BLOCK_COST+" points)");return true;}
    int totalSkips(){return Arrays.stream(skips).sum();}
    int totalMistakes(){return Arrays.stream(mistakes).sum();}
    void ensureEconomy(){
        if(economyInitialized)return;
        // Preserve earlier test-account statistics, without charging or rewarding them retroactively.
        for(String entry:history){
            String prefix=entry.startsWith("Skipped: ")?"Skipped: ":entry.startsWith("Off-task: ")?"Off-task: ":null;
            if(prefix==null)continue;
            String name=entry.substring(prefix.length());int suffix=name.lastIndexOf(" (");if(suffix>=0)name=name.substring(0,suffix);
            final String matchName=name;Task t=tasks.stream().filter(x->x.name.equals(matchName)).findFirst().orElse(null);
            if(t!=null){if(prefix.equals("Skipped: "))skips[t.tier]++;else mistakes[t.tier]++;}
        }
        economyInitialized=true;
    }
    void ensurePenaltyLedger() {
        if(penaltyLedgerInitialized)return;
        skippedOwed=new long[6];offTaskOwed=new long[6];
        // Migrate the old saved history without changing total debt or assuming changed task names.
        for(String entry:history){
            boolean skip=entry.startsWith("Skipped: "),off=entry.startsWith("Off-task: ");
            int amountStart=entry.lastIndexOf(" (+");
            if((!skip&&!off)||amountStart<0||!entry.endsWith(" gp)"))continue;
            String name=entry.substring(skip?9:10,amountStart);
            Task match=tasks.stream().filter(t->t.name.equals(name)).findFirst().orElse(null);
            if(match==null)continue;
            try{
                long amount=Long.parseLong(entry.substring(amountStart+3,entry.length()-4));
                if(amount>=0){long[] ledger=skip?skippedOwed:offTaskOwed;ledger[match.tier]=Math.addExact(ledger[match.tier],amount);}
            }catch(NumberFormatException ignored){}
        }
        penaltyLedgerInitialized=true;
    }
    long tierOwed(int tier){ensurePenaltyLedger();return Math.addExact(skippedOwed[tier],offTaskOwed[tier]);}
    long unattributedOwed(){ensurePenaltyLedger();long sum=0;for(int tier=0;tier<6;tier++)sum=Math.addExact(sum,tierOwed(tier));return Math.max(0,owed-sum);}
    boolean canSkip(int tier){
        Task current=task(active[tier]);
        if(!allowSkipping||current==null||current.done||(rulePack==CA_RULES?tierSkipsRemaining[tier]<=0:rulePack!=FREE_PLAY&&skipTokens<=0))return false;
        // A skip must have a different eligible result for the next roll.
        return tasks.stream().anyMatch(t->(mode==ALL_TIERS||t.tier==tier)&&!t.done&&t.id!=current.id&&!Objects.equals(stored[t.tier],t.id)&&prerequisites(t).isEmpty());
    }
    boolean skip(int tier) {
        if(!canSkip(tier))return false;
        Task t=task(active[tier]);if(t==null||t.done)return false;
        ensureEconomy();if(rulePack==CA_RULES){tierSkipsRemaining[tier]--;rerollsSpent[tier]++;}else if(rulePack!=FREE_PLAY)skipTokens--;skips[tier]++;if(mode==ALL_TIERS)Arrays.fill(excluded,null);excluded[tier]=active[tier];active[tier]=null;
        history.add("Rerolled: "+t.name);return true;
    }
    void ensureEventBaseline(){
        if(eventBaseline)return;
        eventStreak=streak;eventBest=bestStreak;eventTierStreak=tierStreak.clone();eventTierBest=tierBest.clone();eventBaseline=true;
    }
    void recordCompletion(Task t,boolean assigned,boolean bonus,long delta,boolean advance){
        CompletionEvent event=new CompletionEvent();event.id=t.id;event.tier=t.tier;event.assigned=assigned;event.bonus=bonus;event.pointDelta=delta;event.advanceStreak=advance;completionEvents.add(event);
    }
    void undoCompletion(Task t){
        CompletionEvent event=completionEvents.stream().filter(e->e.id==t.id).findFirst().orElse(null);
        if(event!=null){
            points=Math.max(0,points-event.pointDelta);
            if(!event.ignoreStats){
            if(event.assigned)assignedCompletions[t.tier]=Math.max(0,assignedCompletions[t.tier]-1);
            else if(event.bonus)bonusCompletions[t.tier]=Math.max(0,bonusCompletions[t.tier]-1);
            else mistakes[t.tier]=Math.max(0,mistakes[t.tier]-1);
            }
            completionEvents.remove(event);
            streak=eventStreak;bestStreak=eventBest;tierStreak=eventTierStreak.clone();tierBest=eventTierBest.clone();
            for(CompletionEvent e:completionEvents){
                if(e.ignoreStats)continue;
                if(!e.assigned&&!e.bonus){streak=0;Arrays.fill(tierStreak,0);}
                else if(e.assigned&&e.advanceStreak){streak++;bestStreak=Math.max(bestStreak,streak);tierStreak[e.tier]++;tierBest[e.tier]=Math.max(tierBest[e.tier],tierStreak[e.tier]);}
            }
        }else if(manualCompletions.contains(t.id)&&!statsResetBaseline.contains(t.id)){
            // Recover manual repairs made before the reversible completion journal existed.
            boolean assigned=history.stream().anyMatch(h->h.startsWith("Completed assignment: "+t.name+" (+"));
            if(assigned){points=Math.max(0,points-(t.tier+1));assignedCompletions[t.tier]=Math.max(0,assignedCompletions[t.tier]-1);streak=Math.max(0,streak-1);tierStreak[t.tier]=Math.max(0,tierStreak[t.tier]-1);bestStreak=Math.min(bestStreak,streak);tierBest[t.tier]=Math.min(tierBest[t.tier],tierStreak[t.tier]);}
        }
        if(event==null&&eventBaseline&&completionEvents.isEmpty()){
            eventStreak=streak;eventBest=bestStreak;eventTierStreak=tierStreak.clone();eventTierBest=tierBest.clone();
        }
        observed.remove(t.id);
    }
    boolean markIncompleteManually(int id){
        Task target=task(id);if(target==null||!target.done)return false;
        undoCompletion(target);manualCompletions.remove(id);manualIncomplete.add(id);
        List<Task> replacement=new ArrayList<>();for(Task t:tasks)replacement.add(t.id==id?new Task(t.id,t.tier,t.name,t.description,t.boss,false,t.type):t);
        tasks=replacement;history.add("Manual incomplete repair: "+target.name);return true;
    }
    void refreshFromGame(List<Task> fresh){
        Set<Integer> completed=new HashSet<>();for(Task t:fresh)if(t.done)completed.add(t.id);
        for(Task t:new ArrayList<>(tasks))if(t.done&&!completed.contains(t.id))undoCompletion(t);
        manualCompletions.clear();manualIncomplete.clear();sync(fresh);
    }
    boolean markCompleteManually(int id){
        Task target=task(id);if(target==null||target.done)return false;
        if(manualCompletions==null)manualCompletions=new HashSet<>();
        manualIncomplete.remove(id);manualCompletions.add(id);history.add("Manual completion repair: "+target.name);
        sync(new ArrayList<>(tasks),false);return true;
    }
    void sync(List<Task> fresh) {sync(fresh,true);}
    private void sync(List<Task> fresh,boolean fromGame) {
        if(manualCompletions==null)manualCompletions=new HashSet<>();
        List<Task> merged=new ArrayList<>();
        for(Task t:fresh){
            if(fromGame&&t.done)manualCompletions.remove(t.id);
            boolean done=!manualIncomplete.contains(t.id)&&(t.done||manualCompletions.contains(t.id));
            merged.add(done==t.done?t:new Task(t.id,t.tier,t.name,t.description,t.boss,done,t.type));
        }
        fresh=merged;
        tasks=fresh;ensurePenaltyLedger();ensureEconomy();
        if(manualTierUnlocks&&!easyUnlockInitialized&&total(0)>0){
            // Preserve existing activations/assignments; fresh zero-progress accounts start ready.
            tierActivated[0]=tierActivated[0]||completed(0)>0||active[0]!=null||stored[0]!=null;
            easyUnlockInitialized=true;
        }
        // Existing account completions establish a baseline: no retrospective points or penalties.
        if(!baseline){for(Task t:fresh)if(t.done)observed.add(t.id);baseline=true;return;}
        List<Task> correct=new ArrayList<>(),wrong=new ArrayList<>(),bonus=new ArrayList<>();
        for(Task t:fresh)if(t.done&&!observed.contains(t.id)){
            observed.add(t.id);
            if(Objects.equals(active[t.tier],t.id))correct.add(t);else if(relaxedRules)bonus.add(t);else wrong.add(t);
        }
        ensureEventBaseline();
        // Evaluate a simultaneous completion batch independently of catalogue order.
        for(Task t:correct){
            active[t.tier]=null;points=Math.addExact(points,t.tier+1);assignedCompletions[t.tier]++;recordCompletion(t,true,false,t.tier+1,wrong.isEmpty());
            history.add("Completed assignment: "+t.name+" (+"+(t.tier+1)+" points)");
        }
        for(Task t:bonus){
            points=Math.addExact(points,t.tier+1);bonusCompletions[t.tier]++;recordCompletion(t,false,true,t.tier+1,false);
            history.add("Bonus completed: "+t.name+" (+"+(t.tier+1)+" points)");
        }
        for(Task t:wrong){
            long previous=points;
            mistakes[t.tier]++;points=Math.max(0,points-(t.tier+1));recordCompletion(t,false,false,points-previous,false);
            history.add("Off-task: "+t.name+" (-"+(t.tier+1)+" points)");
        }
        if(!wrong.isEmpty()){streak=0;Arrays.fill(tierStreak,0);}
        else for(Task t:correct){
            streak++;bestStreak=Math.max(bestStreak,streak);
            tierStreak[t.tier]++;tierBest[t.tier]=Math.max(tierBest[t.tier],tierStreak[t.tier]);
        }
        for(Task t:fresh)if(t.done&&Objects.equals(stored[t.tier],t.id))stored[t.tier]=null;
    }
}
