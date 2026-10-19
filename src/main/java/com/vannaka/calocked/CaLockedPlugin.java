package com.vannaka.calocked;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import net.runelite.client.input.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Deque;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.input.MouseAdapter;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.*;
import net.runelite.client.ui.overlay.*;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@PluginDescriptor(name="CA Locked",description="Combat Achievement Locked dashboard and manual task picker",tags={"combat","achievement","vannaka"})
public class CaLockedPlugin extends Plugin {
 @Inject Client client;
 @Inject ClientThread clientThread;
 @Inject OverlayManager overlays;
 @Inject TooltipManager tooltips;
 @Inject MouseManager mouseManager;
 @Inject SpriteManager sprites;
 @Inject ConfigManager config;
 @Inject Gson gson;
 @Inject net.runelite.client.account.SessionManager sessions;
 @Inject java.util.concurrent.ScheduledExecutorService executor;
 @Inject CaLockedConfig settings;
 @Inject net.runelite.client.hiscore.HiscoreClient hiscores;
 private long killcountSyncToken;
 @Provides CaLockedConfig provideConfig(ConfigManager manager){return manager.getConfig(CaLockedConfig.class);}
 @Inject net.runelite.client.game.chatbox.ChatboxPanelManager chatbox;
 private final Random random=new Random();
 private Challenge state=new Challenge();
 private String profile;
 private ChallengeStore store;
 private volatile boolean profileReloadRequested;
 private boolean storageUnavailable;
 private Orb orb;
 private volatile BufferedImage mediumSword;
 private MouseAdapter mouse;
 private WidgetNode node;
 private Widget host;
 private boolean open;
 private int selected=0;
 private int view=0,page=0;
 private int taskFilter=1;
 private String bossFilter="All";
 private int dropdown,listScroll,dropdownScroll;
 private Widget scrollViewport,scrollRail,scrollThumb,scrollThumbTop,scrollThumbBottom;
 private boolean menuOpen;
 private Widget disabledRerollButton,disabledStorageButton,unavailableStorageButton;
 private static final class RowHighlight {
  final Widget bar;final int normal,hovered;
  Widget icon,status;boolean locked;
  RowHighlight(Widget bar,int normal,int hovered){this.bar=bar;this.normal=normal;this.hovered=hovered;}
 }
 private final List<RowHighlight> taskRowHighlights=new ArrayList<>();
 private final Map<Widget,Widget[]> helpTargets=new HashMap<>();
 private final Map<Widget,String> hoverHelp=new java.util.concurrent.ConcurrentHashMap<>();
 private Widget unlockGlow,unlockEdge,unlockFill;
 private boolean unlockHovered;
 private int rulesTab,statsTab;
 private boolean mostCompletedAllTiers;
 private int scrollTotal,scrollHeight,scrollTrackY,scrollTrackHeight,scrollThumbHeight;
 private volatile Rectangle scrollBounds=new Rectangle(),scrollRailBounds=new Rectangle(),scrollThumbBounds=new Rectangle(),scrollBarBounds=new Rectangle();
 private volatile boolean draggingScroll;
 private volatile int dragStartPointer,dragStartScroll,currentScroll;
 private volatile boolean swallowScrollClick;
 private volatile Rectangle popupBounds=new Rectangle();
 private final java.util.concurrent.atomic.AtomicInteger latestScrollPointer=new java.util.concurrent.atomic.AtomicInteger();
 private final java.util.concurrent.atomic.AtomicBoolean scrollPointerQueued=new java.util.concurrent.atomic.AtomicBoolean();
 private MouseWheelListener wheel;
 private double wheelRemainder;
 private static final String[] TASK_STATUSES={"All","Incomplete","Complete","Locked"};
 private Challenge.Task detail;
 private int detailPage;
 private int detailReturnScroll;
 private String taskSearch="";
 private long searchRequest;
 private Challenge.Task statusTask;
 private Challenge.Task repairTask;
 private boolean statusLocked;
 private int statusReturnScroll;
 private int[] statusGeometry;
 private final Map<Integer,int[]> pageGeometry=new HashMap<>();
 private final Map<Widget,int[]> pageHeaderWidths=new HashMap<>();
 private final Deque<DetailVisit> detailHistory=new ArrayDeque<>();
 private static final class DetailVisit {
  final Challenge.Task task;final int page;
  DetailVisit(Challenge.Task task,int page){this.task=task;this.page=page;}
 }
 private static final class DetailLine {
  final String text;final int colour;final Challenge.Task target;
  DetailLine(String text,int colour,Challenge.Task target){this.text=text;this.colour=colour;this.target=target;}
 }
 private int setupStep;
 private Widget stepEdge,stepFill;
 private boolean dirty=true;
 private int[] completionSnapshot;
 // Game settings getter script 6716, option 160: varbit 12456 (varp 1795, bit 18).
 private static final int CA_REPEAT_COMPLETION_VARBIT=12456;
 private Boolean repeatCompletionSnapshot;
 private long rollEnd;
 private boolean revealPresented;
 private Challenge.Task reveal;
 private TaskReel reel;
 private final List<Widget> reelRows=new ArrayList<>();
 private Widget reelLayer;
 private int[] reelGeometry;
 private int lastReelStep=-1;
 private final Random visualRandom=new Random();
 private String notice="";
 private Confirmation confirmation;
 private static final class Confirmation {
  final String title,subject,message,account;final Runnable action;
  Confirmation(String title,String subject,String message,String account,Runnable action){this.title=title;this.subject=subject;this.message=message;this.account=account;this.action=action;}
 }
 private Widget content;
 private final List<Widget> added=new ArrayList<>();
 private final int[] completionVarps={3116,3117,3118,3119,3120,3121,3122,3123,3124,3125,3126,3127,3128,3387,3718,3773,3774,4204,4496,4721,5673};
 private final Map<Widget,Boolean> hiddenBefore=new IdentityHashMap<>();
 private static final int GROUP=717;
 private static final int GOLD=0xff981f,WHITE=0xffffff;
 private static final int WARNING_SPRITE=10010001,LOCK_SPRITE=10010002,OPEN_LOCK_SPRITE=10010003;
 private final Map<String,Integer> cardHighlightSprites=new HashMap<>();
 @Subscribe public void onConfigChanged(net.runelite.client.events.ConfigChanged event){
  if(!event.getGroup().equals("calocked"))return;
  if(event.getKey().equals("state")){
   if(store!=null&&Objects.equals(event.getProfile(),profile)&&!store.matches(event.getProfile(),event.getNewValue()))profileReloadRequested=true;
   return;
  }
  if(event.getKey().equals("rollSound")||event.getKey().equals("rollVolume")){
   clientThread.invokeLater(()->{if(open&&view==5&&state.challengeActive)refresh();});return;
  }
  if(!event.getKey().equals("killcountSetup"))return;
  CaLockedConfig.KillcountSetup action=settings.killcountSetup();
  if(action==CaLockedConfig.KillcountSetup.CHOOSE_ACTION)return;
  config.setConfiguration("calocked","killcountSetup",CaLockedConfig.KillcountSetup.CHOOSE_ACTION);
  clientThread.invokeLater(()->{
   if(client.getGameState()!=GameState.LOGGED_IN||profile==null||!profile.equals(config.getRSProfileKey()))return;
   String account=profile;
   if(action==CaLockedConfig.KillcountSetup.NEW_ACCOUNT){
    if(KillCountRisk.startAtZero(state)){
     save();client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: new-account killcounts start at zero. Future kills update them automatically.",null);
    }else client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: this account already has combat achievements or recorded kills. Enter its known boss killcounts instead.",null);
    if(open&&rollEnd==0)refresh();
   }else if(action==CaLockedConfig.KillcountSetup.IMPORT_HISCORES){importHiscores(account);}
   else if(action==CaLockedConfig.KillcountSetup.FILL_MISSING){promptMissingKillcounts(account,++killcountSyncToken,new ArrayList<>(HiscoreKillCounts.missing(state)),0);}
   else chatbox.openTextInput("Boss name = killcount (for example King Black Dragon = 21)").value("")
    .onDone((java.util.function.Consumer<String>)value->clientThread.invokeLater(()->{
     if(!account.equals(profile)||!account.equals(config.getRSProfileKey()))return;
     String message;
     try{message=KillCountRisk.enterCount(state,value);save();}catch(IllegalArgumentException error){message=error.getMessage();}
     client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: "+message,null);
     if(open&&rollEnd==0)refresh();
    })).build();
  });
 }
 private void importHiscores(String account){
  if(client.getLocalPlayer()==null)return;
  String player=client.getLocalPlayer().getName();long token=++killcountSyncToken;
  client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: fetching published boss killcounts. Log out and back in before importing for a fresher snapshot.",null);
  net.runelite.client.hiscore.HiscoreEndpoint endpoint=net.runelite.client.hiscore.HiscoreEndpoint.fromWorldTypes(client.getWorldType());
  hiscores.lookupAsync(player,endpoint).whenComplete((result,error)->clientThread.invokeLater(()->{
   if(!validKillcountSetup(account,token))return;
   if(error!=null||!HiscoreKillCounts.samePlayer(player,result)){
    client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: highscores unavailable or player not listed. No counts changed. Try again later, or choose Fill missing killcounts.",null);return;
   }
   int updated=HiscoreKillCounts.importCounts(state,result);save();if(open&&rollEnd==0)refresh();
   List<String> missing=HiscoreKillCounts.missing(state);
   client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: "+updated+" boss counters updated; "+missing.size()+" relevant counters still need an exact value. Missing highscores are not zero. Enter a count, SKIP, or STOP in the following prompts.",null);
   promptMissingKillcounts(account,token,missing,0);
  }));
 }
 private boolean validKillcountSetup(String account,long token){
  return killcountSyncToken==token&&client.getGameState()==GameState.LOGGED_IN&&account.equals(profile)&&account.equals(config.getRSProfileKey());
 }
 private void promptMissingKillcounts(String account,long token,List<String> missing,int index){
  if(!validKillcountSetup(account,token))return;
  while(index<missing.size()&&(state.zeroKillcountBaseline||state.killCounts.containsKey(missing.get(index))))index++;
  if(index>=missing.size()){
   client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: killcount setup finished. "+HiscoreKillCounts.missing(state).size()+" relevant counters remain unknown.",null);return;
  }
  final int current=index;String boss=missing.get(index);
  Integer lower=KillCountRisk.known(state,boss);
  String prompt=HiscoreKillCounts.displayName(boss)+": enter KC"+(lower==null?"":" (at least "+lower+")")+"; SKIP / STOP";
  chatbox.openTextInput(prompt).value("").onDone((java.util.function.Consumer<String>)value->clientThread.invokeLater(()->{
   if(!validKillcountSetup(account,token))return;
   if(value.trim().equalsIgnoreCase("STOP")||value.trim().isEmpty())return;
   if(value.trim().equalsIgnoreCase("SKIP")){promptMissingKillcounts(account,token,missing,current+1);return;}
   try{
    String message=KillCountRisk.enterCount(state,boss+" = "+value);save();
    client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: "+message,null);
    if(open&&rollEnd==0)refresh();promptMissingKillcounts(account,token,missing,current+1);
   }catch(IllegalArgumentException error){
    client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: "+error.getMessage(),null);
    promptMissingKillcounts(account,token,missing,current);
   }
  })).build();
 }
 @Override protected void startUp(){
  store=new ChallengeStore(new ChallengeStore.Backend(){
   public String currentProfile(){return config.getRSProfileKey();}
   public String read(String key){return config.getConfiguration("calocked",key,"state");}
   public void write(String key,String json){config.setConfiguration("calocked",key,"state",json);}
  },gson);
  orb=new Orb();overlays.add(orb);
  sprites.getSpriteAsync(3400,0,(java.util.function.Consumer<BufferedImage>)image->mediumSword=image);

  mouse=new MouseAdapter(){
   @Override public MouseEvent mousePressed(MouseEvent e){
    if(e.getButton()==MouseEvent.BUTTON1&&open&&scrollRailBounds.contains(e.getPoint())){
     boolean onThumb=scrollThumbBounds.contains(e.getPoint());
     dragStartPointer=e.getY();
     dragStartScroll=onThumb?currentScroll:TaskBrowser.fromPointer(e.getY()-scrollRailBounds.y-scrollThumbHeight/2,scrollTotal,scrollHeight,scrollTrackHeight,scrollThumbHeight);
     draggingScroll=true;swallowScrollClick=true;e.consume();queueScrollPointer(e.getY());return e;
    }
    swallowScrollClick=false;
    if(e.getButton()==MouseEvent.BUTTON1&&open&&!popupBounds.isEmpty()&&!popupBounds.contains(e.getPoint())&&!orb.bounds.contains(e.getPoint())){
     e.consume();clientThread.invokeLater(()->{menuOpen=false;dropdown=0;refresh();});return e;
    }
    if(e.getButton()==MouseEvent.BUTTON1&&orb.bounds.contains(e.getPoint())){
     e.consume();clientThread.invokeLater(()->toggle());
    }
    return e;
   }
   @Override public MouseEvent mouseDragged(MouseEvent e){if(draggingScroll){e.consume();queueScrollPointer(e.getY());}return e;}
   @Override public MouseEvent mouseReleased(MouseEvent e){if(draggingScroll){queueScrollPointer(e.getY());draggingScroll=false;e.consume();}if(orb.bounds.contains(e.getPoint()))e.consume();return e;}
   @Override public MouseEvent mouseClicked(MouseEvent e){if(swallowScrollClick){swallowScrollClick=false;e.consume();}else if(orb.bounds.contains(e.getPoint()))e.consume();return e;}
  };
  mouseManager.registerMouseListener(mouse);
  wheel=e->{if(open&&(scrollBounds.contains(e.getPoint())||scrollBarBounds.contains(e.getPoint()))){double amount=e.getPreciseWheelRotation()*36;e.consume();clientThread.invokeLater(()->{wheelRemainder+=amount;int pixels=(int)wheelRemainder;wheelRemainder-=pixels;scrollBy(pixels);});}return e;};
  mouseManager.registerMouseWheelListener(wheel);
 }
 @Override protected void shutDown(){overlays.remove(orb);mouseManager.unregisterMouseListener(mouse);mouseManager.unregisterMouseWheelListener(wheel);clientThread.invokeLater(()->{
  killcountSyncToken++;close();client.getSpriteOverrides().remove(WARNING_SPRITE);client.getSpriteOverrides().remove(LOCK_SPRITE);client.getSpriteOverrides().remove(OPEN_LOCK_SPRITE);
  for(int id:cardHighlightSprites.values())client.getSpriteOverrides().remove(id);cardHighlightSprites.clear();
  for(int id:new int[]{172,173,314,315})client.getSpriteOverrides().remove(10020000+id);
 });}
 @Subscribe public void onGameStateChanged(GameStateChanged e){
  if(e.getGameState()==GameState.LOGIN_SCREEN&&profile!=null)executor.submit(config::sendConfig);
  if(e.getGameState()!=GameState.LOGGED_IN){killcountSyncToken++;confirmation=null;}
  if(e.getGameState()!=GameState.LOGGED_IN){open=false;node=null;host=null;content=null;profile=null;taskSearch="";searchRequest++;statusGeometry=null;pageGeometry.clear();pageHeaderWidths.clear();state=new Challenge();completionSnapshot=null;rollEnd=0;reel=null;reelRows.clear();reelLayer=null;reelGeometry=null;detail=null;statusTask=null;repairTask=null;detailHistory.clear();clearScroll();dropdown=0;menuOpen=false;added.clear();hiddenBefore.clear();}
  dirty=true;
 }
 @Subscribe public void onChatMessage(ChatMessage event){
  if(!profileWritable())return;
  if(event.getType()!=ChatMessageType.GAMEMESSAGE&&event.getType()!=ChatMessageType.SPAM)return;
  if(TaskSuccesses.record(state,event.getMessage())){save();if(open&&rollEnd==0)refresh();}
  if(KillCountRisk.chat(state,event.getMessage())){save();if(open&&rollEnd==0)refresh();}
 }
 private boolean importKillCounts(){
  boolean changed=false;
  for(String alias:KillCountRisk.aliases()){
   Integer count=config.getRSProfileConfiguration("killcount",alias,Integer.class);
   if(count!=null)changed|=KillCountRisk.observe(state,alias,count);
  }
  return changed;
 }
 @Subscribe public void onVarbitChanged(VarbitChanged e){
  for(int id:completionVarps)if(e.getVarpId()==id){dirty=true;return;}
 }
 @Subscribe public void onGameTick(GameTick e){
  if(client.getGameState()!=GameState.LOGGED_IN)return;
  String key=config.getRSProfileKey();
  if(key==null)return;
  // Also compare the actual bits: completion detection does not depend on an event's id format.
  if(completionSnapshot==null)completionSnapshot=new int[completionVarps.length];
  for(int i=0;i<completionVarps.length;i++){
   int value=client.getVarpValue(completionVarps[i]);
   if(value!=completionSnapshot[i]){completionSnapshot[i]=value;dirty=true;}
  }
  if(!key.equals(profile)||profileReloadRequested||(!storageUnavailable&&!store.current())){
   if(open)close();
   killcountSyncToken++;profile=key;profileReloadRequested=false;setupStep=0;view=0;
   try{state=store.load(key);storageUnavailable=false;notice="";}
   catch(RuntimeException ex){storageUnavailable=true;notice="Saved challenge could not be read. Your saved data has not been overwritten.";return;}
   state.relaxedRules=true;state.manualTierUnlocks=true;
   state.beginReadyNotificationSession();setupStep=0;
   if(!state.tierUnlocksInitialized){
    for(int i=0;i<6;i++)if(state.active[i]!=null||state.stored[i]!=null)state.tierActivated[i]=true;
    state.tierUnlocksInitialized=true;
   }
   if(!state.rerollSettingsInitialized){
    if(state.rulePack==Challenge.CUSTOM_RULES){state.rulePack=Challenge.CA_RULES;state.rerollLimit=3;Arrays.fill(state.tierSkipsRemaining,state.rerollLimit);}
    for(int i=0;i<6;i++)state.rerollsSpent[i]=Math.max(0,state.rerollLimit-state.tierSkipsRemaining[i]);
    state.rerollSettingsInitialized=true;
   }
   dirty=true;
  }
  if(storageUnavailable)return;
  if(importKillCounts()){save();if(open&&rollEnd==0)refresh();}
  if(dirty){List<Challenge.Task> tasks=readTasks();if(!tasks.isEmpty()){state.sync(tasks);
   if(!state.repairMigrationApplied){for(Challenge.Task t:tasks)if(!t.done&&t.name.equals("Sleeping Giant")&&state.manualCompletions.contains(t.id))state.markIncompleteManually(t.id);state.repairMigrationApplied=true;}
   save();dirty=false;if(open&&rollEnd==0)refresh();}}
  List<Integer> readyTiers=state.challengeActive?state.newReadyTiers():Collections.emptyList();
  if(!readyTiers.isEmpty()){
   for(int tier:readyTiers)client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","<col=a54b00>CA Locked: "+Challenge.TIERS[tier]+" can now be unlocked. Open the dashboard to unlock this tier.</col>",null);
   save();
  }
  if(open&&host!=null&&(client.getWidget(GROUP,1)==null||client.getWidget(GROUP,1).isHidden())){open=false;node=null;content=null;added.clear();hiddenBefore.clear();}
 }
 @Subscribe public void onClientTick(ClientTick event){
  if(open){
   boolean repeatEnabled=client.getVarbitValue(CA_REPEAT_COMPLETION_VARBIT)==1;
   if(repeatCompletionSnapshot==null||repeatCompletionSnapshot!=repeatEnabled){
    repeatCompletionSnapshot=repeatEnabled;
    if(view==4&&statsTab==1&&detail==null&&statusTask==null&&repairTask==null&&confirmation==null&&!menuOpen&&dropdown==0)refresh();
   }
   updateScrollBounds();
   updateTaskRowHighlights();
   animateUnlockButton();animateStepButton();
   Widget tierCards=client.getWidget(GROUP,8);
   if(tierCards!=null&&tierCards.getChildren()!=null){int tier=0;for(Widget card:tierCards.getChildren())if(card!=null&&tier<6){
    if(state.readyToUnlock(tier)){int glow=(int)Math.round(100+45*Math.sin(System.currentTimeMillis()/420.0));card.setOpacity(glow);}
    tier++;
   }}
   Widget menu=client.getWidget(GROUP,15);
   if(menu!=null){syncMenuSprite(menu);if(menu.getChildren()!=null)for(Widget child:menu.getChildren())if(child!=null)syncMenuSprite(child);}
  }
  if(!open||rollEnd==0||reel==null)return;
  long now=System.currentTimeMillis();
  if(reel.done(now)&&!revealPresented){
   revealPresented=true;renderReel();
   playPickerSound(3925,1.0);
  }
  if(reel.finished(now)){
   rollEnd=0;restoreReelGeometry();reelRows.clear();refresh();return;
  }
  if(revealPresented)return;
  double position=reel.position(now);int step=(int)Math.floor(position);
  if(step!=lastReelStep){lastReelStep=step;if(reel.entries.size()>1)playPickerSound(3929,0.5);}
  for(int i=0;i<reelRows.size();i++){
   Widget label=reelRows.get(i);
   label.setOriginalY(reel.centreY()+(int)Math.round(reel.offset(i,now)*TaskReel.ROW_HEIGHT));label.revalidate();
  }
 }

