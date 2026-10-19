package com.vannaka.calocked;

import net.runelite.api.Client;
import net.runelite.api.Preferences;

final class PickerAudio {
 static void play(Client client,int sound,int percent,double level){
  int volume=(int)Math.round(127*Math.max(0,Math.min(100,percent))/100.0*Math.max(0,Math.min(1,level)));
  if(volume<=0)return;
  Preferences preferences=client.getPreferences();if(preferences==null)return;
  int original=preferences.getSoundEffectVolume();
  // This API creates the audio stream synchronously, but overrides the supplied gain
  // with the game volume unless muted. Restore the preference before leaving this call.
  try{preferences.setSoundEffectVolume(0);client.playSoundEffect(sound,volume);}
  finally{preferences.setSoundEffectVolume(original);}
 }
}
