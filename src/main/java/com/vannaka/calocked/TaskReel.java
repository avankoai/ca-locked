package com.vannaka.calocked;
import java.util.*;
/** One physical row per eligible task; the ring turns independently of selection. */
final class TaskReel {
 static final int ROW_HEIGHT=34;
 static final long REVEAL_DURATION=2000;
 final List<Challenge.Task> entries=new ArrayList<>();
 final long start,duration;
 final int target;
 TaskReel(List<Challenge.Task> pool,Challenge.Task winner,Random random,long start){
  if(pool.isEmpty()||winner==null)throw new IllegalArgumentException("Reel needs eligible tasks and a winner");
  this.start=start;
  Set<Integer> ids=new HashSet<>();
  for(Challenge.Task task:pool)if(ids.add(task.id))entries.add(task);
  Collections.shuffle(entries,random);
  int index=-1;for(int i=0;i<entries.size();i++)if(entries.get(i).id==winner.id)index=i;
  if(index<0)throw new IllegalArgumentException("Winner must be eligible");
  // Large pools need only a short pass, rather than hundreds of unreadable rows.
  if(entries.size()>9){int stop=8+random.nextInt(Math.min(11,entries.size()-8));Collections.swap(entries,index,stop);index=stop;}
  target=entries.size()==1?0:entries.size()<=9?entries.size()*2+index:index;
  duration=entries.size()==1?700:entries.size()==2?2600:4200;
 }
 double position(long now){
  double t=Math.max(0,Math.min(1,(now-start)/(double)duration));
  return target*(1-Math.pow(1-t,3));
 }
 double offset(int index,long now){
  double n=entries.size(),offset=(index-position(now))%n;
  if(offset< -n/2)offset+=n;
  if(offset>=n/2)offset-=n;
  return offset;
 }
 int height(){return Math.min(106,entries.size()*ROW_HEIGHT);}
 int centreY(){return (height()-ROW_HEIGHT)/2;}
 boolean done(long now){return now-start>=duration;}
 boolean finished(long now){return now-start>=duration+REVEAL_DURATION;}
}