 private void playPickerSound(int sound,double level){
  if(!settings.rollSound())return;
  PickerAudio.play(client,sound,settings.rollVolume(),level);
 }

 private List<Challenge.Task> readTasks(){
  List<Challenge.Task> result=new ArrayList<>();
  for(int tier=0;tier<6;tier++){
   EnumComposition enumeration=client.getEnum(3981+tier);if(enumeration==null||enumeration.getIntVals().length==0)return Collections.emptyList();
   for(int structId:enumeration.getIntVals()){
    StructComposition s=client.getStructComposition(structId);if(s==null)return Collections.emptyList();
    int id=s.getIntValue(1306),index=id/32;boolean done=false;
    if(id>=0&&index<completionVarps.length)done=(client.getVarpValue(completionVarps[index])&(1<<(id%32)))!=0;
    else {notice="Unknown completion mapping; rolls paused";return Collections.emptyList();}
    EnumComposition bosses=client.getEnum(3971);
    result.add(new Challenge.Task(id,tier,s.getStringValue(1308),s.getStringValue(1309),bosses==null?"":bosses.getStringValue(s.getIntValue(1312)),done,s.getIntValue(1311)));
   }
  }
  return result;
 }
 private boolean profileWritable(){
  return client.getGameState()==GameState.LOGGED_IN&&!storageUnavailable&&!profileReloadRequested
   &&profile!=null&&profile.equals(config.getRSProfileKey())&&store!=null&&store.current();
 }
 private JavaScriptCallback accountAction(Runnable action){
  final String owner=profile;final Challenge expected=state;
  return e->{
   if(state!=expected||owner==null||!owner.equals(profile)||!profileWritable())return;
   action.run();save();refresh();
  };
 }
 private void save(){
  if(!profileWritable()){if(!storageUnavailable)profileReloadRequested=true;return;}
  if(!store.save(state))profileReloadRequested=true;
 }
 private void toggle(){if(open){close();return;}if(client.getGameState()!=GameState.LOGGED_IN)return;
  if(!profileWritable()){
   client.addChatMessage(ChatMessageType.GAMEMESSAGE,"","CA Locked: "+(storageUnavailable?notice:"Loading your account's challenge. Please try again shortly."),null);return;
  }
  if(!state.challengeActive){view=5;listScroll=0;}
  // MAINMODAL belongs to the play-world layer; SIDE_* belongs to inventory.
  // Select from the actual active top-level interface, not only isResized().
  int root=client.getTopLevelInterfaceId();
  int modalChild=root==548?41:(root==161||root==164?16:-1);
  host=modalChild<0?null:client.getWidget(root,modalChild);
  if(host==null){notice="Viewport unavailable";return;}
  if(host.getNestedChildren()!=null&&host.getNestedChildren().length>0){notice="Close the current game window first";return;}
  try{
   // Open a local native interface: no menu action or gameplay packet is sent.
   node=client.openInterface(host.getId(),GROUP,0);open=true;
   Widget openingRoot=client.getWidget(GROUP,0);
   if(openingRoot==null)throw new IllegalStateException("Dashboard interface unavailable");
   // Keep native default content out of the first frame while its load scripts run.
   hide(openingRoot);
   for(int i=0;i<18;i++){
    Widget w=client.getWidget(GROUP,i);if(w!=null&&w.getOnLoadListener()!=null)
     client.createScriptEventBuilder(w.getOnLoadListener()).setSource(w).build().setCanSendPackets(false).run();
   }
   openingRoot.setHidden(true);
   final WidgetNode openingNode=node;
   clientThread.invokeLater(()->{
    if(!open||node!=openingNode)return;
    try{
     refresh();
     if(content==null)throw new IllegalStateException("Dashboard content unavailable");
     openingRoot.setHidden(false);

    }catch(RuntimeException ex){notice="Native dashboard could not open: "+ex.getClass().getSimpleName();close();}
   });
  }catch(RuntimeException ex){notice="Native dashboard could not open: "+ex.getClass().getSimpleName();close();}
 }
 private void close(){
  restoreStatusGeometry();restorePageGeometry();statusTask=null;repairTask=null;
  confirmation=null;
  restoreReelGeometry();clearScroll();dropdown=0;menuOpen=false;
  if(content!=null)content.deleteAllChildren();content=null;
  for(Map.Entry<Widget,Boolean> item:hiddenBefore.entrySet())item.getKey().setHidden(item.getValue());hiddenBefore.clear();
  if(node!=null)client.closeInterface(node,true);node=null;open=false;rollEnd=0;restoreReelGeometry();reel=null;reelRows.clear();added.clear();
 }
 private void hide(Widget w){if(w==null)return;hiddenBefore.putIfAbsent(w,w.isSelfHidden());w.setHidden(true);}
 private void refresh(){
  if(!open)return;
  if(view==4)view=0;
  if(!state.challengeActive&&view!=3){view=5;detail=null;statusTask=null;repairTask=null;}
  restoreStatusGeometry();restorePageGeometry();
  Widget universe=client.getWidget(GROUP,1);if(universe==null)return;
  popupBounds=new Rectangle();
  // The native frame, six tier cards, icons and bars are left to the game renderer.
  Widget personal=client.getWidget(GROUP,14);if(personal==null)return;
  hide(personal);
  Widget personalHeader=client.getWidget(GROUP,5);if(personalHeader!=null){personalHeader.setHidden(false);personalHeader.setText(view==0&&state.unlocked(selected)?"Current Task":Challenge.TIERS[selected]+" Tasks");}
  Widget menu=client.getWidget(GROUP,15);
  if(menu!=null){menu.setHidden(false);bindMenu(menu);if(menu.getChildren()!=null)for(Widget child:menu.getChildren())if(child!=null)bindMenu(child);}
  hide(client.getWidget(GROUP,16));hide(client.getWidget(GROUP,17));
  for(int i=16;i<=17;i++){Widget popup=client.getWidget(GROUP,i);if(popup!=null)popup.deleteAllChildren();}
  if(content==null){
   // Attach to the native static right-hand layer. Dynamic widgets cannot nest.
   content=client.getWidget(GROUP,13);
   if(content==null)return;
   content.setNoClickThrough(true);
  }
  Widget frame=client.getWidget(GROUP,2);
  if(frame!=null){
   Widget title=frame.getChild(1);if(title!=null)title.setText("Combat Achievement Locked - "+(view==1?"Tasks":view==3?"Rules":view==4?"Stats":view==5?(state.challengeActive?"Settings":"Challenge"):view==6?"Challenge":"Picker"));
   Widget close=frame.getChild(11);if(close!=null){
    close.clearActions();close.setAction(0,"Close");close.setHasListener(true);
    close.setOnOpListener((JavaScriptCallback)e->close());
   }
  }
  if(personalHeader!=null&&personalHeader.getChildren()!=null)
   for(Widget label:personalHeader.getChildren())if(label!=null&&label.getType()==4)label.setText((view==3?"Rules":view==4?"Statistics":view==5?(state.challengeActive?"Settings":"Challenge"):view==6?"Challenge":view==0&&state.unlocked(selected)?"Current Task":Challenge.TIERS[selected]+" Tasks"));
  updateOverallProgress();overrideCards();applyPageLayout();renderPicker(rollEnd!=0);
  if(menuOpen&&rollEnd==0)renderMenu();
 }
 private void restorePageGeometry(){
  for(Map.Entry<Integer,int[]> entry:pageGeometry.entrySet()){
   Widget w=client.getWidget(GROUP,entry.getKey());int[] g=entry.getValue();
   if(w!=null){w.setPos(g[0],g[1],g[4],g[5]).setSize(g[2],g[3],g[6],g[7]);w.revalidate();}
  }
  pageGeometry.clear();
  for(Map.Entry<Widget,int[]> entry:pageHeaderWidths.entrySet()){entry.getKey().setOriginalWidth(entry.getValue()[0]).setWidthMode(entry.getValue()[1]);entry.getKey().revalidate();}
  pageHeaderWidths.clear();
  Widget column=client.getWidget(GROUP,6);if(column!=null)column.deleteAllChildren();
  for(int i=4;i<=12;i++){Widget w=client.getWidget(GROUP,i);if(w!=null)w.setHidden(false);}
 }
 private void resizePageHeader(Widget label,int width){
  pageHeaderWidths.putIfAbsent(label,new int[]{label.getOriginalWidth(),label.getWidthMode()});
  label.setOriginalWidth(width).setWidthMode(0);label.revalidate();
 }
 private void pageBounds(int id,int x,int y,int width,int height){
  Widget w=client.getWidget(GROUP,id);if(w==null)return;
  pageGeometry.putIfAbsent(id,new int[]{w.getOriginalX(),w.getOriginalY(),w.getOriginalWidth(),w.getOriginalHeight(),w.getXPositionMode(),w.getYPositionMode(),w.getWidthMode(),w.getHeightMode()});
  w.setPos(x,y,0,0).setSize(width,height,0,0);w.revalidate();
 }
 private void applyPageLayout(){
  Widget heading=client.getWidget(GROUP,4);
  if(heading!=null&&heading.getChildren()!=null)for(Widget label:heading.getChildren())if(label!=null&&label.getType()==4)label.setText("Difficulty");
  if(view==0)return;
  Widget frame=client.getWidget(GROUP,1);if(frame==null)return;
  for(int i=4;i<=12;i++){Widget w=client.getWidget(GROUP,i);if(w!=null)w.setHidden(true);}
  // Tiles begin 12px from the frame edge; fields also start 12px after the tile edge.
  int left=view==1||view==4?128:6;
  int setupOffset=showOverallProgress()?0:30;
  pageBounds(13,left,98-setupOffset,frame.getWidth()-left-6,frame.getHeight()-104+setupOffset);
  pageBounds(5,left,75-setupOffset,frame.getWidth()-left-6,23);
  boolean hidePageHeader=view==3||view==5||view==6;
  if(hidePageHeader)pageBounds(13,left,45,frame.getWidth()-left-6,frame.getHeight()-51);
  Widget header=client.getWidget(GROUP,5);if(header!=null){header.setHidden(hidePageHeader);if(header.getChildren()!=null)for(Widget label:header.getChildren())if(label!=null&&label.getType()==4){resizePageHeader(label,header.getWidth());}}
  if(view!=1&&view!=4)return;
  pageBounds(4,6,75-setupOffset,124,23);pageBounds(6,6,98-setupOffset,124,frame.getHeight()-104+setupOffset);
  if(heading!=null){heading.setHidden(false);if(heading.getChildren()!=null)for(Widget label:heading.getChildren())if(label!=null&&label.getType()==4){resizePageHeader(label,124);}}
  Widget parent=content;content=client.getWidget(GROUP,6);if(content==null){content=parent;return;}content.setHidden(false);
  for(int tier=0;tier<6;tier++){
   final int index=tier;int rowY=5+tier*34;boolean locked=!state.unlocked(tier);
   JavaScriptCallback choose=e->{selected=index;listScroll=0;dropdown=0;dropdownScroll=0;bossFilter="All";refresh();};
   List<Widget> parts=new ArrayList<>();
   Widget bg=nativeSprite(content,297,6,rowY,112,28,true);parts.add(bg);
   Widget tint=content.createChild(-1,3);tint.setPos(8,rowY+2).setSize(108,24).setFilled(true).setTextColor(locked?0x171612:0x9d8c6b).setOpacity(locked?65:selected==tier?195:255);tint.revalidate();parts.add(tint);
   // Nine-slice native tier frame: corners stay 9x9, only edge lengths change.
   int[][] frameParts={{913,6,rowY,9,9},{914,109,rowY,9,9},{915,6,rowY+19,9,9},{916,109,rowY+19,9,9},
       {917,6,rowY+9,9,10},{918,15,rowY,94,9},{919,109,rowY+9,9,10},{920,15,rowY+19,94,9}};
   for(int[] edge:frameParts)parts.add(nativeSprite(content,edge[0],edge[1],edge[2],edge[3],edge[4],true));
   Widget name=fittedText(Challenge.TIERS[tier],15,rowY+5,locked?76:94,18,locked?0xaaa18c:selected==tier?0xffcf73:GOLD);name.setXTextAlignment(1);parts.add(name);
   if(locked){if(!client.getSpriteOverrides().containsKey(LOCK_SPRITE))registerSprite(LOCK_SPRITE,StatusSprites.lock());parts.add(nativeSprite(content,LOCK_SPRITE,93,rowY+5,16,18,false));}
   for(Widget part:parts){
    part.setAction(0,(locked?"Locked: ":"Select ")+Challenge.TIERS[tier]);part.setHasListener(true);part.setOnOpListener(choose);
    part.setOnMouseOverListener((JavaScriptCallback)e->tint.setOpacity(locked?65:selected==index?180:220));
    part.setOnMouseLeaveListener((JavaScriptCallback)e->tint.setOpacity(locked?65:selected==index?195:255));
   }
  }
  content=parent;
 }
 private void overrideCards(){
  Widget click=client.getWidget(GROUP,8);
  if(click!=null&&click.getChildren()!=null){int i=0;for(Widget w:click.getChildren())if(w!=null){final int tier=i++;if(tier>=6)break;
   w.clearActions();w.setAction(0,(state.unlocked(tier)?"Select ":state.readyToUnlock(tier)?"Ready to unlock: ":"Locked: ")+Challenge.TIERS[tier]);w.setHasListener(true);
   w.setOnOpListener((JavaScriptCallback)e->{if(rollEnd==0){confirmation=null;selected=tier;page=0;listScroll=0;dropdownScroll=0;dropdown=0;bossFilter="All";detail=null;statusTask=null;repairTask=null;detailHistory.clear();reveal=null;notice="";refresh();}});
   colourCard(w,tier,false);
   w.setOnMouseOverListener((JavaScriptCallback)e->colourCard(w,tier,true));
   w.setOnMouseLeaveListener((JavaScriptCallback)e->colourCard(w,tier,false));
  }}
  renderTierLocks();
  Widget names=client.getWidget(GROUP,10);
  if(names!=null&&names.getChildren()!=null){int i=0;for(Widget w:names.getChildren())if(w!=null&&i<6){int tier=i++;w.setText((state.unlocked(tier)?"":state.readyToUnlock(tier)?"<col=ffcf73>":"<col=888888>")+Challenge.TIERS[tier]);}}
  Widget progress=client.getWidget(GROUP,12);
  if(progress!=null&&progress.getChildren()!=null){int i=0;for(Widget w:progress.getChildren())if(w!=null&&w.getType()==4&&i<6){int tier=i++;int n=state.total(tier);w.setText(state.completed(tier)+"/"+n+" - "+(n==0?0:state.completed(tier)*100/n)+"%");}}
  // Each native tier bar contains four outlined segments and four fill widgets.
  if(progress!=null)for(int tier=0;tier<6;tier++){
   int[] capacities=new int[4];
   for(int segment=0;segment<4;segment++){
    Widget outline=progress.getChild(tier*11+3+segment);
    capacities[segment]=outline==null?0:Math.max(0,outline.getWidth()-2);
   }
   int[] filled=TierProgress.fillWidths(capacities,state.completed(tier),state.total(tier));
   for(int segment=0;segment<4;segment++){
    Widget fill=progress.getChild(tier*11+7+segment);
    if(fill!=null){fill.setTextColor(state.total(tier)>0&&state.completed(tier)==state.total(tier)?0xffcf44:0xbcb6a6);fill.setOriginalWidth(filled[segment]);fill.setHidden(filled[segment]==0);fill.revalidate();}
   }
  }
 }
 private void colourCard(Widget card,int tier,boolean hover){
  boolean chosen=selected==tier;
  boolean unlocked=state.unlocked(tier);
  int colour=unlocked?(chosen?0x9d8c6b:0xa39061):state.readyToUnlock(tier)?0xc49b52:0x171612;
  int width=Math.max(1,card.getWidth()),height=Math.max(1,card.getHeight());
  String key=width+":"+height+":"+colour;
  Integer sprite=cardHighlightSprites.get(key);
  if(sprite==null){sprite=10030000+cardHighlightSprites.size();registerSprite(sprite,StatusSprites.cardHighlight(width,height,colour));cardHighlightSprites.put(key,sprite);}
  card.setType(5);card.setSpriteId(sprite).setSpriteTiling(false).setOpacity(unlocked?(chosen?195:hover?220:255):state.readyToUnlock(tier)?100:65);
 }
 private void renderTierLocks(){
  Widget layer=client.getWidget(GROUP,9),cards=client.getWidget(GROUP,8);
  if(layer==null||cards==null||cards.getChildren()==null)return;
  layer.deleteAllChildren();layer.setHidden(false);
  Widget parent=content;content=layer;Rectangle origin=layer.getBounds();int tier=0;
  for(Widget card:cards.getChildren())if(card!=null&&tier<6){
   final int index=tier++;
   if(state.unlocked(index))continue;
   if(!client.getSpriteOverrides().containsKey(LOCK_SPRITE))registerSprite(LOCK_SPRITE,StatusSprites.lock());
   Rectangle bounds=card.getBounds();
   Widget lock=nativeSprite(layer,LOCK_SPRITE,bounds.x-origin.x+bounds.width-22,bounds.y-origin.y+17,16,18,false);
   lock.setAction(0,(state.readyToUnlock(index)?"Ready to unlock: ":"Locked: ")+Challenge.TIERS[index]);lock.setHasListener(true);
   lock.setOnOpListener((JavaScriptCallback)e->{if(rollEnd==0){confirmation=null;selected=index;detail=null;statusTask=null;repairTask=null;dropdown=0;bossFilter="All";listScroll=0;refresh();}});
  }
  content=parent;
 }
 private Widget text(String text,int x,int y,int width,int height,int colour){
  Widget w=content.createChild(-1,4);w.setPos(x,y).setSize(width,height).setFontId(495).setText(text).setTextColor(colour).setTextShadowed(true);w.revalidate();return w;
 }
 private Widget fittedText(String value,int x,int y,int width,int height,int colour){
  Widget widget=text(value,x,y,width,height,colour);
  FontTypeFace font=widget.getFont();
  widget.setText(TaskText.fit(value,width,font==null?s->s.length()*7:font::getTextWidth));return widget;
 }
 private RowHighlight highlightTaskRow(Widget bar,int normal,int hovered,Widget... parts){
  RowHighlight row=new RowHighlight(bar,normal,hovered);taskRowHighlights.add(row);
  for(Widget part:parts){
   part.setHasListener(true);
   part.setOnMouseOverListener((JavaScriptCallback)e->updateTaskRowHighlights());
   part.setOnMouseLeaveListener((JavaScriptCallback)e->updateTaskRowHighlights());
  }
  return row;
 }
 private void updateTaskRowHighlights(){
  int mouseX=client.getMouseCanvasPosition().getX(),mouseY=client.getMouseCanvasPosition().getY();
  boolean inList=open&&!menuOpen&&dropdown==0&&scrollViewport!=null&&!scrollViewport.isHidden()
   &&scrollViewport.getBounds().contains(mouseX,mouseY);
  for(RowHighlight row:taskRowHighlights){
   boolean hovered=inList&&!row.bar.isHidden()&&row.bar.getBounds().contains(mouseX,mouseY);
   row.bar.setTextColor(hovered?row.hovered:row.normal);
   if(row.icon!=null){
    boolean statusHovered=hovered&&(row.icon.getBounds().contains(mouseX,mouseY)||row.status.getBounds().contains(mouseX,mouseY));
    row.icon.setOpacity(statusHovered?0:45);
    row.status.setTextColor(row.locked?(statusHovered?0xffdc90:0xe6b65e):(statusHovered?0xff9999:0xff5555));
   }
  }
 }
 private void readTask(Widget widget,Challenge.Task task,String action){
  widget.setAction(0,action);widget.setHasListener(true);
  widget.setOnOpListener((JavaScriptCallback)e->showTask(task,false));
 }
 private void showTask(Challenge.Task task,boolean linked){
  if(linked&&detail!=null)detailHistory.push(new DetailVisit(detail,detailPage));else detailHistory.clear();
  if(!linked)detailReturnScroll=listScroll;
  listScroll=0;statusTask=null;repairTask=null;detail=task;detailPage=0;dropdown=0;refresh();
 }
 private Widget statusIcon(int x,int y,boolean locked,Challenge.Task task){
  int spriteId=locked?LOCK_SPRITE:WARNING_SPRITE;
  if(!client.getSpriteOverrides().containsKey(spriteId))registerSprite(spriteId,locked?StatusSprites.lock():StatusSprites.warning(null));
  // Full, untrimmed canvases include padding; game sprite offsets cannot clip the artwork.
  Widget icon=content.createChild(-1,5);icon.setPos(x,y).setSize(20,22)
   .setSpriteId(spriteId).setSpriteTiling(false);icon.revalidate();
  readStatus(icon,task,locked);
  icon.setOpacity(45);
  icon.setOnMouseOverListener((JavaScriptCallback)e->icon.setOpacity(0));
  icon.setOnMouseLeaveListener((JavaScriptCallback)e->icon.setOpacity(45));
  return icon;
 }
 private void readStatus(Widget widget,Challenge.Task task,boolean locked){
  widget.setAction(0,locked?"Why is this task locked?":"Read possible conflict");widget.setHasListener(true);
  widget.setOnOpListener((JavaScriptCallback)e->{
   statusReturnScroll=listScroll;listScroll=0;statusTask=task;statusLocked=locked;menuOpen=false;dropdown=0;refresh();
  });
 }
 private void restoreStatusGeometry(){
  if(statusGeometry==null)return;
  restoreReelGeometry();clearScroll();
  Widget panel=client.getWidget(GROUP,13);
  if(panel!=null){panel.setPos(statusGeometry[0],statusGeometry[1],statusGeometry[4],statusGeometry[5])
   .setSize(statusGeometry[2],statusGeometry[3],statusGeometry[6],statusGeometry[7]);panel.revalidate();}
  for(int i=4;i<=12;i++){Widget w=client.getWidget(GROUP,i);if(w!=null)w.setHidden(false);}
  statusGeometry=null;
 }
 private void expandReadingArea(){
  statusGeometry=new int[]{content.getOriginalX(),content.getOriginalY(),content.getOriginalWidth(),content.getOriginalHeight(),
   content.getXPositionMode(),content.getYPositionMode(),content.getWidthMode(),content.getHeightMode()};
  Widget frame=client.getWidget(GROUP,1);
  content.setPos(6,75,0,0).setSize(frame.getWidth()-12,frame.getHeight()-81,0,0);content.revalidate();
  for(int i=4;i<=12;i++){Widget w=client.getWidget(GROUP,i);if(w!=null)w.setHidden(true);}
 }
 private void renderStatusExplanation(){
  expandReadingArea();
  int width=content.getWidth()-16,viewportWidth=width-16,height=content.getHeight()-66;
  String heading=statusLocked?"Unlock requirements":"Possible conflicts";
  text(heading,16,2,width-36,22,statusLocked?0xe6b65e:0xff5555);
  Widget icon=statusIcon(width-14,2,statusLocked,statusTask);icon.clearActions();icon.setOnOpListener((JavaScriptCallback)e->{});icon.setOnMouseOverListener((Object[])null);icon.setOnMouseLeaveListener((Object[])null);icon.setOpacity(0);
  List<DetailLine> lines=new ArrayList<>();
  Widget measure=text("",0,0,1,1,WHITE);FontTypeFace font=measure.getFont();measure.setHidden(true);
  java.util.function.ToIntFunction<String> pixels=font==null?v->v.length()*7:font::getTextWidth;
  List<Challenge.Task> related=statusLocked?state.prerequisites(statusTask):state.warnings(statusTask);
  boolean tierLocked=statusLocked&&!statusTask.done&&!state.unlocked(statusTask.tier);
  if(tierLocked){
   int tier=statusTask.tier;
   addStatusLines(lines,"Tier locked: "+Challenge.TIERS[tier],viewportWidth-16,0xe6b65e,pixels);
   addStatusLines(lines,state.readyToUnlock(tier)?"Requirements met. Choose this tier in Picker and press Unlock this tier.":"Open this tier by completing:",viewportWidth-16,0xc8c1b2,pixels);
   if(state.mode==Challenge.TIER_BY_TIER){
    for(int i=0;i<tier;i++)addStatusLines(lines,"All "+Challenge.TIERS[i]+" tasks: "+state.completed(i)+" / "+state.total(i),viewportWidth-16,WHITE,pixels);
   }else {
   if(tier>1)addStatusLines(lines,"All "+Challenge.TIERS[tier-2]+" tasks: "+state.completed(tier-2)+" / "+state.total(tier-2),viewportWidth-16,WHITE,pixels);
   if(tier>0)addStatusLines(lines,"At least half of "+Challenge.TIERS[tier-1]+": "+state.completed(tier-1)+" / "+((state.total(tier-1)+1)/2)+" tasks needed",viewportWidth-16,WHITE,pixels);
   }
   lines.add(new DetailLine("",WHITE,null));
  }
  if(statusLocked&&!related.isEmpty()){
   addStatusLines(lines,tierLocked?"This task also needs:":"Complete these tasks first:",viewportWidth-16,0xe6b65e,pixels);
  }else if(!statusLocked)addStatusLines(lines,"While doing this task, you might also complete the tasks below. Be careful not to complete them by accident.",viewportWidth-16,0xc8c1b2,pixels);
  if(!statusLocked&&!DependencyRules.matches(statusTask))addStatusLines(lines,"Task data changed or has not been reviewed. Its overlap rules need checking.",viewportWidth-16,0xff5555,pixels);
  if(related.isEmpty()&&!tierLocked&&DependencyRules.matches(statusTask))addStatusLines(lines,"This status has cleared since you opened the task.",viewportWidth-16,WHITE,pixels);
  lines.add(new DetailLine("",WHITE,null));
  int leftWidth=(viewportWidth-28)*2/5,rightX=leftWidth+20,rightWidth=viewportWidth-rightX-8;
  List<List<String>> names=new ArrayList<>(),categories=new ArrayList<>(),descriptions=new ArrayList<>();
  List<Integer> heights=new ArrayList<>();int total=lines.size()*17+8;
  for(Challenge.Task task:related){
   List<String> name=TaskText.preview(task.name,leftWidth,Integer.MAX_VALUE,pixels);
   List<String> category=TaskText.preview(Challenge.TIERS[task.tier]+" - "+task.boss,leftWidth,Integer.MAX_VALUE,pixels);
   List<String> description=TaskText.preview(task.description,rightWidth,Integer.MAX_VALUE,pixels);
   int rowHeight=Math.max((name.size()+category.size())*17+4,description.size()*17)+16;
   names.add(name);categories.add(category);descriptions.add(description);heights.add(rowHeight);total+=rowHeight+6;
  }
  prepareScrollLayer(8,29,viewportWidth,height,total,listScroll);
  Widget parent=content;content=scrollViewport;
  for(int i=0;i<lines.size();i++){DetailLine line=lines.get(i);text(line.text,8,4+i*17,viewportWidth-16,17,line.colour);}
  int rowY=lines.size()*17+8;
  for(int i=0;i<related.size();i++){
   Widget bar=content.createChild(-1,3);bar.setPos(4,rowY).setSize(viewportWidth-8,heights.get(i)).setFilled(true)
    .setTextColor(i%2==0?0x544b3e:0x4a4235);bar.revalidate();
   Widget name=text(String.join("<br>",names.get(i)),8,rowY+8,leftWidth,names.get(i).size()*17,statusLocked?GOLD:0xff5555);name.setLineHeight(17);
   Widget category=text(String.join("<br>",categories.get(i)),8,rowY+12+names.get(i).size()*17,leftWidth,categories.get(i).size()*17,0xc8c1b2);category.setLineHeight(17);
   Widget description=text(String.join("<br>",descriptions.get(i)),rightX,rowY+8,rightWidth,descriptions.get(i).size()*17,WHITE);description.setLineHeight(17);
   highlightTaskRow(bar,bar.getTextColor(),0x665b49,bar,name,category,description);
   rowY+=heights.get(i)+6;
  }
  content=parent;renderScrollbar(width,29,height,total);
  button(detail==null?"Back to "+(view==1?"tasks":"picker"):"Back to task",8,content.getHeight()-29,width,()->{
   statusTask=null;listScroll=statusReturnScroll;
  },true);
 }
 private void addStatusLines(List<DetailLine> lines,String value,int width,int colour,java.util.function.ToIntFunction<String> pixels){
  for(String line:TaskText.preview(value,width,Integer.MAX_VALUE,pixels))lines.add(new DetailLine(line,colour,null));
 }
 private void registerSprite(int id,BufferedImage image){
  int[] pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
  for(int i=0;i<pixels.length;i++)pixels[i]=(pixels[i]>>>24)==0?0:pixels[i]&0xffffff;
  client.getSpriteOverrides().put(id,client.createSpritePixels(pixels,image.getWidth(),image.getHeight()));
  client.getWidgetSpriteCache().reset();
 }
 private void setHoverHelp(Widget widget,String message){
  hoverHelp.put(widget,message);
  // Buttons with explanatory tooltips use a direct click listener so the native
  // action-name tooltip does not repeat the label beside the explanation.
  for(Widget part:helpTargets.getOrDefault(widget,new Widget[]{widget})){
   Object[] action=part.getOnOpListener();
   part.clearActions();
   if(action!=null){part.setOnClickListener(action);part.setOnOpListener((Object[])null);}
  }
 }
 private Widget button(String label,int x,int y,int width,Runnable action,boolean enabled){
  return button(label,x,y,width,action,enabled,false);
 }
 private Widget button(String label,int x,int y,int width,Runnable action,boolean enabled,boolean selectedButton){
  Widget bg=content.createChild(-1,3);bg.setPos(x,y).setSize(width,20).setFilled(true).setTextColor(selectedButton?0x72542c:enabled?0x403b30:0x38352e);bg.revalidate();
  Widget w=fittedText(label,x+3,y+2,width-6,17,enabled?(selectedButton?0xffcf73:GOLD):0x888888);w.setXTextAlignment(1);
  String help=null;
  if(label.startsWith("Limit per tier:"))help="Enter 0 for unlimited rerolls, or 1-999 for a limit per tier. Rerolls already used remain counted.";
  else if(label.equals("Swap tasks"))help="Exchange your active task and stored task immediately.";
  else if(label.equals("Save progress"))help="Save pending RuneLite configuration changes and request cloud sync if signed in. Use before switching PCs.";
  else if(label.startsWith("Volume:"))help="Set the picker sound volume from 0% (silent) to 100%.";
  else if(label.startsWith("Sound:"))help="Turn task picker and tier unlock sounds on or off.";
  else if(label.equals("Sync with game"))help="Read all achievements from the game and remove manual completion edits. Keep your challenge rules and statistics.";
  else if(label.equals("Take stored task"))help="Make the stored task active and empty this tier's storage slot.";

  if(enabled){
   w.setOnMouseOverListener((JavaScriptCallback)e->{bg.setTextColor(selectedButton?0x8a6533:0x665337);w.setTextColor(0xffdc90);});
   w.setOnMouseLeaveListener((JavaScriptCallback)e->{bg.setTextColor(selectedButton?0x72542c:0x403b30);w.setTextColor(selectedButton?0xffcf73:GOLD);});
   w.setAction(0,label);w.setHasListener(true);w.setOnOpListener(accountAction(action));}
  if(help!=null)setHoverHelp(w,help);w.revalidate();return w;
 }
 private void modeToggle(String label,int x,int y,int width,boolean on,Runnable action){
  int background=on?0x4d5a3c:0x363636,foreground=on?0xe8dfbb:0x929292;
  Widget edge=content.createChild(-1,3);edge.setPos(x,y).setSize(width,22).setFilled(true).setTextColor(on?0x859369:0x555555);edge.revalidate();
  Widget fill=content.createChild(-1,3);fill.setPos(x+1,y+1).setSize(width-2,20).setFilled(true).setTextColor(background);fill.revalidate();
  Widget labelWidget=fittedText(label+": "+(on?"On":"Off"),x+3,y+2,width-6,18,foreground);
  labelWidget.setXTextAlignment(1).setYTextAlignment(1);labelWidget.setAction(0,(on?"Disable ":"Enable ")+label.toLowerCase(Locale.ROOT));labelWidget.setHasListener(true);
  labelWidget.setOnMouseOverListener((JavaScriptCallback)e->{fill.setTextColor(on?0x5b6948:0x454545);labelWidget.setTextColor(on?0xffefd1:0xb4b4b4);});
  labelWidget.setOnMouseLeaveListener((JavaScriptCallback)e->{fill.setTextColor(background);labelWidget.setTextColor(foreground);});
  String help=label.equals("Storage")?(on?"Turn off storing tasks for later. Existing stored tasks can still be taken back.":"Allow one stored task per tier so you can roll another task."):
   label.equals("Rerolling")?(on?"Turn off rerolling. You will need to complete or store your assigned task.":"Allow replacing your assigned task, using the reroll settings below."):
   on?"Turn off swapping. Complete your active task before taking your stored task.":"Allow exchanging your active and stored tasks at any time.";
  if(state.challengeActive){labelWidget.clearActions();setHoverHelp(labelWidget,"These rules are fixed for your current challenge.");labelWidget.revalidate();return;}
  labelWidget.setOnOpListener(accountAction(action));
  setHoverHelp(labelWidget,help);labelWidget.revalidate();
 }
 private void renderPicker(boolean rolling){
  hoverHelp.clear();helpTargets.clear();taskRowHighlights.clear();
  disabledRerollButton=null;disabledStorageButton=null;unavailableStorageButton=null;
  unlockGlow=null;unlockEdge=null;unlockFill=null;unlockHovered=false;stepEdge=null;stepFill=null;
  restoreReelGeometry();clearScroll();
  if(rolling){renderReel();return;}
  content.deleteAllChildren();int width=Math.max(180,content.getWidth()-16);
  if(statusTask!=null){renderStatusExplanation();return;}
  if(confirmation!=null){renderConfirmation(width);return;}
  if(repairTask!=null){renderCompletionRepair(width);return;}
  if(detail!=null){renderTaskDetail(width);return;}
  if(view==0&&state.mode==Challenge.ALL_TIERS){
   for(int i=0;i<6;i++)if(state.active[i]!=null){selected=i;break;}
  }
  int tier=selected;boolean unlocked=state.unlocked(tier);
  if(view==1){renderTasks(width);return;}
  if(view==3){renderRules(width);return;}
  if(view==4){renderStats(width);return;}
  if(view==5){renderMode(width);return;}
  if(view==6){renderChallengeOverview(width);return;}
  if(unlocked){
   Widget edge=content.createChild(-1,3);edge.setPos(8,0).setSize(width,121).setFilled(true).setTextColor(0x766957);edge.revalidate();
   Widget background=content.createChild(-1,3);background.setPos(9,1).setSize(width-2,119).setFilled(true).setTextColor(0x443d32);background.revalidate();
  }
  if(!unlocked&&state.readyToUnlock(tier)){
   Widget tierHeader=client.getWidget(GROUP,5);if(tierHeader!=null)tierHeader.setHidden(true);
   Widget tierTitle=text(Challenge.TIERS[tier]+" Tier Unlocked!",8,36,width,24,GOLD);tierTitle.setFontId(496).setXTextAlignment(1);tierTitle.revalidate();
   if(!client.getSpriteOverrides().containsKey(OPEN_LOCK_SPRITE))registerSprite(OPEN_LOCK_SPRITE,StatusSprites.openLock());
   nativeSprite(content,OPEN_LOCK_SPRITE,8+(width-36)/2,70,36,40,false);
   text("You have met the requirements for "+Challenge.TIERS[tier]+".",8,122,width,40,WHITE).setXTextAlignment(1);
   unlockButton(width,()->{if(state.activateTier(tier)){playPickerSound(3925,1.0);notice="";}});
   return;
  }
  if(!unlocked){
   if(!client.getSpriteOverrides().containsKey(LOCK_SPRITE))registerSprite(LOCK_SPRITE,StatusSprites.lock());
   Widget tierHeader=client.getWidget(GROUP,5);if(tierHeader!=null)tierHeader.setHidden(true);
   Widget tierTitle=text(Challenge.TIERS[tier]+" Tasks",8,36,width,24,GOLD);tierTitle.setFontId(496).setXTextAlignment(1);tierTitle.revalidate();
   nativeSprite(content,LOCK_SPRITE,8+(width-36)/2,70,36,40,false);
   text("Locked",8,116,width,22,0xe6b65e).setXTextAlignment(1);
   text(state.tierRequirement(tier),8,149,width,51,WHITE).setXTextAlignment(1).setLineHeight(17);
   return;
  }
  Challenge.Task active=state.task(state.active[tier]);
  boolean activeWarning=active!=null&&TaskBrowser.warning(state,active);
  int titlePadding=activeWarning?30:0;
  Widget activeTitle=text("",8+titlePadding,23,width-2*titlePadding,34,GOLD);
  FontTypeFace titleFont=activeTitle.getFont();
  activeTitle.setText(String.join("<br>",TaskText.preview(active==null?"No task assigned":active.name,width-2*titlePadding,2,titleFont==null?v->v.length()*7:titleFont::getTextWidth)))
   .setLineHeight(17).setXTextAlignment(1).setYTextAlignment(1);activeTitle.revalidate();
  if(active!=null)readTask(activeTitle,active,"View Task");
  if(activeWarning)statusIcon(width-18,27,false,active);
  List<Challenge.Task> blocked=state.blocked(tier);
  boolean available=!state.pool(tier).isEmpty();
  String message=active==null?"Pick a task when you are ready.":active.description;
  if(active==null&&!available&&!blocked.isEmpty())message="Remaining tasks are locked. Open Tasks to see their prerequisites.";
  else if(active==null&&!available&&state.stored[tier]!=null)message="No rollable tasks. Unblock the task below, or complete prerequisites in another tier.";
  Widget preview=text("",16,65,width-16,34,WHITE);FontTypeFace font=preview.getFont();
  preview.setText(String.join("<br>",TaskText.preview(message,width-18,2,font==null?s->s.length()*7:font::getTextWidth))).setLineHeight(17).setXTextAlignment(1);
  if(active!=null)readTask(preview,active,"View Task");
  preview.revalidate();
  int half=(width-4)/2;
  Widget storeButton=pickerButton(active==null?"Pick Task":state.allowStorage?"Store task":"Storage disabled",8,127,half,()->{
   if(active==null)startRoll(tier);
   else {
    if(Objects.equals(state.active[tier],active.id)&&state.blockTask(tier,true))notice="";else notice="Task changed or storage is occupied.";
   }
  },active==null?available:state.allowStorage&&state.stored[tier]==null,active==null,active!=null&&state.allowStorage);
  if(active!=null&&!state.allowStorage)disabledStorageButton=storeButton;
  String rerollBalance=state.rulePack==Challenge.FREE_PLAY?"Unlimited rerolls.":state.remainingSkips(tier)+" rerolls left in this tier.";
  Widget rerollButton=pickerButton(state.allowSkipping?(state.rulePack==Challenge.FREE_PLAY?"Reroll":"Reroll ("+state.remainingSkips(tier)+" left)"):"Rerolling disabled",12+half,127,half,()->confirm("Reroll this task?",active.name,
   state.rulePack==Challenge.FREE_PLAY?"Roll a different task.":state.rulePack==Challenge.CA_RULES?"Uses 1 reroll from this tier. A different task will be rolled.":"Uses 1 reroll. A different task will be rolled.",()->{
    if(Objects.equals(state.active[tier],active.id)&&state.skip(tier)){notice="";startRoll(tier);}else notice="Task changed or no reroll available.";
   }),active!=null&&state.canSkip(tier),false,state.allowSkipping);
  if(!state.allowSkipping)disabledRerollButton=rerollButton;
  else if(active!=null&&!state.canSkip(tier)){
   boolean hasAlternative=state.tasks.stream().anyMatch(task->(state.mode==Challenge.ALL_TIERS||task.tier==tier)&&!task.done&&task.id!=active.id&&!Objects.equals(state.stored[task.tier],task.id)&&state.prerequisites(task).isEmpty());
   setHoverHelp(rerollButton,(hasAlternative?"No rerolls remaining in this tier.":"No other tasks are available to reroll to.")+" "+rerollBalance);
  }else setHoverHelp(rerollButton,(active==null?"Roll a task first to use rerolling.":"Replace your active task with a different task.")+" "+rerollBalance);
  if(active!=null&&state.allowStorage)setHoverHelp(storeButton,state.stored[tier]==null?"Save this task for later and make room to roll another task.":"Storage already contains a task. Take or swap it before storing another.");
  Challenge.Task blockedTask=state.task(state.stored[tier]);
  Widget slot=content.createChild(-1,3);slot.setPos(8,159).setSize(width,24).setFilled(true).setTextColor(0x302b23);slot.revalidate();
  Widget blockText=fittedText("Stored: "+(blockedTask==null?"Empty":blockedTask.name),12,159,width-8,24,WHITE);
  blockText.setXTextAlignment(1).setYTextAlignment(1);
  if(blockedTask!=null)readTask(blockText,blockedTask,"View Task");
  boolean swap=state.allowStorage&&state.allowSwapping&&active!=null&&blockedTask!=null;
  Widget storedAction=button(swap?"Swap tasks":"Take stored task",8,191,half,()->{
   if(swap){
    if(Objects.equals(state.active[tier],active.id)&&Objects.equals(state.stored[tier],blockedTask.id)&&state.swap(tier))notice="";else notice="Tasks changed or swap unavailable.";
   }
   else confirm("Take this stored task?",blockedTask.name,"Make this your active task again. This is free. Your storage slot becomes empty.",()->{
    if(Objects.equals(state.stored[tier],blockedTask.id)&&state.unblock(tier))notice="";else notice="Stored task changed.";
   });
  },swap?state.canSwap(tier):blockedTask!=null&&state.canUnblock(tier));
  if(!swap&&active!=null&&blockedTask!=null){unavailableStorageButton=storedAction;hoverHelp.remove(storedAction);}
  String pickerNotice=notice;
  if(!pickerNotice.isEmpty())fittedText(pickerNotice,12+half,192,half,20,0xff6666);
 }
 private void startRoll(int tier){
  if(!state.challengeActive)return;
  List<Challenge.Task> eligible=new ArrayList<>(state.pool(tier));
  reveal=state.roll(tier,random);
  if(reveal==null)return;
  selected=reveal.tier;
  long now=System.currentTimeMillis();
  reel=new TaskReel(eligible,reveal,visualRandom,now);rollEnd=now+reel.duration+TaskReel.REVEAL_DURATION;revealPresented=false;lastReelStep=-1;
 }
 private void restoreReelGeometry(){
  if(reelLayer!=null&&reelGeometry!=null){
   reelLayer.deleteAllChildren();
   reelLayer.setScrollHeight(reelGeometry[8]).setScrollWidth(reelGeometry[9]).setScrollY(reelGeometry[10]);
   reelLayer.setPos(reelGeometry[0],reelGeometry[1],reelGeometry[4],reelGeometry[5])
    .setSize(reelGeometry[2],reelGeometry[3],reelGeometry[6],reelGeometry[7]).setHidden(true);reelLayer.revalidate();
  }
  reelLayer=null;reelGeometry=null;
 }
 private void renderReel(){
  restoreReelGeometry();content.deleteAllChildren();reelRows.clear();
  int width=Math.max(180,content.getWidth()-16);
  text(revealPresented?"Task selected!":reel.entries.size()==1?"One task remaining...":(state.mode==Challenge.ALL_TIERS?"Rolling across all tiers...":"Rolling "+Challenge.TIERS[selected]+" task..."),8,15,width,22,GOLD).setXTextAlignment(1);
  if(reel.entries.size()==1&&!revealPresented){text("Revealing your task...",8,75,width,30,WHITE).setXTextAlignment(1);return;}
  reelLayer=client.getWidget(GROUP,14);if(reelLayer==null)return;
  reelGeometry=new int[]{reelLayer.getOriginalX(),reelLayer.getOriginalY(),reelLayer.getOriginalWidth(),reelLayer.getOriginalHeight(),
   reelLayer.getXPositionMode(),reelLayer.getYPositionMode(),reelLayer.getWidthMode(),reelLayer.getHeightMode(),reelLayer.getScrollHeight(),reelLayer.getScrollWidth(),reelLayer.getScrollY()};
  reelLayer.deleteAllChildren();reelLayer.setPos(8,(37+158-reel.height())/2,0,0).setSize(width,reel.height(),0,0).setHidden(false);
  reelLayer.setNoClickThrough(true);reelLayer.revalidate();
  Widget backing=reelLayer.createChild(-1,3);backing.setPos(0,reel.centreY()).setSize(width,34).setFilled(true).setTextColor(revealPresented?0x876333:0x72542c);backing.revalidate();
  if(revealPresented){
   for(int y:new int[]{reel.centreY(),reel.centreY()+33}){
    Widget edge=reelLayer.createChild(-1,3);edge.setPos(0,y).setSize(width,1).setFilled(true).setTextColor(0xd6ae63);edge.revalidate();
   }
  }
  for(Challenge.Task task:reel.entries){
   int i=reelRows.size();
   boolean winner=revealPresented&&task.id==reveal.id;
   long frameTime=revealPresented?reel.start+reel.duration:System.currentTimeMillis();
   Widget label=reelLayer.createChild(-1,4);label.setPos(7,reel.centreY()+(int)Math.round(reel.offset(i,frameTime)*TaskReel.ROW_HEIGHT)).setSize(width-14,34)
    .setFontId(495).setText(task.name).setTextColor(winner?0xffd780:revealPresented?0x82786a:WHITE).setTextShadowed(true).setXTextAlignment(1).setYTextAlignment(1);
   FontTypeFace font=label.getFont();
   label.setText(String.join("<br>",TaskText.preview(task.name,width-14,2,font==null?s->s.length()*7:font::getTextWidth)));
   label.revalidate();reelRows.add(label);
  }
  fittedText(revealPresented?"Category: "+reveal.boss:"The task in the centre will be selected",8,158,width,25,revealPresented?WHITE:GOLD).setXTextAlignment(1);
 }
 private void renderTasks(int width){
  renderSearchField(width);
  dropdownField("Category: "+bossFilter,30,width,1);
  dropdownField("Status: "+TASK_STATUSES[taskFilter],54,width,2);
  List<Challenge.Task> list=TaskBrowser.filter(state,selected,bossFilter,taskFilter,taskSearch);
  int y=80,height=Math.max(60,content.getHeight()-y-4),viewportWidth=width-16;
  prepareScrollLayer(8,y,viewportWidth,height,list.size()*44,listScroll);
  Widget parent=content;content=scrollViewport;
  if(list.isEmpty())text("No matching tasks",4,8,viewportWidth-8,25,WHITE);
  for(int i=0;i<list.size();i++)renderTaskRow(list.get(i),i,i*44,viewportWidth);
  content=parent;int savedDropdownScroll=dropdownScroll;renderScrollbar(width,y,height,list.size()*44);dropdownScroll=savedDropdownScroll;
  if(dropdown!=0)renderChoiceList(dropdown==1?TaskBrowser.categories(state,selected):Arrays.asList(TASK_STATUSES),width);
 }
 private void renderSearchField(int width){
  int fieldWidth=taskSearch.isEmpty()?width:width-26;
  Widget border=content.createChild(-1,3);border.setPos(8,4).setSize(fieldWidth,22).setFilled(true).setTextColor(0x161410);border.revalidate();
  Widget inner=content.createChild(-1,3);inner.setPos(9,5).setSize(fieldWidth-2,20).setFilled(true).setTextColor(0x443d32);inner.revalidate();
  Widget label=fittedText(taskSearch.isEmpty()?"Search achievements...":"Search: "+taskSearch,12,6,fieldWidth-8,18,taskSearch.isEmpty()?0xc8c1b2:GOLD);
  JavaScriptCallback click=e->{
   if(profile==null)return;String account=profile;long request=++searchRequest;
   dropdown=0;menuOpen=false;refresh();
   java.util.function.Consumer<String> update=value->clientThread.invokeLater(()->{
    if(request!=searchRequest||!open||view!=1||detail!=null||statusTask!=null||repairTask!=null||!account.equals(profile)||!account.equals(config.getRSProfileKey()))return;
    String query=value==null?"":value.trim();
    if(query.equals(taskSearch))return;
    taskSearch=query;listScroll=0;dropdown=0;refresh();
   });
   chatbox.openTextInput("Search achievements (results update while typing)").value(taskSearch)
    .onChanged(update).onDone(update).build();
  };
  for(Widget part:new Widget[]{border,inner,label}){part.setAction(0,"Search achievements");part.setHasListener(true);part.setOnOpListener(click);}
  label.setOnMouseOverListener((JavaScriptCallback)e->inner.setTextColor(0x62533e));
  label.setOnMouseLeaveListener((JavaScriptCallback)e->inner.setTextColor(0x443d32));
  if(!taskSearch.isEmpty())button("X",width-14,5,22,()->{searchRequest++;taskSearch="";listScroll=0;},true);
 }
 private int filterCount(int which,String choice){
  return TaskBrowser.filter(state,selected,which==1?choice:bossFilter,which==1?taskFilter:Arrays.asList(TASK_STATUSES).indexOf(choice),taskSearch).size();
 }
 private Widget countedFilterText(String label,int count,int x,int y,int width,int height,int colour){
  Widget widget=text("",x,y,width,height,colour);FontTypeFace font=widget.getFont();
  java.util.function.ToIntFunction<String> pixels=font==null?v->v.length()*7:font::getTextWidth;
  String suffix=" ("+count+")";
  widget.setText(TaskText.fit(label,Math.max(1,width-pixels.applyAsInt(suffix)),pixels)+suffix);
  return widget;
 }
 private void dropdownField(String label,int y,int width,int which){
  Widget border=content.createChild(-1,3);border.setPos(8,y).setSize(width,22).setFilled(true).setTextColor(0x161410);border.revalidate();
  Widget bevel=content.createChild(-1,3);bevel.setPos(9,y+1).setSize(width-2,20).setFilled(true).setTextColor(0x62594a);bevel.revalidate();
  Widget inner=content.createChild(-1,3);inner.setPos(10,y+2).setSize(width-4,18).setFilled(true).setTextColor(0x494034);inner.revalidate();
  Widget title=countedFilterText(label,filterCount(which,which==1?bossFilter:TASK_STATUSES[taskFilter]),12,y+2,width-27,18,GOLD);title.setXTextAlignment(1);
  Widget arrow=nativeSprite(content,dropdown==which?773:788,8+width-19,y+3,16,16,false);
  JavaScriptCallback toggle=e->{dropdown=dropdown==which?0:which;dropdownScroll=0;menuOpen=false;refresh();};
  for(Widget w:new Widget[]{title,arrow}){w.setAction(0,"Select "+(which==1?"category":"status"));w.setHasListener(true);w.setOnOpListener(toggle);}
 }
 private void renderTaskRow(Challenge.Task task,int row,int y,int width){
  boolean locked=TaskBrowser.locked(state,task),warning=TaskBrowser.warning(state,task);
  Widget bg=content.createChild(-1,3);bg.setPos(0,y).setSize(width,42).setFilled(true)
   .setTextColor(locked?(row%2==0?0x383329:0x312d25):row%2==0?0x544b3e:0x4a4235);bg.revalidate();
  int rowColour=bg.getTextColor(),hoverColour=locked?0x484235:0x665b49;
  int textWidth=width-8-(locked||warning?30:0);
  Widget title=fittedText((task.done?"Done: ":"")+task.name,4,y+2,textWidth,15,task.done?0x66bb66:locked?0x999999:GOLD);
  String statusLabel=locked?"Locked":"Possible conflict";
  FontTypeFace rowFont=title.getFont();
  int statusWidth=locked||warning?Math.min(width-8,Math.max(20,(rowFont==null?statusLabel.length()*7:rowFont.getTextWidth(statusLabel))+6)):0;
  Widget boss=fittedText(task.boss,4,y+20,width-8-(locked||warning?statusWidth+4:0),18,locked?0x888888:WHITE);
  Widget rowIcon=null,rowStatus=null;
  if(locked||warning){
   int nameWidth=width-12-statusWidth;
   title.setOriginalWidth(nameWidth).setText(TaskText.fit((task.done?"Done: ":"")+task.name,nameWidth,rowFont==null?v->v.length()*7:rowFont::getTextWidth));title.revalidate();
   Widget icon=statusIcon(width-statusWidth-4+(statusWidth-20)/2,y,locked,task);
   Widget status=fittedText(statusLabel,width-statusWidth-4,y+22,statusWidth,17,locked?0xe6b65e:0xff5555);
   status.setXTextAlignment(1);readStatus(status,task,locked);
   rowIcon=icon;rowStatus=status;
   for(Widget part:new Widget[]{icon,status}){
    part.setOnMouseOverListener((JavaScriptCallback)e->updateTaskRowHighlights());
    part.setOnMouseLeaveListener((JavaScriptCallback)e->updateTaskRowHighlights());
   }
  }
  readTask(bg,task,"View Task");readTask(title,task,"View Task");readTask(boss,task,"View Task");
  RowHighlight highlight=highlightTaskRow(bg,rowColour,hoverColour,bg,title,boss);
  highlight.icon=rowIcon;highlight.status=rowStatus;highlight.locked=locked;
 }
 private Widget nativeSprite(Widget parent,int id,int x,int y,int width,int height,boolean tiled){
  Widget w=parent.createChild(-1,5);w.setPos(x,y).setSize(width,height).setSpriteId(id).setSpriteTiling(tiled);w.revalidate();return w;
 }
 // These resource strips retain the native pixels but omit transparent cache margins.
 private void menuEdge(Widget parent,int source,int x,int y,int width,int height){
  int id=10020000+source;
  if(!client.getSpriteOverrides().containsKey(id)){
   try(java.io.InputStream input=getClass().getResourceAsStream("/com/vannaka/calocked/menu-edge-"+source+".png")){
    registerSprite(id,javax.imageio.ImageIO.read(input));
   }catch(java.io.IOException ex){throw new IllegalStateException("Native menu edge unavailable",ex);}
  }
  nativeSprite(parent,id,x,y,width,height,true);
 }
 private Widget popup(int x,int y,int width,int height,boolean framed){
  Widget layer=client.getWidget(GROUP,16),choices=client.getWidget(GROUP,17),frame=client.getWidget(GROUP,1);
  layer.setPos(0,0,0,0).setSize(frame.getWidth(),frame.getHeight(),0,0).setHidden(false);layer.setNoClickThrough(true);layer.revalidate();
  layer.setOnClickListener((Object[])null);choices.setOnClickListener((Object[])null);
  layer.clearActions();layer.setOnOpListener((Object[])null);
  Rectangle origin=frame.getBounds();popupBounds=new Rectangle(origin.x+x,origin.y+y,width,height);
  nativeSprite(layer,1040,x,y,width,height,true);
  if(framed){
   menuEdge(layer,172,x,y+30,6,Math.max(0,height-60));
   menuEdge(layer,315,x+width-6,y+30,6,Math.max(0,height-60));
   menuEdge(layer,314,x+25,y,Math.max(0,width-50),6);
   menuEdge(layer,173,x+25,y+height-6,Math.max(0,width-50),6);
   nativeSprite(layer,310,x,y,25,30,false);nativeSprite(layer,311,x+width-25,y,25,30,false);
   nativeSprite(layer,312,x,y+height-30,25,30,false);nativeSprite(layer,313,x+width-25,y+height-30,25,30,false);
  }else{
   for(int[] edge:new int[][]{{x,y,width,1},{x,y+height-1,width,1},{x,y,1,height},{x+width-1,y,1,height}}){
    Widget line=layer.createChild(-1,3);line.setPos(edge[0],edge[1]).setSize(edge[2],edge[3]).setFilled(true).setTextColor(0x171510);line.revalidate();
   }
   for(int[] edge:new int[][]{{x+1,y+1,width-2,1},{x+1,y+height-2,width-2,1},{x+1,y+1,1,height-2},{x+width-2,y+1,1,height-2}}){
    Widget bevel=layer.createChild(-1,3);bevel.setPos(edge[0],edge[1]).setSize(edge[2],edge[3]).setFilled(true).setTextColor(0x62594a);bevel.revalidate();
   }
  }
  int inset=framed?6:3;
  choices.setPos(x+inset,y+inset,0,0).setSize(width-inset*2,height-inset*2,0,0).setHidden(false);
  choices.setScrollY(0).setScrollHeight(0).setScrollWidth(0);choices.setNoClickThrough(true);choices.setNoScrollThrough(true);choices.revalidate();return choices;
 }
 private void renderMenu(){
  clearScroll();Widget parent=content;content=popup(0,29,110,state.challengeActive?127:58,true);
  String[] names=state.challengeActive?new String[]{"Picker","Tasks","Rules","Challenge","Settings"}:new String[]{"Challenge","Rules"};
  int[] destinations=state.challengeActive?new int[]{0,1,3,6,5}:new int[]{5,3};
  for(int i=0;i<names.length;i++){
   if(view==destinations[i]){Widget active=content.createChild(-1,3);active.setPos(0,i*23).setSize(98,23).setFilled(true).setTextColor(0x655c4f);active.revalidate();}
   final int destination=destinations[i];Widget label=text(names[i],0,i*23,98,23,view==destination?WHITE:GOLD);label.setXTextAlignment(1).setYTextAlignment(1);
   label.setAction(0,names[i]);label.setHasListener(true);
   label.setOnMouseOverListener((JavaScriptCallback)e->label.setTextColor(WHITE));
   label.setOnMouseLeaveListener((JavaScriptCallback)e->label.setTextColor(view==destination?WHITE:GOLD));
   label.setOnOpListener((JavaScriptCallback)e->{confirmation=null;if(view!=destination)listScroll=0;view=destination;detail=null;statusTask=null;repairTask=null;detailHistory.clear();menuOpen=false;dropdown=0;refresh();});
  }
  content=parent;
 }
 private int paragraph(String message,int x,int y,int width,int colour){
  Widget probe=text("",0,0,1,1,WHITE);FontTypeFace font=probe.getFont();probe.setHidden(true);
  List<String> lines=TaskText.preview(message,width,Integer.MAX_VALUE,font==null?v->v.length()*7:font::getTextWidth);
  int height=lines.size()*16;
  text(String.join("<br>",lines),x,y,width,height,colour).setLineHeight(16);
  return y+height;
 }
 private int ruleRow(String label,String value,int x,int y,int width){
  int labelWidth=Math.min(88,width/3),valueX=x+labelWidth+8;
  int labelBottom=paragraph(label,x,y,labelWidth,GOLD);
  int valueBottom=paragraph(value,valueX,y,width-labelWidth-8,WHITE);
  return Math.max(labelBottom,valueBottom)+10;
 }
 private int ruleOptions(int x,int y,int width){
  y=ruleRow("Rerolling",!state.allowSkipping?"Off":state.rulePack==Challenge.FREE_PLAY?"Unlimited":state.rerollLimit+" per tier",x,y,width);
  y=ruleRow("Storage",state.allowStorage?"1 slot per tier":"Off",x,y,width);
  return ruleRow("Swapping",!state.allowStorage?"Not applicable":state.allowSwapping?"On":"Off",x,y,width);
 }
 private void rerollLimitButton(int x,int y,int width){
  int shownLimit=state.rulePack==Challenge.FREE_PLAY?0:state.rerollLimit;
  button("Limit per tier: "+shownLimit,x,y,width,()->{
   final String account=profile;
   chatbox.openTextInput("Rerolls per tier (0 = unlimited, 1-999 = limited)").value(Integer.toString(shownLimit)).onDone((java.util.function.Consumer<String>)value->clientThread.invokeLater(()->{
    if(state.challengeActive||account==null||!account.equals(profile)||!account.equals(config.getRSProfileKey()))return;
    try{state.setRerollLimit(Challenge.parseRerollLimit(value));notice="";save();}catch(IllegalArgumentException error){notice="Enter a non-negative whole number (0 = unlimited).";}
    if(open)refresh();
   })).build();
  },!state.challengeActive);
 }
 private void finishSettingsScroll(Widget parent,int width,int y,int height,int total){
  scrollViewport.setScrollHeight(Math.max(height,total)).setScrollY(TaskBrowser.clamp(listScroll,total,height));scrollViewport.revalidateScroll();
  content=parent;
  if(total>height)renderScrollbar(width,y,height,total);else {scrollViewport=null;scrollBounds=new Rectangle();listScroll=0;}
 }
 private void renderMode(int width){
  if(!state.challengeActive){renderChallengeSetup(width);return;}
  int viewportWidth=width-16,innerWidth=viewportWidth-16,height=Math.max(60,content.getHeight()-8);
  prepareScrollLayer(8,4,viewportWidth,height,1000,listScroll);
  Widget parent=content;content=scrollViewport;
  int half=(innerWidth-12)/2,right=20+half,y=8;
  text("Sound",8,y,innerWidth,18,GOLD);y+=24;
  button("Sound: "+(settings.rollSound()?"On":"Off"),8,y,half,()->config.setConfiguration("calocked","rollSound",!settings.rollSound()),true,settings.rollSound());
  button("Volume: "+settings.rollVolume()+"%",right,y,half,()->{
   chatbox.openTextInput("Task picker volume (0-100%)").value(Integer.toString(settings.rollVolume())).onDone((java.util.function.Consumer<String>)value->clientThread.invokeLater(()->{
    try{config.setConfiguration("calocked","rollVolume",Math.min(100,Challenge.parseRerollLimit(value)));notice="";}
    catch(IllegalArgumentException error){notice="Enter a non-negative whole number for the volume.";}
    if(open)refresh();
   })).build();
  },true);y+=40;
  divider(8,y,innerWidth);y+=16;
  text("Account progress",8,y,innerWidth,18,GOLD);y+=24;
  y=paragraph(sessions.getAccountSession()==null?"Saved for this character. Sign in to the same RuneLite account on both PCs to sync your challenge.":"RuneLite account connected. This character's challenge uses RuneLite's account sync.",8,y,innerWidth,0xc8c1b2)+12;
  button("Save progress",8,y,Math.min(180,innerWidth),()->executor.submit(config::sendConfig),true);y+=36;
  divider(8,y,innerWidth);y+=16;
  text("Sync achievements",8,y,innerWidth,18,0xc8c1b2);y+=24;
  button("Sync with game",8,y,Math.min(180,innerWidth),()->{
   List<Challenge.Task> fresh=readTasks();if(fresh.isEmpty()){notice="Game data unavailable. Try again shortly.";return;}
   state.refreshFromGame(fresh);dirty=false;notice="";
  },true);y+=32;
  if(!notice.isEmpty())y=paragraph(notice,8,y,innerWidth,0xff6666)+8;
  finishSettingsScroll(parent,width,4,height,y+8);
 }
 private void renderChallengeSetup(int width){
  String[] titles={"Welcome to CA Locked","Choose rerolling","Choose storage","Your challenge"};
  text(titles[setupStep],8,3,width-8,22,GOLD);
  text("Step "+(setupStep+1)+" of 4",8,25,width-8,17,0xc8c1b2);
  clearScroll();listScroll=0;
  int innerWidth=width,halfWidth=(innerWidth-12)/2,right=20+halfWidth,y=54;
  if(setupStep==0){
   y=paragraph("Choose your game mode. CA Locked is the default challenge; you can also choose a different way to play.",8,y,innerWidth,WHITE)+16;
   String[] descriptions={
    "Default challenge. Complete at least 50% of the Easy tasks to unlock Medium. Each later tier requires 50% of the previous tier and 100% of the tier before that.",
    "Pick any unfinished achievement across all six tiers.",
    "Choose a tier and pick only tasks from that specific difficulty.",
    "Complete each tier before moving to the next one."
   };
   for(int i=0;i<Challenge.MODES.length;i++){
    final int choice=i;
    button(i==0?"CA Locked (default)":Challenge.MODES[i],8,y+i*30,halfWidth,()->{
     if(state.challengeActive)return;
     state.mode=choice;notice="";bossFilter="All";dropdown=0;
    },true,state.mode==i);
   }
   text(Challenge.MODES[state.mode],right,y,halfWidth,20,GOLD);
   paragraph(descriptions[state.mode],right,y+28,halfWidth,0xc8c1b2);
  }else if(setupStep==1){
   y=paragraph("Rerolling replaces your active task with a different one. Turn it off to play without rerolls.",8,y,innerWidth,WHITE)+20;
   modeToggle("Rerolling",8,y,halfWidth,state.allowSkipping,()->{state.allowSkipping=!state.allowSkipping;notice="";});
   if(state.allowSkipping){
    rerollLimitButton(right,y,halfWidth);
    paragraph("Choose how many rerolls you allow per tier. Enter 0 for unlimited rerolls. Once your rerolls are used up, they are gone.",right,y+36,halfWidth,0xc8c1b2);
   }
   paragraph(state.allowSkipping?"You can replace a task whenever you have rerolls left.":"You cannot replace your picked task with a new roll. It is the task to complete.",8,y+36,halfWidth,0xc8c1b2);
  }else if(setupStep==2){
   y=paragraph("Storage gives each tier one slot to keep a task for later, so you can pick another task.",8,y,innerWidth,WHITE)+20;
   modeToggle("Storage",8,y,halfWidth,state.allowStorage,()->{state.allowStorage=!state.allowStorage;notice="";});
   paragraph(state.allowStorage?"Keep one task per tier for later.":"Play without stored tasks.",8,y+36,halfWidth,0xc8c1b2);
   if(state.allowStorage){
    modeToggle("Allow swapping",right,y,halfWidth,state.allowSwapping,()->state.allowSwapping=!state.allowSwapping);
    paragraph(state.allowSwapping?"Exchange your active and stored tasks whenever you want.":"Take the stored task only when there is no active task in that tier.",right,y+36,halfWidth,0xc8c1b2);
   }
  }else {
   y=paragraph("Ready to begin? These rules stay fixed during your challenge. Use Back to change your choices.",8,y,innerWidth,WHITE)+16;
   y=ruleRow("Game mode",Challenge.MODES[state.mode]+(state.mode==0?" (default)":""),8,y,innerWidth);
   ruleOptions(8,y,innerWidth);
  }
  if(!notice.isEmpty())fittedText(notice,8,content.getHeight()-57,innerWidth,20,0xff6666);
  int footer=content.getHeight()-31,half=(width-12)/2;
  if(setupStep>0)button("Back",8,footer+3,half,()->{setupStep--;listScroll=0;notice="";},true);
  pulseStepButton(setupStep==3?"Start challenge":"Next",setupStep==0?8:20+half,footer,setupStep==0?width:half,()->{
   if(state.challengeActive)return;
   if(setupStep<3){setupStep++;listScroll=0;notice="";return;}
   List<Challenge.Task> fresh=readTasks();if(fresh.isEmpty()){notice="Game data unavailable. Try again shortly.";return;}
   state=state.started(fresh);dirty=false;notice="";view=0;listScroll=0;selected=0;
   // Short GE coin tinkle, distinct from the tier-unlock ding (RuneLite SoundEffectID).
   playPickerSound(3924,0.65);
   for(int tier=0;tier<6;tier++)if(state.total(tier)>0&&state.readyToUnlock(tier)){selected=tier;break;}
  });
 }
 private void pulseStepButton(String label,int x,int y,int width,Runnable action){
  Widget name=outlinedButton(label,x,y,width,action,true,true);
  Widget[] parts=helpTargets.get(name);stepEdge=parts[0];stepFill=parts[1];animateStepButton();
 }
 private void animateStepButton(){
  if(stepEdge==null||stepFill==null)return;
  double phase=(Math.sin(System.currentTimeMillis()/650.0)+1)/2;
  stepEdge.setTextColor(blendColour(0xa48a5c,0xdac591,phase));
  stepFill.setTextColor(blendColour(0x403b30,0x564833,phase));
 }
 private void renderRules(int width){
  String[] tabs={"Challenge","Modes","Tasks","Storage","Rerolls","Account"};
  String[][][] chapters={
   {
    {"The challenge","Pick a task and work towards completing it. When it is done, pick the next one. Any other achievements you earn along the way count too."},
    {"Your rules","Choose your game mode, rerolls and storage before you start. Those choices stay fixed until you end the challenge."}
   },
   {
    {"CA Locked (default)","Complete at least half of the Easy tasks to unlock Medium. After that, each tier needs 50% of the tier below it and 100% of the one below that."},
    {"All Tiers","Pick from all unfinished tasks, regardless of difficulty."},
    {"One Tier","Choose a difficulty and pick tasks from that tier only."},
    {"Tier by Tier","Finish a whole tier before starting the next."}
   },
   {
    {"Your next task","Select a tier in Picker and use Pick Task to get your assignment."},
    {"Locked tasks","Some killcount and speed tasks must be done in order. Click the lock to see which task comes first. If a whole tier is locked, you need to unlock that tier first. A glowing tier is ready to unlock."},
    {"The task list","Use Tasks to browse achievements, search by name or filter by category and status. Click a task for its full description."}
   },
   {
    {"Save a task for later","If you choose to use storage, each tier gets one slot. Store your active task to make room for another. The stored task stays out of the randomizer."},
    {"With swapping","You can switch between your active task and the task in storage."},
    {"Without swapping","You can take the stored task only when you have no active task in that tier."}
   },
   {
    {"Pick something else","A reroll replaces your active task with a different one. The old task goes back into the pool and can come up again later."},
    {"When they are gone, they are gone","Each tier gets the number of rerolls you chose at the start. They do not refill. A limit of 0 means unlimited rerolls."}
   },
   {
    {"Switching accounts","Your active task, stored tasks and remaining rerolls are saved separately for each account."},
    {"Ending your challenge","Use End challenge on the Challenge page to stop. You can then choose new rules and start again. Your RuneScape achievements stay completed."},
    {"Correcting a task","If a task's completion status looks wrong, open Sync / fix on that task. Refresh from game checks its status in the game. You can also mark it complete or incomplete yourself."},
    {"Syncing all tasks","Sync with game in Settings checks every task and removes manual changes to completion status."},
    {"Sound","Sound and volume can be changed in Settings at any time."}
   }
  };
  int tabWidth=(width-8)/3;
  for(int i=0;i<tabs.length;i++){
   final int tab=i;
   button(tabs[i],8+(i%3)*(tabWidth+4),4+(i/3)*24,tabWidth,()->{rulesTab=tab;listScroll=0;},true,rulesTab==i);
  }
  int viewportWidth=width-16,y=56,height=Math.max(60,content.getHeight()-y-4);
  List<DetailLine> lines=new ArrayList<>();
  Widget measure=text("",0,0,1,1,WHITE);FontTypeFace font=measure.getFont();measure.setHidden(true);
  for(String[] section:chapters[rulesTab]){
   for(String line:TaskText.preview(section[0],viewportWidth-8,Integer.MAX_VALUE,font==null?v->v.length()*7:font::getTextWidth))lines.add(new DetailLine(line,GOLD,null));
   for(String line:TaskText.preview(section[1],viewportWidth-8,Integer.MAX_VALUE,font==null?v->v.length()*7:font::getTextWidth))lines.add(new DetailLine(line,WHITE,null));
   lines.add(new DetailLine("",WHITE,null));
  }
  int total=lines.size()*17+8;
  prepareScrollLayer(8,y,viewportWidth,height,total,listScroll);
  Widget parent=content;content=scrollViewport;
  for(int i=0;i<lines.size();i++){DetailLine line=lines.get(i);text(line.text,4,4+i*17,viewportWidth-8,17,line.colour);}
  content=parent;renderScrollbar(width,y,height,total);
 }
 private void renderChoiceList(List<String> choices,int width){
  Widget parent=content,frame=client.getWidget(GROUP,1);
  Rectangle bounds=parent.getBounds(),origin=frame.getBounds();
  int fieldY=dropdown==1?30:54,localY=fieldY+22;
  int height=Math.min(choices.size()*24+6,Math.max(48,parent.getHeight()-localY-4));
  int x=bounds.x-origin.x+8,y=bounds.y-origin.y+localY;
  Widget list=popup(x,y,width,height,false);
  list.setSize(width-22,height-6,0,0).setScrollHeight(Math.max(height-6,choices.size()*24)).setScrollY(TaskBrowser.clamp(dropdownScroll,choices.size()*24,height-6));list.revalidate();list.revalidateScroll();
  scrollViewport=list;scrollBounds=new Rectangle(list.getBounds());content=list;
  for(int i=0;i<choices.size();i++){
   final int index=i;String choice=choices.get(i);boolean chosen=dropdown==1?choice.equals(bossFilter):i==taskFilter;
   Widget label=countedFilterText(choice,filterCount(dropdown,choice),3,i*24+2,list.getWidth()-6,22,chosen?WHITE:GOLD);label.setXTextAlignment(1).setYTextAlignment(1);
   label.setAction(0,"Select "+choice);label.setHasListener(true);
   label.setOnMouseOverListener((JavaScriptCallback)e->label.setTextColor(WHITE));
   label.setOnMouseLeaveListener((JavaScriptCallback)e->label.setTextColor(chosen?WHITE:GOLD));
   label.setOnOpListener((JavaScriptCallback)e->{if(dropdown==1)bossFilter=choice;else taskFilter=index;dropdown=0;listScroll=0;refresh();});
  }
  content=client.getWidget(GROUP,16);renderScrollbar(x+width-11,y+3,height-6,choices.size()*24);content=parent;
 }
 private void prepareScrollLayer(int x,int y,int width,int height,int total,int offset){
  reelLayer=client.getWidget(GROUP,14);
  reelGeometry=new int[]{reelLayer.getOriginalX(),reelLayer.getOriginalY(),reelLayer.getOriginalWidth(),reelLayer.getOriginalHeight(),
   reelLayer.getXPositionMode(),reelLayer.getYPositionMode(),reelLayer.getWidthMode(),reelLayer.getHeightMode(),reelLayer.getScrollHeight(),reelLayer.getScrollWidth(),reelLayer.getScrollY()};
  reelLayer.deleteAllChildren();reelLayer.setPos(x,y,0,0).setSize(width,height,0,0).setHidden(false);
  reelLayer.setScrollHeight(Math.max(height,total)).setScrollWidth(width).setScrollY(TaskBrowser.clamp(offset,total,height));
  reelLayer.setNoClickThrough(true);reelLayer.setNoScrollThrough(true);reelLayer.revalidate();reelLayer.revalidateScroll();
  scrollViewport=reelLayer;scrollBounds=new Rectangle(reelLayer.getBounds());
  listScroll=reelLayer.getScrollY();
 }
 private void renderScrollbar(int width,int y,int height,int total){
  scrollTotal=total;scrollHeight=height;scrollTrackY=y+16;scrollTrackHeight=height-32;
  scrollThumbHeight=Math.max(10,TaskBrowser.thumbHeight(scrollTrackHeight,total,height));
  int x=width-8;
  Widget rail=nativeSprite(content,792,x,scrollTrackY,16,scrollTrackHeight,true);scrollRail=rail;
  scrollThumb=nativeSprite(content,790,x,scrollTrackY+5,16,Math.max(0,scrollThumbHeight-10),true);
  scrollThumbTop=nativeSprite(content,789,x,scrollTrackY,16,5,false);
  scrollThumbBottom=nativeSprite(content,791,x,scrollTrackY+scrollThumbHeight-5,16,5,false);
  for(Widget part:new Widget[]{rail,scrollThumb,scrollThumbTop,scrollThumbBottom}){
   part.setAction(0,"Drag scrollbar");part.setHasListener(true);part.setOnOpListener((JavaScriptCallback)e->{});
  }
  scrollArrow(x,y,773,-36);scrollArrow(x,y+height-16,788,36);scrollTo(scrollViewport.getScrollY());
 }
 private void scrollArrow(int x,int y,int sprite,int delta){
  Widget arrow=nativeSprite(content,sprite,x,y,16,16,false);arrow.setAction(0,delta<0?"Scroll up":"Scroll down");arrow.setHasListener(true);
  arrow.setOnOpListener((JavaScriptCallback)e->scrollBy(delta));
 }
 private void clearScroll(){
  wheelRemainder=0;scrollViewport=null;scrollRail=null;scrollThumb=null;scrollThumbTop=null;scrollThumbBottom=null;scrollBounds=new Rectangle();scrollRailBounds=new Rectangle();scrollThumbBounds=new Rectangle();scrollBarBounds=new Rectangle();draggingScroll=false;
 }
 /** Newly created widgets have no painted canvas position yet; publish geometry after rendering. */
 private void updateScrollBounds(){
  if(scrollViewport==null||scrollRail==null)return;
  Rectangle rail=scrollRail.getBounds(),viewport=scrollViewport.getBounds();
  if(rail.x<0||rail.y<0||rail.width!=16||rail.height!=scrollTrackHeight)return;
  if(viewport.x>=0&&viewport.y>=0)scrollBounds=new Rectangle(viewport);
  scrollBarBounds=new Rectangle(rail.x,rail.y-16,16,scrollHeight);
  scrollThumbBounds=new Rectangle(rail.x,rail.y+TaskBrowser.thumbTop(currentScroll,scrollTotal,scrollHeight,scrollTrackHeight,scrollThumbHeight),16,scrollThumbHeight);
  scrollRailBounds=new Rectangle(rail);
 }
 private void queueScrollPointer(int pointerY){
  latestScrollPointer.set(pointerY);
  if(scrollPointerQueued.compareAndSet(false,true))clientThread.invokeLater(()->{
   scrollPointerQueued.set(false);scrollFromPointer(latestScrollPointer.get());
  });
 }
 private void scrollBy(int delta){if(scrollViewport!=null&&open)scrollTo(scrollViewport.getScrollY()+delta);}
 private void scrollFromPointer(int pointerY){
  if(scrollViewport==null||!open)return;
  scrollTo(TaskBrowser.dragTo(pointerY-dragStartPointer,dragStartScroll,scrollTotal,scrollHeight,scrollTrackHeight,scrollThumbHeight));
 }
 private void scrollTo(int value){
  if(scrollViewport==null)return;
  int offset=TaskBrowser.clamp(value,scrollTotal,scrollHeight);currentScroll=offset;scrollViewport.setScrollY(offset);scrollViewport.revalidateScroll();
  if(dropdown==0)listScroll=offset;else dropdownScroll=offset;
  if(scrollThumb!=null){
   int top=scrollTrackY+TaskBrowser.thumbTop(offset,scrollTotal,scrollHeight,scrollTrackHeight,scrollThumbHeight);
   scrollThumb.setOriginalY(top+5);scrollThumb.revalidate();scrollThumbTop.setOriginalY(top);scrollThumbTop.revalidate();
   scrollThumbBottom.setOriginalY(top+scrollThumbHeight-5);scrollThumbBottom.revalidate();
   if(!scrollRailBounds.isEmpty())scrollThumbBounds=new Rectangle(scrollRailBounds.x,scrollRailBounds.y+top-scrollTrackY,16,scrollThumbHeight);
  }
 }
 private void syncMenuSprite(Widget widget){
  int wanted=menuOpen?2290:2289;
  if(widget.getType()==5&&widget.getSpriteId()!=wanted){widget.setSpriteId(wanted);widget.revalidate();}
 }
 private void bindMenu(Widget widget){
  widget.clearActions();widget.setAction(0,menuOpen?"Hide Menu":"Show Menu");widget.setHasListener(true);
  widget.setOnClickListener((Object[])null);
  // Native hover scripts latch the pressed sprite until another mouse-leave event.
  // The button's appearance follows our menu state instead of native hover state.
  widget.setOnMouseOverListener((Object[])null);
  widget.setOnMouseLeaveListener((Object[])null);
  widget.setOnMouseRepeatListener((Object[])null);widget.setOnHoldListener((Object[])null);widget.setOnReleaseListener((Object[])null);
  if(widget.getType()==5){widget.setSpriteId(menuOpen?2290:2289);widget.revalidate();}
  widget.setOnOpListener((JavaScriptCallback)e->{if(rollEnd==0){dropdown=0;menuOpen=!menuOpen;refresh();}});
 }

