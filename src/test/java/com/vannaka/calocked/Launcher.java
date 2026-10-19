package com.vannaka.calocked;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
public class Launcher {
 public static void main(String[] args)throws Exception{
  ExternalPluginManager.loadBuiltin(CaLockedPlugin.class);
  RuneLite.main(args);
 }
}
