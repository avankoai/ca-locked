package com.vannaka.calocked;
import java.awt.*;
import java.awt.image.BufferedImage;
/** Whole canvases avoid the cache sprites' trimmed offsets when drawn as widgets. */
final class StatusSprites {
 static BufferedImage cardHighlight(int width,int height,int colour){
  BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
  // Native tier corners cut only the outer two pixels, not a seven-pixel diagonal.
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   if(x+y<2||(width-1-x)+y<2||x+(height-1-y)<2||(width-1-x)+(height-1-y)<2)continue;
   image.setRGB(x,y,0xff000000|colour);
  }
  return image;
 }
 static BufferedImage warning(BufferedImage source){
  BufferedImage image=new BufferedImage(20,22,BufferedImage.TYPE_INT_ARGB);
  Graphics2D g=image.createGraphics();
  try{
   // Deliberate pixel edges keep the small warning crisp, without resampling.
   g.setColor(new Color(0x30261b));g.fillPolygon(new int[]{9,18,18,2,2},new int[]{2,18,20,20,18},5);
   g.setColor(new Color(0xe8b744));g.fillPolygon(new int[]{9,16,3},new int[]{4,18,18},3);
   g.setColor(new Color(0xffd568));g.drawLine(9,4,4,16);g.drawLine(4,18,15,18);
   g.setColor(new Color(0x392817));g.fillRect(8,8,3,6);g.fillRect(8,16,3,2);
  }finally{g.dispose();}
  return image;
 }
 static BufferedImage lock(){return lock(false);}
 static BufferedImage openLock(){return lock(true);}
 private static BufferedImage lock(boolean open){
  BufferedImage image=new BufferedImage(20,22,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();
  try{
   if(open){
    // Move the shackle left and lift its free end clear of the body.
    g.setColor(new Color(0x332b21));g.fillRect(4,0,8,2);g.fillRect(2,2,3,5);g.fillRect(10,2,3,9);
    g.setColor(new Color(0xc7b791));g.fillRect(5,1,6,2);g.fillRect(3,3,2,3);g.fillRect(11,3,2,8);
    g.setColor(new Color(0x877654));g.fillRect(5,3,1,3);g.fillRect(10,3,1,8);
   }else{
    g.setColor(new Color(0x332b21));g.fillRect(6,2,8,2);g.fillRect(4,4,12,8);
    g.setColor(new Color(0xc7b791));g.fillRect(7,3,6,2);g.fillRect(5,5,2,6);g.fillRect(13,5,2,6);
    g.setColor(new Color(0x877654));g.fillRect(7,5,1,6);g.fillRect(12,5,1,6);
   }
   g.setColor(new Color(0x392a1c));g.fillRect(3,10,14,10);g.fillRect(4,9,12,12);
   g.setColor(new Color(0xd6a750));g.fillRect(4,10,12,9);
   g.setColor(new Color(0xf0ce7b));g.drawLine(4,10,15,10);g.drawLine(4,11,4,18);
   g.setColor(new Color(0x987035));g.drawLine(5,19,15,19);g.drawLine(15,11,15,18);
   g.setColor(new Color(0x49331c));g.fillRect(8,12,4,3);g.fillRect(9,15,2,3);
   g.setColor(new Color(0x79552a));g.fillRect(8,12,1,2);
  }finally{g.dispose();}
  return image;
 }
}