 private void renderTaskDetail(int ignoredWidth){
  expandReadingArea();int width=content.getWidth()-16;
  Challenge.Task current=state.task(detail.id);if(current!=null)detail=current;
  boolean locked=TaskBrowser.locked(state,detail),warning=TaskBrowser.warning(state,detail);
  int headerWidth=width-82;
  Widget measure=text("",0,0,1,1,WHITE);FontTypeFace font=measure.getFont();measure.setHidden(true);
  java.util.function.ToIntFunction<String> pixels=font==null?v->v.length()*7:font::getTextWidth;
  List<String> title=TaskText.preview(detail.name,headerWidth,Integer.MAX_VALUE,pixels);
  int titleHeight=title.size()*17,bossY=7+titleHeight,bodyY=bossY+55;
  text(String.join("<br>",title),8,4,headerWidth,titleHeight,locked?0xaaa49a:GOLD).setLineHeight(17);
  fittedText("Category: "+detail.boss,8,bossY,headerWidth,18,locked?0x929088:WHITE);
  Widget repair=fittedText("Sync / fix",width-60,12,68,18,0xc8c1b2);repair.setXTextAlignment(2);
  repair.setAction(0,"Completion recovery");repair.setHasListener(true);
  repair.setOnMouseOverListener((JavaScriptCallback)e->repair.setTextColor(GOLD));
  repair.setOnMouseLeaveListener((JavaScriptCallback)e->repair.setTextColor(0xc8c1b2));
  repair.setOnOpListener((JavaScriptCallback)e->{repairTask=detail;refresh();});
  fittedText("Completions: "+state.successes(detail.id)+" (while tracking)",8,bossY+21,headerWidth,18,0xc8c1b2);
  divider(8,bossY+45,width);
  int backY=content.getHeight()-29,lockY=backY-70;
  if(locked||warning){
   Widget icon=statusIcon(8+(width-36)/2,lockY,locked,detail);icon.setSize(36,40);icon.revalidate();
   Widget status=fittedText(locked?"Locked":"Conflict",8+(width-72)/2,lockY+44,72,18,locked?0xe6b65e:0xff5555);
   status.setXTextAlignment(1);readStatus(status,detail,locked);
   JavaScriptCallback hover=e->{icon.setOpacity(0);status.setTextColor(locked?0xffdc90:0xff9999);};
   JavaScriptCallback leave=e->{icon.setOpacity(45);status.setTextColor(locked?0xe6b65e:0xff5555);};
   for(Widget part:new Widget[]{icon,status}){part.setOnMouseOverListener(hover);part.setOnMouseLeaveListener(leave);}
  }
  int height=Math.max(17,(locked||warning?lockY-8:backY-8)-bodyY),viewportWidth=width-16;
  List<String> description=TaskText.preview(detail.description,viewportWidth-8,Integer.MAX_VALUE,pixels);
  int total=description.size()*17+8;
  prepareScrollLayer(8,bodyY,viewportWidth,height,total,listScroll);
  Widget parent=content;content=scrollViewport;
  for(int i=0;i<description.size();i++)text(description.get(i),4,4+i*17,viewportWidth-8,17,locked?0xaaa69e:WHITE);
  content=parent;
  if(total>height)renderScrollbar(width,bodyY,height,total);
  button(!detailHistory.isEmpty()?"Back to previous task":view==1?"Back to tasks":view==4?"Back to stats":"Back to picker",8,backY,width,()->{
   if(detailHistory.isEmpty()){listScroll=detailReturnScroll;detail=null;statusTask=null;repairTask=null;}
   else{DetailVisit previous=detailHistory.pop();detail=previous.task;detailPage=previous.page;listScroll=0;}
  },true);
 }
 private void renderCompletionRepair(int width){
  Challenge.Task target=state.task(repairTask.id);
  text("Completion recovery",8,4,width,22,GOLD);
  fittedText(repairTask.name,8,30,width,22,WHITE);
  Widget help=text("",8,60,width,51,0xc8c1b2);FontTypeFace font=help.getFont();
  help.setText(String.join("<br>",TaskText.preview("Refresh restores the real game status and clears manual edits. You can also mark this task complete or incomplete manually.",width,3,font==null?v->v.length()*7:font::getTextWidth))).setLineHeight(17);
  button("Refresh from game",8,118,width,()->{
   List<Challenge.Task> fresh=readTasks();
   if(fresh.isEmpty()){notice="Game data unavailable. Try again shortly.";return;}
   state.refreshFromGame(fresh);dirty=false;detail=state.task(repairTask.id);repairTask=null;notice="";
  },true);
  button(target!=null&&target.done?"Mark incomplete manually":"Mark complete manually",8,148,width,()->{
   final int id=repairTask.id;String name=repairTask.name;boolean undo=target!=null&&target.done;repairTask=null;
   confirm(undo?"Mark incomplete manually?":"Mark complete manually?",name,undo?"This restores the task to incomplete in CA Locked and removes its recorded completion credit.":"Confirm only if you earned it in game. This updates CA Locked only.",()->{
    if(undo?state.markIncompleteManually(id):state.markCompleteManually(id)){detail=state.task(id);notice="";}else notice="Task already complete or no longer available.";
   });
  },target!=null);
  if(!notice.isEmpty())fittedText(notice,8,172,width,18,0xc8c1b2);
  button("Back to task",8,199,width,()->repairTask=null,true);
 }
 private void detailLines(List<DetailLine> lines,String value,int columns,int colour,Challenge.Task target){
  for(String line:TaskText.wrap(value,columns))lines.add(new DetailLine(line,colour,target));
 }
 private void relatedTaskLines(List<DetailLine> lines,Challenge.Task task,int columns,int colour){
  String risk=KillCountRisk.explanation(state,detail,task);
  if(risk!=null)detailLines(lines,risk,columns,colour,null);
  detailLines(lines,Challenge.TIERS[task.tier]+": "+task.name,columns,colour,task);
  detailLines(lines,task.description,columns,0xc8c1b2,null);
  detailLines(lines,"",columns,WHITE,null);
 }
 private void divider(int x,int y,int width){
  Widget line=content.createChild(-1,3);line.setPos(x,y).setSize(width,1).setFilled(true).setTextColor(0x766957);line.revalidate();
 }
 private boolean showOverallProgress(){return state.challengeActive&&(view==0||view==1);}
 private void updateOverallProgress(){
  Widget bar=client.getWidget(GROUP,3);if(bar==null)return;
  hiddenBefore.putIfAbsent(bar,bar.isSelfHidden());bar.setHidden(!showOverallProgress());
  if(!showOverallProgress())return;
  int total=state.tasks.size(),done=(int)state.tasks.stream().filter(t->t.done).count();
  Widget background=bar.getChild(1),fill=bar.getChild(2),label=bar.getChild(3);
  if(background!=null&&fill!=null){
   fill.setSize(total==0?0:(int)Math.round(background.getWidth()*done/(double)total),background.getHeight(),0,0);fill.revalidate();
  }
  if(label!=null)label.setText("Tasks completed: "+done+" / "+total+" ("+(total==0?"0":String.format(Locale.ROOT,"%.1f",done*100.0/total))+"%)");
  if(bar.getChildren()!=null)for(Widget child:bar.getChildren())if(child!=null){child.clearActions();child.setOnOpListener((Object[])null);child.setOnMouseOverListener((Object[])null);child.setOnMouseLeaveListener((Object[])null);}
 }
 private void confirm(String title,String subject,String message,Runnable action){
  confirmation=new Confirmation(title,subject,message,profile,action);menuOpen=false;dropdown=0;detail=null;statusTask=null;repairTask=null;refresh();
 }
 private void renderConfirmation(int width){
  Confirmation prompt=confirmation;
  Widget subject=text("",8,0,width,51,WHITE);FontTypeFace font=subject.getFont();
  List<String> subjectLines=TaskText.preview(prompt.subject,width,3,font==null?v->v.length()*7:font::getTextWidth);
  List<String> messageLines=TaskText.preview(prompt.message,width,4,font==null?v->v.length()*7:font::getTextWidth);
  int subjectHeight=subjectLines.size()*17,messageHeight=messageLines.size()*17;
  int top=Math.max(12,(174-(22+10+subjectHeight+16+messageHeight))/2);
  text(prompt.title,8,top,width,22,GOLD).setXTextAlignment(1);
  subject.setPos(8,top+32).setSize(width,subjectHeight).setText(String.join("<br>",subjectLines)).setLineHeight(17).setXTextAlignment(1);subject.revalidate();
  Widget message=text(String.join("<br>",messageLines),8,top+48+subjectHeight,width,messageHeight,0xc8c1b2);
  message.setLineHeight(17).setXTextAlignment(1);message.revalidate();
  int half=(width-4)/2;
  button("Yes",8,184,half,()->{
   confirmation=null;
   if(prompt.account!=null&&prompt.account.equals(profile)&&prompt.account.equals(config.getRSProfileKey()))prompt.action.run();
  },true);
  button("Cancel",12+half,184,half,()->confirmation=null,true);
 }
 private void renderStats(int width){
  int half=(width-4)/2;
  button("Overview",8,4,half,()->{statsTab=0;listScroll=0;},true,statsTab==0);
  button("Most Completed",12+half,4,half,()->{statsTab=1;listScroll=0;},true,statsTab==1);
  int viewportWidth=width-16,y=32,height=content.getHeight()-36;
  if(statsTab==1){renderSuccessStats(width,viewportWidth,y,height);return;}
  int overviewHeight=199;
  prepareScrollLayer(8,y,viewportWidth,height,overviewHeight,listScroll);
  Widget parent=content;content=scrollViewport;int innerWidth=viewportWidth-8;
  int[] tierTotals=TaskSuccesses.totals(state,selected,false),allTotals=TaskSuccesses.totals(state,selected,true);
  text(Challenge.TIERS[selected]+" challenge record",4,4,innerWidth,18,GOLD);
  completionStats(tierTotals,4,29,innerWidth);
  text("Rerolls: "+state.skips[selected]+" | Swaps: "+state.swaps[selected],4,71,innerWidth,18,WHITE);
  divider(4,98,innerWidth);
  text("All tiers",4,108,innerWidth,18,GOLD);
  completionStats(allTotals,4,132,innerWidth);
  text("Rerolls: "+state.totalSkips()+" | Swaps: "+state.totalSwaps(),4,174,innerWidth,18,WHITE);
  finishSettingsScroll(parent,width,y,height,overviewHeight);
 }
 private void completionStats(int[] totals,int x,int y,int width){
  Widget unique=text("Unique tasks completed: "+totals[0],x,y,width,18,WHITE);
  Widget total=text("Total tasks completed: "+totals[1],x,y+21,width,18,WHITE);
  setHoverHelp(unique,"Different tasks completed during this challenge. Earlier account achievements are not included.");
  setHoverHelp(total,"Completions during this challenge, including repeats reported by the game. Kill Count and Speed repeats are not reported.");
 }
 private void unlockButton(int width,Runnable action){
  unlockGlow=content.createChild(-1,3);unlockGlow.setPos(6,172).setSize(width+4,30).setFilled(true).setTextColor(0xffd778);unlockGlow.revalidate();
  unlockEdge=content.createChild(-1,3);unlockEdge.setPos(8,174).setSize(width,26).setFilled(true).setTextColor(0xffedb2);unlockEdge.revalidate();
  unlockFill=content.createChild(-1,3);unlockFill.setPos(10,176).setSize(width-4,22).setFilled(true).setTextColor(0x654d2e);unlockFill.revalidate();
  Widget name=fittedText("Unlock this tier",12,178,width-8,18,0xfff1c9);name.setXTextAlignment(1).setYTextAlignment(1);
  for(Widget part:new Widget[]{unlockGlow,unlockEdge,unlockFill,name}){
   part.setAction(0,"Unlock this tier");part.setHasListener(true);
   part.setOnMouseOverListener((JavaScriptCallback)e->{unlockHovered=true;animateUnlockButton();});
   part.setOnMouseLeaveListener((JavaScriptCallback)e->{unlockHovered=false;animateUnlockButton();});
   part.setOnOpListener(accountAction(action));
  }
  animateUnlockButton();
 }
 private void animateUnlockButton(){
  if(unlockGlow==null||unlockEdge==null||unlockFill==null)return;
  double phase=(Math.sin(System.currentTimeMillis()/420.0)+1)/2;
  unlockGlow.setOpacity((int)Math.round(200-85*phase));
  unlockEdge.setTextColor(blendColour(0xd8b86b,0xfff5d4,unlockHovered?1:phase));
  unlockFill.setTextColor(blendColour(0x51402a,0x795c35,unlockHovered?1:phase));
 }
 private int blendColour(int from,int to,double amount){
  int result=0;
  for(int shift:new int[]{16,8,0})result|=((int)Math.round(((from>>shift)&255)*(1-amount)+((to>>shift)&255)*amount))<<shift;
  return result;
 }
 private Widget pickerButton(String label,int x,int y,int width,Runnable action,boolean enabled,boolean primary,boolean secondary){
  return primary||secondary?outlinedButton(label,x,y-2,width,action,enabled,primary):button(label,x,y,width,action,enabled);
 }
 private void resetButton(String label,int x,int y,int width,Runnable action){outlinedButton(label,x,y,width,action,true,true);}
 private Widget outlinedButton(String label,int x,int y,int width,Runnable action,boolean enabled,boolean prominent){
  int edge=enabled?(prominent?0xa48a5c:0x766957):0x554f41;
  int foreground=enabled?(prominent?0xffcf73:GOLD):0x888888;
  Widget border=content.createChild(-1,3);border.setPos(x,y).setSize(width,26).setFilled(true).setTextColor(edge);border.revalidate();
  Widget fill=content.createChild(-1,3);fill.setPos(x+1,y+1).setSize(width-2,24).setFilled(true).setTextColor(enabled?0x403b30:0x38352e);fill.revalidate();
  Widget name=fittedText(label,x+4,y+4,width-8,18,foreground);name.setXTextAlignment(1).setYTextAlignment(1);
  if(enabled)for(Widget part:new Widget[]{border,fill,name}){
   part.setAction(0,label);part.setHasListener(true);
   part.setOnMouseOverListener((JavaScriptCallback)e->{border.setTextColor(prominent?0xd6b574:0x987f52);fill.setTextColor(prominent?0x665337:0x534936);name.setTextColor(prominent?0xffe2a0:0xffdc90);});
   part.setOnMouseLeaveListener((JavaScriptCallback)e->{border.setTextColor(edge);fill.setTextColor(0x403b30);name.setTextColor(foreground);});
   part.setOnOpListener(accountAction(action));
  }
  helpTargets.put(name,new Widget[]{border,fill,name});
  return name;
 }
 private void renderChallengeOverview(int width){
  String[] modeDescriptions={
   "Unlock tiers at 50% of the previous tier and 100% of the tier before that.",
   "Pick unfinished tasks from all six tiers.",
   "Choose a tier and pick only tasks from that specific difficulty.",
   "Complete each tier before moving on to the next."
  };
  int bodyY=4,height=Math.max(60,content.getHeight()-8),viewportWidth=width-16,innerWidth=viewportWidth-16;
  prepareScrollLayer(8,bodyY,viewportWidth,height,1000,listScroll);
  Widget parent=content;content=scrollViewport;int y=4;
  text("Your challenge rules",8,y,innerWidth,20,GOLD);y+=30;
  y=paragraph("Game mode: "+Challenge.MODES[state.mode],8,y,innerWidth,WHITE)+8;
  y=paragraph(modeDescriptions[state.mode],8,y,innerWidth,0xc8c1b2)+18;
  y=ruleOptions(8,y,innerWidth);
  if(!notice.isEmpty())y=paragraph(notice,8,y,innerWidth,0xff6666)+8;
  int endWidth=Math.min(200,innerWidth),endX=parent.getWidth()/2-scrollViewport.getOriginalX()-endWidth/2;
  outlinedButton("End challenge",endX,y+8,endWidth,()->confirm("End this challenge?",Challenge.MODES[state.mode],
   "End the current challenge and clear its assignments, storage and statistics. You can then choose new rules and start again. Your achievements in the game stay completed.",()->{
    List<Challenge.Task> fresh=readTasks();if(fresh.isEmpty()){notice="Game data unavailable. Try again shortly.";return;}
    state=state.restarted(fresh);setupStep=0;view=5;dirty=false;listScroll=0;detail=null;statusTask=null;repairTask=null;notice="";
   }),true,false);
  finishSettingsScroll(parent,width,bodyY,height,y+42);
 }

