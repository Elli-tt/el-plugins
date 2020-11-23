package net.runelite.client.plugins.ElTutorial;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import net.runelite.client.plugins.botutils.BotUtils;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.time.Instant;
import static net.runelite.client.plugins.ElTutorial.ElTutorialState.*;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
		name = "El Tutorial",
		description = "Completes tutorial",
		type = PluginType.SKILLING
)
@Slf4j
public class ElTutorialPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	ItemManager itemManager;

	@Inject
	private ElTutorialConfig config;

	@Inject
	private ElTutorialOverlay overlay;



	//plugin data
	GameObject targetObject;
	MenuEntry targetMenu;
	WallObject targetWall;
	NPC targetNPC;
	int clientTickBreak = 0;
	int tickTimer;
	boolean startTutorial;
	ElTutorialState status;
	int varbitValue;
	int tutorialSectionProgress;
	int ironmanProgress;

	//overlay data
	Instant botTimer;
	int clientTickCounter;
	boolean clientClick;


	// Provides our config
	@Provides
	ElTutorialConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElTutorialConfig.class);
	}

	@Override
	protected void startUp()
	{
		botTimer = Instant.now();
		setValues();
		startTutorial=false;
		log.info("Plugin started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		setValues();
		startTutorial=false;
		log.info("Plugin stopped");
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElTutorial"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTutorial)
			{
				startTutorial = true;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
			} else {
				shutDown();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElTutorial"))
		{
			return;
		}
		startTutorial = false;
	}

	private void setValues()
	{
		clientTickCounter=-1;
		clientTickBreak=0;
		clientClick=false;
		tutorialSectionProgress=0;
		varbitValue = 0;
	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (!startTutorial)
		{
			return;
		}
		clientTickCounter=0;
		status = checkPlayerStatus();
		switch (status) {
			case ANIMATING:
			case NULL_PLAYER:
			case TICK_TIMER:
				break;
			case MOVING:
				shouldRun();
				break;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(),targetMenu.getTarget(),targetMenu.getIdentifier(),targetMenu.getOpcode(),targetMenu.getParam0(),targetMenu.getParam1());
			targetMenu=null;
		}
	}

	private long sleepDelay()
	{
		if(config.customDelays()){
			return utils.randomDelay(config.sleepWeighted(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		} else {
			return utils.randomDelay(false, 60, 350, 100, 100);
		}

	}

	private int tickDelay()
	{
		if(config.customDelays()){
			return (int) utils.randomDelay(config.tickWeighted(),config.tickMin(), config.tickMax(), config.tickDeviation(), config.tickTarget());
		} else {
			return (int) utils.randomDelay(false,1, 3, 2, 2);
		}

	}

	private ElTutorialState checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return NULL_PLAYER;
		}
		if(utils.iterating){
			return ITERATING;
		}
		if(player.getPoseAnimation()!=813 && player.getPoseAnimation()!=5160 && player.getPoseAnimation()!=808){
			return MOVING;
		}
		if(player.getAnimation()!=-1){
			return ANIMATING;
		}
		if(tickTimer>0)
		{
			tickTimer--;
			return TICK_TIMER;
		}
		tickTimer=tickDelay();
		return getRegularState();
	}

	private Point getRandomNullPoint()
	{
		if(client.getWidget(161,34)!=null){
			Rectangle nullArea = client.getWidget(161,34).getBounds();
			return new Point ((int)nullArea.getX()+utils.getRandomIntBetweenRange(0,nullArea.width), (int)nullArea.getY()+utils.getRandomIntBetweenRange(0,nullArea.height));
		}

		return new Point(client.getCanvasWidth()-utils.getRandomIntBetweenRange(0,2),client.getCanvasHeight()-utils.getRandomIntBetweenRange(0,2));
	}

	private void shouldRun()
	{
		if(client.getWidget(160,23)!=null){ //if run widget is visible
			if(Integer.parseInt(client.getWidget(160,23).getText())>(30+utils.getRandomIntBetweenRange(0,20))){ //if run > 30+~20
				if(client.getWidget(160,27).getSpriteId()==1069){ //if run is off
					targetMenu = new MenuEntry("Toggle Run","",1,57,-1,10485782,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
					return;
				}
			}
		}
	}

	private ElTutorialState getRegularState()
	{
		varbitValue = client.getVarpValue(281);
		switch(varbitValue){
			case 1:
				switch(tutorialSectionProgress){
					case 0:
						if(client.getWidget(162,45)!=null && !client.getWidget(162,45).isHidden()){
							client.setVar(VarClientInt.INPUT_TYPE, 15);
							client.setVar(VarClientStr.INPUT_TEXT, String.valueOf("zezima"));
							client.runScript(681);
							client.runScript(ScriptID.MESSAGE_LAYER_CLOSE);
							break;
						} else if(client.getWidget(558,14)!=null && !client.getWidget(558,14).isHidden()){
							selectName();
							break;
						} else if(client.getWidget(558,12)!=null && !client.getWidget(558,12).isHidden()){
							if(client.getWidget(558,12).getText().contains("available")){
								setName();
								tickTimer+=6;
								break;
							}
						} else if(config.female()){
							changeLook(44499010);
							tutorialSectionProgress++;
							break;
						} else if(!config.female()){
							tutorialSectionProgress++;
							break;
						}
						break;
					case 1:
					case 2:
						changeLook(44498956);
						tutorialSectionProgress++;
						break;
					case 3:
						changeLook(44498991);
						tutorialSectionProgress++;
						break;
					case 4:
					case 5:
						changeLook(44498995);
						tutorialSectionProgress++;
						break;
					case 6:
						changeLook(44499012);
						break;
				}
				break;
			case 2:
				if(client.getWidget(219,1)!=null && client.getWidget(219,1).getChild(0).getText().contains("your experience")){
					answerExperienceQuestions();
					break;
				} else {
					switch(tutorialSectionProgress){
						case 0:
							talkNPC(3308);
							tutorialSectionProgress++;
							break;
						case 1:
							pressSpace();
							break;
					}
				}
				break;
			case 3:
				switch(tutorialSectionProgress){
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747945);
						break;
				}
				break;
			case 7:
				if(client.getWidget(548,3)!=null && !client.getWidget(548,3).isHidden()){
					openTab(17104930);
					break;
				}
				switch(tutorialSectionProgress){
					case 0:
						talkNPC(3308);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 10:
				switch(tutorialSectionProgress){
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openDoor(9398);
						break;
				}
				break;
			case 20:
				talkNPC(8503);
				break;
			case 30:
				switch(tutorialSectionProgress){
					case 0:
					case 1:
					case 2:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 3:
						openTab(10747960);
						break;
				}
				break;
			case 40:
				talkNPC(3317);
				break;
			case 50:
				openTab(10747958);
				break;
			case 60:
				switch(tutorialSectionProgress){
					case 0:
						talkNPC(8503);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 70:
				switch(tutorialSectionProgress){
					case 0:
					case 1:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 2:
						interactObject(9730,3);
						break;
				}
				break;
			case 80:
				client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
				client.setSelectedItemSlot(utils.getInventoryWidgetItem(2511).getIndex());
				client.setSelectedItemID(2511);
				targetMenu = new MenuEntry("","",590,31,utils.getInventoryWidgetItem(590).getIndex(),9764864,false);
				utils.delayMouseClick(utils.getInventoryWidgetItem(590).getCanvasBounds(), sleepDelay());
				break;
			case 90:
				client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
				client.setSelectedItemSlot(utils.getInventoryWidgetItem(2514).getIndex());
				client.setSelectedItemID(2514);
				interactObject(26185,1);
				break;
			case 120:
				openDoor(9470);
				break;
			case 130:
				openDoor(9709);
				break;
			case 140:
				switch(tutorialSectionProgress){
					case 0:
						talkNPC(3305);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 150:
				client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
				client.setSelectedItemSlot(utils.getInventoryWidgetItem(2516).getIndex());
				client.setSelectedItemID(2516);
				targetMenu = new MenuEntry("","",1929,31,utils.getInventoryWidgetItem(1929).getIndex(),9764864,false);
				utils.delayMouseClick(utils.getInventoryWidgetItem(2516).getCanvasBounds(), sleepDelay());
				break;
			case 160:
				client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
				client.setSelectedItemSlot(utils.getInventoryWidgetItem(2307).getIndex());
				client.setSelectedItemID(2307);
				interactObject(9736,1);
				break;
			case 170:
				openDoor(9710);
				break;
			case 200:
				openDoor(9716);
				break;
			case 220:
				talkNPC(3312);
				break;
			case 230:
				switch(tutorialSectionProgress){
					case 0:
						utils.pressKey(KeyEvent.VK_SPACE);
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747959);
						break;
				}
				break;
			case 240:
				switch(tutorialSectionProgress){
					case 0:
						talkNPC(3312);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 250:
				switch(tutorialSectionProgress){
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						interactObject(9726,3);
						break;
				}
				break;
			case 260:
				switch(tutorialSectionProgress){
					case 0:
						utils.walk(new WorldPoint(3081,9506,0),0,sleepDelay());
						tutorialSectionProgress++;
						break;
					case 1:
						utils.setMenuEntry(null);
						targetMenu=null;
						talkNPC(3311);
						tutorialSectionProgress++;
						break;
					case 2:
						pressSpace();
						break;
				}
				break;
			case 270:
				pressSpace();
				break;
			case 300:
				interactObject(10080,3);
				break;
			case 310:
				interactObject(10079,3);
				break;
			case 320:
				client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
				client.setSelectedItemSlot(utils.getInventoryWidgetItem(438).getIndex());
				client.setSelectedItemID(438);
				interactObject(10082,1);
				break;
			case 330:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3311);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 340:
				interactObject(2097,3);
				break;
			case 350:
				smithDagger();
				break;
			case 360:
				openDoor(9718);
				break;
			case 370:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3307);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 390:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747961);
						break;
				}
				break;
			case 400:
				openTab(25362433);
				break;
			case 405:
				switch(tutorialSectionProgress) {
					case 0:
						closeInterface(5505027);
						tutorialSectionProgress++;
						break;
					case 1:
						equipItem(1205);
						break;
				}
				break;
			case 410:
				talkNPC(3307);
				break;
			case 420:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						equipItem(1277);
						tutorialSectionProgress++;
						break;
					case 2:
						equipItem(1171);
						break;
				}
				break;
			case 430:
				openTab(10747957);
				break;
			case 440:
				openDoor(9719);
				break;
			case 450:
				attackRat();
				break;
			case 470:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openDoor(9719);
						tutorialSectionProgress++;
						break;
					case 2:
						talkNPC(3307);
						tutorialSectionProgress++;
						break;
					case 3:
						pressSpace();
						break;
				}
				break;
			case 480:
				switch(tutorialSectionProgress) {
					case 0:
						equipItem(841);
						tutorialSectionProgress++;
						break;
					case 1:
						equipItem(882);
						tutorialSectionProgress++;
						break;
					case 2:
						attackRat();
						break;
				}
				break;
			case 500:
				interactObject(9727,3);
				break;
			case 510:
				interactObject(10083,3);
				break;
			case 520:
				switch(tutorialSectionProgress) {
					case 0:
						closeInterface(786434);
						tutorialSectionProgress++;
						break;
					case 1:
						interactObject(26815,3);
						tutorialSectionProgress++;
						break;
					case 2:
						pressSpace();
						break;
				}
				break;
			case 525:
				switch(tutorialSectionProgress) {
					case 0:
						closeInterface(22609922);
						tutorialSectionProgress++;
						break;
					case 1:
						openDoor(9721);
						break;
				}
				break;
			case 530:
			case 532:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3310);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 531:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747943);
						break;
				}
				break;
			case 540:
				openDoor(9722);
				break;
			case 550:
				switch(tutorialSectionProgress) {
					case 0:
						utils.walk(new WorldPoint(3124,3106,0),0,sleepDelay());
						tutorialSectionProgress++;
						break;
					case 1:
						utils.setMenuEntry(null);
						targetMenu=null;
						talkNPC(3319);
						tutorialSectionProgress++;
						break;
					case 2:
						pressSpace();
						break;
				}
				break;
			case 560:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747962);
						break;
				}
				break;
			case 570:
			case 600:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3319);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 580:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openTab(10747944);
						break;
				}
				break;
			case 610:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						openDoor(9723);
						break;
				}
				break;
			case 620:
				if(config.type().equals(ElTutorialType.REGULAR)){
					switch(tutorialSectionProgress) {
						case 0:
							utils.walk(new WorldPoint(3141,3090,0),0,sleepDelay());
							tutorialSectionProgress++;
							break;
						case 1:
							utils.setMenuEntry(null);
							targetMenu=null;
							talkNPC(3309);
							tutorialSectionProgress++;
							break;
						case 2:
							pressSpace();
							break;
					}
				} else {
					switch(tutorialSectionProgress) {
						case 0:
							utils.walk(new WorldPoint(3131,3087,0),0,sleepDelay());
							tutorialSectionProgress++;
							break;
						case 1:
							utils.setMenuEntry(null);
							targetMenu=null;
							talkNPC(7941);
							tutorialSectionProgress++;
							ironmanProgress=0;
							break;
						case 2:
							makeIronman();
							break;
						case 3:
						case 4:
							talkNPC(3309);
							tutorialSectionProgress++;
							break;
						case 5:
							pressSpace();
							break;
					}
				}
				break;
			case 630:
				openTab(10747963);
				break;
			case 640:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3309);
						tutorialSectionProgress++;
						break;
					case 1:
						pressSpace();
						break;
				}
				break;
			case 650:
				switch(tutorialSectionProgress) {
					case 0:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 1:
						castSpell();
						break;
				}
				break;
			case 670:
				switch(tutorialSectionProgress) {
					case 0:
						talkNPC(3309);
						tutorialSectionProgress++;
						break;
					case 1:
					case 3:
						pressSpace();
						tutorialSectionProgress++;
						break;
					case 2:
						pressOption(1);
						tutorialSectionProgress++;
						break;
					case 4:
						if(client.getWidget(219,1)!=null && client.getWidget(219,1).getChild(1).getText().contains("Iron")){
							pressOption(3);
							break;
						}
						pressSpace();
						break;
				}
				break;
			case 1000:
				utils.logout();
				startTutorial=false;
				break;
			default:
				return UNKNOWN;
		}
		return WORKING;
	}

	@Subscribe
	private void onVarbitChanged(VarbitChanged event)
	{
		if(event.getIndex()==281){
			tutorialSectionProgress=0;
		}
	}

	private void makeIronman()
	{
		log.info(String.valueOf(ironmanProgress));
		switch(ironmanProgress){
			default:
				pressSpace();
				ironmanProgress++;
				break;
			case 1:
			case 13:
			case 16:
				pressOption(1);
				ironmanProgress++;
				break;
			case 15:
				switch(config.type()){
					case IRONMAN:
						selectIron();
						ironmanProgress++;
						break;
					case HARDCORE_IRONMAN:
						selectHardcore();
						ironmanProgress++;
						break;
				}
				break;
			case 17:
			case 21:
				pressKey(config.bankPin().charAt(0));
				ironmanProgress++;
				break;
			case 18:
			case 22:
				pressKey(config.bankPin().charAt(1));
				ironmanProgress++;
				break;
			case 19:
			case 23:
				pressKey(config.bankPin().charAt(2));
				ironmanProgress++;
				break;
			case 20:
			case 24:
				pressKey(config.bankPin().charAt(3));
				ironmanProgress++;
				break;
			case 25:
				pressSpace();
				tutorialSectionProgress++;
				break;
		}
	}

	private void changeLook(int id)
	{
		targetMenu = new MenuEntry("","",1,57,-1,id,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void openDoor(int id)
	{
		targetWall = utils.findNearestWallObject(id);
		if(targetWall!=null){
			targetMenu = new MenuEntry("","",targetWall.getId(),3,targetWall.getLocalLocation().getSceneX(),targetWall.getLocalLocation().getSceneY(),false);
			utils.delayMouseClick(targetWall.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void talkNPC(int id)
	{
		targetNPC = utils.findNearestNpc(id);
		if(targetNPC!=null){
			targetMenu = new MenuEntry("","",targetNPC.getIndex(),9,0,0,false);
			if(targetNPC.getConvexHull()!=null){
				utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
			} else {
				utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
			}
		}
	}

	private void openTab(int param1)
	{
		targetMenu = new MenuEntry("","",1,57,-1,param1,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void pressSpace()
	{
		utils.pressKey(KeyEvent.VK_SPACE);
	}

	private void interactObject(int id, int opcode)
	{
		targetObject = utils.findNearestGameObject(id);
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),opcode,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void smithDagger()
	{
		targetMenu = new MenuEntry("","",1,57,-1,20447241,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void closeInterface(int id)
	{
		targetMenu = new MenuEntry("","",1,57,11,id,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void equipItem(int id)
	{
		targetMenu = new MenuEntry("","",id,34,utils.getInventoryWidgetItem(id).getIndex(),9764864,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void attackRat()
	{
		targetNPC = utils.findNearestNpc(3313);
		if(targetNPC!=null){
			targetMenu = new MenuEntry("","",targetNPC.getIndex(),10,0,0,false);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private void castSpell()
	{
		targetNPC = utils.findNearestNpc(3316);
		if(targetNPC!=null){
			client.setSelectedSpellWidget(utils.getSpellWidget("Wind Strike").getId());
			client.setSelectedSpellChildIndex(-1);
			targetMenu = new MenuEntry("","",targetNPC.getIndex(),8,0,0,false);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}

	}

	private void pressOption(int option)
	{
		targetMenu = new MenuEntry("","",0,30,option,14352385,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());

	}

	private void selectName()
	{
		targetMenu = new MenuEntry("","",1,57,-1,36569102,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void setName()
	{
		targetMenu = new MenuEntry("","",1,57,-1,36569106,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void selectIron()
	{
		targetMenu = new MenuEntry("","",1,57,-1,14090251,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void selectHardcore()
	{
		targetMenu = new MenuEntry("","",1,57,-1,14090252,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}

	private void answerExperienceQuestions()
	{
		for(Widget option : client.getWidget(219,1).getChildren()){
			if(option.getText().contains("experienced")){
				pressOption(option.getIndex());
			}
		}
	}

	public void pressKey(char key)
	{
		keyEvent(401, key);
		keyEvent(402, key);
		keyEvent(400, key);
	}

	private void keyEvent(int id, char key)
	{
		KeyEvent e = new KeyEvent(
				client.getCanvas(), id, System.currentTimeMillis(),
				0, KeyEvent.VK_UNDEFINED, key
		);

		client.getCanvas().dispatchEvent(e);
	}
}
