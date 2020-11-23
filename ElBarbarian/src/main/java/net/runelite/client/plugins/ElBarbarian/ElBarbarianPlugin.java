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
package net.runelite.client.plugins.ElBarbarian;

import com.google.inject.Provides;
import com.owain.chinbreakhandler.ChinBreakHandler;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
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
import static net.runelite.client.plugins.ElBarbarian.ElBarbarianState.*;


@Extension
@PluginDependency(BotUtils.class)
@PluginDescriptor(
	name = "El Barbarian",
	enabledByDefault = false,
	description = "Fishes and cooks in Barbarian Village",
	tags = {"fish, barbarian, fishing, el"},
	type = PluginType.SKILLING
)
@Slf4j
public class ElBarbarianPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ElBarbarianConfiguration config;

	@Inject
	private BotUtils utils;

	@Inject
	private ConfigManager configManager;

	@Inject
	PluginManager pluginManager;

	@Inject
	OverlayManager overlayManager;

	@Inject
	private ElBarbarianOverlay overlay;

	@Inject
	private ChinBreakHandler chinBreakHandler;


	ElBarbarianState state;
	GameObject targetObject;
	NPC targetNPC;
	MenuEntry targetMenu;
	WorldPoint skillLocation;
	Instant botTimer;
	LocalPoint beforeLoc;
	Player player;
	Rectangle altRect = new Rectangle(-100,-100, 10, 10);

	int timeout = 0;
	long sleepLength;
	boolean startBarbarianFisher;
	boolean firstTimeUsingChisel;
	private final Set<Integer> rawFishIds = new HashSet<>();
	private final Set<Integer> cookedFishIds = new HashSet<>();
	private final Set<Integer> requiredIds = new HashSet<>();

	@Provides
	ElBarbarianConfiguration provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ElBarbarianConfiguration.class);
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
		startBarbarianFisher = false;
		requiredIds.clear();
		rawFishIds.clear();
	}

	@Subscribe
	private void onConfigButtonPressed(ConfigButtonClicked configButtonClicked)
	{
		if (!configButtonClicked.getGroup().equalsIgnoreCase("ElBarbarian"))
		{
			return;
		}
		log.info("button {} pressed!", configButtonClicked.getKey());
		if (configButtonClicked.getKey().equals("startButton"))
		{
			if (!startBarbarianFisher)
			{
				startBarbarianFisher = true;
				chinBreakHandler.startPlugin(this);
				state = null;
				targetMenu = null;
				botTimer = Instant.now();
				setLocation();
				overlayManager.add(overlay);
				rawFishIds.addAll(Arrays.asList(331,335));
				cookedFishIds.addAll(Arrays.asList(333,329,343));
				requiredIds.addAll(Arrays.asList(314,309));
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
		if (!event.getGroup().equals("ElBarbarian"))
		{
			return;
		}
		startBarbarianFisher = false;
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

	private void interactFishingSpot()
	{
		targetNPC = utils.findNearestNpcWithin(player.getWorldLocation(), 10, Collections.singleton(1526));
		if (targetNPC != null)
		{
			targetMenu = new MenuEntry("", "", targetNPC.getIndex(), 9, 0, 0, false);
			utils.setMenuEntry(targetMenu);
			utils.delayMouseClick(targetNPC.getConvexHull().getBounds(), sleepDelay());
		}
		else
		{
			log.info("Fishing Spot is null");
		}
	}

	private void interactFire(int fishId)
	{
		targetObject = utils.findNearestGameObjectWithin(player.getWorldLocation(),10,26185);
		if(targetObject!=null){
			targetMenu = new MenuEntry("","",targetObject.getId(),1,targetObject.getSceneMinLocation().getX(),targetObject.getSceneMinLocation().getY(),false);
			utils.setModifiedMenuEntry(targetMenu,fishId,utils.getInventoryWidgetItem(fishId).getIndex(),1);
			if(targetObject.getConvexHull()!=null) {
				utils.delayMouseClick(targetObject.getConvexHull().getBounds(), sleepDelay());
			} else {
				utils.delayMouseClick(new Point(0,0), sleepDelay());
			}
		} else {
			utils.sendGameMessage("Fire is null.");
		}
	}

	private void completeCookingMenu()
	{
		targetMenu = new MenuEntry("","",1,57,-1,17694734,false);
		utils.setMenuEntry(targetMenu);
		utils.delayMouseClick(client.getWidget(270,14).getBounds(), sleepDelay());
	}

	public ElBarbarianState getState()
	{
		if (timeout > 0)
		{
			return TIMEOUT;
		}
		if(utils.iterating){
			return ITERATING;
		}
		if (!utils.inventoryContains(requiredIds))
		{
			return MISSING_ITEMS;
		}
		if (utils.isMoving(beforeLoc))
		{
			timeout = 2 + tickDelay();
			return MOVING;
		}
		if(client.getLocalPlayer().getAnimation()!=-1){
			return ANIMATING;
		}
		if (chinBreakHandler.shouldBreak(this))
		{
			return HANDLE_BREAK;
		}
		if (utils.inventoryFull())
		{
			return getBarbarianFisherState();
		}
		return FIND_NPC;
	}

	@Subscribe
	private void onGameTick(GameTick tick)
	{
		if (!startBarbarianFisher || chinBreakHandler.isBreakActive(this))
		{
			return;
		}
		player = client.getLocalPlayer();
		if (client != null && player != null && skillLocation != null)
		{
			if (!client.isResized())
			{
				utils.sendGameMessage("client must be set to resizable");
				startBarbarianFisher = false;
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
				case FIND_NPC:
					interactFishingSpot();
					timeout = tickDelay();
					break;
				case FIND_GAMEOBJECT:
					if(utils.inventoryContains(335)){
						interactFire(335);
						timeout = tickDelay();
						break;
					} else if(utils.inventoryContains(331)){
						interactFire(331);
						timeout = tickDelay();
						break;
					}
					timeout = tickDelay();
					break;
				case COOKING_MENU:
					completeCookingMenu();
					timeout = tickDelay();
					break;
				case DROPPING_ITEMS:
					utils.dropItems(cookedFishIds, true, config.sleepMin(), config.sleepMax());
					timeout = tickDelay();
					break;
				case MISSING_ITEMS:
					startBarbarianFisher = false;
					utils.sendGameMessage("Missing required items IDs: " + String.valueOf(requiredIds) + " from inventory. Stopping.");
					resetVals();
					break;
				case HANDLE_BREAK:
					chinBreakHandler.startBreak(this);
					timeout = 10;
					break;
				case ANIMATING:
				case MOVING:
					utils.handleRun(30, 20);
					timeout = tickDelay();
					break;
				case ITERATING:
					timeout = tickDelay();
					break;
			}
		}
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && startBarbarianFisher)
		{
			state = TIMEOUT;
			timeout = 2;
		}
	}

	private ElBarbarianState getBarbarianFisherState()
	{
		if(client.getWidget(270,5)!=null && !client.getWidget(270,5).isHidden()){
			return COOKING_MENU;
		}
		if(utils.inventoryContains(rawFishIds)){
			return FIND_GAMEOBJECT;
		}
		if(!utils.inventoryContains(rawFishIds)){
			return DROPPING_ITEMS;
		}
		return TIMEOUT;
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event){
		log.debug(event.toString());
	}
}