 private void renderSuccessStats(int width,int viewportWidth,int y,int height){
  int half=(width-4)/2;
  button("This tier",8,y,half,()->{mostCompletedAllTiers=false;listScroll=0;},true,!mostCompletedAllTiers);
  button("All tiers",12+half,y,half,()->{mostCompletedAllTiers=true;listScroll=0;},true,mostCompletedAllTiers);
  y+=28;height-=28;
  List<Challenge.Task> ranked=TaskSuccesses.ranked(state,selected,mostCompletedAllTiers);
  Widget probe=text("",0,0,1,1,WHITE);FontTypeFace font=probe.getFont();
  java.util.function.ToIntFunction<String> measure=font==null?v->v.length()*7:font::getTextWidth;
  List<String> explanation=TaskText.preview("Counts RuneScape completion messages.<br>Kill Count and Speed tasks are excluded.",viewportWidth-8,Integer.MAX_VALUE,measure);
  boolean showNote=client.getVarbitValue(CA_REPEAT_COMPLETION_VARBIT)!=1;
  List<String> note=TaskText.preview("Repeat completion messages are currently off, so repeated achievements are not being recorded. Enable Combat Achievement Tasks - Repeat completion in the game's settings to include them in these statistics.",viewportWidth-16,Integer.MAX_VALUE,measure);
  int explanationHeight=explanation.size()*17,rowsY=24+explanationHeight+12;
  int noteHeight=30+note.size()*15,noteY=Math.max(rowsY+Math.max(1,ranked.size())*44+14,height-noteHeight-8),total=showNote?noteY+noteHeight+8:rowsY+Math.max(1,ranked.size())*44+8;
  prepareScrollLayer(8,y,viewportWidth,height,total,listScroll);
  Widget parent=content;content=scrollViewport;
  text("Recorded while this plugin is running",4,4,viewportWidth-8,18,GOLD);
  text(String.join("<br>",explanation),4,24,viewportWidth-8,explanationHeight,0xc8c1b2).setLineHeight(17);
  if(ranked.isEmpty())text(mostCompletedAllTiers?"No completions recorded yet.":"No completions recorded for this tier yet.",4,rowsY,viewportWidth-8,32,WHITE);
  for(int i=0;i<ranked.size();i++){
   Challenge.Task task=ranked.get(i);int rowY=rowsY+i*44;
   Widget bg=content.createChild(-1,3);bg.setPos(0,rowY).setSize(viewportWidth,42).setFilled(true).setTextColor(i%2==0?0x544b3e:0x4a4235);bg.revalidate();readTask(bg,task,"View Task");
   Widget name=fittedText(task.name,4,rowY+3,viewportWidth-8,17,GOLD);readTask(name,task,"View Task");
   Widget counter=text("Completions: "+state.successes(task.id),4,rowY+23,viewportWidth-8,17,WHITE);readTask(counter,task,"View Task");
   highlightTaskRow(bg,bg.getTextColor(),0x665b49,bg,name,counter);
  }
  if(showNote){
   divider(4,noteY,viewportWidth-8);
   text("Note: repeat tracking is off",8,noteY+10,viewportWidth-16,17,0xb9ab8f);
   text(String.join("<br>",note),8,noteY+30,viewportWidth-16,note.size()*15,0xa9a397).setLineHeight(15);
  }
  content=parent;renderScrollbar(width,y,height,total);
 }

