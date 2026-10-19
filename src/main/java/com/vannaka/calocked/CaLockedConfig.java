package com.vannaka.calocked;
import net.runelite.client.config.*;
@ConfigGroup("calocked")
public interface CaLockedConfig extends Config {
 enum KillcountSetup {
  CHOOSE_ACTION("Choose an action"), NEW_ACCOUNT("New account: start at zero"), IMPORT_HISCORES("Import highscores and fill gaps"), FILL_MISSING("Fill missing killcounts"), ENTER_BOSS_KC("Enter a boss killcount");
  private final String label;
  KillcountSetup(String label){this.label=label;}
  @Override public String toString(){return label;}
 }
 @ConfigItem(keyName="rollSound",name="Roll sounds",description="Play a tick while tasks scroll and a confirmation when the roll finishes",position=0)
 default boolean rollSound(){return true;}
 @Range(min=0,max=100)
 @ConfigItem(keyName="rollVolume",name="Roll volume (%)",description="Task picker volume from 0 (silent) to 100 (maximum). Applies to rolling ticks and the final confirmation sound.",position=1)
 default int rollVolume(){return 55;}
 @ConfigItem(keyName="killcountSetup",name="Killcount setup",description="Apply to the character currently logged in. Import highscores after logging out and back in, fill missing counts, or start a genuinely new account at zero.",position=2)
 default KillcountSetup killcountSetup(){return KillcountSetup.CHOOSE_ACTION;}
}
