package com.vannaka.calocked;
import java.util.*;
/** Bounded lines allow arbitrarily long descriptions to be paged in the dashboard. */
final class TaskText {
 static String fit(String text,int width,java.util.function.ToIntFunction<String> measure){
  if(measure.applyAsInt(text)<=width)return text;
  String suffix="...";if(measure.applyAsInt(suffix)>width)return "";
  int end=text.length();
  while(end>0&&measure.applyAsInt(text.substring(0,end)+suffix)>width)end--;
  return text.substring(0,end).stripTrailing()+suffix;
 }
 /** Picker previews are measured in game-font pixels and explicitly limited in height. */
 static List<String> preview(String text,int width,int maxLines,java.util.function.ToIntFunction<String> measure){
  List<String> lines=new ArrayList<>();
  String plain=text.replace("<br>","\n").replaceAll("<[^>]*>","");
  for(String paragraph:plain.split("\n",-1)){
   String line="";
   for(String word:paragraph.trim().split("\\s+")){
    if(word.isEmpty())continue;
    if(!line.isEmpty()&&measure.applyAsInt(line+" "+word)>width){lines.add(line);line="";}
    while(measure.applyAsInt(word)>width){
     int end=word.length();while(end>1&&measure.applyAsInt(word.substring(0,end))>width)end--;
     lines.add(word.substring(0,end));word=word.substring(end);
    }
    if(!word.isEmpty())line+=(line.isEmpty()?"":" ")+word;
   }
   lines.add(line);
  }
  if(lines.size()>maxLines){
   lines=new ArrayList<>(lines.subList(0,maxLines));String last=lines.get(maxLines-1);int end=last.length();
   while(end>0&&measure.applyAsInt(last.substring(0,end)+"...")>width)end--;
   lines.set(maxLines-1,last.substring(0,end).stripTrailing()+"...");
  }
  return lines;
 }
 static List<String> wrap(String text,int columns){
  List<String> lines=new ArrayList<>();
  String plain=text.replace("<br>","\n").replaceAll("<[^>]*>","");
  for(String paragraph:plain.split("\n",-1)){
   String line="";
   for(String word:paragraph.trim().split("\\s+")){
    if(word.isEmpty())continue;
    if(!line.isEmpty()&&line.length()+1+word.length()>columns){lines.add(line);line="";}
    while(word.length()>columns){if(!line.isEmpty()){lines.add(line);line="";}lines.add(word.substring(0,columns));word=word.substring(columns);}
    if(!word.isEmpty())line+= (line.isEmpty()?"":" ")+word;
   }
   lines.add(line);
  }
  return lines;
 }
}