 final class Orb extends Overlay {
  Rectangle bounds=new Rectangle();
  Orb(){setPosition(OverlayPosition.DYNAMIC);setLayer(OverlayLayer.ABOVE_WIDGETS);setPriority(OverlayPriority.HIGH);}
  @Override public Dimension render(Graphics2D graphics){
   bounds=new Rectangle();if(client.getGameState()!=GameState.LOGGED_IN)return null;
   if(open&&!menuOpen&&dropdown==0){
    for(Map.Entry<Widget,String> help:hoverHelp.entrySet())if(!help.getKey().isHidden()
       &&help.getKey().getBounds().contains(client.getMouseCanvasPosition().getX(),client.getMouseCanvasPosition().getY()))
     tooltips.add(new Tooltip(String.join("<br>",TaskText.wrap(help.getValue(),52))));
    for(Widget disabled:new Widget[]{disabledRerollButton,disabledStorageButton,unavailableStorageButton})if(disabled!=null&&!disabled.isHidden()
       &&disabled.getBounds().contains(client.getMouseCanvasPosition().getX(),client.getMouseCanvasPosition().getY()))
     tooltips.add(new Tooltip(disabled==disabledRerollButton?"Enable rerolling in Settings to reroll tasks.":disabled==disabledStorageButton?"Enable storage in Settings to store tasks.":"Finish your active task before taking a stored task. Enable swapping in Settings to exchange them."));
   }
   Widget map=client.getWidget(WidgetInfo.MINIMAP_WORLDMAP_ORB);if(map==null||map.isHidden())return null;
   Rectangle b=map.getBounds();int size=34;
   // Continue the minimap's right-hand arc above the existing world-map button.
   int x=Math.min(b.x+10,client.getCanvasWidth()-size-4),y=b.y-39;
   bounds=new Rectangle(x,y,size,size);
   boolean hovered=bounds.contains(client.getMouseCanvasPosition().getX(),client.getMouseCanvasPosition().getY());
   if(hovered)tooltips.add(new Tooltip("Combat Achievements"));
   drawOrb(graphics,x,y,size,hovered);
   return null;
  }
  private void drawOrb(Graphics2D graphics,int x,int y,int size,boolean hovered){
   Graphics2D g=(Graphics2D)graphics.create();
   try{
    // Keep the rim at native pixel resolution, with a restrained worn texture.
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
    g.setColor(new Color(0x221e17));g.fillOval(x,y,size,size);
    g.setPaint(new GradientPaint(x,y,new Color(hovered?0x897c5d:0x6a6047),
      x,y+size,new Color(hovered?0x574c37:0x3c3528)));
    g.fillOval(x+1,y+1,size-2,size-2);
    g.setPaint(new GradientPaint(x,y+2,new Color(0x514833),x,y+size-2,new Color(0x685d43)));
    g.fillOval(x+3,y+3,size-6,size-6);
    g.setColor(new Color(0x30281b));g.fillOval(x+4,y+4,size-8,size-8);
    g.setPaint(new GradientPaint(x,y+5,new Color(hovered?0xe69335:0xc77a27),x,y+size-5,new Color(0x7f390e)));
    g.fillOval(x+5,y+5,size-10,size-10);
    BufferedImage book=new BufferedImage(16,18,BufferedImage.TYPE_INT_ARGB);
    Graphics2D icon=book.createGraphics();drawBook(icon,0,0);icon.dispose();
    int iconWidth=16,iconHeight=18;
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(book,x+(size-iconWidth)/2,y+(size-iconHeight)/2,iconWidth,iconHeight,null);
    if((state.challengeActive&&state.hasReadyTier())){
     double pulse=(Math.sin(System.currentTimeMillis()/420.0)+1)/2;
     g.setStroke(new BasicStroke(1.3f));
     for(int ring=3;ring>=1;ring--){g.setColor(new Color(255,213,118,(int)Math.round((20+50*pulse)/(double)ring)));g.drawOval(x-ring,y-ring,size+ring*2,size+ring*2);}
     g.setColor(new Color(255,239,187,(int)Math.round(75+140*pulse)));g.drawOval(x+1,y+1,size-2,size-2);
    }
   }finally{g.dispose();}
  }
  private void drawBook(Graphics2D g,int x,int y){
   // Stepped pixels and a bevelled spine keep the tiny book close to native icons.
   g.setColor(new Color(0x3e3025));g.fillRect(x+2,y,12,18);g.fillRect(x+1,y+1,14,16);
   g.setColor(new Color(0x754b3b));g.fillRect(x+2,y+1,12,14);
   g.setColor(new Color(0x95644c));g.drawLine(x+2,y+1,x+13,y+1);g.drawLine(x+2,y+2,x+2,y+14);
   g.setColor(new Color(0x53382d));g.fillRect(x+12,y+2,2,13);
   g.setColor(new Color(0xb7af90));g.fillRect(x+3,y+15,10,2);
   g.setColor(new Color(0x83785c));g.drawLine(x+3,y+16,x+12,y+16);
   BufferedImage sword=mediumSword;
   if(sword!=null){
    double scale=Math.min(10.0/sword.getWidth(),12.0/sword.getHeight());
    int w=Math.max(1,(int)Math.round(sword.getWidth()*scale));
    int h=Math.max(1,(int)Math.round(sword.getHeight()*scale));
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    g.drawImage(sword,x+(16-w)/2,y+2+(12-h)/2,w,h,null);
   }
  }
 }
}
