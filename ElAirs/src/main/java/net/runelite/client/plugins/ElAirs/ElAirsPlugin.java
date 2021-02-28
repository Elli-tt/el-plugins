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
package net.runelite.client.plugins.elairs;

import com.google.inject.Provides;
import net.runelite.client.plugins.elbreakhandler.ElBreakHandler;
import java.awt.Rectangle;
import java.time.Instant;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.elutils.ElUtils;
import net.runelite.client.ui.overlay.OverlayManager;
import org.pf4j.Extension;

import static net.runelite.client.plugins.elairs.ElAirsState.*;
import static net.runelite.client.plugins.elairs.ElAirsType.*;


@Extension
@PluginDependency(ElUtils.class)
@PluginDescriptor(
	name = "El Airs",
	enabledByDefault = false,
	description = "Crafts at the air altar.",
	tags = {"rune, craft, runecraft, air, el"}
)
@Slf4j
public class ElAirsPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElAirsConfiguration config;

	@Inject
	private ElUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElAirsOverlay overlay;

	@Inject
	private ElBreakHandler elBreakHandler;


	ElAirsState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	Rectangle altRect = new Rectangle(-100,-100, 10, 10);
	Rectangle clickBounds;

	WorldArea FALADOR_EAST_BANK = new WorldArea(new WorldPoint(3009,3353,0),new WorldPoint(3019,3359,0));
	WorldArea FIRST_POINT = new WorldArea(new WorldPoint(3004,3314,0),new WorldPoint(3009,3319,0));
	WorldArea SECOND_POINT = new WorldArea(new WorldPoint(3003,3325,0),new WorldPoint(3010,3333,0));
	WorldArea AIR_ALTAR = new WorldArea(new WorldPoint(2839,4826,0),new WorldPoint(2849,4840,0));

	WorldPoint FIRST_CLICK = new WorldPoint(3006,3315,0);
	WorldPoint SECOND_CLICK = new WorldPoint(3006,3330,0);
	WorldPoint OUTSIDE_ALTAR = new WorldPoint(2983,3288,0);

	int timeout = 0;
	long sleepLength;
	boolean startTeaks;
	int essenceValue;


	@Provides
	ElAirsConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElAirsConfiguration.class);
	}

	@Override
	protected void startUp()
	{
		resetVals();
		elBreakHandler.registerPlugin(this);
	}

	@Override
	protected void shutDown()
	{
		resetVals();
		elBreakHandler.unregisterPlugin(this);
	}

	private void resetVals()
	{
		overlayManager.remove(overlay);
		elBreakHandler.stopPlugin(this);
		state = null;
		timeout = 0;
		botTimer = null;
		skillLocation = null;
		if(config.useRuneEssence()){
			essenceValue = 1436;
		} else {
			essenceValue = 7936;
		}
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElAirs"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startTeaks)
			{
				startTeaks = true;
				elBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
			}
			else
			{
				startTeaks=false;
				resetVals();
			}
		}
	}

	@Subscribe
	private void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("ElAirs"))
		{
			return;
		}
		startTeaks = false;
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

	private ElAirsState getBankState()
	{
		if(utils.inventoryFull()){
			if(config.mode().equals(TIARAS)){
				if(utils.inventoryItemContainsAmount(5525,14,false,true)
						&& utils.inventoryItemContainsAmount(1438,14,false,true)){
					return WALK_FIRST_POINT;
				} else {
					utils.depositAll();
					return UNHANDLED_STATE;
				}
			}
			return WALK_FIRST_POINT;
		}
		if(utils.inventoryContains(556) || utils.inventoryContains(5527)){
			return DEPOSIT_ITEMS;
		} else {
			return WITHDRAW_ITEMS;
		}
	}

	public ElAirsState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		else if (utils.isMoving(beforeLoc))
		{
			timeout = 2 + tickDelay();
			return MOVING;
		}
		else if (elBreakHandler.shouldBreak(this) && player.getWorldArea().intersectsWith(FALADOR_EAST_BANK))
		{
			return HANDLE_BREAK;
		}
		else if(utils.isBankOpen()){
			return getBankState();
		}
		else if(client.getLocalPlayer().getAnimation()!=-1){
			return ANIMATING;
		}
		else {
			return getAirsState();
		}
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startTeaks || elBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startTeaks = false;
				return;
			}
			state = getState();
			beforeLoc = player.getLocalLocation();
			utils.setMenuEntry(null);
			switch (state)
			{
				case TIMEOUT:
					utils.handleRun(30, 20);
					timeout--;
					break;
				case HANDLE_BREAK:
					elBreakHandler.startBreak(this);
					timeout = 10;
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case FIND_BANK:
					useGameObject(24101,4);
					timeout = tickDelay();
					break;
				case DEPOSIT_ITEMS:
					if(config.mode().equals(RUNES)){
						depositItem(556);
					} else if(config.mode().equals(TIARAS)){
						depositItem(5527);
					}
					timeout = tickDelay();
					break;
				case WITHDRAW_ITEMS:
					if(config.useStams()) {
						if (client.getVar(Varbits.RUN_SLOWED_DEPLETION_ACTIVE) == 0 && checkRunEnergy() < config.minEnergy()) {
							if (utils.inventoryContains(12631)) {
								targetMenu = new MenuEntry("Drink", "<col=ff9040>Stamina potion(1)</col>", 9, 1007, utils.getInventoryWidgetItem(12631).getIndex(), 983043, false);
								utils.delayMouseClick(utils.getInventoryWidgetItem(12631).getCanvasBounds(), sleepDelay());
								return;
							} else {
								if (utils.inventoryFull()) {
									utils.depositAll();
									return;
								} else {
									targetMenu = new MenuEntry("Withdraw-1", "<col=ff9040>Stamina potion(1)</col>", 1, 57, utils.getBankItemWidget(12631).getIndex(), 786444, false);
									utils.delayMouseClick(utils.getBankItemWidget(12631).getBounds(), sleepDelay());
									return;
								}
							}
						} else if (checkRunEnergy() < config.minEnergyStam()) {
							if (utils.inventoryContains(12631)) {
								targetMenu = new MenuEntry("Drink", "<col=ff9040>Stamina potion(1)</col>", 9, 1007, utils.getInventoryWidgetItem(12631).getIndex(), 983043, false);
								utils.delayMouseClick(utils.getInventoryWidgetItem(12631).getCanvasBounds(), sleepDelay());
								return;
							} else {
								if (utils.inventoryFull()) {
									utils.depositAll();
									return;
								} else {
									targetMenu = new MenuEntry("Withdraw-1", "<col=ff9040>Stamina potion(1)</col>", 1, 57, utils.getBankItemWidget(12631).getIndex(), 786444, false);
									utils.delayMouseClick(utils.getBankItemWidget(12631).getBounds(), sleepDelay());
									return;
								}
							}
						}
					}
					if(utils.inventoryContains(229)){
						utils.depositAll();
						return;
					}
					if(config.mode().equals(RUNES)){
						utils.withdrawAllItem(essenceValue);
					} else if(config.mode().equals(TIARAS)){
						if(!utils.inventoryItemContainsAmount(5525,14,false,true)){
							withdrawX(5525);
							return;
						} else if(!utils.inventoryItemContainsAmount(1438,14,false,true)){
							withdrawX(1438);
							return;
						}
					}
					timeout = tickDelay();
					break;
				case ENTER_ALTAR:
					if(utils.inventoryContains(1438)){
						useTalismanOnAltar();
					} else {
						useGameObject(34813,3);
					}
					timeout = tickDelay();
					break;
				case CRAFT_RUNES:
					useGameObject(34760,3);
					timeout = tickDelay();
					break;
				case USE_PORTAL:
					useGameObject(34748,3);
					timeout = tickDelay();
					break;
				case WALK_SECOND_POINT:
					utils.walk(SECOND_CLICK,2,sleepDelay());
					timeout = tickDelay();
					break;
				case WALK_FIRST_POINT:
					utils.walk(FIRST_CLICK,1,sleepDelay());
					timeout = tickDelay();
					break;
				case CRAFT_TIARAS:
					client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
					client.setSelectedItemSlot(utils.getInventoryWidgetItem(1438).getIndex());
					client.setSelectedItemID(1438);
					useGameObject(34760,1);
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startTeaks)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private ElAirsState getAirsState()
	{
		utils.setMenuEntry(null);
		if(config.mode().equals(RUNES)){
			if(utils.inventoryContains(essenceValue)){
				if(player.getWorldArea().intersectsWith(FIRST_POINT)){
					return ENTER_ALTAR;
				} else if (player.getWorldArea().intersectsWith(AIR_ALTAR)){
					return CRAFT_RUNES;
				}
			}
			else {
				if (player.getWorldArea().intersectsWith(AIR_ALTAR)){
					return USE_PORTAL;
				} else if (player.getWorldLocation().equals(OUTSIDE_ALTAR)){
					return WALK_SECOND_POINT;
				} else if (player.getWorldArea().intersectsWith(SECOND_POINT)){
					return FIND_BANK;
				} else if (player.getWorldArea().intersectsWith(FALADOR_EAST_BANK)){
					return FIND_BANK;
				}
			}
			return UNHANDLED_STATE;
		}
		else if(config.mode().equals(TIARAS)){
			if(utils.inventoryContains(5525)){
				if(player.getWorldArea().intersectsWith(FIRST_POINT)){
					return ENTER_ALTAR;
				} else if (player.getWorldArea().intersectsWith(AIR_ALTAR)){
					return CRAFT_TIARAS;
				}
			}
			else {
				if (player.getWorldArea().intersectsWith(AIR_ALTAR)){
					return USE_PORTAL;
				} else if (player.getWorldLocation().equals(OUTSIDE_ALTAR)){
					return WALK_SECOND_POINT;
				} else if (player.getWorldArea().intersectsWith(SECOND_POINT)){
					return FIND_BANK;
				} else if (player.getWorldArea().intersectsWith(FALADOR_EAST_BANK)){
					return FIND_BANK;
				}
			}
			return UNHANDLED_STATE;
		}
		return UNHANDLED_STATE;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		if(targetMenu!=null){
			menuAction(event,targetMenu.getOption(), targetMenu.getTarget(), targetMenu.getIdentifier(), targetMenu.getMenuAction(),
					targetMenu.getParam0(), targetMenu.getParam1());
			targetMenu = null;
		}
	}

	public void menuAction(MenuOptionClicked menuOptionClicked, String option, String target, int identifier, MenuAction menuAction, int param0, int param1)
	{
		menuOptionClicked.setMenuOption(option);
		menuOptionClicked.setMenuTarget(target);
		menuOptionClicked.setId(identifier);
		menuOptionClicked.setMenuAction(menuAction);
		menuOptionClicked.setActionParam(param0);
		menuOptionClicked.setWidgetId(param1);
	}

	private void useTalismanOnAltar()
	{
		targetObject = utils.findNearestGameObject(34813);
		if(targetObject!=null){
			client.setSelectedItemWidget(WidgetInfo.INVENTORY.getId());
			client.setSelectedItemSlot(utils.getInventoryWidgetItem(1438).getIndex());
			client.setSelectedItemID(1438);
			targetMenu = new MenuEntry("","",targetObject.getId(),1,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			//utils.setMenuEntry(targetMenu);
			if(targetObject.getConvexHull()!=null){
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(),sleepDelay());
			} else {
				utils.delayMouseClick(new Point(0,0),sleepDelay());
			}

		}
	}

	private void useGameObject(int id, int opcode)
	{
		targetObject = utils.findNearestGameObject(id);
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),opcode,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			//utils.setMenuEntry(targetMenu);
			if(targetObject.getConvexHull()!=null){
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(),sleepDelay());
			} else {
				utils.delayMouseClick(new Point(0,0),sleepDelay());
			}
		}
	}

	private void depositItem(int id)
	{
		targetMenu = new MenuEntry("", "", 8, 57, utils.getInventoryWidgetItem(id).getIndex(),983043,false);
		//utils.setMenuEntry(targetMenu);
		utils.delayMouseClick(utils.getInventoryWidgetItem(id).getCanvasBounds(),sleepDelay());
	}

	private void withdrawX(int ID){
		if(client.getVarbitValue(3960)!=14){
			utils.withdrawItemAmount(ID,14);
			timeout+=3;
		} else {
			targetMenu = new MenuEntry("", "", (client.getVarbitValue(6590) == 3) ? 1 : 5, MenuAction.CC_OP.getId(), utils.getBankItemWidget(ID).getIndex(), 786444, false);
			//utils.setMenuEntry(targetMenu);
			clickBounds = utils.getBankItemWidget(ID).getBounds()!=null ? utils.getBankItemWidget(ID).getBounds() : new Rectangle(client.getCenterX() - 50, client.getCenterY() - 50, 100, 100);
			utils.delayMouseClick(clickBounds,sleepDelay());
		}
	}

		private int checkRunEnergy()
		{
			try{
				return client.getEnergy();
			} catch (Exception ignored) {

			}
			return 0;
		}
}
