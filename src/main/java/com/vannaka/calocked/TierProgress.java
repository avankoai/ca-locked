package com.vannaka.calocked;

import java.util.Arrays;

final class TierProgress {
 static int[] fillWidths(int[] capacities,int completed,int total){
  int available=Arrays.stream(capacities).sum();
  int remaining=total<=0?0:(int)Math.round(available*Math.min(1.0,Math.max(0.0,completed/(double)total)));
  int[] result=new int[capacities.length];
  for(int i=0;i<result.length;i++){result[i]=Math.min(capacities[i],remaining);remaining-=result[i];}
  return result;
 }
}
