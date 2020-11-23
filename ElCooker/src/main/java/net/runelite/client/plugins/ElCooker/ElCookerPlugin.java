/*
 * Copyright (c) 2018, SomeoneWithAnInternetConnection
 * Copyright (c) 2018, oplosthee <https://github.com/oplosthee>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.ElCooker;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.awt.Rectangle;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginType;
import net.runelite.client.plugins.botutils.BotUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;
import static net.runelite.client.plugins.ElCooker.ElCookerState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Cooker",
	enabledByDefault = false,
	description = "Cooks food.",
	tags = {"cook, food, cooking, el"},
	type = PluginType.SKILLING
)
@Slf4j
public class ElCookerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElCookerConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElCookerOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;


	ElCookerState state;
	GameObject targetObject;
	NPC targetNpc;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	boolean firstTime;
	int opcode;
	Rectangle clickBounds;
	Rectangle altRect = new Rectangle(-100,-100, 10, 10);

	WorldPoint HOSIDIUS_BANK = new WorldPoint(1676,3615,0);
	WorldPoint HOSIDIUS_RANGE = new WorldPoint(1677,3621,0);

	WorldArea HOSIDIUS_HOUSE = new WorldArea(new WorldPoint(1673,3613,0),new WorldPoint(1685,3624,0));

	int timeout = 0;
	long sleepLength;
	boolean startCooker;
	private final Set<Integer> itemIds = new HashSet<>();

	int startRaw;
	int currentRaw;


	@Provides
	ElCookerConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElCookerConfiguration.class);
	}

	@Override
	protected void startUp()
	{
		chinBreakHandler.registerPlugin(this);
	}

	@Override
	protected void shutDown()
	{
		resetVals();
		chinBreakHandler.unregisterPlugin(this);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		chinBreakHandler.stopPlugin(this);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		startCooker = false;
		startRaw=0;
		currentRaw=0;
		firstTime=true;
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElCooker"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startCooker)
			{
				startCooker = true;
				chinBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
				firstTime=true;
			}
			else
			{
				resetVals();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElCooker"))
		{
			return;
		}
		startCooker = false;
	}

	public void setLocation()
	{
		if (client != null && client.getLocalPlayer() != null && client.getGameState().equals(GameState.LOGGED_IN))
		{
			skillLocation = client.getLocalPlayer().getWorldLocation();
			beforeLoc = client.getLocalPlayer().getLocalLocation();
		}
		else
		{
			log.debug("Tried to start bot before being logged in");
			skillLocation = null;
			resetVals();
		}
	}

	private long sleepDelay()
	{
		sleepLength = utils.randomDelay(config.sleepWeightedDistribution(), config.sleepMin(), config.sleepMax(), config.sleepDeviation(), config.sleepTarget());
		return sleepLength;
	}

	private int tickDelay()
	{
		int tickLength = (int) utils.randomDelay(config.tickDelayWeightedDistribution(), config.tickDelayMin(), config.tickDelayMax(), config.tickDelayDeviation(), config.tickDelayTarget());
		log.debug("tick delay for {} ticks", tickLength);
		return tickLength;
	}

	private void interactCooker()
	{
		targetObject = utils.findNearestGameObjectWithin(client.getLocalPlayer().getWorldLocation(),25,config.rangeObjectId());
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),1,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			if(config.seaweedMode()){
				utils.setModifiedMenuEntry(targetMenu,21504,utils.getInventoryWidgetItem(21504).getIndex(),1);
			} else {
				utils.setModifiedMenuEntry(targetMenu,config.rawFoodId(),utils.getInventoryWidgetItem(config.rawFoodId()).getIndex(),1);
			}
			if(targetObject.getConvexHull()!=null) {
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			} else {
				utils.delayMouseClick(new Point(0,0),sleepDelay());
			}
		} else {
			utils.sendGameMessage("cooker is null.");
		}
	}

	private void interactFire()
	{
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(),25,26185);
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),1,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			if(config.seaweedMode()){
				utils.setModifiedMenuEntry(targetMenu,21504,utils.getInventoryWidgetItem(21504).getIndex(),1);
			} else {
				utils.setModifiedMenuEntry(targetMenu,config.rawFoodId(),utils.getInventoryWidgetItem(config.rawFoodId()).getIndex(),1);
			}
			if(targetObject.getConvexHull()!=null) {
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			} else {
				utils.delayMouseClick(new Point(0,0),sleepDelay());
			}
		}
	}

	private void openBank()
	{
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(),25,config.bankObjectId());
		if (targetObject != null)
		{
			targetMenu = new MenuEntry("", "", targetObject.getId(), config.bankOpCode(),
					targetObject.getSceneMinLocation().getX(), targetObject.getSceneMinLocation().getY(), false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Cooker is null.");
		}
	}

	private void openBankRogues()
	{
		targetNpc = utils.findNearestNpcWithin(player.getWorldLocation(),5, Collections.singleton(3194));
		if(targetNpc!=null){
			targetMenu = new MenuEntry("Bank", "<col=ffff00>Emerald Benedict", targetNpc.getIndex(), 11,
					0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetNpc.getConvexHull().getBounds(), sleepDelay());
		}
	}

	private ElCookerState getBankState()
	{
		if (startRaw == 0) {
			if(config.seaweedMode()){
				startRaw=utils.getBankItemWidget(21504).getItemQuantity();
			} else {
				startRaw=utils.getBankItemWidget(config.rawFoodId()).getItemQuantity();
			}

		}
		if(config.seaweedMode()){
			currentRaw = utils.getBankItemWidget(21504).getItemQuantity();
		} else {
			currentRaw = utils.getBankItemWidget(config.rawFoodId()).getItemQuantity();
		}

		if(utils.inventoryEmpty()){
			return WITHDRAW_ITEMS;
		}
		if(!config.seaweedMode()){
			if(!utils.inventoryFull() && !utils.inventoryEmpty()){
				return DEPOSIT_ITEMS;
			}
		}
		if(utils.inventoryContains(config.rawFoodId())){ //inventory contains raw food
			if(utils.getInventoryItemCount(config.rawFoodId(),false)==28){ //contains 28 raw food
				return FIND_OBJECT;
			}
		}
		if(config.seaweedMode()){
			if(!utils.inventoryContains(21504)){ //inventory doesnt contain raw food
				return DEPOSIT_ITEMS;
			}
			if(!utils.bankContains(21504,4)){
				return MISSING_ITEMS;
			}
			if(utils.inventoryContains(21504)){ //inventory contains raw food
				if(utils.getInventoryItemCount(21504,false)==4){ //contains 28 raw food
					return FIND_OBJECT;
				}
			}
		} else {
			if(!utils.inventoryContains(config.rawFoodId())){ //inventory doesnt contain raw food
				return DEPOSIT_ITEMS;
			}
			if(!utils.bankContains(config.rawFoodId(),28)){
				return MISSING_ITEMS;
			}
		}
		return UNHANDLED_STATE;
	}

	public ElCookerState getState()
	{
		if(client.getLocalPlayer().getAnimation()==897) {
			if(utils.inventoryContains(config.rawFoodId())){
				timeout=3;
			} else {
				timeout=0;
			}
		}
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		if (utils.isMoving(beforeLoc))
		{
			timeout = tickDelay();
			return MOVING;
		}
		if (chinBreakHandler.shouldBreak(this))
		{
			return HANDLE_BREAK;
		}
		if(client.getLocalPlayer().getAnimation()!=-1){
			return ANIMATING;
		}
		if(utils.isBankOpen()){ //if bank is open
			return getBankState(); //check bank state
		}
		return getElCookerState();
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startCooker || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startCooker = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			switch (state)
			{
				case TIMEOUT:
					utils.handleRun(30, 20);
					timeout--;
					break;
				case FIND_OBJECT:
					if(config.roguesDen()){
						interactFire();
					} else {
						interactCooker();
					}
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startCooker = false;
					utils.sendGameMessage("OUT OF FOOD");
					resetVals();
					break;
				case HANDLE_BREAK:
					firstTime=true;
					chinBreakHandler.startBreak(this);
					timeout = 10;
					break;
				case ANIMATING:
				case MOVING:
					firstTime=true;
					utils.handleRun(30, 20);
					timeout = 1+tickDelay();
					break;
				case FIND_BANK:
					if(config.roguesDen()){
						openBankRogues();
					} else {
						openBank();
					}
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					utils.depositAll();
					timeout = tickDelay();
					break;
				case WITHDRAW_ITEMS:
					if(config.seaweedMode()){
						withdrawX(21504);
						timeout = 2+tickDelay();
						break;
					}
					utils.withdrawAllItem(config.rawFoodId());
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startCooker)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private ElCookerState getElCookerState()
	{
		log.info("getting cooker state");
		if(utils.inventoryContains(config.rawFoodId()))
		{
			if(client.getWidget(270,15)!=null){
				if(client.getWidget(270,15).getName().equals("<col=ff9040>Cooked karambwan</col>")){
					timeout=3;
					targetMenu=new MenuEntry("","",1,57,-1,17694735,false);
					utils.setMenuEntry(targetMenu);
					if(client.getWidget(270,15).getBounds()!=null){
						utils.delayMouseClick(client.getWidget(270,15).getBounds(), sleepDelay());
					} else {
						utils.delayMouseClick(new Point(0,0), sleepDelay());
					}
				}
			}
			if(client.getWidget(270,5)!=null){
				if(client.getWidget(270,5).getText().equals("How many would you like to cook?")){
					timeout=3;
					targetMenu=new MenuEntry("","",1,57,-1,17694734,false);
					utils.setMenuEntry(targetMenu);
					if(client.getWidget(270,5).getBounds()!=null){
						utils.delayMouseClick(client.getWidget(270,5).getBounds(), sleepDelay());
					} else {
						utils.delayMouseClick(new Point(0,0), sleepDelay());
					}
				}
			} else {
				return FIND_OBJECT;
			}
		} else if(utils.inventoryContains(21504)) {
			if(client.getWidget(270,5)!=null){
				if(client.getWidget(270,5).getText().equals("How many would you like to cook?")){
					timeout=3;
					targetMenu=new MenuEntry("","",1,57,-1,17694734,false);
					utils.setMenuEntry(targetMenu);
					if(client.getWidget(270,5).getBounds()!=null){
						utils.delayMouseClick(client.getWidget(270,5).getBounds(), sleepDelay());
					} else {
						utils.delayMouseClick(new Point(0,0), sleepDelay());
					}
				}
			} else {
				return FIND_OBJECT;
			}
		} else {
			return FIND_BANK;
		}
		return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.debug(event.toString());
		if(config.valueFinder()){
			utils.sendGameMessage("Id: " + event.getIdentifier() + ", Op Code: " + event.getOpcode() + ".");
		}
	}

	private void withdrawX(int ID){
		if(client.getVarbitValue(3960)!=4){
			utils.withdrawItemAmount(ID,4);
			timeout+=3;
		} else {
			targetMenu = new MenuEntry("", "", (client.getVarbitValue(6590) == 3) ? 1 : 5, MenuOpcode.CC_OP.getId(), utils.getBankItemWidget(ID).getIndex(), 786444, false);
			utils.setMenuEntry(targetMenu);
			clickBounds = utils.getBankItemWidget(ID).getBounds()!=null ? utils.getBankItemWidget(ID).getBounds() : new Rectangle(client.getCenterX() - 50, client.getCenterY() - 50, 100, 100);
			utils.delayMouseClick(clickBounds,sleepDelay());
		}
	}
}
