package net.runelite.client.plugins.ElPlanks;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
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
import java.util.*;
import java.util.List;
import static net.runelite.client.plugins.ElPlanks.ElPlanksState.*;

@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
		name = "El Planks",
		description = "Makes planks.",
		type = PluginType.SKILLING
)
@Slf4j
public class ElPlanksPlugin extends Plugin
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
	private ElPlanksConfig config;

	@Inject
	private ElPlanksOverlay overlay;



	//plugin data
	GameObject targetObject;
	MenuEntry targetMenu;
	NPC targetNPC;
	int tickTimer;
	boolean startPlanks;
	ElPlanksState status;
	int runecraftProgress = 0;
	Point clickPoint;
	//overlay data
	Instant botTimer;
	int logId;
	List<Integer> REQUIRED_ITEMS = new ArrayList<>();

	WorldArea PVP_BANK = new WorldArea(new WorldPoint(2755,3476,0),new WorldPoint(2760,3482,0));

	// Provides our config
	@Provides
	ElPlanksConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElPlanksConfig.class);
	}

	@Override
	protected void startUp()
	{
		botTimer = Instant.now();
		setValues();
		startPlanks=false;
		log.info("Plugin started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		setValues();
		startPlanks=false;
		log.info("Plugin stopped");
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElPlanks"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startPlanks)
			{
				startPlanks = true;
				targetMenu = null;
				botTimer = Instant.now();
				overlayManager.add(overlay);
				switch(config.type()){
					case OAK:
						logId=1521;
						break;
					case TEAK:
						logId=6333;
						break;
					case MAHOGANY:
						logId=6332;
						break;
				}
			} else {
				shutDown();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElPlanks"))
		{
			return;
		}
		startPlanks = false;
	}

	private void setValues()
	{
		runecraftProgress = 0;
		if(config.dustStaff()){
			REQUIRED_ITEMS = List.of(995,563);
		} else {
			REQUIRED_ITEMS = List.of(995,12791);
		}

	}

	@Subscribe
	private void onGameTick(GameTick gameTick)
	{
		if (!startPlanks)
		{
			return;
		}
		if (!client.isResized())
		{
			utils.sendGameMessage("client must be set to resizable");
			startPlanks = false;
			return;
		}
		status = checkPlayerStatus();
		switch (status) {
			case ANIMATING:
			case NULL_PLAYER:
			case TICK_TIMER:
				break;
			case MOVING:
				shouldRun();
				break;
			case OPENING_BANK:
				openBank();
				break;
			case MISSING_REQUIRED:
				utils.sendGameMessage("MISSING LAW RUNES OR CASH.");
				break;
			case DEPOSIT_INVENT:
				utils.depositAllExcept(REQUIRED_ITEMS);
				runecraftProgress=0;
				break;
			case WITHDRAW_LOGS:
				utils.withdrawAllItem(logId);
				break;
			case HOUSE_TELE:
				houseTeleport();
				break;
			case CLOSE_BANK:
				utils.closeBank();
				break;
			case TELE_CAMMY:
				teleCammy();
				break;

		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event)
	{
		log.debug(event.toString());
		if(targetMenu!=null){
			event.consume();
			client.invokeMenuAction(targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getOpcode(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}

	}

	private long sleepDelay()
	{
		return utils.randomDelay(false, config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
	}

	private int tickDelay()
	{
		return (int) utils.randomDelay(false,config.tickMin(), config.tickMax(), config.tickDeviation(), config.tickTarget());
	}

	private ElPlanksState checkPlayerStatus()
	{
		Player player = client.getLocalPlayer();
		if(player==null){
			return NULL_PLAYER;
		}
		if(player.getPoseAnimation()!=813){
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
		if(player.getWorldArea().intersectsWith(PVP_BANK)){
			if(!utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
				if(!utils.isBankOpen()){
					return OPENING_BANK;
				} else {
					return MISSING_REQUIRED;
				}
			}
			if(utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
				if(!utils.isBankOpen()){
					if(utils.inventoryContains(logId)){
						return HOUSE_TELE;
					} else {
						return OPENING_BANK;
					}
				} else {
					if(utils.inventoryContains(logId)){
						return CLOSE_BANK;
					} else {
						return WITHDRAW_LOGS;
					}
				}
			}
		}
		if(!utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
			return TELE_CAMMY;
		}
		if(utils.inventoryContainsAllOf(REQUIRED_ITEMS)){
			if(utils.inventoryContains(logId)){
				if(distanceToButler()>3){
					log.info("distance to butler >3");
					if(client.getVarcIntValue(171)!=11){
						openSettingsTab();
						return WORKING;
					} else {
						if(client.getWidget(370,19)!=null && !client.getWidget(370,19).isHidden()){
							callServant();
							return WORKING;
						} else {
							openHouseOptions();
							return WORKING;
						}
					}
				} else {
					log.info("distance to butler <=3");
					if(client.getWidget(219,1)!=null && !client.getWidget(219,1).isHidden()){
						if(client.getWidget(219,1).getChild(1).getText().contains("back to the")){
							return TELE_CAMMY;
						} else if(client.getWidget(219,1).getChild(1).getText().contains("erve")){
							useLogsOnButler();
							return WORKING;
						} else {
							pressOption(1);
							return WORKING;
						}
					} else if(client.getWidget(231,0)!=null && !client.getWidget(231,0).isHidden()) {
						pressSpace();
						return WORKING;
					} else if(client.getWidget(162,45)!=null && !client.getWidget(162,45).isHidden()) {
						client.setVar(VarClientInt.INPUT_TYPE, 7);
						client.setVar(VarClientStr.INPUT_TEXT, String.valueOf(99));
						client.runScript(681);
						client.runScript(ScriptID.MESSAGE_LAYER_CLOSE);
						return WORKING;
					}
					else {
						log.info("trying to use logs");
						talkButler();
						return WORKING;
					}
				}
			} else {
				return TELE_CAMMY;
			}
		}
		return UNKNOWN;
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
				log.info(String.valueOf(client.getVarbitValue(173)));
				if(client.getWidget(160,27).getSpriteId()==1069){ //if run is off
					targetMenu = new MenuEntry("Toggle Run","",1,57,-1,10485782,false);
					utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
				}
			}
		}
	}

	private void openBank()
	{
		targetObject = utils.findNearestGameObject(10777);
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),3,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			if (targetObject.getConvexHull() != null) {
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			} else {
				utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
			}
		}
	}

	private void houseTeleport()
	{
		targetMenu = new MenuEntry("","",1,57,-1,14286876,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void teleCammy()
	{
		targetMenu = new MenuEntry("","",1,57,-1,14286879,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void openSettingsTab()
	{
		targetMenu = new MenuEntry("","",1,57,-1,10551338,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void openHouseOptions()
	{
		targetMenu = new MenuEntry("","",1,57,-1,17104996,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private void callServant()
	{
		targetMenu = new MenuEntry("","",1,57,-1,24248339,false);
		utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
	}

	private int distanceToButler()
	{
		targetNPC = utils.findNearestNpc(229);
		if(targetNPC!=null){
			return client.getLocalPlayer().getWorldLocation().distanceTo2D(targetNPC.getWorldLocation());
		}
		return 100;
	}

	private void useLogsOnButler()
	{

		targetNPC = utils.findNearestNpc(229);
		if(targetNPC!=null){
			client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
			client.setSelectedItemSlot(utils.getInventoryWidgetItem(logId).getIndex());
			client.setSelectedItemID(logId);
			targetMenu = new MenuEntry("","",targetNPC.getIndex(),7,0,0,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}
	}

	private void talkButler()
	{

		targetNPC = utils.findNearestNpc(229);
		if(targetNPC!=null){
			targetMenu = new MenuEntry("","",targetNPC.getIndex(),9,0,0,false);
			utils.delayMouseClick(getRandomNullPoint(),sleepDelay());
		}
	}


	private void pressSpace()
	{
		utils.pressKey(KeyEvent.VK_SPACE);
	}

	private void pressOption(int option)
	{
		targetMenu = new MenuEntry("","",0,30,option,14352385,false);
		utils.delayMouseClick(getRandomNullPoint(), sleepDelay());
	}
}
